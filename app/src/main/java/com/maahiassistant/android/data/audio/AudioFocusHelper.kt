package com.maahiassistant.android.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

class AudioFocusHelper(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private var hasFocus = false

    fun requestFocus(onLoss: () -> Unit = {}) {
        if (hasFocus) return

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes)
            .setAcceptsDelayedFocusGain(false)
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        hasFocus = false
                        onLoss()
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        hasFocus = true
                    }
                }
            }
            .build()

        val res = audioManager.requestAudioFocus(focusRequest!!)
        hasFocus = (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
    }

    fun abandonFocus() {
        if (!hasFocus) return
        focusRequest?.let {
            audioManager.abandonAudioFocusRequest(it)
        }
        hasFocus = false
    }
}
