package com.maahiassistant.android.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "maahi_settings")

class AppPreferences(private val context: Context) {

    companion object {
        val KEY_MODEL_ID = stringPreferencesKey("model_id")
        val KEY_VOICE = stringPreferencesKey("voice_name")
        val KEY_USE_SPEAKER = booleanPreferencesKey("use_speaker")
        val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

        const val DEFAULT_MODEL_ID = "gemini-3.8-live"
        const val DEFAULT_VOICE = "Kore"
        const val DEFAULT_USE_SPEAKER = true
        const val DEFAULT_KEEP_SCREEN_ON = true
    }

    val modelIdFlow: Flow<String> = context.appDataStore.data.map { prefs ->
        val raw = prefs[KEY_MODEL_ID] ?: DEFAULT_MODEL_ID
        if (raw.isBlank()) DEFAULT_MODEL_ID else raw
    }

    val voiceFlow: Flow<String> = context.appDataStore.data.map { prefs ->
        prefs[KEY_VOICE] ?: DEFAULT_VOICE
    }

    val useSpeakerFlow: Flow<Boolean> = context.appDataStore.data.map { prefs ->
        prefs[KEY_USE_SPEAKER] ?: DEFAULT_USE_SPEAKER
    }

    val keepScreenOnFlow: Flow<Boolean> = context.appDataStore.data.map { prefs ->
        prefs[KEY_KEEP_SCREEN_ON] ?: DEFAULT_KEEP_SCREEN_ON
    }

    val onboardingCompletedFlow: Flow<Boolean> = context.appDataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setModelId(modelId: String) {
        val clean = modelId.trim().removePrefix("models/")
        context.appDataStore.edit { prefs ->
            prefs[KEY_MODEL_ID] = if (clean.isBlank()) DEFAULT_MODEL_ID else clean
        }
    }

    suspend fun setVoice(voice: String) {
        context.appDataStore.edit { prefs ->
            prefs[KEY_VOICE] = voice.trim().ifEmpty { DEFAULT_VOICE }
        }
    }

    suspend fun setUseSpeaker(useSpeaker: Boolean) {
        context.appDataStore.edit { prefs ->
            prefs[KEY_USE_SPEAKER] = useSpeaker
        }
    }

    suspend fun setKeepScreenOn(keepOn: Boolean) {
        context.appDataStore.edit { prefs ->
            prefs[KEY_KEEP_SCREEN_ON] = keepOn
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.appDataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun resetSettings() {
        context.appDataStore.edit { prefs ->
            prefs[KEY_MODEL_ID] = DEFAULT_MODEL_ID
            prefs[KEY_VOICE] = DEFAULT_VOICE
            prefs[KEY_USE_SPEAKER] = DEFAULT_USE_SPEAKER
            prefs[KEY_KEEP_SCREEN_ON] = DEFAULT_KEEP_SCREEN_ON
        }
    }

    suspend fun clearAll() {
        context.appDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
