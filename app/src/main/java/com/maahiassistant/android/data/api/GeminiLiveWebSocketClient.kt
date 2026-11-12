package com.maahiassistant.android.data.api

import android.util.Log
import com.maahiassistant.android.data.api.models.GeminiLiveMessages
import com.maahiassistant.android.data.api.models.ServerTurnChunk
import com.maahiassistant.android.data.local.LiveLogManager
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

    // IMPORTANT: No OkHttp pingInterval to avoid "sent ping but didn't receive pong within 15000ms"
    // Rely on readTimeout 0 and server-level messages & reconnects instead.
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(0, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun connect(
        apiKey: String,
        modelId: String,
        voiceName: String,
        resumptionHandle: String? = null
    ) {
        if (apiKey.isBlank()) {
            val err = "Missing Gemini API Key. Add it in Settings."
            LiveLogManager.error("WebSocket", err)
            listener.onError(err, true)
            return
        }

        disconnect()

        val cleanKey = apiKey.trim()
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$cleanKey"

        val request = Request.Builder()
            .url(url)
            .build()

        LiveLogManager.info("WebSocket", "Connecting to Gemini Live API with model='$modelId', voice='$voiceName'...")
        Log.i(TAG, "Opening WebSocket connection to Gemini Live API...")

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected.set(true)
                isSetupComplete.set(false)
                LiveLogManager.success("WebSocket", "WebSocket connected (HTTP 101 Switching Protocols). Sending setup frame for model '$modelId'...")
                Log.i(TAG, "WebSocket connection established. Sending initial setup message...")

                // Send setup message first
                val setupPayload = GeminiLiveMessages.buildSetupMessage(
                    modelId = modelId,
                    voiceName = voiceName,
                    resumptionHandle = resumptionHandle
                )
                webSocket.send(setupPayload)
                LiveLogManager.frame("Setup", "Sent setup JSON: model=$modelId, voice=$voiceName, modalities=[AUDIO]")

                scope.launch {
                    listener.onConnected()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val chunk = GeminiLiveMessages.parseServerMessage(text)
                if (chunk != null) {
                    if (chunk.isSetupComplete) {
                        isSetupComplete.set(true)
                        LiveLogManager.success("Gemini", "setupComplete received! Model is active and ready for audio/text.")
                        Log.i(TAG, "Gemini session setup complete! Audio and text streaming unblocked.")
                        scope.launch {
                            listener.onSetupComplete()
                        }
                    }

                    if (chunk.serverError != null) {
                        LiveLogManager.error("Gemini", "Server error frame: ${chunk.serverError}")
                        Log.e(TAG, "Server error reported in message payload: ${chunk.serverError}")
                        scope.launch {
                            listener.onError(chunk.serverError, false)
                        }
                    }

                    if (!chunk.inputText.isNullOrEmpty()) {
                        LiveLogManager.info("SpeechToText", "User speech transcript: \"${chunk.inputText}\"")
                    }

                    if (!chunk.outputText.isNullOrEmpty()) {
                        LiveLogManager.info("VoiceToText", "Maahi speech transcript: \"${chunk.outputText}\"")
                    }

                    if (chunk.audioBytes != null) {
                        LiveLogManager.frame("AudioOutput", "Received 24kHz audio chunk: ${chunk.audioBytes.size} bytes PCM")
                    }

                    if (chunk.isInterrupted) {
                        LiveLogManager.warn("VAD", "Interrupted (Barge-in): User speech interrupted model output.")
                    }

                    if (chunk.isTurnComplete) {
                        LiveLogManager.info("Turn", "turnComplete received from Gemini.")
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

                // Strict security: ensure API key is never included in error logs or UI messages
                val sanitizedError = if (cleanKey.isNotBlank() && finalError.contains(cleanKey)) {
                    finalError.replace(cleanKey, "[REDACTED]")
                } else {
                    finalError
                }

                LiveLogManager.error("WebSocket", "Failure (Code $code): $sanitizedError")
                Log.e(TAG, "WebSocket failure: $sanitizedError")
                scope.launch {
                    listener.onError(sanitizedError, isAuthError)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected.set(false)
                isSetupComplete.set(false)
                LiveLogManager.warn("WebSocket", "Closed: code=$code, reason=$reason")
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
        if (!isConnected.get() || !isSetupComplete.get()) {
            return false
        }
        LiveLogManager.info("ClientContent", "Sending user text to Gemini Live: \"$text\"")
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
