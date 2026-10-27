package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "encrypted_notes")
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val encryptedTitle: String,
    val titleIv: String,
    val encryptedContent: String,
    val contentIv: String,
    val aiSummary: String?,
    val tagsJson: String,
    val sentiment: String,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val colorHex: String,
    val audioDurationSeconds: Int,
    val createdAt: Long,
    val updatedAt: Long
)
