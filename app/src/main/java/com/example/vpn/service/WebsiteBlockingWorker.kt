package com.example.vpn.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class WebsiteBlockingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val manager = BlockingManager.getInstance(applicationContext)
            manager.reconcileActiveRules()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
