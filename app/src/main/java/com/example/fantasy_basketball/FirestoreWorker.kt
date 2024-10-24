package com.example.fantasy_basketball

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val sharedPreferences = appContext.getSharedPreferences("PlayerStatusPrefs", Context.MODE_PRIVATE)
    private val isFirstLoadKey = "isFirstLoad"

    override suspend fun doWork(): Result {
        val firestore = FirebaseFirestore.getInstance()

        try {
            // Fetch player data from Firestore
            val snapshot = firestore.collection("players").get().await()

            // Check if this is the first load
            val isFirstLoad = sharedPreferences.getBoolean(isFirstLoadKey, true)

            // Filter the top 5 players from the snapshot
            val topPlayers = snapshot.documents.take(5)

            for (document in topPlayers) {
                val playerName = document.getString("longName") ?: "Unknown Player"
                val injuryMap = document.get("Injury") as? Map<*, *>
                val currentStatus = injuryMap?.get("status") as? String
                val injuryDescription = injuryMap?.get("description") as? String ?: "No description available"

                // Skip if no status information
                if (currentStatus == null) continue

                // Get the previous status from SharedPreferences
                val previousStatus = sharedPreferences.getString(playerName, null)

                // Handle the first load scenario
                if (isFirstLoad) {
                    if (currentStatus == "Out" || currentStatus == "Day-To-Day") {
                        sendLocalNotification(playerName, currentStatus, injuryDescription)
                        // Save the current status in SharedPreferences
                        sharedPreferences.edit().putString(playerName, currentStatus).apply()
                    }
                } else {
                    // Only send a notification if the status has changed
                    if (currentStatus != previousStatus && (currentStatus == "Out" || currentStatus == "Day-To-Day")) {
                        sendLocalNotification(playerName, currentStatus, injuryDescription)

                        // Update the stored status in SharedPreferences
                        sharedPreferences.edit().putString(playerName, currentStatus).apply()
                    }
                }
            }

            // After the first load, set isFirstLoad to false
            if (isFirstLoad) {
                sharedPreferences.edit().putBoolean(isFirstLoadKey, false).apply()
            }

            return Result.success()

        } catch (e: Exception) {
            Log.e("FirestoreWorker", "Error fetching data: ${e.message}", e)
            // Retry if an error occurs
            return Result.retry()
        }
    }

    // Function to send local notifications with injury description
    private fun sendLocalNotification(playerName: String, currentStatus: String, injuryDescription: String) {
        // Check if the app has POST_NOTIFICATIONS permission (required for Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
                Log.w("FirestoreWorker", "Notification permission not granted")
                return
            }
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "work_manager_channel"
        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notification)  // Ensure this drawable exists
            .setContentTitle("Player Status Update")
            .setContentText("$playerName is now $currentStatus: $injuryDescription")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$playerName is now $currentStatus: $injuryDescription"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Create notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(channelId)
        }

        // Show the notification
        with(NotificationManagerCompat.from(applicationContext)) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }

    // Create notification channel for Android 8.0+
    private fun createNotificationChannel(channelId: String) {
        val channelName = "Player Updates"
        val channelDescription = "Notifies about player status changes"
        val importance = NotificationManager.IMPORTANCE_HIGH

        val channel = NotificationChannel(channelId, channelName, importance).apply {
            description = channelDescription
        }

        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)
    }
}





