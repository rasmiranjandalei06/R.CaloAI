package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.example.MainActivity
import com.example.R
import java.util.concurrent.TimeUnit

class NotificationHelper(private val context: Context) {

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NutriTrack AI Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "NutriTrack reminders for water intake, meals, and summary."
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun triggerNotification(title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                // If permission is not granted on API 33+, WorkManager tasks will fail silently or respect OS rules safely.
                notify(System.currentTimeMillis().toInt(), builder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    companion object {
        const val CHANNEL_ID = "nutritrack_reminders"

        fun schedulePeriodicReminders(context: Context) {
            // Meal and water periodic tasks using WorkManager
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val waterWorkRequest = PeriodicWorkRequestBuilder<WaterReminderWorker>(
                2, TimeUnit.HOURS // Repeat every 2 hours
            )
            .setConstraints(constraints)
                .build()

            val mealWorkRequest = PeriodicWorkRequestBuilder<MealReminderWorker>(
                4, TimeUnit.HOURS // Repeat every 4 hours for screen reminders
            )
            .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "water_reminder_work",
                ExistingPeriodicWorkPolicy.KEEP,
                waterWorkRequest
            )

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "meal_reminder_work",
                ExistingPeriodicWorkPolicy.KEEP,
                mealWorkRequest
            )
        }

        fun cancelPeriodicReminders(context: Context) {
            WorkManager.getInstance(context).cancelAllWork()
        }
    }
}

class WaterReminderWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val helper = NotificationHelper(applicationContext)
        helper.triggerNotification(
            "💧 Time to drink water!",
            "Stay hydrated to support your metabolism! Let's drink a glass of water."
        )
        return Result.success()
    }
}

class MealReminderWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val helper = NotificationHelper(applicationContext)
        helper.triggerNotification(
            "🍳 Time to log your food!",
            "Don't lose your streak. Tap to quickly search or scan your food to stay on track."
        )
        return Result.success()
    }
}
