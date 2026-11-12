package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.ui.screens.AlarmRingingActivity

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(AlarmSchedulerHelper.EXTRA_ALARM_ID) ?: "default_alarm"
        val alarmName = intent.getStringExtra(AlarmSchedulerHelper.EXTRA_ALARM_NAME) ?: "Alarm"
        val alarmMode = intent.getStringExtra(AlarmSchedulerHelper.EXTRA_ALARM_MODE) ?: "NORMAL"
        val challengeCat = intent.getStringExtra(AlarmSchedulerHelper.EXTRA_ALARM_CHALLENGE_CAT) ?: "MATH"
        val challengeDiff = intent.getStringExtra(AlarmSchedulerHelper.EXTRA_ALARM_CHALLENGE_DIFF) ?: "MEDIUM"
        val challengeCount = intent.getIntExtra(AlarmSchedulerHelper.EXTRA_ALARM_CHALLENGE_COUNT, 3)
        val ringtoneId = intent.getStringExtra(AlarmSchedulerHelper.EXTRA_ALARM_RINGTONE_ID) ?: "zen_crystal"
        val ringtoneUri = intent.getStringExtra(AlarmSchedulerHelper.EXTRA_ALARM_RINGTONE_URI)
        val vibration = intent.getBooleanExtra(AlarmSchedulerHelper.EXTRA_ALARM_VIBRATION, true)
        val snoozeMin = intent.getIntExtra(AlarmSchedulerHelper.EXTRA_ALARM_SNOOZE_MINUTES, 10)

        // Keep the CPU awake briefly while alarm audio and the full-screen notification start
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "prime:AlarmWakeLock"
        )
        wakeLock?.acquire(30_000L)

        // Start ringing sound & vibration
        AlarmSoundManager.startAlarmRinging(context, ringtoneId, ringtoneUri, vibration)

        // Prepare full-screen intent activity
        val ringIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmSchedulerHelper.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmSchedulerHelper.EXTRA_ALARM_NAME, alarmName)
            putExtra(AlarmSchedulerHelper.EXTRA_ALARM_MODE, alarmMode)
            putExtra(AlarmSchedulerHelper.EXTRA_ALARM_CHALLENGE_CAT, challengeCat)
            putExtra(AlarmSchedulerHelper.EXTRA_ALARM_CHALLENGE_DIFF, challengeDiff)
            putExtra(AlarmSchedulerHelper.EXTRA_ALARM_CHALLENGE_COUNT, challengeCount)
            putExtra(AlarmSchedulerHelper.EXTRA_ALARM_SNOOZE_MINUTES, snoozeMin)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            alarmId.hashCode() + 5000,
            ringIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Post high-priority notification with full-screen intent
        val channelId = "prime_alarm_channel_high"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Prime Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical Alarm Notifications"
                setBypassDnd(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(alarmName)
            .setContentText("Tap to dismiss or snooze")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setAutoCancel(true)
            .setOngoing(true)
            .build()

        notificationManager.notify(alarmId.hashCode(), notification)

        // Also launch activity directly
        try {
            context.startActivity(ringIntent)
        } catch (_: Throwable) { }

        // If it's a one-time alarm, disable it in repository
        val repo = com.example.data.OpalRepository(context, loadAppsOnInit = false)
        repo.onAlarmTriggered(alarmId)
    }
}
