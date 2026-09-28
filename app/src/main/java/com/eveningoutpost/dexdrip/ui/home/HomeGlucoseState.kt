package com.eveningoutpost.dexdrip.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * View-agnostic holder for the Home header glucose state (current value, delta,
 * and semantic level). Populated from [com.eveningoutpost.dexdrip.Home] and read
 * by Compose. Reactive so the UI recomposes when the value changes.
 */
class HomeGlucoseState {
    var value: String by mutableStateOf("")
        private set
    var delta: String by mutableStateOf("")
        private set
    var level: GlucoseLevel by mutableStateOf(GlucoseLevel.IN_RANGE)
        private set

    fun update(value: String, delta: String, level: GlucoseLevel) {
        this.value = value
        this.delta = delta
        this.level = level
    }
}
