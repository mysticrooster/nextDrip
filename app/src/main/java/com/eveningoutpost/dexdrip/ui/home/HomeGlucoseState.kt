package com.eveningoutpost.dexdrip.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * View-agnostic holder for the Home header glucose state. Populated from
 * [com.eveningoutpost.dexdrip.Home] and read by Compose.
 *
 * The edge cases that used to be rendered as text-formatting hacks
 * (strikethrough/underline/italic, and prefix glyphs) are captured here as
 * semantic flags so the Compose header can present them as modern badges.
 */
class HomeGlucoseState {
    var value: String by mutableStateOf("")
        private set
    var delta: String by mutableStateOf("")
        private set
    var level: GlucoseLevel by mutableStateOf(GlucoseLevel.IN_RANGE)
        private set
    var slopeArrow: String by mutableStateOf("")
        private set
    var isStale: Boolean by mutableStateOf(false)
        private set
    var isFiltered: Boolean by mutableStateOf(false)
        private set
    var isNoise: Boolean by mutableStateOf(false)
        private set
    var isPredictive: Boolean by mutableStateOf(false)
        private set
    var fromPlugin: Boolean by mutableStateOf(false)
        private set

    fun update(
        value: String,
        delta: String,
        level: GlucoseLevel,
        slopeArrow: String = "",
        isStale: Boolean = false,
        isFiltered: Boolean = false,
        isNoise: Boolean = false,
        isPredictive: Boolean = false,
        fromPlugin: Boolean = false,
    ) {
        this.value = value
        this.delta = delta
        this.level = level
        this.slopeArrow = slopeArrow
        this.isStale = isStale
        this.isFiltered = isFiltered
        this.isNoise = isNoise
        this.isPredictive = isPredictive
        this.fromPlugin = fromPlugin
    }

    fun clear() {
        value = ""
        delta = ""
        level = GlucoseLevel.IN_RANGE
        slopeArrow = ""
        isStale = false
        isFiltered = false
        isNoise = false
        isPredictive = false
        fromPlugin = false
    }
}
