package org.plumvillage.mt

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import org.plumvillage.mt.HeadlineRepository.Set

class HomeActivity : ComponentActivity() {

    private lateinit var group: RadioGroup
    private lateinit var fiveRadio: RadioButton
    private lateinit var fourteenRadio: RadioButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        PersistService.start(this)
    }

    override fun onResume() {
        super.onResume()
        syncSelection()
    }

    private fun buildUi() {
        val density = resources.displayMetrics.density
        val pad = (16 * density).toInt()

        val title = TextView(this).apply {
            textSize = 24f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(pad, pad, pad, pad / 2)
            text = getString(R.string.home_title)
        }

        val intro = TextView(this).apply {
            text = getString(R.string.home_intro)
            setPadding(pad, 0, pad, pad / 2)
            setTextAppearance(android.R.style.TextAppearance_Small)
        }

        fiveRadio = RadioButton(this).apply {
            text = getString(R.string.set_five)
        }
        fourteenRadio = RadioButton(this).apply {
            text = getString(R.string.set_fourteen)
        }

        val fiveSection = expandableSection(
            descShort = getString(R.string.desc_five_short),
            descFull = getString(R.string.desc_five_full)
        )
        val fourteenSection = expandableSection(
            descShort = getString(R.string.desc_fourteen_short),
            descFull = getString(R.string.desc_fourteen_full)
        )

        group = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
            setPadding(pad, 0, pad, pad)
        }

        // Wrapping each radio button together with its description in a
        // container row keeps the toggle aligned with its own description,
        // while the RadioGroup still guarantees mutual exclusion.
        group.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(fiveRadio)
            addView(fiveSection)
        })
        group.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(fourteenRadio)
            addView(fourteenSection)
        })

        val viewButton = Button(this).apply {
            text = getString(R.string.view_today)
            setOnClickListener {
                startActivity(Intent(this@HomeActivity, MainActivity::class.java))
            }
        }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            addView(title)
            addView(intro)
            addView(group)
            addView(viewButton)
        }
        setContentView(ScrollView(this).apply { addView(column) })

        group.setOnCheckedChangeListener { _, checkedId ->
            val newSet = if (checkedId == fourteenRadio.id) Set.FOURTEEN else Set.FIVE
            if (newSet != HeadlineRepository.selectedSet(this)) {
                HeadlineRepository.selectSet(this, newSet)
            }
        }
        syncSelection()
    }

    /**
     * Builds a description block that starts collapsed, showing the opening
     * sentence of the set's introductory text plus a "More" affordance;
     * tapping it swaps in the full introductory text and flips to "Less".
     * Returns the toggle TextView; expansion state is held by its tag.
     */
    private fun expandableSection(descShort: String, descFull: String): TextView {
        val smallPad = (8 * resources.displayMetrics.density).toInt()
        val indent = (32 * resources.displayMetrics.density).toInt()
        val moreHint = getString(R.string.expand_more)
        val lessHint = getString(R.string.expand_less)
        return TextView(this).apply {
            text = descShort + "\n" + moreHint
            textSize = 14f
            setPadding(indent, 0, smallPad, smallPad)
            setOnClickListener {
                val expanded = (tag as? Boolean) == true
                if (expanded) {
                    text = descShort + "\n" + moreHint
                    tag = false
                } else {
                    text = descFull + "\n" + lessHint
                    tag = true
                }
            }
        }
    }

    private fun syncSelection() {
        val current = HeadlineRepository.selectedSet(this)
        group.check(if (current == Set.FOURTEEN) fourteenRadio.id else fiveRadio.id)
    }
}
