package com.example.tasktracker.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.tasktracker.TaskTrackerApp
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/** Pushes pending changes and pulls the cloud copy whenever the network is available. */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as TaskTrackerApp
        if (app.auth.uid == null) return Result.success()
        return try {
            app.repository.sync()
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val PERIODIC_NAME = "task_tracker_periodic_sync"
        private const val ONCE_NAME = "task_tracker_sync_when_online"

        private fun needsNetwork() = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /** 6-hourly background sync. */
        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(needsNetwork())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, req)
        }

        /** One sync as soon as the device is online (used after a failed sync). */
        fun syncWhenOnline(context: Context) {
            val req = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(needsNetwork())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(ONCE_NAME, ExistingWorkPolicy.REPLACE, req)
        }
    }
}
