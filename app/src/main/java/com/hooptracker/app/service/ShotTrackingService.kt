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
import com.hooptracker.app.HoopTrackerApplication
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.Locale

class ShotTrackingService : Service() {

    enum class StartTrackingResult {
        STARTED,
        ALREADY_RUNNING,
        UNAVAILABLE
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var repository: ShotRepository
    private lateinit var preferences: Preferences
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
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

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        val app = application as HoopTrackerApplication
        repository = app.repository
        preferences = app.preferences
        currentSessionId = preferences.currentSessionId
        createNotificationChannel()
        initializeVibrator()
        initializeTTS()
        refreshSessionStats()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                runBlocking(Dispatchers.IO) {
                    currentSessionId?.let { repository.endSession(it) }
                    currentSessionId = null
                    preferences.currentSessionId = null
                }
                stopTracking()
                return START_NOT_STICKY
            }

            ACTION_UNDO_SHOT -> {
                undoLastShot()
                return START_STICKY
            }
        }

        startForeground(NOTIFICATION_ID, createNotification())

        if (preferences.trackingActive && !isListening) {
            startTracking()
        }

        return if (preferences.trackingActive) START_STICKY else START_NOT_STICKY
    }

    fun setSessionId(sessionId: Long?) {
        currentSessionId = sessionId
        preferences.currentSessionId = sessionId
        refreshSessionStats()
    }

    fun setShotType(shotType: ShotType) {
        currentShotType = shotType
        updateNotification()
    }

    fun isTrackingActive(): Boolean = isListening

    fun startTracking(): StartTrackingResult {
        if (isListening) {
            return StartTrackingResult.ALREADY_RUNNING
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            preferences.trackingActive = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return StartTrackingResult.UNAVAILABLE
        }

        destroyRecognizer()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(createRecognitionListener())
        }

        isListening = true
        preferences.trackingActive = true
        refreshSessionStats()
        startListening()
        updateNotification()
        return StartTrackingResult.STARTED
    }

    fun stopTracking() {
        isListening = false
        preferences.trackingActive = false
        destroyRecognizer()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun refreshSessionStats() {
        serviceScope.launch(Dispatchers.IO) {
            syncSessionStats()
            withContext(Dispatchers.Main) {
                updateNotification()
            }
        }
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
        } catch (_: Exception) {
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

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            isRecognizerReady = true

            if (error == SpeechRecognizer.ERROR_CLIENT) {
                return
            }

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
                    return@forEach
                }
            }

            if (isListening) {
                serviceScope.launch {
                    delay(300)
                    startListening()
                }
            }
        }

        override fun onPartialResults(partialResults: android.os.Bundle?) = Unit

        override fun onEvent(eventType: Int, params: android.os.Bundle?) = Unit
    }

    private fun processVoiceCommand(command: String): Boolean {
        var processed = false
        var isHit = false
        var detectedShotType = currentShotType

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

        when {
            command.contains("hit") || command.contains("make") ||
                command.contains("made") || command.contains("good") ||
                command.contains("in") -> {
                isHit = true
                processed = true
            }

            command.contains("miss") || command.contains("missed") ||
                command.contains("no good") || command.contains("brick") -> {
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
                    repository.recordHit(currentSessionId, detectedShotType)
                } else {
                    repository.recordMiss(currentSessionId, detectedShotType)
                }

                syncSessionStats(recordedShotWasHit = isHit)

                withContext(Dispatchers.Main) {
                    provideFeedback(isHit)
                    updateNotification()
                }
            }
        }

        return processed
    }

    private suspend fun syncSessionStats(recordedShotWasHit: Boolean? = null) {
        val sessionId = currentSessionId
        if (sessionId != null) {
            val sessionStats = repository.getSessionStats(sessionId).stats
            totalHits = sessionStats.hits
            totalMisses = sessionStats.misses
            return
        }

        when (recordedShotWasHit) {
            true -> totalHits++
            false -> totalMisses++
            null -> Unit
        }
    }

    private fun provideFeedback(isHit: Boolean) {
        if (preferences.hapticFeedbackEnabled) {
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

        if (preferences.voiceFeedbackEnabled) {
            textToSpeech?.speak(
                if (isHit) getString(R.string.made_result) else getString(R.string.miss_result),
                TextToSpeech.QUEUE_FLUSH,
                null,
                null
            )
        }
    }

    private fun provideUndoFeedback() {
        if (preferences.hapticFeedbackEnabled) {
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(60)
                }
            }
        }

        if (preferences.voiceFeedbackEnabled) {
            textToSpeech?.speak(getString(R.string.voice_feedback_undone), TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun undoLastShot() {
        serviceScope.launch(Dispatchers.IO) {
            val deletedShot = repository.undoLastShot()
            if (deletedShot != null) {
                if (currentSessionId != null) {
                    syncSessionStats()
                } else if (deletedShot.isHit && totalHits > 0) {
                    totalHits--
                } else if (!deletedShot.isHit && totalMisses > 0) {
                    totalMisses--
                }

                withContext(Dispatchers.Main) {
                    provideUndoFeedback()
                    updateNotification()
                }
            }
        }
    }

    private fun destroyRecognizer() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        isRecognizerReady = true
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
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ShotTrackingService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val undoIntent = Intent(this, ShotTrackingService::class.java).apply {
            action = ACTION_UNDO_SHOT
        }
        val undoPendingIntent = PendingIntent.getService(
            this,
            1,
            undoIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val total = totalHits + totalMisses
        val percentage = if (total > 0) (totalHits * 100 / total) else 0

        val shotTypeText = if (currentShotType != ShotType.GENERAL) {
            " [${currentShotType.name.replace("_", " ")}]"
        } else {
            ""
        }

        val contentText = if (total > 0) {
            getString(R.string.tracking_notification_session, totalHits, total, percentage)
        } else {
            getString(R.string.tracking_notification_listening)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.tracking_notification_title) + shotTypeText)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_basketball)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stop, getString(R.string.notification_stop), stopPendingIntent)
            .addAction(0, getString(R.string.notification_undo), undoPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification())
    }

    fun resetSessionStats() {
        totalHits = 0
        totalMisses = 0
        updateNotification()
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyRecognizer()
        textToSpeech?.shutdown()
        serviceScope.cancel()
    }
}
