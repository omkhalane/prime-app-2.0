package com.example.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity

/** Keeps timer-completion audio alive after the broadcast receiver returns. */
class ClockTimerAlertService : Service() {
    private var ownsPlayback = false
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY can restart a killed service with a null Intent. Do not
        // interpret that as a new timer completion or start a phantom alarm.
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent.action == ACTION_STOP) {
            if (ownsPlayback) AlarmSoundManager.stopPlayback(this)
            ownsPlayback = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val prefs = getSharedPreferences("prime_focus_prefs", Context.MODE_PRIVATE)
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Timer complete"
        val message = intent?.getStringExtra(EXTRA_MESSAGE) ?: "Your session has finished."
        val ringtoneId = prefs.getString("clock_default_ringtone_id", "zen_crystal") ?: "zen_crystal"
        val ringtoneUri = prefs.getString("clock_default_ringtone_uri", null)
        val vibration = prefs.getBoolean("clock_alarm_vibration", true)
        val soundEnabled = intent?.getBooleanExtra(EXTRA_SOUND_ENABLED, true) ?: true

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Timer alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Countdown and Pomodoro completion alerts"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setSound(null, null)
                    enableVibration(false)
                }
            )
        }

        val openApp = PendingIntent.getActivity(
            this, 4208,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 4209,
            Intent(this, ClockTimerAlertService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(soundEnabled)
            .setAutoCancel(!soundEnabled)
            .setContentIntent(openApp)
            .apply {
                if (soundEnabled) addAction(android.R.drawable.ic_media_pause, "Stop sound", stopIntent)
            }
            .build()

        startForeground(NOTIFICATION_ID, notification)
        if (soundEnabled) {
            AlarmSoundManager.startAlarmRinging(this, ringtoneId, ringtoneUri, vibration)
            ownsPlayback = true
            return START_STICKY
        } else {
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
            return START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        if (ownsPlayback) AlarmSoundManager.stopPlayback(this)
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "prime_clock_timer_alerts"
        private const val NOTIFICATION_ID = 4207
        const val ACTION_STOP = "com.example.action.CLOCK_TIMER_STOP"
        const val EXTRA_TITLE = "clock_timer_title"
        const val EXTRA_MESSAGE = "clock_timer_message"
        const val EXTRA_SOUND_ENABLED = "clock_timer_sound_enabled"
    }
}
