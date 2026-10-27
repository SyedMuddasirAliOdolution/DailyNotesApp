package com.example.data.model

import java.util.UUID

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "user_default",
    val title: String,
    val content: String,
    val aiSummary: String? = null,
    val tags: List<String> = emptyList(),
    val sentiment: String = "NEUTRAL",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val colorHex: String = "#6366F1", // Default indigo accent
    val audioDurationSeconds: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
