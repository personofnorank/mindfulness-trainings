package org.plumvillage.mt

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat

/**
 * A minimal foreground service whose only job is to keep the daily-notification
 * alive. On Android 14+ the system permits the user to swipe even an ongoing
 * foreground-service notification away, so this service also handles the
 * notification's deleteIntent: every swipe lands here as a new start, and
 * onStartCommand immediately re-posts the notification. Net effect on 14+:
 * the notification reappears instantly instead of being permanently
 * dismissible. The service performs no other work; START_STICKY restarts it
 * if the system kills it.
 */
class PersistService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // (Re)post the current day's notification as this service's
        // foreground notification.
        DailyNotifier.postForeground(this)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        /** Starts (or re-starts) the service. Safe to call repeatedly. */
        fun start(context: Context) {
            val intent = Intent(context, PersistService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
