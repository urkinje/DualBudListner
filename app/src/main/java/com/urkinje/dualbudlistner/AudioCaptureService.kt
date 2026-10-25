package com.urkinje.dualbudlistner

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log

class AudioCaptureService : Service() {

    private lateinit var audioManager: AudioManager
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var captureThread: Thread? = null

    companion object {
        private const val TAG = "AudioCaptureService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "AudioCaptureChannel"
        private const val SAMPLE_RATE = 16000
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        startCapture()
        return START_STICKY
    }

    private fun startCapture() {
        if (isRecording) return

        // 1. Force Bluetooth SCO mode for routing
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.startBluetoothSco()
        audioManager.isBluetoothScoOn = true

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize
            )

            audioRecord?.startRecording()
            isRecording = true

            // Read loop (we just discard data for now to prove it works)
            captureThread = Thread {
                val buffer = ByteArray(minBufferSize)
                while (isRecording) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        // Audio is being captured!
                        // We can log the routed device to see which bud is active
                        val routed = audioRecord?.routedDevice
                        if (routed != null) {
                            Log.d(TAG, "Currently recording from: ${routed.productName}")
                        }
                    }
                }
            }
            captureThread?.start()
            Log.d(TAG, "Capture started successfully")

        } catch (e: SecurityException) {
            Log.e(TAG, "Permission missing", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting capture", e)
        }
    }

    private fun stopCapture() {
        isRecording = false
        captureThread?.join()
        captureThread = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        audioManager.stopBluetoothSco()
        audioManager.isBluetoothScoOn = false
        audioManager.mode = AudioManager.MODE_NORMAL
    }

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Audio Capture",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("DualBud Listener")
                .setContentText("Capturing audio...")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("DualBud Listener")
                .setContentText("Capturing audio...")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .build()
        }
    }
}
