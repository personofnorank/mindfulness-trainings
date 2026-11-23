package org.plumvillage.mt

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object DailyNotifier {
    const val CHANNEL_ID = "daily_digest"
    const val SILENT_CHANNEL_ID = "silent_hold"
    private const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
        // Minimal silent channel used only to satisfy the FGS contract when
        // POST_NOTIFICATIONS is not yet granted.
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(
                NotificationChannel(
                    SILENT_CHANNEL_ID,
                    context.getString(R.string.channel_name),
                    NotificationManager.IMPORTANCE_MIN
                )
            )
    }

    /** Builder for the daily training notification. Caller must hold POST_NOTIFICATIONS. */
    fun notification(context: Context): NotificationCompat.Builder {
        ensureChannel(context)
        val set = HeadlineRepository.selectedSet(context)
        val all = HeadlineRepository.load(context, set)
        val idx = HeadlineRepository.todaysIndex(context, set, all.size)
        val headline = all[idx]

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Android 14+ allows the user to dismiss even ongoing
        // foreground-service notifications, so true "never dismissible" is
        // impossible. Instead: when the notification IS swiped away, this
        // deleteIntent starts PersistService again, which immediately
        // re-posts it — the swipe becomes a no-op from the user's view.
        val deleteIntent = PendingIntent.getService(
            context, 1, Intent(context, PersistService::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${idx + 1} of ${all.size}: ${headline.title}")
            .setContentIntent(contentIntent)
            .setDeleteIntent(deleteIntent)
            .setOngoing(true)
    }

    /** Posts the daily training as a plain notification (worker path). */
    fun postToday(context: Context) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID, notification(context).build())
    }

    /** Posts the daily training as the persist-service's foreground notification. */
    fun postForeground(persistService: PersistService) {
        val notifAllowed = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                persistService, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

        if (notifAllowed) {
            persistService.startForeground(
                NOTIFICATION_ID, notification(persistService).build()
            )
        } else {
            // Permission not granted yet. Android REQUIRES startForeground()
            // after startForegroundService() within ~10 s even if the
            // notification would be blocked; post a silent minimal
            // notification to satisfy the contract, then stop cleanly.
            // The activity re-starts the service once permission is granted.
            persistService.startForeground(
                NOTIFICATION_ID,
                NotificationCompat.Builder(persistService, SILENT_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(persistService.getString(R.string.app_name))
                    .setOngoing(true)
                    .build()
            )
            persistService.stopSelf()
        }
    }
}

class DailyDigestWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Refresh the pinned notification with today's training. Starting the
        // service re-posts it; if the service was killed the system restarts
        // it (START_STICKY) and onStartCommand posts again.
        PersistService.start(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "daily-digest"
        private const val POST_HOUR = 8

        /** Schedules the daily post at POST_HOUR (next occurrence), surviving reboots via WorkManager. */
        fun schedule(context: Context) {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(POST_HOUR, 0)
            if (!now.toLocalTime().isBefore(LocalTime.of(POST_HOUR, 0))) {
                next = next.plusDays(1)
            }
            val delayMinutes = Duration.between(now, next).toMinutes()
            val request = PeriodicWorkRequestBuilder<DailyDigestWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}
