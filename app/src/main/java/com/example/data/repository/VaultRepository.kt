package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.VaultDao
import com.example.data.model.FileCategory
import com.example.data.model.VaultFile
import com.example.data.model.VaultType
import com.example.data.storage.VaultFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class ImportResult(
    val importedCount: Int,
    val originalMediaStoreUris: List<Uri>
)

class VaultRepository(
    private val context: Context,
    private val dao: VaultDao,
    private val fileManager: VaultFileManager
) {
    fun getFiles(vaultType: VaultType, category: FileCategory): Flow<List<VaultFile>> {
        return dao.getFilesByCategory(vaultType, category)
    }

    fun getAllFiles(vaultType: VaultType): Flow<List<VaultFile>> {
        return dao.getAllFiles(vaultType)
    }

    fun getCount(vaultType: VaultType, category: FileCategory): Flow<Int> {
        return dao.countByCategory(vaultType, category)
    }

    fun getTotalCount(vaultType: VaultType): Flow<Int> {
        return dao.countAll(vaultType)
    }

    fun createGalleryDeleteIntentSender(uris: List<Uri>) = fileManager.createGalleryDeleteIntentSender(uris)

    suspend fun importFiles(uris: List<Uri>, vaultType: VaultType): ImportResult = withContext(Dispatchers.IO) {
        var importedCount = 0
        val mediaStoreUris = mutableListOf<Uri>()

        for (uri in uris) {
            val imported = fileManager.importFileFromUri(uri, vaultType)
            if (imported != null) {
                dao.insertFile(
                    VaultFile(
                        fileName = imported.fileName,
                        relativePath = imported.relativePath,
                        category = imported.category,
                        mimeType = imported.mimeType,
                        fileSizeBytes = imported.sizeBytes,
                        vaultType = vaultType
                    )
                )
                importedCount++
                if (imported.originalMediaStoreUri != null) {
                    mediaStoreUris.add(imported.originalMediaStoreUri)
                }
            }
        }
        ImportResult(importedCount, mediaStoreUris)
    }

    suspend fun saveNote(title: String, content: String, vaultType: VaultType, existingId: Long? = null) = withContext(Dispatchers.IO) {
        val safeTitle = if (title.isBlank()) "Untitled Note" else title.trim()
        if (existingId != null && existingId > 0) {
            val existing = dao.getFileById(existingId).firstOrNull()
            if (existing != null) {
                dao.updateFile(
                    existing.copy(
                        fileName = safeTitle,
                        noteContent = content,
                        fileSizeBytes = content.toByteArray().size.toLong()
                    )
                )
                return@withContext
            }
        }

        dao.insertFile(
            VaultFile(
                fileName = safeTitle,
                relativePath = "",
                category = FileCategory.NOTE,
                mimeType = "text/plain",
                fileSizeBytes = content.toByteArray().size.toLong(),
                vaultType = vaultType,
                noteContent = content
            )
        )
    }

    suspend fun deleteFile(file: VaultFile) = withContext(Dispatchers.IO) {
        if (file.category != FileCategory.NOTE && file.relativePath.isNotEmpty()) {
            fileManager.deletePhysicalFile(file.relativePath)
        }
        dao.deleteFile(file)
    }

    suspend fun deleteFiles(files: List<VaultFile>) = withContext(Dispatchers.IO) {
        for (f in files) {
            deleteFile(f)
        }
    }

    suspend fun exportFile(file: VaultFile): Boolean = withContext(Dispatchers.IO) {
        if (file.category == FileCategory.NOTE) {
            // Can export note as text file to Downloads
            return@withContext exportTextNote(file)
        }
        fileManager.exportToPublicStorage(file.relativePath, file.fileName, file.mimeType, file.category)
    }

    private fun exportTextNote(note: VaultFile): Boolean {
        return try {
            val fileName = "${note.fileName.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.txt"
            val tempFile = File(context.cacheDir, fileName)
            FileOutputStream(tempFile).use { it.write((note.noteContent ?: "").toByteArray()) }
            val relativePath = "cache/$fileName"
            fileManager.exportToPublicStorage(tempFile.absolutePath, fileName, "text/plain", FileCategory.DOCUMENT)
        } catch (_: Exception) {
            false
        }
    }

    fun getPhysicalFile(relativePath: String): File {
        return fileManager.getFileFromRelativePath(relativePath)
    }

    fun getShareUri(file: File): Uri {
        return fileManager.getShareableUri(file)
    }

    suspend fun seedDecoyIfEmpty() = withContext(Dispatchers.IO) {
        val count = dao.countAll(VaultType.DECOY).firstOrNull() ?: 0
        if (count == 0) {
            // Add a few realistic decoy notes & document placeholders to look convincing
            dao.insertFile(
                VaultFile(
                    fileName = "Grocery List & Weekend Errands",
                    relativePath = "",
                    category = FileCategory.NOTE,
                    mimeType = "text/plain",
                    fileSizeBytes = 120,
                    vaultType = VaultType.DECOY,
                    noteContent = "- Organic milk & Greek yogurt\n- Whole wheat bread\n- Olive oil & sea salt\n- Car wash and tire check on Saturday afternoon"
                )
            )
            dao.insertFile(
                VaultFile(
                    fileName = "Home Wifi & Router Credentials",
                    relativePath = "",
                    category = FileCategory.NOTE,
                    mimeType = "text/plain",
                    fileSizeBytes = 94,
                    vaultType = VaultType.DECOY,
                    noteContent = "SSID: NetGear_Guest_5G\nPassword: CoffeeMorning$2024\nIP: 192.168.1.1\nAdmin: admin / guestwifi"
                )
            )
            dao.insertFile(
                VaultFile(
                    fileName = "Apartment Lease Checklist",
                    relativePath = "",
                    category = FileCategory.NOTE,
                    mimeType = "text/plain",
                    fileSizeBytes = 150,
                    vaultType = VaultType.DECOY,
                    noteContent = "1. Keys handed over on 1st of month\n2. Security deposit receipt filed\n3. Utility account transferred to electric company"
                )
            )
        }
    }
}
