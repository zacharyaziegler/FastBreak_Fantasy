package com.example.fantasy_basketball

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import android.util.Log


class ProcessLeaguesWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {


    override suspend fun doWork(): Result {
        return try {
            processAllLeaguesForWeek()
            Log.d("ProcessLeaguesWorker", "Successfully processed leagues.")
            Result.success()
        } catch (e: Exception) {
            Log.e("ProcessLeaguesWorker", "Error processing leagues", e)
            Result.retry()
        }
    }
}


