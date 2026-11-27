package org.plumvillage.mt

import android.app.Activity
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Ensures the screen's scroll container starts below the status bar
 * (clock/battery icons) and above the gesture/navigation bar. Android 15+
 * enforces edge-to-edge windows, so the app must reserve that space itself;
 * on older versions this is a safe no-op that simply reports the same inset.
 * It also declares light status-bar appearance so the tray icons contrast
 * with the app's light background. Applied to the ScrollView of every screen
 * so no heading is ever obscured by system UI and the trailing button never
 * sits flush under the gesture bar.
 */
object StatusBars {

    /**
     * Pads the given root view by the status-bar height once known, and
     * declares the light status-bar appearance so the system draws dark
     * icons: in dark mode the otherwise-white icons would be invisible
     * against the app's white background.
     */
    fun pad(activity: Activity, root: View) {
        WindowCompat.getInsetsController(
            activity.window, activity.window.decorView
        ).isAppearanceLightStatusBars = true
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            // Insets can be dispatched more than once (rotation, layout
            // passes); keying the layout-time padding in the tag keeps this
            // idempotent instead of accumulating on every dispatch.
            val baseTop = v.getTag(R.id.status_bar_base_padding) as? Int
                ?: v.paddingTop.also {
                    v.setTag(R.id.status_bar_base_padding, it)
                }
            // Same treatment for the bottom: reserve the gesture/nav bar so
            // the trailing button is never flush under the system bar.
            val baseBottom = v.getTag(R.id.nav_bar_base_padding) as? Int
                ?: v.paddingBottom.also {
                    v.setTag(R.id.nav_bar_base_padding, it)
                }
            val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            v.setPadding(v.paddingLeft, baseTop + top, v.paddingRight, baseBottom + bottom)
            WindowInsetsCompat.CONSUMED
        }
    }
}
