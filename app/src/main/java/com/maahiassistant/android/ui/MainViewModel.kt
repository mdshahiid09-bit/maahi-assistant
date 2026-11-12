package com.maahiassistant.android.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maahiassistant.android.data.api.models.ConnectionStatus
import com.maahiassistant.android.data.api.models.SessionOrbState
import com.maahiassistant.android.data.db.ChatMessageEntity
import com.maahiassistant.android.data.repository.ChatRepository
import com.maahiassistant.android.data.repository.SettingsRepository
import com.maahiassistant.android.service.MaahiSessionService
import com.maahiassistant.android.ui.components.BottomTab
import com.maahiassistant.android.ui.components.DrawerDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class KeyTestState {
    object Idle : KeyTestState()
    object Testing : KeyTestState()
    data class Success(val message: String) : KeyTestState()
    data class Error(val error: String) : KeyTestState()
}

class MainViewModel(
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // Live session states from Service
    val isSessionActive: StateFlow<Boolean> = MaahiSessionService.isSessionActive
    val orbState: StateFlow<SessionOrbState> = MaahiSessionService.orbState
    val connectionStatus: StateFlow<ConnectionStatus> = MaahiSessionService.connectionStatus
    val isMuted: StateFlow<Boolean> = MaahiSessionService.isMuted
    val isSpeakerOn: StateFlow<Boolean> = MaahiSessionService.isSpeakerOn
    val audioInputLevel: StateFlow<Float> = MaahiSessionService.audioInputLevel
    val audioOutputLevel: StateFlow<Float> = MaahiSessionService.audioOutputLevel
    val currentUserText: StateFlow<String> = MaahiSessionService.currentUserText
    val currentAssistantText: StateFlow<String> = MaahiSessionService.currentAssistantText
    val isToolSearching: StateFlow<Boolean> = MaahiSessionService.isToolSearching
    val serviceErrorMessage: StateFlow<String?> = MaahiSessionService.errorMessage

    // Navigation state
    private val _currentTab = MutableStateFlow(BottomTab.HOME)
    val currentTab: StateFlow<BottomTab> = _currentTab.asStateFlow()

    private val _currentDrawerDestination = MutableStateFlow(DrawerDestination.HOME)
    val currentDrawerDestination: StateFlow<DrawerDestination> = _currentDrawerDestination.asStateFlow()

    // Chat
    val chatMessages: StateFlow<List<ChatMessageEntity>> = chatRepository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings
    val modelId: StateFlow<String> = settingsRepository.modelId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "gemini-3.8-live")

    val voice: StateFlow<String> = settingsRepository.voice
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Kore")

    val useSpeakerPreference: StateFlow<Boolean> = settingsRepository.useSpeaker
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val keepScreenOn: StateFlow<Boolean> = settingsRepository.keepScreenOn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val onboardingCompleted: StateFlow<Boolean> = settingsRepository.onboardingCompleted
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _keyTestState = MutableStateFlow<KeyTestState>(KeyTestState.Idle)
    val keyTestState: StateFlow<KeyTestState> = _keyTestState.asStateFlow()

    private val _hasApiKey = MutableStateFlow(settingsRepository.hasApiKey())
    val hasApiKey: StateFlow<Boolean> = _hasApiKey.asStateFlow()

    fun refreshKeyStatus() {
        _hasApiKey.value = settingsRepository.hasApiKey()
    }

    fun getStoredApiKey(): String? = settingsRepository.getApiKey()

    fun selectTab(tab: BottomTab) {
        _currentTab.value = tab
    }

    fun selectDrawerDestination(destination: DrawerDestination) {
        _currentDrawerDestination.value = destination
        when (destination) {
            DrawerDestination.HOME -> _currentTab.value = BottomTab.HOME
            DrawerDestination.CHAT -> _currentTab.value = BottomTab.CHAT
            DrawerDestination.SETTINGS -> _currentTab.value = BottomTab.SETTINGS
            DrawerDestination.LIVE_LOGS -> { /* Handled in drawer / view */ }
            DrawerDestination.CHAT_HISTORY -> { /* Handled in drawer / view */ }
            DrawerDestination.HELP_GUIDE -> { /* Handled in drawer / view */ }
        }
    }

    fun toggleSession(context: Context, onMissingKey: () -> Unit, onMicPermissionNeeded: () -> Unit) {
        if (!settingsRepository.hasApiKey()) {
            onMissingKey()
            return
        }

        if (isSessionActive.value) {
            MaahiSessionService.stopService(context)
        } else {
            MaahiSessionService.startService(context)
        }
    }

    fun toggleMute(context: Context) {
        MaahiSessionService.toggleMute(context)
    }

    fun toggleSpeaker(context: Context) {
        val nextSpeaker = !isSpeakerOn.value
        MaahiSessionService.setSpeaker(context, nextSpeaker)
        viewModelScope.launch {
            settingsRepository.setUseSpeaker(nextSpeaker)
        }
    }

    fun sendTextMessage(context: Context, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            MaahiSessionService.sendText(context, trimmed)
        }
    }

    fun sendTextMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            if (isSessionActive.value) {
                MaahiSessionService.sendText(trimmed)
            } else {
                chatRepository.saveUserMessage(trimmed)
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            chatRepository.clearHistory()
        }
    }

    fun testAndSaveApiKey(apiKey: String) {
        val trimmed = apiKey.trim()
        if (trimmed.isEmpty()) {
            _keyTestState.value = KeyTestState.Error("Please enter a valid Gemini API key")
            return
        }

        _keyTestState.value = KeyTestState.Testing
        viewModelScope.launch {
            val result = settingsRepository.testApiKey(trimmed)
            if (result.isSuccess) {
                val validation = result.getOrNull()
                settingsRepository.saveApiKey(trimmed)
                if (validation?.selectedBidiModel != null) {
                    settingsRepository.setModelId(validation.selectedBidiModel)
                }
                refreshKeyStatus()
                _keyTestState.value = KeyTestState.Success(validation?.message ?: "Key is valid and saved!")
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Key validation failed"
                _keyTestState.value = KeyTestState.Error(errorMsg)
            }
        }
    }

    fun updateModelId(id: String) {
        viewModelScope.launch {
            settingsRepository.setModelId(id)
        }
    }

    fun updateVoice(voiceName: String) {
        viewModelScope.launch {
            settingsRepository.setVoice(voiceName)
        }
    }

    fun updateUseSpeaker(useSpeaker: Boolean) {
        viewModelScope.launch {
            settingsRepository.setUseSpeaker(useSpeaker)
        }
    }

    fun updateKeepScreenOn(keepOn: Boolean) {
        viewModelScope.launch {
            settingsRepository.setKeepScreenOn(keepOn)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(true)
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            settingsRepository.resetSettings()
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            settingsRepository.deleteAllData()
            chatRepository.clearHistory()
            refreshKeyStatus()
        }
    }
}
