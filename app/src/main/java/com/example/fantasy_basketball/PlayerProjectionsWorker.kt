package com.example.fantasy_basketball

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
class PlayerProjectionsWorker (

    context: Context,
    workerParams: WorkerParameters
    ) : Worker(context, workerParams) {

        //private val playerDataManager = PlayerDataManager()

        override fun doWork(): Result {
            // Use a coroutine to run the task asynchronously
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Fetch player projections and update Firestore
//                    playerDataManager.fetchAndStorePlayerProjections()
                } catch (e: Exception) {
                    e.printStackTrace()
                    // If an error occurs, we return Result.failure()
                    return@launch
                }
            }

            // Return success (if no errors were thrown synchronously)
            return Result.success()
        }
    }