package com.maahiassistant.android.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel(val label: String) {
    INFO("INFO"),
    SUCCESS("SUCCESS"),
    WARN("WARN"),
    ERROR("ERROR"),
    FRAME("FRAME")
}

data class LiveLogEntry(
    val id: Long = System.nanoTime(),
    val timestamp: String,
    val level: LogLevel,
    val tag: String,
    val message: String
)

object LiveLogManager {
    private const val MAX_LOGS = 600
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    private val _logs = MutableStateFlow<List<LiveLogEntry>>(emptyList())
    val logs: StateFlow<List<LiveLogEntry>> = _logs.asStateFlow()

    @Synchronized
    fun log(level: LogLevel, tag: String, message: String) {
        val entry = LiveLogEntry(
            timestamp = timeFormat.format(Date()),
            level = level,
            tag = tag,
            message = message
        )
        when (level) {
            LogLevel.ERROR -> android.util.Log.e(tag, message)
            LogLevel.WARN -> android.util.Log.w(tag, message)
            else -> android.util.Log.i(tag, message)
        }
        val current = _logs.value
        val updated = if (current.size >= MAX_LOGS) {
            current.drop(current.size - MAX_LOGS + 1) + entry
        } else {
            current + entry
        }
        _logs.value = updated
    }

    fun info(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    fun success(tag: String, message: String) = log(LogLevel.SUCCESS, tag, message)
    fun warn(tag: String, message: String) = log(LogLevel.WARN, tag, message)
    fun error(tag: String, message: String) = log(LogLevel.ERROR, tag, message)
    fun frame(tag: String, message: String) = log(LogLevel.FRAME, tag, message)

    fun clear() {
        _logs.value = emptyList()
    }

    fun getAllLogsAsFormattedText(): String {
        val list = _logs.value
        if (list.isEmpty()) return "No logs recorded yet."
        return list.joinToString("\n") { entry ->
            "[${entry.timestamp}] [${entry.level.label}] [${entry.tag}] ${entry.message}"
        }
    }
}
