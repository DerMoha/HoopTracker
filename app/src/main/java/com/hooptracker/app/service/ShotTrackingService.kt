package com.hooptracker.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import com.hooptracker.app.R
import com.hooptracker.app.data.ShotRepository
import com.hooptracker.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale

class ShotTrackingService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var speechRecognizer: SpeechRecognizer? = null
    private var repository: ShotRepository? = null
    private var isListening = false
    private var totalHits = 0
    private var totalMisses = 0

    companion object {
        private const val CHANNEL_ID = "shot_tracking_channel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_STOP_SERVICE = "com.hooptracker.app.STOP_SERVICE"
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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopTracking()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }

    fun setRepository(repo: ShotRepository) {
        this.repository = repo
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

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Don't play audio prompts to avoid interfering with music
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000)
            putExtra("android.speech.extra.DICTATION_MODE", true)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            // Retry after a delay if failed
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (isListening) startListening()
            }, 1000)
        }
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: android.os.Bundle?) {
            // Ready to listen
        }

        override fun onBeginningOfSpeech() {
            // User started speaking
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Volume level changed
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            // Audio buffer received
        }

        override fun onEndOfSpeech() {
            // User stopped speaking
        }

        override fun onError(error: Int) {
            // Restart listening on error to keep service running
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (isListening) startListening()
            }, 500)
        }

        override fun onResults(results: android.os.Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.firstOrNull()?.let { result ->
                processVoiceCommand(result.lowercase(Locale.getDefault()))
            }

            // Continue listening
            if (isListening) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    startListening()
                }, 300)
            }
        }

        override fun onPartialResults(partialResults: android.os.Bundle?) {
            // Partial results available
        }

        override fun onEvent(eventType: Int, params: android.os.Bundle?) {
            // Speech event
        }
    }

    private fun processVoiceCommand(command: String) {
        val repo = repository ?: return

        serviceScope.launch(Dispatchers.IO) {
            when {
                command.contains("hit") || command.contains("make") || command.contains("made") -> {
                    repo.recordHit()
                    totalHits++
                    launch(Dispatchers.Main) { updateNotification() }
                }
                command.contains("miss") || command.contains("missed") -> {
                    repo.recordMiss()
                    totalMisses++
                    launch(Dispatchers.Main) { updateNotification() }
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
                setSound(null, null) // Silent notification to not interfere with music
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

        val total = totalHits + totalMisses
        val percentage = if (total > 0) (totalHits * 100 / total) else 0

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🏀 HoopTracker Active")
            .setContentText("Session: $totalHits/$total ($percentage%) • Say 'hit' or 'miss'")
            .setSmallIcon(R.drawable.ic_basketball)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setSilent(true) // Silent to not interfere with music
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
        serviceScope.cancel()
    }
}
