package com.maahiassistant.android.data.api

import com.maahiassistant.android.data.local.LiveLogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class KeyValidationResult(
    val message: String,
    val selectedBidiModel: String?,
    val allBidiModels: List<String>
)

class GeminiRestValidator(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    suspend fun validateKey(apiKey: String): Result<KeyValidationResult> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("API key cannot be empty"))
        }

        LiveLogManager.info("Validator", "Validating key against Google AI Studio models endpoint...")
        val url = "https://generativelanguage.googleapis.com/v1beta/models?key=${apiKey.trim()}"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val bidiModels = mutableListOf<String>()
                    try {
                        val json = JSONObject(bodyString)
                        val modelsArray = json.optJSONArray("models")
                        if (modelsArray != null) {
                            for (i in 0 until modelsArray.length()) {
                                val modelObj = modelsArray.optJSONObject(i) ?: continue
                                val name = modelObj.optString("name")
                                val methods = modelObj.optJSONArray("supportedGenerationMethods")
                                if (methods != null) {
                                    for (j in 0 until methods.length()) {
                                        if (methods.optString(j) == "bidiGenerateContent") {
                                            bidiModels.add(name.removePrefix("models/"))
                                            break
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        LiveLogManager.warn("Validator", "Failed to parse models json: ${e.message}")
                    }

                    LiveLogManager.success("Validator", "API key HTTP 200 OK. bidiGenerateContent models: $bidiModels")

                    // Select the best model that supports bidiGenerateContent
                    val pickedModel = when {
                        bidiModels.contains("gemini-3.8-live") -> "gemini-3.8-live"
                        bidiModels.contains("gemini-3.8-live-extended-thinking") -> "gemini-3.8-live-extended-thinking"
                        bidiModels.contains("gemini-2.0-flash-exp") -> "gemini-2.0-flash-exp"
                        bidiModels.any { it.contains("3.8") } -> bidiModels.first { it.contains("3.8") }
                        bidiModels.any { it.contains("live") } -> bidiModels.first { it.contains("live") }
                        bidiModels.any { it.contains("flash") } -> bidiModels.first { it.contains("flash") }
                        bidiModels.isNotEmpty() -> bidiModels.first()
                        else -> "gemini-3.8-live"
                    }

                    val msg = if (bidiModels.isNotEmpty()) {
                        "API key verified! Live voice model: $pickedModel"
                    } else {
                        "API key verified successfully! (Default live voice model: $pickedModel)"
                    }

                    Result.success(KeyValidationResult(msg, pickedModel, bidiModels))
                } else {
                    val errorMessage = try {
                        val json = JSONObject(bodyString)
                        val error = json.optJSONObject("error")
                        error?.optString("message") ?: "HTTP error ${response.code}: ${response.message}"
                    } catch (e: Exception) {
                        "HTTP error ${response.code}: ${response.message}"
                    }
                    Result.failure(IOException(errorMessage))
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException(e.message ?: "Network error validating key"))
        }
    }
}
