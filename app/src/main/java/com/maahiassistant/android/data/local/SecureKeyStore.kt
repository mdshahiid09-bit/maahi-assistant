package com.maahiassistant.android.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureKeyStore(context: Context) {

    companion object {
        private const val TAG = "SecureKeyStore"
        private const val PREF_FILE = "maahi_secure_prefs"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
    }

    private val sharedPreferences: SharedPreferences? = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREF_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e(TAG, "Error initializing EncryptedSharedPreferences, fallback to private mode", e)
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
    }

    fun saveApiKey(apiKey: String) {
        sharedPreferences?.edit()?.putString(KEY_GEMINI_API_KEY, apiKey.trim())?.apply()
    }

    fun getApiKey(): String? {
        val key = sharedPreferences?.getString(KEY_GEMINI_API_KEY, null)?.trim()
        return if (key.isNullOrEmpty()) null else key
    }

    fun hasApiKey(): Boolean {
        return !getApiKey().isNullOrBlank()
    }

    fun clearApiKey() {
        sharedPreferences?.edit()?.remove(KEY_GEMINI_API_KEY)?.apply()
    }
}
