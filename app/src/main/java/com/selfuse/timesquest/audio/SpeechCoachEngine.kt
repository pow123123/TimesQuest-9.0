package com.selfuse.timesquest.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Offline Android TextToSpeech coach for Chinese multiplication rhymes ("三七二十一！").
 * Gracefully no-ops if Chinese TTS voice data is unavailable on the device.
 */
class SpeechCoachEngine(context: Context) {
    private var tts: TextToSpeech? = null
    @Volatile
    private var ready = false

    init {
        val appContext = context.applicationContext
        try {
            tts = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val engine = tts ?: return@TextToSpeech
                    val res = engine.setLanguage(Locale.CHINA)
                    if (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED) {
                        engine.setPitch(1.08f)
                        ready = true
                    }
                }
            }
        } catch (_: Exception) {
            ready = false
        }
    }

    fun speakQuestion(text: String, enabled: Boolean) {
        if (!enabled || !ready) return
        try {
            tts?.setSpeechRate(1.0f)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "q_${System.currentTimeMillis()}")
        } catch (_: Exception) {
        }
    }

    fun speakRhymeCelebration(rhyme: String, enabled: Boolean) {
        if (!enabled || !ready) return
        try {
            tts?.setSpeechRate(1.12f)
            tts?.speak(rhyme, TextToSpeech.QUEUE_FLUSH, null, "r_${System.currentTimeMillis()}")
        } catch (_: Exception) {
        }
    }

    fun speakSlowCorrection(rhyme: String, enabled: Boolean) {
        if (!enabled || !ready) return
        try {
            tts?.setSpeechRate(0.84f)
            tts?.speak(rhyme, TextToSpeech.QUEUE_FLUSH, null, "c_${System.currentTimeMillis()}")
        } catch (_: Exception) {
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
        tts = null
        ready = false
    }
}
