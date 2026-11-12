package com.maahiassistant.android.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.maahiassistant.android.MaahiApplication
import com.maahiassistant.android.R
import com.maahiassistant.android.data.api.GeminiLiveWebSocketClient
import com.maahiassistant.android.data.api.models.ConnectionStatus
import com.maahiassistant.android.data.api.models.ServerTurnChunk
import com.maahiassistant.android.data.api.models.SessionOrbState
import com.maahiassistant.android.data.audio.AudioCaptureManager
import com.maahiassistant.android.data.audio.AudioPlaybackManager
import com.maahiassistant.android.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MaahiSessionService : Service() {

    companion object {
        private const val TAG = "MaahiLive"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START_SESSION = "com.maahiassistant.android.START_SESSION"
        const val ACTION_STOP_SESSION = "com.maahiassistant.android.STOP_SESSION"
        const val ACTION_TOGGLE_MUTE = "com.maahiassistant.android.TOGGLE_MUTE"
        const val ACTION_END_CALL = "com.maahiassistant.android.END_CALL"
        const val ACTION_SET_SPEAKER = "com.maahiassistant.android.SET_SPEAKER"
        const val EXTRA_SPEAKER_ON = "extra_speaker_on"

        // Singleton state access for UI components
        private val _isSessionActive = MutableStateFlow(false)
        val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

        private val _orbState = MutableStateFlow(SessionOrbState.IDLE)
        val orbState: StateFlow<SessionOrbState> = _orbState.asStateFlow()

        private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
        val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

        private val _isMuted = MutableStateFlow(false)
        val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

        private val _isSpeakerOn = MutableStateFlow(true)
        val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

        private val _audioInputLevel = MutableStateFlow(0f)
        val audioInputLevel: StateFlow<Float> = _audioInputLevel.asStateFlow()

        private val _audioOutputLevel = MutableStateFlow(0f)
        val audioOutputLevel: StateFlow<Float> = _audioOutputLevel.asStateFlow()

        private val _currentUserText = MutableStateFlow("")
        val currentUserText: StateFlow<String> = _currentUserText.asStateFlow()

        private val _currentAssistantText = MutableStateFlow("")
        val currentAssistantText: StateFlow<String> = _currentAssistantText.asStateFlow()

        private val _isToolSearching = MutableStateFlow(false)
        val isToolSearching: StateFlow<Boolean> = _isToolSearching.asStateFlow()

        private val _errorMessage = MutableStateFlow<String?>(null)
        val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

        @Volatile
        var activeInstance: MaahiSessionService? = null
            private set

        fun sendText(text: String) {
            activeInstance?.sendTextMessage(text)
        }

        fun startService(context: Context) {
            val intent = Intent(context, MaahiSessionService::class.java).apply {
                action = ACTION_START_SESSION
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MaahiSessionService::class.java).apply {
                action = ACTION_STOP_SESSION
            }
            context.startService(intent)
        }

        fun toggleMute(context: Context) {
            val intent = Intent(context, MaahiSessionService::class.java).apply {
                action = ACTION_TOGGLE_MUTE
            }
            context.startService(intent)
        }

        fun setSpeaker(context: Context, speaker: Boolean) {
            val intent = Intent(context, MaahiSessionService::class.java).apply {
                action = ACTION_SET_SPEAKER
                putExtra(EXTRA_SPEAKER_ON, speaker)
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val binder = LocalBinder()

    private var audioCapture: AudioCaptureManager? = null
    private var audioPlayback: AudioPlaybackManager? = null
    private var webSocketClient: GeminiLiveWebSocketClient? = null

    private var resumptionHandle: String? = null
    private var isNetworkAvailable = true
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private var reconnectJob: Job? = null
    private val liveUserBuffer = StringBuilder()
    private val liveAssistantBuffer = StringBuilder()

    inner class LocalBinder : Binder() {
        fun getService(): MaahiSessionService = this@MaahiSessionService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        activeInstance = this
        setupNetworkMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SESSION -> {
                startForegroundSession()
            }
            ACTION_STOP_SESSION, ACTION_END_CALL -> {
                stopForegroundSession()
            }
            ACTION_TOGGLE_MUTE -> {
                toggleMuteInternal()
            }
            ACTION_SET_SPEAKER -> {
                val speaker = intent.getBooleanExtra(EXTRA_SPEAKER_ON, true)
                setSpeakerInternal(speaker)
            }
        }
        return START_NOT_STICKY
    }

    @SuppressLint("ForegroundServiceType")
    private fun startForegroundSession() {
        if (_isSessionActive.value) return

        _isSessionActive.value = true
        _orbState.value = SessionOrbState.CONNECTING
        _connectionStatus.value = ConnectionStatus.CONNECTING
        _errorMessage.value = null

        val notification = buildNotification(isMuted = _isMuted.value, isSpeaking = false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        initAudioAndWebSocket()
    }

    private fun initAudioAndWebSocket() {
        val app = MaahiApplication.instance

        // Audio Playback: initialize and start playback thread (ready for 24kHz audio)
        audioPlayback = AudioPlaybackManager(this) { isSpeaking, outputLevel ->
            _audioOutputLevel.value = outputLevel
            if (isSpeaking) {
                _orbState.value = SessionOrbState.SPEAKING
                _connectionStatus.value = ConnectionStatus.SPEAKING
                updateNotification(isMuted = _isMuted.value, isSpeaking = true)
            } else if (_isSessionActive.value) {
                if (_isMuted.value) {
                    _orbState.value = SessionOrbState.MUTED
                    _connectionStatus.value = ConnectionStatus.MUTED
                } else {
                    _orbState.value = SessionOrbState.LISTENING
                    _connectionStatus.value = ConnectionStatus.LISTENING
                }
                updateNotification(isMuted = _isMuted.value, isSpeaking = false)
            }
        }
        audioPlayback?.setSpeakerMode(_isSpeakerOn.value)
        audioPlayback?.start(serviceScope)

        // Audio Capture: prepared, but NOT started until setupComplete arrives
        audioCapture = AudioCaptureManager(
            onAudioChunk = { buffer, offset, length ->
                webSocketClient?.sendAudioChunk(buffer, offset, length)
            },
            onAudioLevel = { level ->
                _audioInputLevel.value = level
            },
            onError = { error ->
                Log.e(TAG, "Audio capture error: $error")
                _errorMessage.value = error
                _orbState.value = SessionOrbState.ERROR
                serviceScope.launch {
                    app.chatRepository.addSystemNote("Microphone error: $error")
                }
            }
        )
        audioCapture?.setMute(_isMuted.value)

        // WebSocket Client
        webSocketClient = GeminiLiveWebSocketClient(object : GeminiLiveWebSocketClient.LiveSessionListener {
            override fun onConnected() {
                serviceScope.launch {
                    Log.i(TAG, "WebSocket connected, waiting for setupComplete...")
                }
            }

            override fun onSetupComplete() {
                serviceScope.launch {
                    Log.i(TAG, "setupComplete confirmed by Gemini. Starting audio capture...")
                    val wasReconnecting = _connectionStatus.value == ConnectionStatus.RECONNECTING

                    // Only start mic capture after setupComplete has arrived
                    val hasMic = ContextCompat.checkSelfPermission(
                        this@MaahiSessionService,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasMic) {
                        audioCapture?.start(serviceScope)
                    } else {
                        val permErr = "Microphone permission not granted"
                        Log.e(TAG, permErr)
                        _errorMessage.value = permErr
                        app.chatRepository.addSystemNote(permErr)
                    }

                    if (_isMuted.value) {
                        _orbState.value = SessionOrbState.MUTED
                        _connectionStatus.value = ConnectionStatus.MUTED
                    } else {
                        _orbState.value = SessionOrbState.LISTENING
                        _connectionStatus.value = ConnectionStatus.LISTENING
                    }
                    _errorMessage.value = null
                    updateNotification(isMuted = _isMuted.value, isSpeaking = false)

                    if (wasReconnecting) {
                        app.chatRepository.addSystemNote("Reconnected")
                    }
                }
            }

            override fun onServerChunk(chunk: ServerTurnChunk) {
                serviceScope.launch {
                    // Cache session resumption token
                    chunk.newResumptionHandle?.let { handle ->
                        resumptionHandle = handle
                    }

                    // Tool activity / Search grounding indicator
                    if (chunk.isSearching) {
                        _isToolSearching.value = true
                    }

                    // User speech transcription delta
                    chunk.inputText?.let { userText ->
                        liveUserBuffer.append(userText)
                        _currentUserText.value = liveUserBuffer.toString()
                    }

                    // Model speech transcription delta
                    chunk.outputText?.let { assistantText ->
                        liveAssistantBuffer.append(assistantText)
                        _currentAssistantText.value = liveAssistantBuffer.toString()
                    }

                    // Audio output bytes (24kHz PCM)
                    chunk.audioBytes?.let { pcm ->
                        audioPlayback?.enqueueAudio(pcm)
                        _orbState.value = SessionOrbState.SPEAKING
                        _connectionStatus.value = ConnectionStatus.SPEAKING
                    }

                    // Barge-in interruption
                    if (chunk.isInterrupted) {
                        Log.w(TAG, "Interruption triggered. Flushing audio playback.")
                        audioPlayback?.bargeIn()
                        commitAssistantTurn()
                        _isToolSearching.value = false
                        if (!_isMuted.value) {
                            _orbState.value = SessionOrbState.LISTENING
                            _connectionStatus.value = ConnectionStatus.LISTENING
                        }
                        updateNotification(isMuted = _isMuted.value, isSpeaking = false)
                    }

                    // Turn complete or Generation complete
                    if (chunk.isTurnComplete || chunk.isGenerationComplete) {
                        Log.i(TAG, "Turn/generation complete signal received.")
                        commitUserTurn()
                        commitAssistantTurn()
                        _isToolSearching.value = false
                    }
                }
            }

            override fun onError(message: String, isAuthError: Boolean) {
                serviceScope.launch {
                    Log.e(TAG, "Live session error: $message, isAuth: $isAuthError")
                    _errorMessage.value = message
                    _orbState.value = SessionOrbState.ERROR
                    _connectionStatus.value = ConnectionStatus.ERROR
                    app.chatRepository.addSystemNote(message)

                    if (!isAuthError && _isSessionActive.value) {
                        scheduleReconnection()
                    }
                }
            }

            override fun onClosed(code: Int, reason: String) {
                serviceScope.launch {
                    Log.i(TAG, "Session closed: code=$code, reason=$reason")
                    if (code != 1000) {
                        val closeMsg = "Connection closed ($code: $reason)"
                        _errorMessage.value = closeMsg
                        _orbState.value = SessionOrbState.ERROR
                        _connectionStatus.value = ConnectionStatus.ERROR
                        app.chatRepository.addSystemNote(closeMsg)
                    }
                    if (_isSessionActive.value && code != 1000) {
                        scheduleReconnection()
                    }
                }
            }
        })

        connectLiveSession()
    }

    private fun connectLiveSession() {
        val app = MaahiApplication.instance
        val apiKey = app.settingsRepository.getApiKey().orEmpty()
        if (apiKey.isBlank()) {
            val keyErr = "Gemini API key is missing. Please add your key in Settings."
            _errorMessage.value = keyErr
            _orbState.value = SessionOrbState.ERROR
            _connectionStatus.value = ConnectionStatus.ERROR
            serviceScope.launch {
                app.chatRepository.addSystemNote(keyErr)
            }
            return
        }

        serviceScope.launch {
            val modelId = app.settingsRepository.modelId.first()
            val voiceName = app.settingsRepository.voice.first()

            Log.i(TAG, "Connecting session with model='$modelId', voice='$voiceName'...")
            webSocketClient?.connect(
                apiKey = apiKey,
                modelId = modelId,
                voiceName = voiceName,
                resumptionHandle = resumptionHandle
            )
        }
    }

    fun sendTextMessage(text: String) {
        if (text.isBlank()) return
        serviceScope.launch {
            MaahiApplication.instance.chatRepository.saveUserMessage(text)
            webSocketClient?.sendTextMessage(text)
        }
    }

    private fun commitUserTurn() {
        val text = liveUserBuffer.toString().trim()
        if (text.isNotEmpty()) {
            serviceScope.launch {
                MaahiApplication.instance.chatRepository.saveUserMessage(text)
            }
            liveUserBuffer.clear()
            _currentUserText.value = ""
        }
    }

    private fun commitAssistantTurn() {
        val text = liveAssistantBuffer.toString().trim()
        if (text.isNotEmpty()) {
            val searching = _isToolSearching.value
            serviceScope.launch {
                MaahiApplication.instance.chatRepository.saveMaahiMessage(text, searching)
            }
            liveAssistantBuffer.clear()
            _currentAssistantText.value = ""
        }
    }

    private fun scheduleReconnection() {
        if (reconnectJob?.isActive == true) return
        reconnectJob = serviceScope.launch {
            if (!isNetworkAvailable) {
                _connectionStatus.value = ConnectionStatus.NO_NETWORK
                return@launch
            }

            _orbState.value = SessionOrbState.CONNECTING
            _connectionStatus.value = ConnectionStatus.RECONNECTING
            delay(2000)
            if (_isSessionActive.value) {
                connectLiveSession()
            }
        }
    }

    private fun toggleMuteInternal() {
        val newMute = !_isMuted.value
        _isMuted.value = newMute
        audioCapture?.setMute(newMute)

        if (newMute) {
            _orbState.value = SessionOrbState.MUTED
            _connectionStatus.value = ConnectionStatus.MUTED
        } else {
            if (_audioOutputLevel.value > 0.05f) {
                _orbState.value = SessionOrbState.SPEAKING
                _connectionStatus.value = ConnectionStatus.SPEAKING
            } else {
                _orbState.value = SessionOrbState.LISTENING
                _connectionStatus.value = ConnectionStatus.LISTENING
            }
        }
        updateNotification(isMuted = newMute, isSpeaking = _orbState.value == SessionOrbState.SPEAKING)
    }

    private fun setSpeakerInternal(speaker: Boolean) {
        _isSpeakerOn.value = speaker
        audioPlayback?.setSpeakerMode(speaker)
    }

    private fun stopForegroundSession() {
        _isSessionActive.value = false
        _orbState.value = SessionOrbState.IDLE
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        _audioInputLevel.value = 0f
        _audioOutputLevel.value = 0f
        _isToolSearching.value = false
        commitUserTurn()
        commitAssistantTurn()

        reconnectJob?.cancel()
        reconnectJob = null

        audioCapture?.stop()
        audioCapture?.release()
        audioCapture = null

        audioPlayback?.stop()
        audioPlayback?.release()
        audioPlayback = null

        webSocketClient?.disconnect()
        webSocketClient = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun setupNetworkMonitoring() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isNetworkAvailable = true
                serviceScope.launch {
                    if (_isSessionActive.value && _connectionStatus.value == ConnectionStatus.NO_NETWORK) {
                        scheduleReconnection()
                    }
                }
            }

            override fun onLost(network: Network) {
                isNetworkAvailable = false
                serviceScope.launch {
                    if (_isSessionActive.value) {
                        _connectionStatus.value = ConnectionStatus.NO_NETWORK
                    }
                }
            }
        }

        try {
            connectivityManager?.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering network callback", e)
        }
    }

    private fun buildNotification(isMuted: Boolean, isSpeaking: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val muteIntent = Intent(this, MaahiSessionService::class.java).apply {
            action = ACTION_TOGGLE_MUTE
        }
        val mutePendingIntent = PendingIntent.getService(
            this,
            1,
            muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val endIntent = Intent(this, MaahiSessionService::class.java).apply {
            action = ACTION_END_CALL
        }
        val endPendingIntent = PendingIntent.getService(
            this,
            2,
            endIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = when {
            isMuted -> "Microphone muted"
            isSpeaking -> getString(R.string.session_speaking)
            else -> getString(R.string.session_listening)
        }

        val muteActionTitle = if (isMuted) getString(R.string.action_unmute) else getString(R.string.action_mute)

        return NotificationCompat.Builder(this, MaahiApplication.SESSION_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(statusText)
            .setSmallIcon(if (isMuted) R.drawable.ic_mic_off else R.drawable.ic_mic)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                if (isMuted) R.drawable.ic_mic else R.drawable.ic_mic_off,
                muteActionTitle,
                mutePendingIntent
            )
            .addAction(
                R.drawable.ic_stop,
                getString(R.string.action_end_call),
                endPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(isMuted: Boolean, isSpeaking: Boolean) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(isMuted, isSpeaking))
    }

    override fun onDestroy() {
        activeInstance = null
        try {
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        } catch (e: Exception) {
            // Ignore
        }
        stopForegroundSession()
        serviceScope.cancel()
        super.onDestroy()
    }
}
