package org.plumvillage.mt

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) PersistService.start(this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissionIfNeeded()
        DailyDigestWorker.schedule(this)
        showTodaysHeadline()
    }

    private fun requestPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // Pin the daily notification via the persist service (also posts
            // immediately so the user sees the app working on first launch).
            PersistService.start(this)
        }
    }

    private fun showTodaysHeadline() {
        val density = resources.displayMetrics.density
        val pad = (16 * density).toInt()

        val title = TextView(this).apply {
            textSize = 24f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(pad, pad, pad, pad / 2)
        }
        val body = TextView(this).apply {
            textSize = 16f
            setPadding(pad, 0, pad, pad)
        }
        val homeButton = Button(this).apply {
            text = getString(R.string.back_to_home)
            setOnClickListener { finish() } // returns to HomeActivity underneath
        }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            addView(title)
            addView(body)
            addView(homeButton)
        }
        setContentView(ScrollView(this).apply { addView(column) })

        try {
            val headline = HeadlineRepository.todaysHeadline(this)
            title.text = headline.title
            body.text = headline.body
        } catch (e: Exception) {
            title.text = getString(R.string.error_title)
            body.text = e.message ?: getString(R.string.error_generic)
        }
    }
}
