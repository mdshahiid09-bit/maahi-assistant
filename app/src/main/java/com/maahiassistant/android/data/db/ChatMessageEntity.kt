package com.maahiassistant.android.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String, // "user", "maahi", "system"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSearching: Boolean = false,
    val isSystemNote: Boolean = false
)
