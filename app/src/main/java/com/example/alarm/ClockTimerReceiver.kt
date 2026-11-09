package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat

/** Delivers countdown/Pomodoro completion alerts even if Android killed the app process. */
class ClockTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, ClockTimerAlertService::class.java).apply {
            putExtra(ClockTimerAlertService.EXTRA_TITLE, intent.getStringExtra(ClockTimerAlertService.EXTRA_TITLE) ?: "Timer complete")
            putExtra(ClockTimerAlertService.EXTRA_MESSAGE, intent.getStringExtra(ClockTimerAlertService.EXTRA_MESSAGE) ?: "Your session has finished.")
            putExtra(ClockTimerAlertService.EXTRA_SOUND_ENABLED, intent.getBooleanExtra(ClockTimerAlertService.EXTRA_SOUND_ENABLED, true))
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    companion object {
        private const val ACTION_FIRE = "com.example.action.CLOCK_TIMER_FIRE"
        private const val REQUEST_CODE = 4206

        fun schedule(context: Context, delayMillis: Long, title: String, message: String, soundEnabled: Boolean = true) {
            cancel(context)
            if (delayMillis <= 0L) return
            val intent = Intent(context, ClockTimerReceiver::class.java).apply {
                action = ACTION_FIRE
                putExtra(ClockTimerAlertService.EXTRA_TITLE, title)
                putExtra(ClockTimerAlertService.EXTRA_MESSAGE, message)
                putExtra(ClockTimerAlertService.EXTRA_SOUND_ENABLED, soundEnabled)
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
