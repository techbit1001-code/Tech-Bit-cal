package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FileCategory
import com.example.data.model.VaultFile
import com.example.data.model.VaultType
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_files WHERE vaultType = :vaultType AND category = :category ORDER BY dateAdded DESC")
    fun getFilesByCategory(vaultType: VaultType, category: FileCategory): Flow<List<VaultFile>>

    @Query("SELECT * FROM vault_files WHERE vaultType = :vaultType ORDER BY dateAdded DESC")
    fun getAllFiles(vaultType: VaultType): Flow<List<VaultFile>>

    @Query("SELECT * FROM vault_files WHERE id = :id LIMIT 1")
    fun getFileById(id: Long): Flow<VaultFile?>

    @Query("SELECT COUNT(*) FROM vault_files WHERE vaultType = :vaultType AND category = :category")
    fun countByCategory(vaultType: VaultType, category: FileCategory): Flow<Int>

    @Query("SELECT COUNT(*) FROM vault_files WHERE vaultType = :vaultType")
    fun countAll(vaultType: VaultType): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: VaultFile): Long

    @Update
    suspend fun updateFile(file: VaultFile)

    @Delete
    suspend fun deleteFile(file: VaultFile)

    @Query("DELETE FROM vault_files WHERE id = :id")
    suspend fun deleteFileById(id: Long)

    @Query("DELETE FROM vault_files WHERE id IN (:ids)")
    suspend fun deleteFilesByIds(ids: List<Long>)
}
