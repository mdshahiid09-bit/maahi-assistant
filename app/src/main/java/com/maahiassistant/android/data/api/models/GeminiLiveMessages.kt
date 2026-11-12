package com.maahiassistant.android.data.api.models

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

data class ServerTurnChunk(
    val text: String? = null,
    val audioBytes: ByteArray? = null,
    val isInterrupted: Boolean = false,
    val isTurnComplete: Boolean = false,
    val isSearching: Boolean = false,
    val newResumptionHandle: String? = null
)

object GeminiLiveMessages {

    fun buildSetupMessage(
        modelId: String,
        voiceName: String,
        resumptionHandle: String? = null
    ): String {
        val root = JSONObject()
        val setup = JSONObject()

        val cleanModel = modelId.trim()
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

        // Google Search Grounding Tool
        val toolsArray = JSONArray()
        val googleSearchTool = JSONObject().apply {
            put("googleSearch", JSONObject())
        }
        toolsArray.put(googleSearchTool)
        setup.put("tools", toolsArray)

        // System Instruction
        val systemInstruction = JSONObject()
        val partsArray = JSONArray().apply {
            put(JSONObject().apply {
                put("text", "You are Maahi, an intelligent, empathetic, and warm AI voice assistant. Speak naturally with concise, conversational phrasing suitable for real-time speech dialogue. Keep responses direct and helpful. When live Google Search is needed, weave answers in smoothly.")
            })
        }
        systemInstruction.put("parts", partsArray)
        setup.put("systemInstruction", systemInstruction)

        // Session Resumption
        if (!resumptionHandle.isNullOrBlank()) {
            val sessionResumption = JSONObject().apply {
                put("handle", resumptionHandle)
            }
            setup.put("sessionResumption", sessionResumption)
        }

        root.put("setup", setup)
        return root.toString()
    }

    fun buildRealtimeAudioChunk(pcmBytes: ByteArray, offset: Int, length: Int): String {
        val base64Data = Base64.encodeToString(pcmBytes, offset, length, Base64.NO_WRAP)
        val root = JSONObject()
        val realtimeInput = JSONObject()
        val mediaChunks = JSONArray().apply {
            put(JSONObject().apply {
                put("mimeType", "audio/pcm")
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
        return root.toString()
    }

    fun parseServerMessage(jsonText: String): ServerTurnChunk? {
        return try {
            val json = JSONObject(jsonText)
            var textDelta: StringBuilder? = null
            var pcmBytes: ByteArray? = null
            var interrupted = false
            var turnComplete = false
            var searching = false
            var newHandle: String? = null

            // Session Resumption update
            if (json.has("sessionResumptionUpdate")) {
                val updateObj = json.optJSONObject("sessionResumptionUpdate")
                newHandle = updateObj?.optString("newHandle")
                    ?: updateObj?.optString("handle")
            }

            // Tool call or grounding
            if (json.has("toolCall") || json.has("groundingMetadata")) {
                searching = true
            }

            if (json.has("serverContent")) {
                val serverContent = json.getJSONObject("serverContent")

                if (serverContent.optBoolean("interrupted", false)) {
                    interrupted = true
                }
                if (serverContent.optBoolean("turnComplete", false)) {
                    turnComplete = true
                }

                if (serverContent.has("groundingMetadata")) {
                    searching = true
                }

                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.optJSONObject(i) ?: continue

                            // Text transcript delta
                            if (part.has("text")) {
                                if (textDelta == null) textDelta = StringBuilder()
                                textDelta.append(part.getString("text"))
                            }

                            // Audio PCM chunk
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
                    }
                }
            }

            ServerTurnChunk(
                text = textDelta?.toString(),
                audioBytes = pcmBytes,
                isInterrupted = interrupted,
                isTurnComplete = turnComplete,
                isSearching = searching,
                newResumptionHandle = newHandle
            )
        } catch (e: Exception) {
            null
        }
    }
}
