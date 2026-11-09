package com.example.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

/**
 * Delivers a timer/Pomodoro completion alert even when Android has killed the app process.
 * The action button stops the continuous alarm audio without requiring the app to stay open.
 */
class ClockTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_STOP) {
            AlarmSoundManager.stopPlayback(context)
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.cancel(NOTIFICATION_ID)
            return
        }

        val prefs = context.getSharedPreferences("prime_focus_prefs", Context.MODE_PRIVATE)
        val ringtoneId = prefs.getString("clock_default_ringtone_id", "zen_crystal") ?: "zen_crystal"
        val ringtoneUri = prefs.getString("clock_default_ringtone_uri", null)
        val vibration = prefs.getBoolean("clock_alarm_vibration", true)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Timer complete"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Your session has finished."

        AlarmSoundManager.startAlarmRinging(context, ringtoneId, ringtoneUri, vibration)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Timer alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Countdown and Pomodoro completion alerts"
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
            )
        }

        val openApp = PendingIntent.getActivity(
            context, 4208, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getBroadcast(
            context, 4209, Intent(context, ClockTimerReceiver::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.example.R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(openApp)
            .addAction(android.R.drawable.ic_media_pause, "Stop sound", stopIntent)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "prime_clock_timer_alerts"
        private const val NOTIFICATION_ID = 4207
        private const val REQUEST_CODE = 4206
        private const val ACTION_FIRE = "com.example.action.CLOCK_TIMER_FIRE"
        private const val ACTION_STOP = "com.example.action.CLOCK_TIMER_STOP"
        private const val EXTRA_TITLE = "clock_timer_title"
        private const val EXTRA_MESSAGE = "clock_timer_message"

        fun schedule(context: Context, delayMillis: Long, title: String, message: String) {
            cancel(context)
            if (delayMillis <= 0L) return
            val intent = Intent(context, ClockTimerReceiver::class.java).apply {
                action = ACTION_FIRE
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_MESSAGE, message)
            }
            val pending = PendingIntent.getBroadcast(
                context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val triggerAt = System.currentTimeMillis() + delayMillis
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                }
            } catch (_: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                }
            }
        }

        fun cancel(context: Context) {
            val intent = Intent(context, ClockTimerReceiver::class.java).setAction(ACTION_FIRE)
            val pending = PendingIntent.getBroadcast(
                context, REQUEST_CODE, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            ) ?: return
            (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.cancel(pending)
            pending.cancel()
        }
    }
}
