package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.model.AlarmItem
import com.example.model.AlarmRecurrence
import java.util.Calendar

object AlarmSchedulerHelper {

    const val EXTRA_ALARM_ID = "extra_alarm_id"
    const val EXTRA_ALARM_NAME = "extra_alarm_name"
    const val EXTRA_ALARM_MODE = "extra_alarm_mode"
    const val EXTRA_ALARM_CHALLENGE_CAT = "extra_alarm_challenge_cat"
    const val EXTRA_ALARM_CHALLENGE_DIFF = "extra_alarm_challenge_diff"
    const val EXTRA_ALARM_CHALLENGE_COUNT = "extra_alarm_challenge_count"
    const val EXTRA_ALARM_RINGTONE_ID = "extra_alarm_ringtone_id"
    const val EXTRA_ALARM_RINGTONE_URI = "extra_alarm_ringtone_uri"
    const val EXTRA_ALARM_VIBRATION = "extra_alarm_vibration"
    const val EXTRA_ALARM_SNOOZE_MINUTES = "extra_alarm_snooze_minutes"

    /**
     * Compute next trigger timestamp in milliseconds for an alarm.
     * Returns null if one-time and in the past with specific date.
     */
    fun calculateNextTriggerMillis(alarm: AlarmItem, nowMillis: Long = System.currentTimeMillis()): Long? {
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }

        if (alarm.recurrence == AlarmRecurrence.ONCE && alarm.specificDateMillis != null) {
            val target = Calendar.getInstance().apply {
                timeInMillis = alarm.specificDateMillis
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return if (target.timeInMillis > nowMillis) target.timeInMillis else null
        }

        // Days active for recurrence:
        val activeDays: Set<Int> = when (alarm.recurrence) {
            AlarmRecurrence.ONCE -> emptySet()
            AlarmRecurrence.DAILY -> setOf(1, 2, 3, 4, 5, 6, 7) // Mon..Sun
            AlarmRecurrence.WEEKDAYS -> setOf(1, 2, 3, 4, 5) // Mon..Fri
            AlarmRecurrence.WEEKENDS -> setOf(6, 7) // Sat, Sun
            AlarmRecurrence.CUSTOM -> alarm.selectedDays
        }

        if (alarm.recurrence == AlarmRecurrence.ONCE) {
            val candidate = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (candidate.timeInMillis <= nowMillis) candidate.add(Calendar.DAY_OF_YEAR, 1)
            return candidate.timeInMillis
        }
        // A custom alarm with no valid selected days is disabled, not a daily alarm.
        if (activeDays.none { it in 1..7 }) return null

        // For recurring, check next 7 days
        for (offset in 0..7) {
            val candidate = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Convert Calendar.DAY_OF_WEEK (Sun=1, Mon=2..Sat=7) to 1=Mon..7=Sun
            val calDow = candidate.get(Calendar.DAY_OF_WEEK)
            val customDow = if (calDow == Calendar.SUNDAY) 7 else calDow - 1

            if (activeDays.contains(customDow)) {
                if (candidate.timeInMillis > nowMillis) {
                    return candidate.timeInMillis
                }
            }
        }

        return null
    }

    /**
     * Human-friendly formatted time remaining until alarm (e.g. "Rings in 6 hr 15 min" or "Rings tomorrow, 7:00 AM")
     */
    fun formatRemainingTime(triggerMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val diff = triggerMillis - nowMillis
        if (diff <= 0) return "Ringing now"
        val totalMinutes = diff / (1000 * 60)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours == 0L && minutes == 0L -> "Rings in less than a minute"
            hours == 0L -> "Rings in $minutes min"
            hours < 24L -> if (minutes > 0) "Rings in $hours hr $minutes min" else "Rings in $hours hr"
            else -> {
                val days = hours / 24
                val remHours = hours % 24
                "Rings in $days d $remHours hr"
            }
        }
    }

    fun scheduleAlarm(context: Context, alarm: AlarmItem) {
        if (!alarm.isEnabled) {
            cancelAlarm(context, alarm.id)
            return
        }

        val triggerAtMillis = calculateNextTriggerMillis(alarm) ?: run {
            cancelAlarm(context, alarm.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = createAlarmPendingIntent(context, alarm)

        try {
            val showIntent = Intent(context, com.example.MainActivity::class.java)
            val showPendingIntent = PendingIntent.getActivity(
                context,
                alarm.id.hashCode() + 1000,
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val info = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
            alarmManager.setAlarmClock(info, pendingIntent)
        } catch (_: SecurityException) {
            // Fallback if setAlarmClock lacks exact alarm permission
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } catch (_: Throwable) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: Throwable) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun scheduleSnooze(context: Context, alarmId: String, name: String, snoozeMinutes: Int) {
        val triggerAtMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_NAME, "$name (Snoozed)")
            putExtra(EXTRA_ALARM_SNOOZE_MINUTES, snoozeMinutes)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ("snooze_$alarmId").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val showIntent = Intent(context, com.example.MainActivity::class.java)
            val showPendingIntent = PendingIntent.getActivity(
                context,
                alarmId.hashCode() + 2000,
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent), pendingIntent)
        } catch (_: Throwable) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelAlarm(context: Context, alarmId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun createAlarmPendingIntent(context: Context, alarm: AlarmItem): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_ALARM_NAME, alarm.name)
            putExtra(EXTRA_ALARM_MODE, alarm.alarmMode.name)
            putExtra(EXTRA_ALARM_CHALLENGE_CAT, alarm.challengeConfig.category.name)
            putExtra(EXTRA_ALARM_CHALLENGE_DIFF, alarm.challengeConfig.difficulty.name)
            putExtra(EXTRA_ALARM_CHALLENGE_COUNT, alarm.challengeConfig.requiredConsecutive)
            putExtra(EXTRA_ALARM_RINGTONE_ID, alarm.ringtoneId)
            putExtra(EXTRA_ALARM_RINGTONE_URI, alarm.ringtoneUriString)
            putExtra(EXTRA_ALARM_VIBRATION, alarm.vibrationEnabled)
            putExtra(EXTRA_ALARM_SNOOZE_MINUTES, alarm.snoozeDurationMinutes)
        }

        return PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
