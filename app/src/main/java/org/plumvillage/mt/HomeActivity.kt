package org.plumvillage.mt

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import org.plumvillage.mt.HeadlineRepository.Cadence
import org.plumvillage.mt.HeadlineRepository.Set
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class HomeActivity : ComponentActivity() {

    private lateinit var group: RadioGroup
    private lateinit var fiveRadio: RadioButton
    private lateinit var fourteenRadio: RadioButton
    private lateinit var cadenceGroup: RadioGroup
    private lateinit var dailyRadio: RadioButton
    private lateinit var weeklyRadio: RadioButton

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) PersistService.start(this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            // Fresh install (Android 13+): ask for POST_NOTIFICATIONS; the
            // persist service starts either in the callback (granted) or
            // immediately (denied / pre-13), never blocking the UI.
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            PersistService.start(this)
        } else {
            PersistService.start(this)
        }
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
            descFull = getString(R.string.desc_five_full),
            set = Set.FIVE
        )
        val fourteenSection = expandableSection(
            descShort = getString(R.string.desc_fourteen_short),
            descFull = getString(R.string.desc_fourteen_full),
            set = Set.FOURTEEN
        )

        group = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
            setPadding(pad, 0, pad, 0)
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

        val cadenceLabel = TextView(this).apply {
            text = getString(R.string.cadence_label)
            setPadding(pad, pad, pad, pad / 2)
            setTypeface(typeface, Typeface.BOLD)
        }

        dailyRadio = RadioButton(this).apply {
            text = getString(R.string.cadence_daily)
        }
        weeklyRadio = RadioButton(this).apply {
            text = getString(R.string.cadence_weekly)
        }
        cadenceGroup = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
            setPadding(pad, 0, pad, pad)
        }.apply {
            addView(dailyRadio)
            addView(weeklyRadio)
        }

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
            addView(cadenceLabel)
            addView(cadenceGroup)
            addView(viewButton)
        }
        setContentView(ScrollView(this).apply { addView(column) }.also {
            StatusBars.pad(this, it)
        })

        group.setOnCheckedChangeListener { _, checkedId ->
            val newSet = if (checkedId == fourteenRadio.id) Set.FOURTEEN else Set.FIVE
            if (newSet != HeadlineRepository.selectedSet(this)) {
                HeadlineRepository.selectSet(this, newSet)
                // Selection changed: the pinned notification must show the
                // other set's training immediately, not on the next tick.
                PersistService.start(this)
            }
        }
        cadenceGroup.setOnCheckedChangeListener { _, checkedId ->
            val newCadence =
                if (checkedId == weeklyRadio.id) Cadence.WEEKLY else Cadence.DAILY
            if (newCadence != HeadlineRepository.cadence(this)) {
                HeadlineRepository.selectCadence(this, newCadence)
                PersistService.start(this)
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
    private fun expandableSection(descShort: String, descFull: String, set: Set): TextView {
        val smallPad = (8 * resources.displayMetrics.density).toInt()
        val indent = (32 * resources.displayMetrics.density).toInt()
        val moreHint = getString(R.string.expand_more)
        val lessHint = getString(R.string.expand_less)
        return TextView(this).apply {
            text = linked(descShort + "\n" + moreHint, set)
            textSize = 14f
            setPadding(indent, 0, smallPad, smallPad)
            // LinkMovementMethod routes taps that land on the link span to
            // the span; taps elsewhere still toggle expand/collapse.
            movementMethod = LinkMovementMethod.getInstance()
            setOnClickListener {
                val expanded = (tag as? Boolean) == true
                if (expanded) {
                    text = linked(descShort + "\n" + moreHint, set)
                    tag = false
                } else {
                    text = linked(descFull + "\n" + lessHint, set)
                    tag = true
                }
            }
        }
    }

    /**
     * Builds the given description text with the training-set's name phrase
     * rendered as an underlined link that opens the full text of that set.
     */
    private fun linked(text: String, set: Set): CharSequence {
        val phrase = getString(
            if (set == Set.FIVE) R.string.link_five else R.string.link_fourteen
        )
        val sb = SpannableStringBuilder(text)
        val start = text.indexOf(phrase)
        if (start >= 0) {
            val end = start + phrase.length
            sb.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    TrainingsActivity.start(this@HomeActivity, set)
                }

                override fun updateDrawState(ds: TextPaint) {
                    ds.isUnderlineText = true
                    ds.color = ContextCompat.getColor(
                        this@HomeActivity, android.R.color.holo_blue_dark
                    )
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return sb
    }

    private fun syncSelection() {
        val current = HeadlineRepository.selectedSet(this)
        group.check(if (current == Set.FOURTEEN) fourteenRadio.id else fiveRadio.id)
        val cadence = HeadlineRepository.cadence(this)
        cadenceGroup.check(
            if (cadence == Cadence.WEEKLY) weeklyRadio.id else dailyRadio.id
        )
    }
}
