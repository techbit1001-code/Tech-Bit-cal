package com.example.data.storage

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.data.model.FileCategory
import com.example.data.model.VaultType
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class ImportedFileInfo(
    val fileName: String,
    val relativePath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val category: FileCategory,
    val originalMediaStoreUri: Uri? = null
)

class VaultFileManager(private val context: Context) {

    fun getFileFromRelativePath(relativePath: String): File {
        return File(context.filesDir, relativePath)
    }

    fun getShareableUri(file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    fun importFileFromUri(uri: Uri, vaultType: VaultType): ImportedFileInfo? {
        return try {
            val contentResolver = context.contentResolver
            var fileName = "file_${System.currentTimeMillis()}"
            var size: Long = 0

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }

            val mimeType = contentResolver.getType(uri) ?: getMimeTypeFromFileName(fileName)
            val category = determineCategory(mimeType, fileName)

            val vaultFolder = if (vaultType == VaultType.PRIMARY) "primary" else "decoy"
            val targetDir = File(context.filesDir, "vault_files/$vaultFolder")
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            val extension = fileName.substringAfterLast('.', "")
            val safeExtension = if (extension.isNotEmpty()) ".$extension" else ""
            val uniqueFileName = "${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}$safeExtension"
            val targetFile = File(targetDir, uniqueFileName)

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            val finalSize = if (size > 0) size else targetFile.length()
            val relativePath = "vault_files/$vaultFolder/$uniqueFileName"

            val originalMediaStoreUri = resolveOriginalMediaStoreUri(
                uri = uri,
                fileName = fileName,
                sizeBytes = finalSize,
                isVideo = (category == FileCategory.VIDEO)
            )

            ImportedFileInfo(
                fileName = fileName,
                relativePath = relativePath,
                mimeType = mimeType,
                sizeBytes = finalSize,
                category = category,
                originalMediaStoreUri = originalMediaStoreUri
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun resolveOriginalMediaStoreUri(uri: Uri, fileName: String, sizeBytes: Long, isVideo: Boolean): Uri? {
        val contentResolver = context.contentResolver

        // 1. Direct MediaStore URI
        if (uri.authority == "media" && uri.path?.contains("external") == true) {
            return uri
        }

        val baseUri = if (isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        // 2. Photo picker uri often ends with media id (e.g. content://media/picker/.../100000001)
        val lastSegment = uri.lastPathSegment
        val id = lastSegment?.toLongOrNull()
        if (id != null) {
            val candidateUri = ContentUris.withAppendedId(baseUri, id)
            try {
                contentResolver.query(candidateUri, arrayOf(MediaStore.MediaColumns._ID), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        return candidateUri
                    }
                }
            } catch (_: Exception) {}
        }

        // 3. Match by filename and size in MediaStore
        try {
            val projection = arrayOf(MediaStore.MediaColumns._ID)
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.SIZE} = ?"
            val selectionArgs = arrayOf(fileName, sizeBytes.toString())
            contentResolver.query(baseUri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val foundId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    return ContentUris.withAppendedId(baseUri, foundId)
                }
            }
        } catch (_: Exception) {}

        // 4. Return candidateUri if id was present, or fallback to original URI
        return if (id != null) ContentUris.withAppendedId(baseUri, id) else uri
    }

    fun createGalleryDeleteIntentSender(uris: List<Uri>): IntentSender? {
        if (uris.isEmpty()) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }

    fun deleteFromGalleryLegacy(uri: Uri): IntentSender? {
        return try {
            context.contentResolver.delete(uri, null, null)
            null
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                e.userAction.actionIntent.intentSender
            } else {
                null
            }
        }
    }

    fun deletePhysicalFile(relativePath: String): Boolean {
        return try {
            val file = getFileFromRelativePath(relativePath)
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun exportToPublicStorage(relativePath: String, originalFileName: String, mimeType: String, category: FileCategory): Boolean {
        val sourceFile = getFileFromRelativePath(relativePath)
        if (!sourceFile.exists()) return false

        return try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, originalFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            }

            val collectionUri: Uri = when (category) {
                FileCategory.PHOTO -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/RestoredVault")
                        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    } else {
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }
                }
                FileCategory.VIDEO -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/RestoredVault")
                        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    } else {
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    }
                }
                else -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/RestoredVault")
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI
                    } else {
                        MediaStore.Files.getContentUri("external")
                    }
                }
            }

            val destUri = resolver.insert(collectionUri, contentValues) ?: return false
            resolver.openOutputStream(destUri)?.use { out ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun determineCategory(mimeType: String, fileName: String): FileCategory {
        val lowerMime = mimeType.lowercase()
        val lowerName = fileName.lowercase()
        return when {
            lowerMime.startsWith("image/") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                    lowerName.endsWith(".png") || lowerName.endsWith(".gif") || lowerName.endsWith(".webp") ||
                    lowerName.endsWith(".bmp") || lowerName.endsWith(".heic") -> FileCategory.PHOTO

            lowerMime.startsWith("video/") || lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") ||
                    lowerName.endsWith(".mov") || lowerName.endsWith(".webm") || lowerName.endsWith(".avi") -> FileCategory.VIDEO

            else -> FileCategory.DOCUMENT
        }
    }

    private fun getMimeTypeFromFileName(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "zip" -> "application/zip"
            else -> "application/octet-stream"
        }
    }
}
