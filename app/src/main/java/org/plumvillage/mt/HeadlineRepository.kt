package org.plumvillage.mt

import android.content.Context
import java.time.LocalDate

data class Headline(val title: String, val body: String)

object HeadlineRepository {

    enum class Set(val fileName: String, val displayName: String) {
        FIVE("five_trainings.txt", "Five Mindfulness Trainings"),
        FOURTEEN("fourteen_trainings.txt", "Fourteen Mindfulness Trainings")
    }

    private const val PREFS = "app_prefs"
    private const val KEY_FIRST_RUN_PREFIX = "first_run_date_"
    private const val KEY_SELECTED_SET = "selected_set"

    /** The user's chosen set of trainings; defaults to FIVE. */
    fun selectedSet(context: Context): Set {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_SELECTED_SET, Set.FIVE.name)
        return try { Set.valueOf(name!!) } catch (_: IllegalArgumentException) { Set.FIVE }
    }

    fun selectSet(context: Context, set: Set) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_SELECTED_SET, set.name).apply()
    }

    /**
     * The epoch-day on which the given set was first shown; recorded on first
     * access, persisted thereafter. Each set has its own independent anchor.
     */
    private fun startDay(context: Context, set: Set): Long {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getLong(KEY_FIRST_RUN_PREFIX + set.name, -1L)
        if (stored >= 0L) return stored

        val today = LocalDate.now().toEpochDay()
        prefs.edit().putLong(KEY_FIRST_RUN_PREFIX + set.name, today).apply()
        return today
    }

    fun load(context: Context, set: Set): List<Headline> {
        val text = context.assets.open(set.fileName).bufferedReader().use { it.readText() }
        return text.split(Regex("(?m)^###\\s*$"))
            .mapNotNull { block ->
                val lines = block.trim().lines()
                if (lines.isEmpty() || lines.first().isBlank()) null
                else Headline(
                    title = lines.first().trim(),
                    body = lines.drop(1).joinToString("\n").trim()
                )
            }
    }

    /** Deterministic index: day 0 = first-run day shows entry 1, then one per calendar day, cycling. */
    fun todaysIndex(context: Context, set: Set, listSize: Int): Int {
        require(listSize > 0) { "${set.fileName} contains no entries" }
        val dayNumber = LocalDate.now().toEpochDay() - startDay(context, set)
        return (((dayNumber % listSize) + listSize) % listSize).toInt()
    }

    fun todaysHeadline(context: Context): Headline {
        val set = selectedSet(context)
        val all = load(context, set)
        return all[todaysIndex(context, set, all.size)]
    }
}
