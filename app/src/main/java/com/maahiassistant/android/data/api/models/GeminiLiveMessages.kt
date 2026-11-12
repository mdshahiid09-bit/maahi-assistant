package com.maahiassistant.android.data.api.models

import android.util.Base64
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

data class ServerTurnChunk(
    val isSetupComplete: Boolean = false,
    val inputText: String? = null,
    val outputText: String? = null,
    val audioBytes: ByteArray? = null,
    val isInterrupted: Boolean = false,
    val isTurnComplete: Boolean = false,
    val isGenerationComplete: Boolean = false,
    val isSearching: Boolean = false,
    val newResumptionHandle: String? = null,
    val serverError: String? = null
)

object GeminiLiveMessages {

    private const val TAG = "MaahiLive"

    fun buildSetupMessage(
        modelId: String,
        voiceName: String,
        resumptionHandle: String? = null
    ): String {
        val root = JSONObject()
        val setup = JSONObject()

        val cleanModel = modelId.trim().ifEmpty { "gemini-2.0-flash-exp" }
        val formattedModel = if (cleanModel.startsWith("models/")) cleanModel else "models/$cleanModel"
        setup.put("model", formattedModel)

        val genConfig = JSONObject()
        val modalities = JSONArray().apply { put("AUDIO") }
        genConfig.put("responseModalities", modalities)

        val speechConfig = JSONObject()
        val voiceConfig = JSONObject()
        val prebuiltVoiceConfig = JSONObject().apply {
            put("voiceName", voiceName.trim().ifEmpty { "Kore" })
        }
        voiceConfig.put("prebuiltVoiceConfig", prebuiltVoiceConfig)
        speechConfig.put("voiceConfig", voiceConfig)
        genConfig.put("speechConfig", speechConfig)
        setup.put("generationConfig", genConfig)

        // Real-time voice transcriptions: both user's input and Maahi's output
        setup.put("inputAudioTranscription", JSONObject())
        setup.put("outputAudioTranscription", JSONObject())

        // System Instruction - Jarvis persona
        val systemInstruction = JSONObject()
        val partsArray = JSONArray().apply {
            put(JSONObject().apply {
                put("text", "You are Maahi, an intelligent, empathetic, and warm AI voice assistant like Jarvis. Speak naturally with concise, conversational phrasing suitable for real-time speech dialogue. Keep responses direct and helpful. When live Google Search is needed, weave answers in smoothly.")
            })
        }
        systemInstruction.put("parts", partsArray)
        setup.put("systemInstruction", systemInstruction)

        // Google Search Grounding Tool
        val toolsArray = JSONArray()
        val googleSearchTool = JSONObject().apply {
            put("googleSearch", JSONObject())
        }
        toolsArray.put(googleSearchTool)
        setup.put("tools", toolsArray)

        // Session Resumption (if present)
        if (!resumptionHandle.isNullOrBlank()) {
            val sessionResumption = JSONObject().apply {
                put("handle", resumptionHandle)
            }
            setup.put("sessionResumption", sessionResumption)
        }

        root.put("setup", setup)
        val payload = root.toString()
        Log.d(TAG, "Outgoing setup message: $payload")
        return payload
    }

    fun buildRealtimeAudioChunk(pcmBytes: ByteArray, offset: Int, length: Int): String {
        val base64Data = Base64.encodeToString(pcmBytes, offset, length, Base64.NO_WRAP)
        val root = JSONObject()
        val realtimeInput = JSONObject()
        val mediaChunks = JSONArray().apply {
            put(JSONObject().apply {
                put("mimeType", "audio/pcm;rate=16000")
                put("data", base64Data)
            })
        }
        realtimeInput.put("mediaChunks", mediaChunks)
        root.put("realtimeInput", realtimeInput)
        return root.toString()
    }

    fun buildClientContentTextMessage(text: String): String {
        val root = JSONObject()
        val clientContent = JSONObject()
        val turns = JSONArray().apply {
            val turn = JSONObject()
            turn.put("role", "user")
            val parts = JSONArray().apply {
                put(JSONObject().apply {
                    put("text", text)
                })
            }
            turn.put("parts", parts)
            put(turn)
        }
        clientContent.put("turns", turns)
        clientContent.put("turnComplete", true)
        root.put("clientContent", clientContent)
        Log.d(TAG, "Outgoing clientContent text: $text")
        return root.toString()
    }

    fun parseServerMessage(jsonText: String): ServerTurnChunk? {
        return try {
            val json = JSONObject(jsonText)
            Log.d(TAG, "Received message: $jsonText")

            var isSetupComplete = false
            var inputText: String? = null
            var outputText: String? = null
            var pcmBytes: ByteArray? = null
            var interrupted = false
            var turnComplete = false
            var generationComplete = false
            var searching = false
            var newHandle: String? = null
            var serverError: String? = null

            // 1. Setup Complete acknowledgement
            if (json.has("setupComplete")) {
                Log.i(TAG, "setupComplete received from Gemini Live API")
                isSetupComplete = true
            }

            // 2. Server Error frames
            if (json.has("error")) {
                val errObj = json.optJSONObject("error")
                serverError = if (errObj != null) {
                    val code = errObj.optInt("code", 0)
                    val msg = errObj.optString("message")
                    val status = errObj.optString("status")
                    "Gemini Error ($code $status): $msg".trim()
                } else {
                    json.optString("error")
                }
                Log.e(TAG, "Server error frame received: $serverError")
            } else if (json.has("errorMessage")) {
                serverError = "Gemini Error: ${json.optString("errorMessage")}"
                Log.e(TAG, "Server errorMessage frame received: $serverError")
            }

            // 3. Session Resumption update
            if (json.has("sessionResumptionUpdate")) {
                val updateObj = json.optJSONObject("sessionResumptionUpdate")
                newHandle = updateObj?.optString("newHandle")
                    ?: updateObj?.optString("handle")
                Log.d(TAG, "Resumption handle received: $newHandle")
            }

            // 4. Tool call or grounding
            if (json.has("toolCall") || json.has("groundingMetadata")) {
                searching = true
                Log.d(TAG, "Tool activity detected")
            }

            // 5. Server Content
            if (json.has("serverContent")) {
                val serverContent = json.getJSONObject("serverContent")

                if (serverContent.optBoolean("interrupted", false)) {
                    interrupted = true
                    Log.w(TAG, "Interrupted signal (barge-in)")
                }
                if (serverContent.optBoolean("turnComplete", false)) {
                    turnComplete = true
                    Log.i(TAG, "turnComplete: true")
                }
                if (serverContent.optBoolean("generationComplete", false)) {
                    generationComplete = true
                    Log.i(TAG, "generationComplete: true")
                }

                if (serverContent.has("groundingMetadata")) {
                    searching = true
                }

                // User speech transcription
                if (serverContent.has("inputTranscription")) {
                    val inputObj = serverContent.optJSONObject("inputTranscription")
                    val transcript = inputObj?.optString("text").orEmpty()
                    if (transcript.isNotEmpty()) {
                        inputText = transcript
                        Log.d(TAG, "User inputTranscription: $transcript")
                    }
                }

                // Model speech transcription
                if (serverContent.has("outputTranscription")) {
                    val outputObj = serverContent.optJSONObject("outputTranscription")
                    val transcript = outputObj?.optString("text").orEmpty()
                    if (transcript.isNotEmpty()) {
                        outputText = transcript
                        Log.d(TAG, "Model outputTranscription: $transcript")
                    }
                }

                // Model turn parts (Audio chunks and inline text)
                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts")
                    if (parts != null) {
                        val textParts = StringBuilder()
                        for (i in 0 until parts.length()) {
                            val part = parts.optJSONObject(i) ?: continue

                            // Model text part (if present)
                            if (part.has("text")) {
                                textParts.append(part.getString("text"))
                            }

                            // Model PCM audio part (24kHz little-endian mono)
                            if (part.has("inlineData")) {
                                val inlineData = part.getJSONObject("inlineData")
                                val base64Data = inlineData.optString("data")
                                if (base64Data.isNotEmpty()) {
                                    val decoded = Base64.decode(base64Data, Base64.DEFAULT)
                                    pcmBytes = if (pcmBytes == null) {
                                        decoded
                                    } else {
                                        pcmBytes + decoded
                                    }
                                }
                            }
                        }
                        if (textParts.isNotEmpty() && outputText == null) {
                            outputText = textParts.toString()
                        }
                    }
                }
            }

            ServerTurnChunk(
                isSetupComplete = isSetupComplete,
                inputText = inputText,
                outputText = outputText,
                audioBytes = pcmBytes,
                isInterrupted = interrupted,
                isTurnComplete = turnComplete,
                isGenerationComplete = generationComplete,
                isSearching = searching,
                newResumptionHandle = newHandle,
                serverError = serverError
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing server message", e)
            null
        }
    }
}
