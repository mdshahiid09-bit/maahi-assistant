package com.maahiassistant.android.data.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.maahiassistant.android.data.local.LiveLogManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

class AudioCaptureManager(
    private val onAudioChunk: (ByteArray, Int, Int) -> Unit,
    private val onAudioLevel: (Float) -> Unit,
    private val onError: (String) -> Unit
) {

    companion object {
        private const val TAG = "AudioCaptureMgr"
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        // 50ms chunk = 16000 * 0.05 * 2 bytes = 1600 bytes (800 shorts)
        const val CHUNK_SIZE_BYTES = 1600
    }

    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null
    private val isRecording = AtomicBoolean(false)
    val isMuted = AtomicBoolean(false)

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope) {
        if (isRecording.get()) return

        val minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (minBufSize <= 0) {
            onError("Unsupported audio hardware settings")
            return
        }

        val bufferSize = maxOf(minBufSize * 2, CHUNK_SIZE_BYTES * 4)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                val err = "Failed to initialize microphone hardware"
                LiveLogManager.error("AudioRecord", err)
                onError(err)
                release()
                return
            }

            audioRecord?.startRecording()
            isRecording.set(true)
            LiveLogManager.info("AudioRecord", "Microphone recording started (16000Hz PCM Mono)")

            captureJob = scope.launch(Dispatchers.IO) {
                val buffer = ByteArray(CHUNK_SIZE_BYTES)
                while (isActive && isRecording.get()) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (read > 0) {
                        val level = calculateRms(buffer, read)
                        onAudioLevel(level)

                        if (!isMuted.get()) {
                            onAudioChunk(buffer, 0, read)
                        }
                    } else if (read < 0) {
                        Log.e(TAG, "AudioRecord read error: $read")
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "AudioRecord start error", e)
            onError(e.message ?: "Microphone recording failure")
            release()
        }
    }

    fun setMute(muted: Boolean) {
        isMuted.set(muted)
        LiveLogManager.info("AudioRecord", "Microphone mute toggled: $muted")
    }

    fun stop() {
        if (!isRecording.get()) return
        isRecording.set(false)
        captureJob?.cancel()
        captureJob = null
        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        }
        LiveLogManager.info("AudioRecord", "Microphone recording stopped cleanly")
    }
    }

    fun release() {
        stop()
        try {
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore
        }
        audioRecord = null
    }

    private fun calculateRms(bytes: ByteArray, length: Int): Float {
        var sum = 0.0
        val sampleCount = length / 2
        if (sampleCount == 0) return 0f

        for (i in 0 until length step 2) {
            val sample = (bytes[i].toInt() and 0xFF) or (bytes[i + 1].toInt() shl 8)
            val shortSample = sample.toShort().toFloat()
            sum += shortSample * shortSample
        }
        val rms = sqrt(sum / sampleCount)
        // Normalize 0..32767 to 0f..1f with non-linear scaling for nice visual orb responsiveness
        val normalized = (rms / 32767.0).toFloat().coerceIn(0f, 1f)
        return (normalized * 2.5f).coerceIn(0f, 1f)
    }
}
