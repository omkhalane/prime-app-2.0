package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.OpalRepository

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val repository = OpalRepository(context)
            val enabledAlarms = repository.alarmsFlow.value.filter { it.isEnabled }
            for (alarm in enabledAlarms) {
                AlarmSchedulerHelper.scheduleAlarm(context, alarm)
            }
            val timer = repository.getSavedStandardTimerState()
            if (timer.third && !repository.getSavedStandardTimerPaused()) {
                ClockTimerReceiver.schedule(
                    context,
                    (timer.first.coerceAtLeast(1)) * 1000L,
                    "Focus timer complete",
                    "Your focus session is finished. Take a mindful break."
                )
            }
            val pomodoro = repository.pomodoroStateFlow.value
            if (pomodoro.isRunning && !pomodoro.isPaused) {
                ClockTimerReceiver.schedule(
                    context,
                    pomodoro.remainingSeconds.coerceAtLeast(1) * 1000L,
                    "Pomodoro session complete",
                    "Your ${pomodoro.sessionType.displayName.lowercase()} session is finished.",
                    repository.clockPreferencesFlow.value.sessionSoundsEnabled
                )
            }
            com.example.vpn.service.BlockingRecoveryManager.reconcileOnBoot(context)
        }
    }
}
