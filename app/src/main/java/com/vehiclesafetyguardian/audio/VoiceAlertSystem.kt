package com.vehiclesafetyguardian.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.content.ContextCompat
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceAlertSystem @Inject constructor(
    private val context: Context
) : TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private val soundPool: SoundPool
    private val soundIds = mutableMapOf<AlertType, Int>()
    private val messageQueue = ConcurrentLinkedQueue<String>()
    private var isInitialized = false
    private var isSpeaking = false

    init {
        // Initialize SoundPool for pre-recorded alerts
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        // Initialize TTS engine
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.getDefault()
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeaking = true
                }

                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                    processNextMessage()
                }

                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                    processNextMessage()
                }
            })
            isInitialized = true
            loadAlertSounds()
        }
    }

    private fun loadAlertSounds() {
        soundIds[AlertType.SPEED_LIMIT] = soundPool.load(context, R.raw.alert_speed_limit, 1)
        soundIds[AlertType.LANE_DEPARTURE] = soundPool.load(context, R.raw.alert_lane_departure, 1)
        soundIds[AlertType.COLLISION_WARNING] = soundPool.load(context, R.raw.alert_collision, 1)
    }

    fun speak(message: String, alertType: AlertType? = null, isUrgent: Boolean = false) {
        if (!isInitialized) {
            messageQueue.add(message)
            return
        }

        // Play sound effect if available
        alertType?.let { type ->
            soundIds[type]?.let { soundId ->
                soundPool.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f)
            }
        }

        // Queue or speak immediately based on priority
        if (isSpeaking && !isUrgent) {
            messageQueue.add(message)
        } else {
            speakImmediately(message, isUrgent)
        }
    }

    private fun speakImmediately(message: String, isUrgent: Boolean) {
        val params = HashMap<String, String>()
        params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = System.currentTimeMillis().toString()
        params[TextToSpeech.Engine.KEY_PARAM_STREAM] = AudioManager.STREAM_NOTIFICATION.toString()

        if (isUrgent) {
            params[TextToSpeech.Engine.KEY_PARAM_VOLUME] = "1.0"
            tts.setSpeechRate(1.1f)
        } else {
            params[TextToSpeech.Engine.KEY_PARAM_VOLUME] = "0.8"
            tts.setSpeechRate(0.9f)
        }

        tts.speak(message, TextToSpeech.QUEUE_FLUSH, params)
    }

    private fun processNextMessage() {
        if (messageQueue.isNotEmpty()) {
            speakImmediately(messageQueue.poll(), false)
        }
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
        soundPool.release()
    }
}

enum class AlertType {
    SPEED_LIMIT,
    LANE_DEPARTURE,
    COLLISION_WARNING,
    GENERAL_WARNING
}