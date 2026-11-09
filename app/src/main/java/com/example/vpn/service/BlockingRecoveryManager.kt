package com.example.vpn.service

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object BlockingRecoveryManager {

    private const val WORK_NAME = "prime_website_block_reconcile"

    fun reconcileOnBoot(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val manager = BlockingManager.getInstance(context)
            manager.reconcileActiveRules()
        }
        enqueuePeriodicReconciliation(context)
    }

    fun enqueuePeriodicReconciliation(context: Context) {
        try {
            val request = PeriodicWorkRequestBuilder<WebsiteBlockingWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        } catch (_: Exception) {}
    }
}
