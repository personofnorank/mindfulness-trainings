package org.plumvillage.mt

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-posts the daily notification after a device reboot. Foreground services
 * do not survive a restart; without this receiver the reminder would stay
 * gone until the next scheduled daily-worker tick.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            PersistService.start(context)
            // Alarms do not survive a reboot; re-arm tonight's 00:05 refresh.
            DailyAlarms.schedule(context)
        }
    }
}
