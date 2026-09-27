package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.FileCategory
import com.example.data.model.VaultType

class VaultConverters {
    @TypeConverter
    fun fromVaultType(type: VaultType): String = type.name

    @TypeConverter
    fun toVaultType(value: String): VaultType = try {
        VaultType.valueOf(value)
    } catch (_: Exception) {
        VaultType.PRIMARY
    }

    @TypeConverter
    fun fromFileCategory(category: FileCategory): String = category.name

    @TypeConverter
    fun toFileCategory(value: String): FileCategory = try {
        FileCategory.valueOf(value)
    } catch (_: Exception) {
        FileCategory.DOCUMENT
    }
}
