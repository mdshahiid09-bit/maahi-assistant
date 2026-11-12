package com.maahiassistant.android.data.repository

import com.maahiassistant.android.data.db.ChatMessageDao
import com.maahiassistant.android.data.db.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val dao: ChatMessageDao) {

    val allMessages: Flow<List<ChatMessageEntity>> = dao.getAllMessages()

    suspend fun saveUserMessage(text: String): Long {
        return dao.insertMessage(
            ChatMessageEntity(
                role = "user",
                text = text.trim()
            )
        )
    }

    suspend fun saveMaahiMessage(text: String, isSearching: Boolean = false): Long {
        return dao.insertMessage(
            ChatMessageEntity(
                role = "maahi",
                text = text.trim(),
                isSearching = isSearching
            )
        )
    }

    suspend fun addSystemNote(note: String): Long {
        return dao.insertMessage(
            ChatMessageEntity(
                role = "system",
                text = note.trim(),
                isSystemNote = true
            )
        )
    }

    suspend fun clearHistory() {
        dao.clearAllMessages()
    }
}
