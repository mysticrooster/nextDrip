package com.eveningoutpost.dexdrip.ui.settings

import android.content.Context
import android.content.res.Configuration
import java.text.Normalizer
import java.util.Locale

/**
 * Destination-level settings search (AAPS-style).
 *
 * The index is derived from every non-[SettingsScreen.Root] destination, so it stays complete as
 * the Compose migration grows. Matching is relevance-ranked and diacritics-insensitive, and each
 * destination carries its own keyword aliases. Only destinations that are unreachable and not
 * self-selectable are filtered out (see [SettingsScreen.isAvailable]); data-source provider screens
 * stay searchable so users can switch collection methods.
 */
internal data class SettingsSearchEntry(
    val screen: SettingsScreen,
    val localizedTitle: String,
    val englishTitle: String,
    val keywords: List<String>,
)

/** Builds the index once per [context] locale; callers should `remember(context)` the result. */
internal fun buildSettingsSearchIndex(context: Context): List<SettingsSearchEntry> {
    val englishContext = context.createConfigurationContext(
        Configuration(context.resources.configuration).apply { setLocale(Locale.ENGLISH) }
    )
    return SettingsScreen.entries
        .filter { it != SettingsScreen.Root }
        .map { screen ->
            SettingsSearchEntry(
                screen = screen,
                localizedTitle = screen.title(context),
                englishTitle = screen.title(englishContext),
                keywords = screen.keywords,
            )
        }
}

/**
 * Ranks [index] against [query] and drops hidden destinations. [state] is read for the reactive
 * engineering-mode gate. Blank queries return no results.
 */
internal fun searchSettings(
    index: List<SettingsSearchEntry>,
    query: String,
    state: SettingsState,
): List<SettingsSearchEntry> {
    val normalizedQuery = normalize(query.trim())
    if (normalizedQuery.isEmpty()) return emptyList()
    return index
        .filter { it.screen.isAvailable(state) }
        .mapNotNull { entry ->
            val score = relevance(entry, normalizedQuery)
            if (score > 0) entry to score else null
        }
        .sortedByDescending { it.second }
        .map { it.first }
}

/** Mirrors the AAPS weights: localized title > English title > keyword. */
private fun relevance(entry: SettingsSearchEntry, normalizedQuery: String): Int {
    var score = 0
    val localized = normalize(entry.localizedTitle)
    if (localized.contains(normalizedQuery)) {
        score = 100 + if (localized.startsWith(normalizedQuery)) 50 else 0
    }
    val english = normalize(entry.englishTitle)
    if (english.contains(normalizedQuery)) {
        score = maxOf(score, 80)
    }
    entry.keywords.forEach { keyword ->
        val normalizedKeyword = normalize(keyword)
        val keywordScore = when {
            normalizedKeyword == normalizedQuery -> 80
            normalizedKeyword.startsWith(normalizedQuery) -> 60
            normalizedKeyword.contains(normalizedQuery) -> 40
            else -> 0
        }
        score = maxOf(score, keywordScore)
    }
    return score
}

/** Lowercases then strips combining marks so accented and unaccented queries match. */
private fun normalize(value: String): String {
    val lower = value.lowercase(Locale.ROOT)
    return Normalizer.normalize(lower, Normalizer.Form.NFD).replace(COMBINING_MARKS, "")
}

private val COMBINING_MARKS = Regex("\\p{Mn}+")
