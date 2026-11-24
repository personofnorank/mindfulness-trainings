package org.plumvillage.mt

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity

/**
 * Shows the complete text of one training set, so users can read any
 * training at will without waiting for it to appear in the daily digest.
 * Reached via the link in the set's description on the home page.
 */
class TrainingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val set = try {
            HeadlineRepository.Set.valueOf(intent.getStringExtra(EXTRA_SET) ?: "")
        } catch (_: IllegalArgumentException) {
            HeadlineRepository.Set.FIVE
        }

        val density = resources.displayMetrics.density
        val pad = (16 * density).toInt()
        val gap = (8 * density).toInt()

        val title = TextView(this).apply {
            textSize = 24f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(pad, pad, pad, pad / 2)
            text = set.displayName
        }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            addView(title)
        }

        try {
            for (headline in HeadlineRepository.load(this, set)) {
                column.addView(TextView(this).apply {
                    textSize = 18f
                    setTypeface(typeface, Typeface.BOLD)
                    setPadding(pad, gap, pad, 0)
                    text = headline.title
                })
                column.addView(TextView(this).apply {
                    textSize = 16f
                    setPadding(pad, gap, pad, pad)
                    text = headline.body
                })
            }
        } catch (e: Exception) {
            column.addView(TextView(this).apply {
                textSize = 16f
                setPadding(pad, gap, pad, pad)
                text = e.message ?: getString(R.string.error_generic)
            })
        }

        column.addView(Button(this).apply {
            text = getString(R.string.back_to_home)
            setOnClickListener { finish() } // returns to HomeActivity underneath
        })

        setContentView(ScrollView(this).apply { addView(column) }.also {
            StatusBars.pad(this, it)
        })
    }

    companion object {
        private const val EXTRA_SET = "set"

        /** Opens the full text of the given training set. */
        fun start(context: Context, set: HeadlineRepository.Set) {
            context.startActivity(
                Intent(context, TrainingsActivity::class.java)
                    .putExtra(EXTRA_SET, set.name)
            )
        }
    }
}
