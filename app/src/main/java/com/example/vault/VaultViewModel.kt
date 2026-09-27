package com.example.vault

import android.app.Application
import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.VaultDatabase
import com.example.data.model.FileCategory
import com.example.data.model.VaultFile
import com.example.data.model.VaultType
import com.example.data.repository.VaultRepository
import com.example.data.security.SecurityManager
import com.example.data.storage.VaultFileManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

data class VaultUiState(
    val vaultType: VaultType = VaultType.PRIMARY,
    val selectedCategory: FileCategory = FileCategory.PHOTO,
    val files: List<VaultFile> = emptyList(),
    val totalCount: Int = 0,
    val photoCount: Int = 0,
    val videoCount: Int = 0,
    val docCount: Int = 0,
    val noteCount: Int = 0,
    val selectedFileIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val activeViewerFile: VaultFile? = null,
    val activeEditingNote: VaultFile? = null,
    val isCreatingNote: Boolean = false,
    val showSecuritySettings: Boolean = false,
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val showGalleryDeleteDialog: Boolean = false,
    val pendingGalleryDeleteUris: List<Uri> = emptyList(),
    val pendingDeleteCount: Int = 0
)

sealed interface VaultEvent {
    data class ShowToast(val message: String) : VaultEvent
    data class OpenExternalFile(val uri: Uri, val mimeType: String) : VaultEvent
    data class LaunchGalleryDelete(val intentSender: IntentSender) : VaultEvent
    data object LockVault : VaultEvent
}

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val db = VaultDatabase.getInstance(application)
    private val fileManager = VaultFileManager(application)
    val repository = VaultRepository(application, db.vaultDao(), fileManager)
    val securityManager = SecurityManager.getInstance(application)

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<VaultEvent>()
    val events: SharedFlow<VaultEvent> = _events.asSharedFlow()

    fun initVault(type: VaultType) {
        _uiState.value = _uiState.value.copy(
            vaultType = type,
            selectedCategory = FileCategory.PHOTO,
            selectedFileIds = emptySet(),
            isSelectionMode = false,
            searchQuery = ""
        )
        if (type == VaultType.DECOY) {
            viewModelScope.launch {
                repository.seedDecoyIfEmpty()
            }
        }
        observeFiles()
        observeCounts()
    }

    private fun observeFiles() {
        viewModelScope.launch {
            repository.getFiles(_uiState.value.vaultType, _uiState.value.selectedCategory)
                .collectLatest { list ->
                    val query = _uiState.value.searchQuery.trim().lowercase()
                    val filtered = if (query.isEmpty()) {
                        list
                    } else {
                        list.filter {
                            it.fileName.lowercase().contains(query) ||
                                    (it.noteContent?.lowercase()?.contains(query) == true)
                        }
                    }
                    _uiState.value = _uiState.value.copy(files = filtered)
                }
        }
    }

    private fun observeCounts() {
        val type = _uiState.value.vaultType
        viewModelScope.launch {
            repository.getTotalCount(type).collectLatest { count ->
                _uiState.value = _uiState.value.copy(totalCount = count)
            }
        }
        viewModelScope.launch {
            repository.getCount(type, FileCategory.PHOTO).collectLatest { count ->
                _uiState.value = _uiState.value.copy(photoCount = count)
            }
        }
        viewModelScope.launch {
            repository.getCount(type, FileCategory.VIDEO).collectLatest { count ->
                _uiState.value = _uiState.value.copy(videoCount = count)
            }
        }
        viewModelScope.launch {
            repository.getCount(type, FileCategory.DOCUMENT).collectLatest { count ->
                _uiState.value = _uiState.value.copy(docCount = count)
            }
        }
        viewModelScope.launch {
            repository.getCount(type, FileCategory.NOTE).collectLatest { count ->
                _uiState.value = _uiState.value.copy(noteCount = count)
            }
        }
    }

    fun selectCategory(category: FileCategory) {
        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            selectedFileIds = emptySet(),
            isSelectionMode = false
        )
        observeFiles()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        observeFiles()
    }

    fun toggleFileSelection(id: Long) {
        val current = _uiState.value.selectedFileIds.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _uiState.value = _uiState.value.copy(
            selectedFileIds = current,
            isSelectionMode = current.isNotEmpty()
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedFileIds = emptySet(),
            isSelectionMode = false
        )
    }

    fun importUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.importFiles(uris, _uiState.value.vaultType)
            _uiState.value = _uiState.value.copy(isLoading = false)

            if (result.originalMediaStoreUris.isNotEmpty() && securityManager.isDeleteFromGalleryEnabled()) {
                _uiState.value = _uiState.value.copy(
                    showGalleryDeleteDialog = true,
                    pendingGalleryDeleteUris = result.originalMediaStoreUris,
                    pendingDeleteCount = result.importedCount
                )
            } else {
                _events.emit(VaultEvent.ShowToast("Successfully hid ${result.importedCount} file(s) in vault"))
            }
        }
    }

    fun confirmDeleteFromGallery() {
        val uris = _uiState.value.pendingGalleryDeleteUris
        _uiState.value = _uiState.value.copy(
            showGalleryDeleteDialog = false,
            pendingGalleryDeleteUris = emptyList()
        )
        if (uris.isEmpty()) return

        viewModelScope.launch {
            val intentSender = repository.createGalleryDeleteIntentSender(uris)
            if (intentSender != null) {
                _events.emit(VaultEvent.LaunchGalleryDelete(intentSender))
            } else {
                // Fallback for Android 10 or earlier
                for (u in uris) {
                    val legacySender = fileManager.deleteFromGalleryLegacy(u)
                    if (legacySender != null) {
                        _events.emit(VaultEvent.LaunchGalleryDelete(legacySender))
                        return@launch
                    }
                }
                _events.emit(VaultEvent.ShowToast("Originals removed from gallery! Files are now only in your hidden vault."))
            }
        }
    }

    fun dismissDeleteFromGalleryDialog() {
        _uiState.value = _uiState.value.copy(
            showGalleryDeleteDialog = false,
            pendingGalleryDeleteUris = emptyList()
        )
        viewModelScope.launch {
            _events.emit(VaultEvent.ShowToast("Files secured in vault (originals kept in gallery)"))
        }
    }

    fun onGalleryDeleteSuccess() {
        viewModelScope.launch {
            _events.emit(VaultEvent.ShowToast("Originals deleted from main gallery! Files are now ONLY in your hidden vault."))
        }
    }

    fun openFile(file: VaultFile) {
        when (file.category) {
            FileCategory.PHOTO, FileCategory.VIDEO -> {
                _uiState.value = _uiState.value.copy(activeViewerFile = file)
            }
            FileCategory.NOTE -> {
                _uiState.value = _uiState.value.copy(activeEditingNote = file, isCreatingNote = false)
            }
            FileCategory.DOCUMENT -> {
                // If it's a plain text file, we can also preview it or open with intent
                if (file.mimeType.startsWith("text/")) {
                    _uiState.value = _uiState.value.copy(activeViewerFile = file)
                } else {
                    val physicalFile = repository.getPhysicalFile(file.relativePath)
                    if (physicalFile.exists()) {
                        val uri = repository.getShareUri(physicalFile)
                        viewModelScope.launch {
                            _events.emit(VaultEvent.OpenExternalFile(uri, file.mimeType))
                        }
                    } else {
                        viewModelScope.launch {
                            _events.emit(VaultEvent.ShowToast("File not found on disk"))
                        }
                    }
                }
            }
        }
    }

    fun closeViewer() {
        _uiState.value = _uiState.value.copy(activeViewerFile = null)
    }

    fun startCreateNote() {
        _uiState.value = _uiState.value.copy(isCreatingNote = true, activeEditingNote = null)
    }

    fun closeNoteDialog() {
        _uiState.value = _uiState.value.copy(isCreatingNote = false, activeEditingNote = null)
    }

    fun saveNote(title: String, content: String, noteId: Long? = null) {
        viewModelScope.launch {
            repository.saveNote(title, content, _uiState.value.vaultType, noteId)
            closeNoteDialog()
            _events.emit(VaultEvent.ShowToast("Note saved"))
        }
    }

    fun deleteFile(file: VaultFile) {
        viewModelScope.launch {
            repository.deleteFile(file)
            if (_uiState.value.activeViewerFile?.id == file.id) {
                closeViewer()
            }
            _events.emit(VaultEvent.ShowToast("File deleted from vault"))
        }
    }

    fun deleteSelectedFiles() {
        val selectedIds = _uiState.value.selectedFileIds
        if (selectedIds.isEmpty()) return
        viewModelScope.launch {
            val toDelete = _uiState.value.files.filter { selectedIds.contains(it.id) }
            repository.deleteFiles(toDelete)
            clearSelection()
            _events.emit(VaultEvent.ShowToast("${toDelete.size} file(s) deleted"))
        }
    }

    fun unhideFile(file: VaultFile) {
        viewModelScope.launch {
            val success = repository.exportFile(file)
            if (success) {
                repository.deleteFile(file)
                if (_uiState.value.activeViewerFile?.id == file.id) {
                    closeViewer()
                }
                _events.emit(VaultEvent.ShowToast("File unhidden & restored to device"))
            } else {
                _events.emit(VaultEvent.ShowToast("Failed to restore file"))
            }
        }
    }

    fun unhideSelectedFiles() {
        val selectedIds = _uiState.value.selectedFileIds
        if (selectedIds.isEmpty()) return
        viewModelScope.launch {
            val toRestore = _uiState.value.files.filter { selectedIds.contains(it.id) }
            var successCount = 0
            for (f in toRestore) {
                if (repository.exportFile(f)) {
                    repository.deleteFile(f)
                    successCount++
                }
            }
            clearSelection()
            _events.emit(VaultEvent.ShowToast("$successCount file(s) unhidden & restored"))
        }
    }

    fun getPhysicalFile(relativePath: String): File {
        return repository.getPhysicalFile(relativePath)
    }

    fun toggleSecuritySettings(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSecuritySettings = show)
    }

    fun lockVault() {
        viewModelScope.launch {
            _events.emit(VaultEvent.LockVault)
        }
    }
}
