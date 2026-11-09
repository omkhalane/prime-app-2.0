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
            com.example.vpn.service.BlockingRecoveryManager.reconcileOnBoot(context)
        }
    }
}
