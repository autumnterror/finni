package github.detrig.internetbooster.time

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import github.detrig.internetbooster.FinPetApplication
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

class TimeReconciliationWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        (applicationContext as FinPetApplication).appComponent.reconcileTimedEvents()
        Result.success()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        Result.retry()
    }
}

internal object TimeWorkScheduler {
    private const val WORK_NAME = "finpet_time_reconciliation"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<TimeReconciliationWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request,
        )
    }
}
