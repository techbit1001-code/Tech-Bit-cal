package com.example.vault

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.FileCategory
import com.example.data.model.VaultFile
import com.example.data.model.VaultType
import com.example.vault.components.DocumentViewerDialog
import com.example.vault.components.NoteEditDialog
import com.example.vault.components.PhotoViewerDialog
import com.example.vault.components.SecuritySettingsDialog
import com.example.vault.components.VideoPlayerDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    uiState: VaultUiState,
    onLockToCalculator: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSearch by remember { mutableStateOf(false) }

    // Intercept hardware/system back button to lock and return to calculator
    BackHandler {
        if (uiState.isSelectionMode) {
            viewModel.clearSelection()
        } else if (showSearch) {
            showSearch = false
            viewModel.onSearchQueryChange("")
        } else {
            onLockToCalculator()
        }
    }

    // Modern Zero-Permission Photo/Video Pickers
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importUris(uris)
        }
    }

    // Document Picker
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importUris(uris)
        }
    }

    // System MediaStore Delete Launcher (for removing originals from device gallery)
    val galleryDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onGalleryDeleteSuccess()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is VaultEvent.LaunchGalleryDelete) {
                val request = IntentSenderRequest.Builder(event.intentSender).build()
                galleryDeleteLauncher.launch(request)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (uiState.isSelectionMode) {
                // Selection Action Bar
                TopAppBar(
                    title = {
                        Text(
                            text = "${uiState.selectedFileIds.size} Selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear selection")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.unhideSelectedFiles() },
                            modifier = Modifier.testTag("batch_unhide_button")
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = "Unhide selected")
                        }
                        IconButton(
                            onClick = { viewModel.deleteSelectedFiles() },
                            modifier = Modifier.testTag("batch_delete_button")
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete selected",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            } else {
                // Normal Vault Top Bar
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (uiState.vaultType == VaultType.PRIMARY) Icons.Default.Shield else Icons.Default.FolderSpecial,
                                    contentDescription = null,
                                    tint = if (uiState.vaultType == VaultType.PRIMARY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.vaultType == VaultType.PRIMARY) "Private Vault" else "Personal Space",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (uiState.vaultType == VaultType.PRIMARY) "Encrypted Storage" else "Safe Partition",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showSearch = !showSearch },
                            modifier = Modifier.testTag("vault_search_button")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search files")
                        }

                        IconButton(
                            onClick = { viewModel.toggleSecuritySettings(true) },
                            modifier = Modifier.testTag("vault_settings_button")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Security Settings")
                        }

                        // Panic button: instant lock to calculator
                        IconButton(
                            onClick = onLockToCalculator,
                            modifier = Modifier.testTag("panic_lock_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = "Quick Camouflage Lock",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (uiState.selectedCategory) {
                        FileCategory.PHOTO -> {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        FileCategory.VIDEO -> {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        }
                        FileCategory.DOCUMENT -> {
                            docPickerLauncher.launch(arrayOf("*/*"))
                        }
                        FileCategory.NOTE -> {
                            viewModel.startCreateNote()
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("vault_fab_add")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Item"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input
            AnimatedVisibility(visible = showSearch) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = { Text("Search hidden files or notes...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("vault_search_input")
                )
            }

            // Category Tab Row
            val tabs = listOf(
                Triple(FileCategory.PHOTO, "Photos", uiState.photoCount),
                Triple(FileCategory.VIDEO, "Videos", uiState.videoCount),
                Triple(FileCategory.DOCUMENT, "Docs", uiState.docCount),
                Triple(FileCategory.NOTE, "Notes", uiState.noteCount)
            )

            ScrollableTabRow(
                selectedTabIndex = tabs.indexOfFirst { it.first == uiState.selectedCategory }.coerceAtLeast(0),
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEach { (cat, label, count) ->
                    val selected = uiState.selectedCategory == cat
                    Tab(
                        selected = selected,
                        onClick = { viewModel.selectCategory(cat) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (count > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                shape = CircleShape
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = count.toString(),
                                            fontSize = 11.sp,
                                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        },
                        icon = {
                            val icon = when (cat) {
                                FileCategory.PHOTO -> Icons.Default.Image
                                FileCategory.VIDEO -> Icons.Default.Movie
                                FileCategory.DOCUMENT -> Icons.Default.Description
                                FileCategory.NOTE -> Icons.Default.EditNote
                            }
                            Icon(icon, contentDescription = label)
                        }
                    )
                }
            }

            // Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.files.isEmpty()) {
                    EmptyVaultPlaceholder(category = uiState.selectedCategory, vaultType = uiState.vaultType)
                } else {
                    when (uiState.selectedCategory) {
                        FileCategory.PHOTO -> {
                            PhotoGrid(
                                files = uiState.files,
                                selectedIds = uiState.selectedFileIds,
                                isSelectionMode = uiState.isSelectionMode,
                                getPhysicalFile = { viewModel.getPhysicalFile(it) },
                                onItemClick = { file ->
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleFileSelection(file.id)
                                    } else {
                                        viewModel.openFile(file)
                                    }
                                },
                                onItemLongClick = { file ->
                                    viewModel.toggleFileSelection(file.id)
                                }
                            )
                        }
                        FileCategory.VIDEO -> {
                            VideoGrid(
                                files = uiState.files,
                                selectedIds = uiState.selectedFileIds,
                                isSelectionMode = uiState.isSelectionMode,
                                getPhysicalFile = { viewModel.getPhysicalFile(it) },
                                onItemClick = { file ->
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleFileSelection(file.id)
                                    } else {
                                        viewModel.openFile(file)
                                    }
                                },
                                onItemLongClick = { file ->
                                    viewModel.toggleFileSelection(file.id)
                                }
                            )
                        }
                        FileCategory.DOCUMENT -> {
                            DocumentList(
                                files = uiState.files,
                                selectedIds = uiState.selectedFileIds,
                                isSelectionMode = uiState.isSelectionMode,
                                onItemClick = { file ->
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleFileSelection(file.id)
                                    } else {
                                        viewModel.openFile(file)
                                    }
                                },
                                onItemLongClick = { file ->
                                    viewModel.toggleFileSelection(file.id)
                                }
                            )
                        }
                        FileCategory.NOTE -> {
                            NoteList(
                                notes = uiState.files,
                                selectedIds = uiState.selectedFileIds,
                                isSelectionMode = uiState.isSelectionMode,
                                onItemClick = { note ->
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleFileSelection(note.id)
                                    } else {
                                        viewModel.openFile(note)
                                    }
                                },
                                onItemLongClick = { note ->
                                    viewModel.toggleFileSelection(note.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Active File Viewers and Editors
    uiState.activeViewerFile?.let { file ->
        val physicalFile = viewModel.getPhysicalFile(file.relativePath)
        when (file.category) {
            FileCategory.PHOTO -> {
                PhotoViewerDialog(
                    file = file,
                    physicalFile = physicalFile,
                    onClose = { viewModel.closeViewer() },
                    onUnhide = { viewModel.unhideFile(file) },
                    onDelete = { viewModel.deleteFile(file) },
                    getShareIntent = {
                        val shareUri = viewModel.repository.getShareUri(physicalFile)
                        Intent(Intent.ACTION_SEND).apply {
                            type = file.mimeType
                            putExtra(Intent.EXTRA_STREAM, shareUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    }
                )
            }
            FileCategory.VIDEO -> {
                VideoPlayerDialog(
                    file = file,
                    physicalFile = physicalFile,
                    onClose = { viewModel.closeViewer() },
                    onUnhide = { viewModel.unhideFile(file) },
                    onDelete = { viewModel.deleteFile(file) },
                    getShareIntent = {
                        val shareUri = viewModel.repository.getShareUri(physicalFile)
                        Intent(Intent.ACTION_SEND).apply {
                            type = file.mimeType
                            putExtra(Intent.EXTRA_STREAM, shareUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    }
                )
            }
            FileCategory.DOCUMENT -> {
                DocumentViewerDialog(
                    file = file,
                    physicalFile = physicalFile,
                    onClose = { viewModel.closeViewer() },
                    onUnhide = { viewModel.unhideFile(file) },
                    onDelete = { viewModel.deleteFile(file) },
                    getShareIntent = {
                        val shareUri = viewModel.repository.getShareUri(physicalFile)
                        Intent(Intent.ACTION_SEND).apply {
                            type = file.mimeType
                            putExtra(Intent.EXTRA_STREAM, shareUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    },
                    getOpenIntent = {
                        val shareUri = viewModel.repository.getShareUri(physicalFile)
                        Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(shareUri, file.mimeType)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    }
                )
            }
            else -> {}
        }
    }

    // Note Editor / Creator Dialog
    if (uiState.isCreatingNote || uiState.activeEditingNote != null) {
        NoteEditDialog(
            note = uiState.activeEditingNote,
            onDismiss = { viewModel.closeNoteDialog() },
            onSave = { title, content, noteId ->
                viewModel.saveNote(title, content, noteId)
            },
            onDelete = { note ->
                viewModel.deleteFile(note)
            }
        )
    }

    // Security Settings Dialog
    if (uiState.showSecuritySettings) {
        SecuritySettingsDialog(
            securityManager = viewModel.securityManager,
            currentVaultType = uiState.vaultType,
            onDismiss = { viewModel.toggleSecuritySettings(false) },
            onToast = { /* Shown inside */ }
        )
    }

    // Confirmation dialog to delete original photos/videos from device's main gallery
    if (uiState.showGalleryDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteFromGalleryDialog() },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Hide from Device Gallery?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${uiState.pendingDeleteCount} item(s) successfully secured in hidden vault.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "To make sure they are completely hidden and ONLY shown in your secret vault, remove the originals from your phone's main gallery.\n\nTap 'Delete from Gallery' to approve removal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeleteFromGallery() },
                    modifier = Modifier.testTag("confirm_delete_from_gallery_button")
                ) {
                    Text("Delete from Gallery")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissDeleteFromGalleryDialog() },
                    modifier = Modifier.testTag("keep_in_gallery_button")
                ) {
                    Text("Keep in Gallery")
                }
            }
        )
    }
}

@Composable
fun EmptyVaultPlaceholder(category: FileCategory, vaultType: VaultType) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val (icon, title, desc) = when (category) {
                FileCategory.PHOTO -> Triple(
                    Icons.Default.Image,
                    "No Hidden Photos",
                    "Tap the '+' button below to hide personal pictures from your device gallery."
                )
                FileCategory.VIDEO -> Triple(
                    Icons.Default.Movie,
                    "No Hidden Videos",
                    "Tap the '+' button below to secure videos inside this vault."
                )
                FileCategory.DOCUMENT -> Triple(
                    Icons.Default.Description,
                    "No Hidden Documents",
                    "Tap the '+' button below to hide confidential PDFs, spreadsheets, and files."
                )
                FileCategory.NOTE -> Triple(
                    Icons.Default.EditNote,
                    "No Secret Notes",
                    "Tap the '+' button below to write encrypted notes and save passwords."
                )
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoGrid(
    files: List<VaultFile>,
    selectedIds: Set<Long>,
    isSelectionMode: Boolean,
    getPhysicalFile: (String) -> File,
    onItemClick: (VaultFile) -> Unit,
    onItemLongClick: (VaultFile) -> Unit
) {
    val context = LocalContext.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(files, key = { it.id }) { file ->
            val isSelected = selectedIds.contains(file.id)
            val physicalFile = remember(file.relativePath) { getPhysicalFile(file.relativePath) }

            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .combinedClickable(
                        onClick = { onItemClick(file) },
                        onLongClick = { onItemLongClick(file) }
                    )
                    .testTag("photo_item_${file.id}")
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(physicalFile)
                        .crossfade(true)
                        .build(),
                    contentDescription = file.fileName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (isSelected) Color.Black.copy(alpha = 0.4f) else Color.Transparent)
                            .padding(6.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoGrid(
    files: List<VaultFile>,
    selectedIds: Set<Long>,
    isSelectionMode: Boolean,
    getPhysicalFile: (String) -> File,
    onItemClick: (VaultFile) -> Unit,
    onItemLongClick: (VaultFile) -> Unit
) {
    val context = LocalContext.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(files, key = { it.id }) { file ->
            val isSelected = selectedIds.contains(file.id)
            val physicalFile = remember(file.relativePath) { getPhysicalFile(file.relativePath) }

            val formattedSize = remember(file.fileSizeBytes) {
                val mb = file.fileSizeBytes / (1024.0 * 1024.0)
                String.format(Locale.US, "%.1f MB", mb)
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onItemClick(file) },
                        onLongClick = { onItemLongClick(file) }
                    )
                    .testTag("video_item_${file.id}"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.3f)
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(physicalFile)
                                .crossfade(true)
                                .build(),
                            contentDescription = file.fileName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Play",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(44.dp)
                        )

                        if (isSelectionMode) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                contentAlignment = Alignment.TopEnd
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = file.fileName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formattedSize,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentList(
    files: List<VaultFile>,
    selectedIds: Set<Long>,
    isSelectionMode: Boolean,
    onItemClick: (VaultFile) -> Unit,
    onItemLongClick: (VaultFile) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(files, key = { it.id }) { file ->
            val isSelected = selectedIds.contains(file.id)

            val formattedSize = remember(file.fileSizeBytes) {
                val kb = file.fileSizeBytes / 1024.0
                val mb = kb / 1024.0
                if (mb >= 1.0) String.format(Locale.US, "%.1f MB", mb) else String.format(Locale.US, "%.0f KB", kb)
            }

            val formattedDate = remember(file.dateAdded) {
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(file.dateAdded))
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onItemClick(file) },
                        onLongClick = { onItemLongClick(file) }
                    )
                    .testTag("doc_item_${file.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.fileName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$formattedSize • $formattedDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isSelectionMode) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteList(
    notes: List<VaultFile>,
    selectedIds: Set<Long>,
    isSelectionMode: Boolean,
    onItemClick: (VaultFile) -> Unit,
    onItemLongClick: (VaultFile) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(notes, key = { it.id }) { note ->
            val isSelected = selectedIds.contains(note.id)
            val formattedDate = remember(note.dateAdded) {
                SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(note.dateAdded))
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onItemClick(note) },
                        onLongClick = { onItemLongClick(note) }
                    )
                    .testTag("note_item_${note.id}")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = note.fileName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelectionMode) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = note.noteContent ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
