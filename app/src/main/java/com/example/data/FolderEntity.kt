package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isLocked: Boolean = false,
    val lockType: String = "PIN", // "PIN" or "PATTERN"
    val lockValue: String = "",   // Numeric PIN (e.g., "1234") or 0-indexed dot path (e.g., "0,1,2,5,8")
    val createdAt: Long = System.currentTimeMillis()
)
