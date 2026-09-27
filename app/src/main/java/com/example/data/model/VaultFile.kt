package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class VaultType {
    PRIMARY,
    DECOY
}

enum class FileCategory {
    PHOTO,
    VIDEO,
    DOCUMENT,
    NOTE
}

@Entity(tableName = "vault_files")
data class VaultFile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val relativePath: String, // Relative path inside internal files directory
    val category: FileCategory,
    val mimeType: String,
    val fileSizeBytes: Long,
    val dateAdded: Long = System.currentTimeMillis(),
    val vaultType: VaultType = VaultType.PRIMARY,
    val noteContent: String? = null,
    val durationSeconds: Long? = null
)
