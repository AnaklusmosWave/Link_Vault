package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "links")
data class LinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long, // References FolderEntity.id; 0 or default can represent "General"
    val title: String,
    val url: String,
    val note: String = "",
    val tags: String = "", // Comma-separated list of tags, e.g., "social,tech"
    val createdAt: Long = System.currentTimeMillis()
)
