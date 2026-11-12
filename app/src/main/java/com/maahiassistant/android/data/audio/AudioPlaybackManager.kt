package com.maahiassistant.android.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import com.maahiassistant.android.data.local.LiveLogManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

class AudioPlaybackManager(
    private val context: Context,
    private val onSpeakingState: (isSpeaking: Boolean, outputLevel: Float) -> Unit
) {

    companion object {
        private const val TAG = "AudioPlaybackMgr"
        const val SAMPLE_RATE = 24000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val audioFocusHelper = AudioFocusHelper(context)

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val queue = LinkedBlockingQueue<ByteArray>()
    private val isRunning = AtomicBoolean(false)
    private var useSpeaker = true

    fun setSpeakerMode(speaker: Boolean) {
        useSpeaker = speaker
        applyAudioRouting()
    }

    private fun applyAudioRouting() {
        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val devices = audioManager.availableCommunicationDevices
                val targetType = if (useSpeaker) {
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                } else {
                    AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
                }
                val device = devices.find { it.type == targetType }
                if (device != null) {
                    audioManager.setCommunicationDevice(device)
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.isSpeakerphoneOn = useSpeaker
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = useSpeaker
            }
        } catch (e: Exception) {
            Log.e(TAG, "Audio routing failed", e)
        }
    }

    fun start(scope: CoroutineScope) {
        if (isRunning.get()) return

        applyAudioRouting()

        val minBufSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = maxOf(minBufSize * 2, 8192)

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val format = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(CHANNEL_CONFIG)
            .setEncoding(AUDIO_FORMAT)
            .build()

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
        isRunning.set(true)
        LiveLogManager.info("AudioTrack", "AudioTrack initialized at 24000Hz PCM Mono STREAM mode")

        playbackJob = scope.launch(Dispatchers.IO) {
            while (isActive && isRunning.get()) {
                val chunk = try {
                    queue.poll(50, java.util.concurrent.TimeUnit.MILLISECONDS)
                } catch (e: InterruptedException) {
                    null
                }

                if (chunk != null && chunk.isNotEmpty()) {
                    audioFocusHelper.requestFocus()
                    val level = calculateRms(chunk)
                    onSpeakingState(true, level)

                    audioTrack?.write(chunk, 0, chunk.size)
                } else {
                    if (queue.isEmpty()) {
                        onSpeakingState(false, 0f)
                        audioFocusHelper.abandonFocus()
                    }
                }
            }
        }
    }

    fun enqueueAudio(bytes: ByteArray) {
        if (bytes.isNotEmpty() && isRunning.get()) {
            queue.offer(bytes)
        }
    }

    /**
     * Barge-in: stops and flushes current playback immediately when user interrupts.
     */
    fun bargeIn() {
        queue.clear()
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Barge-in error", e)
        }
        onSpeakingState(false, 0f)
        audioFocusHelper.abandonFocus()
        LiveLogManager.warn("AudioTrack", "Barge-in triggered: AudioTrack paused & flushed")
    }

    fun stop() {
        if (!isRunning.get()) return
        isRunning.set(false)
        playbackJob?.cancel()
        playbackJob = null
        queue.clear()

        try {
            audioTrack?.stop()
            audioTrack?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioTrack", e)
        }
        audioFocusHelper.abandonFocus()
        onSpeakingState(false, 0f)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.clearCommunicationDevice()
            }
            audioManager.mode = AudioManager.MODE_NORMAL
        } catch (e: Exception) {
            // Ignore
        }
        LiveLogManager.info("AudioTrack", "AudioTrack stopped cleanly")
    }

    fun release() {
        stop()
        try {
            audioTrack?.release()
        } catch (e: Exception) {
            // Ignore
        }
        audioTrack = null
    }

    private fun calculateRms(bytes: ByteArray): Float {
        var sum = 0.0
        val sampleCount = bytes.size / 2
        if (sampleCount == 0) return 0f

        for (i in 0 until bytes.size step 2) {
            val sample = (bytes[i].toInt() and 0xFF) or (bytes[i + 1].toInt() shl 8)
            val shortSample = sample.toShort().toFloat()
            sum += shortSample * shortSample
        }
        val rms = sqrt(sum / sampleCount)
        val normalized = (rms / 32767.0).toFloat().coerceIn(0f, 1f)
        return (normalized * 2.5f).coerceIn(0f, 1f)
    }
}
