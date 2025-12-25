package com.hooptracker.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import com.hooptracker.app.R
import com.hooptracker.app.data.Preferences
import com.hooptracker.app.data.ShotRepository
import com.hooptracker.app.data.ShotType
import com.hooptracker.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class ShotTrackingService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var repository: ShotRepository? = null
    private var preferences: Preferences? = null
    private var vibrator: Vibrator? = null

    private var isListening = false
    private var isRecognizerReady = true
    private var totalHits = 0
    private var totalMisses = 0
    private var currentSessionId: Long? = null
    private var currentShotType: ShotType = ShotType.GENERAL

    companion object {
        private const val CHANNEL_ID = "shot_tracking_channel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_STOP_SERVICE = "com.hooptracker.app.STOP_SERVICE"
        const val ACTION_UNDO_SHOT = "com.hooptracker.app.UNDO_SHOT"

        private const val MAX_RESTART_ATTEMPTS = 3
        private const val RESTART_DELAY_MS = 1000L
    }

    inner class LocalBinder : Binder() {
        fun getService(): ShotTrackingService = this@ShotTrackingService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializeVibrator()
        initializeTTS()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                stopTracking()
                return START_NOT_STICKY
            }
            ACTION_UNDO_SHOT -> {
                undoLastShot()
            }
        }

        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }

    fun setRepository(repo: ShotRepository) {
        this.repository = repo
    }

    fun setPreferences(prefs: Preferences) {
        this.preferences = prefs
    }

    fun setSessionId(sessionId: Long?) {
        this.currentSessionId = sessionId
    }

    fun setShotType(shotType: ShotType) {
        this.currentShotType = shotType
    }

    private fun initializeVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun initializeTTS() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
            }
        }
    }

    fun startTracking() {
        if (isListening) return

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(createRecognitionListener())
        }

        startListening()
        isListening = true
        updateNotification()
    }

    fun stopTracking() {
        isListening = false
        speechRecognizer?.destroy()
        speechRecognizer = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startListening(attempt: Int = 0) {
        if (!isListening || !isRecognizerReady) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000)
            putExtra("android.speech.extra.DICTATION_MODE", true)
        }

        try {
            speechRecognizer?.startListening(intent)
            isRecognizerReady = false
        } catch (e: Exception) {
            if (attempt < MAX_RESTART_ATTEMPTS) {
                serviceScope.launch {
                    delay(RESTART_DELAY_MS * (attempt + 1))
                    if (isListening) startListening(attempt + 1)
                }
            }
        }
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: android.os.Bundle?) {
            isRecognizerReady = true
        }

        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            isRecognizerReady = true

            // Don't restart on client-side errors
            if (error == SpeechRecognizer.ERROR_CLIENT) {
                return
            }

            // Restart listening after a brief delay
            serviceScope.launch {
                delay(500)
                if (isListening) {
                    startListening()
                }
            }
        }

        override fun onResults(results: android.os.Bundle?) {
            isRecognizerReady = true
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

            matches?.forEach { result ->
                if (processVoiceCommand(result.lowercase(Locale.getDefault()))) {
                    // Command was processed, stop checking other results
                    return@forEach
                }
            }

            // Continue listening
            if (isListening) {
                serviceScope.launch {
                    delay(300)
                    startListening()
                }
            }
        }

        override fun onPartialResults(partialResults: android.os.Bundle?) {}
        override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
    }

    private fun processVoiceCommand(command: String): Boolean {
        val repo = repository ?: return false
        var processed = false
        var isHit = false
        var isMiss = false
        var detectedShotType = currentShotType

        // Detect shot type from command
        when {
            command.contains("three") || command.contains("3") -> {
                detectedShotType = ShotType.THREE_POINTER
            }
            command.contains("mid") || command.contains("midrange") -> {
                detectedShotType = ShotType.MID_RANGE
            }
            command.contains("layup") || command.contains("lay up") -> {
                detectedShotType = ShotType.LAYUP
            }
            command.contains("free throw") || command.contains("freethrow") -> {
                detectedShotType = ShotType.FREE_THROW
            }
        }

        // Detect hit/miss
        when {
            command.contains("hit") || command.contains("make") ||
            command.contains("made") || command.contains("good") ||
            command.contains("in") -> {
                isHit = true
                processed = true
            }
            command.contains("miss") || command.contains("missed") ||
            command.contains("no good") || command.contains("brick") -> {
                isMiss = true
                processed = true
            }
            command.contains("undo") || command.contains("cancel") ||
            command.contains("take back") -> {
                undoLastShot()
                return true
            }
        }

        if (processed) {
            serviceScope.launch(Dispatchers.IO) {
                if (isHit) {
                    repo.recordHit(currentSessionId, detectedShotType)
                    totalHits++
                } else if (isMiss) {
                    repo.recordMiss(currentSessionId, detectedShotType)
                    totalMisses++
                }

                launch(Dispatchers.Main) {
                    provideFeedback(isHit)
                    updateNotification()
                }
            }
        }

        return processed
    }

    private fun provideFeedback(isHit: Boolean) {
        // Haptic feedback
        if (preferences?.hapticFeedbackEnabled == true) {
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (isHit) {
                        it.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        it.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 50, 50), -1))
                    }
                } else {
                    @Suppress("DEPRECATION")
                    if (isHit) {
                        it.vibrate(100)
                    } else {
                        it.vibrate(longArrayOf(0, 50, 50, 50), -1)
                    }
                }
            }
        }

        // Voice feedback
        if (preferences?.voiceFeedbackEnabled == true) {
            val message = if (isHit) "Hit" else "Miss"
            textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun undoLastShot() {
        val repo = repository ?: return

        serviceScope.launch(Dispatchers.IO) {
            if (repo.undoLastShot()) {
                // Adjust session stats
                if (totalHits > 0 || totalMisses > 0) {
                    if (totalHits > totalMisses && totalHits > 0) {
                        totalHits--
                    } else if (totalMisses > 0) {
                        totalMisses--
                    }
                }

                launch(Dispatchers.Main) {
                    provideFeedback(false) // Short feedback for undo
                    updateNotification()
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Shot Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks basketball shots via voice"
                setSound(null, null)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val mainIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ShotTrackingService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val undoIntent = Intent(this, ShotTrackingService::class.java).apply {
            action = ACTION_UNDO_SHOT
        }
        val undoPendingIntent = PendingIntent.getService(
            this, 1, undoIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val total = totalHits + totalMisses
        val percentage = if (total > 0) (totalHits * 100 / total) else 0

        val shotTypeText = if (currentShotType != ShotType.GENERAL) {
            " [${currentShotType.name.replace("_", " ")}]"
        } else ""

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🏀 HoopTracker Active$shotTypeText")
            .setContentText("Session: $totalHits/$total ($percentage%) • Say 'hit' or 'miss'")
            .setSmallIcon(R.drawable.ic_basketball)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            .addAction(0, "Undo", undoPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        val notification = createNotification()
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    fun resetSessionStats() {
        totalHits = 0
        totalMisses = 0
        updateNotification()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        textToSpeech?.shutdown()
        serviceScope.cancel()
    }
}
