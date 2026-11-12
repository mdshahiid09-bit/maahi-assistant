package com.maahiassistant.android.data.repository

import com.maahiassistant.android.data.api.GeminiRestValidator
import com.maahiassistant.android.data.api.KeyValidationResult
import com.maahiassistant.android.data.local.AppPreferences
import com.maahiassistant.android.data.local.SecureKeyStore
import kotlinx.coroutines.flow.Flow

class SettingsRepository(
    private val secureKeyStore: SecureKeyStore,
    private val appPreferences: AppPreferences,
    private val validator: GeminiRestValidator = GeminiRestValidator()
) {

    val modelId: Flow<String> = appPreferences.modelIdFlow
    val voice: Flow<String> = appPreferences.voiceFlow
    val useSpeaker: Flow<Boolean> = appPreferences.useSpeakerFlow
    val keepScreenOn: Flow<Boolean> = appPreferences.keepScreenOnFlow
    val onboardingCompleted: Flow<Boolean> = appPreferences.onboardingCompletedFlow

    fun getApiKey(): String? = secureKeyStore.getApiKey()

    fun hasApiKey(): Boolean = secureKeyStore.hasApiKey()

    fun saveApiKey(apiKey: String) {
        secureKeyStore.saveApiKey(apiKey)
    }

    fun clearApiKey() {
        secureKeyStore.clearApiKey()
    }

    suspend fun testApiKey(apiKey: String): Result<KeyValidationResult> {
        return validator.validateKey(apiKey)
    }

    suspend fun setModelId(modelId: String) = appPreferences.setModelId(modelId)

    suspend fun setVoice(voice: String) = appPreferences.setVoice(voice)

    suspend fun setUseSpeaker(useSpeaker: Boolean) = appPreferences.setUseSpeaker(useSpeaker)

    suspend fun setKeepScreenOn(keepOn: Boolean) = appPreferences.setKeepScreenOn(keepOn)

    suspend fun setOnboardingCompleted(completed: Boolean) = appPreferences.setOnboardingCompleted(completed)

    suspend fun resetSettings() {
        appPreferences.resetSettings()
    }

    suspend fun deleteAllData() {
        secureKeyStore.clearApiKey()
        appPreferences.clearAll()
    }
}
