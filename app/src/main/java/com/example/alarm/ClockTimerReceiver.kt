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
        const val TIMER_STANDARD = "standard"
        const val TIMER_POMODORO = "pomodoro"
        private const val ACTION_FIRE_STANDARD = "com.example.action.CLOCK_TIMER_FIRE_STANDARD"
        private const val ACTION_FIRE_POMODORO = "com.example.action.CLOCK_TIMER_FIRE_POMODORO"
        private const val REQUEST_CODE_STANDARD = 4206
        private const val REQUEST_CODE_POMODORO = 4207

        private fun requestCode(timerType: String): Int = when (timerType) {
            TIMER_STANDARD -> REQUEST_CODE_STANDARD
            TIMER_POMODORO -> REQUEST_CODE_POMODORO
            else -> throw IllegalArgumentException("Unsupported timer type: $timerType")
        }

        private fun action(timerType: String): String = when (timerType) {
            TIMER_STANDARD -> ACTION_FIRE_STANDARD
            TIMER_POMODORO -> ACTION_FIRE_POMODORO
            else -> throw IllegalArgumentException("Unsupported timer type: $timerType")
        }

        fun schedule(context: Context, delayMillis: Long, title: String, message: String,
                     soundEnabled: Boolean = true, timerType: String = TIMER_STANDARD) {
            cancel(context, timerType)
            if (delayMillis <= 0L) return
            val intent = Intent(context, ClockTimerReceiver::class.java).apply {
                action = action(timerType)
                putExtra(ClockTimerAlertService.EXTRA_TITLE, title)
                putExtra(ClockTimerAlertService.EXTRA_MESSAGE, message)
                putExtra(ClockTimerAlertService.EXTRA_SOUND_ENABLED, soundEnabled)
            }
            val pending = PendingIntent.getBroadcast(context, requestCode(timerType), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val triggerAt = System.currentTimeMillis() + delayMillis
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                }
            } catch (_: SecurityException) {
                // Best-effort fallback; the app must still deliver a completion notification.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                }
            }
        }

        fun cancel(context: Context, timerType: String = TIMER_STANDARD) {
            val intent = Intent(context, ClockTimerReceiver::class.java).setAction(action(timerType))
            val pending = PendingIntent.getBroadcast(context, requestCode(timerType), intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) ?: return
            (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.cancel(pending)
            pending.cancel()
        }
    }
}
