package com.maahiassistant.android.data.api

import android.util.Log
import com.maahiassistant.android.data.api.models.GeminiLiveMessages
import com.maahiassistant.android.data.api.models.ServerTurnChunk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class GeminiLiveWebSocketClient(
    private val listener: LiveSessionListener
) {

    interface LiveSessionListener {
        fun onConnected()
        fun onServerChunk(chunk: ServerTurnChunk)
        fun onError(message: String, isAuthError: Boolean)
        fun onClosed(code: Int, reason: String)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var webSocket: WebSocket? = null
    private val isConnected = AtomicBoolean(false)

    private val client = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Keep-alive for streaming
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    fun connect(
        apiKey: String,
        modelId: String,
        voiceName: String,
        resumptionHandle: String? = null
    ) {
        if (apiKey.isBlank()) {
            listener.onError("Missing Gemini API Key", true)
            return
        }

        disconnect()

        val cleanKey = apiKey.trim()
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$cleanKey"

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected.set(true)
                Log.d(TAG, "WebSocket opened. Sending setup payload...")

                // Immediately send initial setup message
                val setupPayload = GeminiLiveMessages.buildSetupMessage(
                    modelId = modelId,
                    voiceName = voiceName,
                    resumptionHandle = resumptionHandle
                )
                webSocket.send(setupPayload)
                scope.launch {
                    listener.onConnected()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val chunk = GeminiLiveMessages.parseServerMessage(text)
                if (chunk != null) {
                    scope.launch {
                        listener.onServerChunk(chunk)
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected.set(false)
                val code = response?.code ?: 0
                val responseBody = try { response?.body?.string().orEmpty() } catch (e: Exception) { "" }
                val isAuthError = code == 400 || code == 403 || responseBody.contains("API_KEY_INVALID")

                val errorDesc = when {
                    isAuthError -> "Invalid Gemini API key or unauthorized"
                    code != 0 -> "Connection failed (HTTP $code: ${response?.message})"
                    else -> t.message ?: "Connection failure"
                }

                Log.e(TAG, "WebSocket failure: $errorDesc", t)
                scope.launch {
                    listener.onError(errorDesc, isAuthError)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected.set(false)
                Log.d(TAG, "WebSocket closed: $code / $reason")
                scope.launch {
                    listener.onClosed(code, reason)
                }
            }
        })
    }

    fun sendAudioChunk(pcmBytes: ByteArray, offset: Int, length: Int): Boolean {
        val ws = webSocket ?: return false
        if (!isConnected.get()) return false
        val message = GeminiLiveMessages.buildRealtimeAudioChunk(pcmBytes, offset, length)
        return ws.send(message)
    }

    fun sendTextMessage(text: String): Boolean {
        val ws = webSocket ?: return false
        if (!isConnected.get()) return false
        val message = GeminiLiveMessages.buildClientContentTextMessage(text)
        return ws.send(message)
    }

    fun isSessionOpen(): Boolean = isConnected.get()

    fun disconnect() {
        isConnected.set(false)
        try {
            webSocket?.close(1000, "Normal closure")
        } catch (e: Exception) {
            // Ignore
        }
        webSocket = null
    }

    companion object {
        private const val TAG = "GeminiLiveWS"
    }
}
