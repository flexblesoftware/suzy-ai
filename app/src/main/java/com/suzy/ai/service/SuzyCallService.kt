package com.suzy.ai.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.suzy.ai.MainActivity

object CallServiceBridge {
    @Volatile
    var onToggleMuteRequested: (() -> Unit)? = null

    @Volatile
    var onStopCallRequested: (() -> Unit)? = null

    fun startService(context: Context, personaName: String, statusText: String, isMuted: Boolean) {
        val intent = Intent(context, SuzyCallService::class.java).apply {
            action = SuzyCallService.ACTION_START_OR_UPDATE
            putExtra(SuzyCallService.EXTRA_PERSONA, personaName)
            putExtra(SuzyCallService.EXTRA_STATUS, statusText)
            putExtra(SuzyCallService.EXTRA_MUTED, isMuted)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun updateNotification(context: Context, personaName: String, statusText: String, isMuted: Boolean) {
        if (!SuzyCallService.isRunning) return
        val intent = Intent(context, SuzyCallService::class.java).apply {
            action = SuzyCallService.ACTION_START_OR_UPDATE
            putExtra(SuzyCallService.EXTRA_PERSONA, personaName)
            putExtra(SuzyCallService.EXTRA_STATUS, statusText)
            putExtra(SuzyCallService.EXTRA_MUTED, isMuted)
        }
        try {
            context.startService(intent)
        } catch (_: Exception) {
        }
    }

    fun stopService(context: Context) {
        try {
            context.stopService(Intent(context, SuzyCallService::class.java))
        } catch (_: Exception) {
        }
    }
}

class SuzyCallService : Service() {

    companion object {
        const val CHANNEL_ID = "suzy_live_call_channel"
        const val NOTIFICATION_ID = 3801
        const val ACTION_START_OR_UPDATE = "com.suzy.ai.action.START_OR_UPDATE"
        const val ACTION_TOGGLE_MUTE = "com.suzy.ai.action.TOGGLE_MUTE"
        const val ACTION_STOP_CALL = "com.suzy.ai.action.STOP_CALL"
        const val EXTRA_PERSONA = "extra_persona"
        const val EXTRA_STATUS = "extra_status"
        const val EXTRA_MUTED = "extra_muted"

        @Volatile
        var isRunning: Boolean = false
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE_MUTE -> {
                CallServiceBridge.onToggleMuteRequested?.invoke()
                return START_STICKY
            }
            ACTION_STOP_CALL -> {
                CallServiceBridge.onStopCallRequested?.invoke()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                val persona = intent?.getStringExtra(EXTRA_PERSONA) ?: "Suzy"
                val status = intent?.getStringExtra(EXTRA_STATUS) ?: "Live voice call active"
                val isMuted = intent?.getBooleanExtra(EXTRA_MUTED, false) ?: false
                val notification = buildNotification(persona, status, isMuted)
                startForegroundCompat(notification)
            }
        }
        return START_STICKY
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(persona: String, status: String, isMuted: Boolean): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPending = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val muteIntent = Intent(this, SuzyCallService::class.java).apply {
            action = ACTION_TOGGLE_MUTE
        }
        val mutePending = PendingIntent.getService(
            this,
            1,
            muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, SuzyCallService::class.java).apply {
            action = ACTION_STOP_CALL
        }
        val stopPending = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("$persona • Live Call")
            .setContentText(if (isMuted) "Mic Muted • Screen-off mode active" else status)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPending)
            .addAction(
                android.R.drawable.ic_lock_silent_mode,
                if (isMuted) "Unmute" else "Mute",
                mutePending
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "End Call",
                stopPending
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Suzy Live Call",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps live voice conversation running when screen is locked or off"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SuzyAI::CallWakeLock").apply {
                setReferenceCounted(false)
                acquire(6 * 60 * 60 * 1000L) // Up to 6 hours safe timeout
            }
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        isRunning = false
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        }
        wakeLock = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
