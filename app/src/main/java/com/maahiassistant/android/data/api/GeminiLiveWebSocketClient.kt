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
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class GeminiLiveWebSocketClient(
    private val listener: LiveSessionListener
) {

    companion object {
        private const val TAG = "MaahiLive"
    }

    interface LiveSessionListener {
        fun onConnected()
        fun onSetupComplete()
        fun onServerChunk(chunk: ServerTurnChunk)
        fun onError(message: String, isAuthError: Boolean)
        fun onClosed(code: Int, reason: String)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var webSocket: WebSocket? = null
    private val isConnected = AtomicBoolean(false)
    private val isSetupComplete = AtomicBoolean(false)

    private val client = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    fun connect(
        apiKey: String,
        modelId: String,
        voiceName: String,
        resumptionHandle: String? = null
    ) {
        if (apiKey.isBlank()) {
            listener.onError("Missing Gemini API Key. Add it in Settings.", true)
            return
        }

        disconnect()

        val cleanKey = apiKey.trim()
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$cleanKey"

        val request = Request.Builder()
            .url(url)
            .build()

        Log.i(TAG, "Opening WebSocket connection to Gemini Live API...")

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected.set(true)
                isSetupComplete.set(false)
                Log.i(TAG, "WebSocket connected successfully. Sending initial setup message...")

                // Send setup message first
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
                    if (chunk.isSetupComplete) {
                        isSetupComplete.set(true)
                        scope.launch {
                            listener.onSetupComplete()
                        }
                    }

                    if (chunk.serverError != null) {
                        scope.launch {
                            listener.onError(chunk.serverError, false)
                        }
                    }

                    scope.launch {
                        listener.onServerChunk(chunk)
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected.set(false)
                isSetupComplete.set(false)
                val code = response?.code ?: 0
                val rawBody = try { response?.body?.string().orEmpty() } catch (e: Exception) { "" }

                var detailedError: String? = null
                if (rawBody.isNotEmpty()) {
                    try {
                        val json = JSONObject(rawBody)
                        val errObj = json.optJSONObject("error")
                        detailedError = errObj?.optString("message") ?: json.optString("message")
                    } catch (e: Exception) {
                        detailedError = rawBody
                    }
                }

                val isAuthError = code == 400 || code == 403 || (detailedError != null && detailedError.contains("API_KEY_INVALID", ignoreCase = true))

                val finalError = when {
                    detailedError != null -> "Gemini API error ($code): $detailedError"
                    isAuthError -> "Invalid Gemini API key or unauthorized (HTTP $code)"
                    code != 0 -> "Connection failed (HTTP $code: ${response?.message})"
                    else -> t.message ?: "Connection failure"
                }

                Log.e(TAG, "WebSocket failure: $finalError", t)
                scope.launch {
                    listener.onError(finalError, isAuthError)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected.set(false)
                isSetupComplete.set(false)
                Log.i(TAG, "WebSocket closed: code=$code, reason=$reason")
                scope.launch {
                    listener.onClosed(code, reason)
                }
            }
        })
    }

    fun isReadyForAudio(): Boolean = isConnected.get() && isSetupComplete.get()

    fun sendAudioChunk(pcmBytes: ByteArray, offset: Int, length: Int): Boolean {
        val ws = webSocket ?: return false
        if (!isConnected.get() || !isSetupComplete.get()) {
            return false
        }
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
        isSetupComplete.set(false)
        try {
            webSocket?.close(1000, "Normal closure")
        } catch (e: Exception) {
            // Ignore
        }
        webSocket = null
    }
}
