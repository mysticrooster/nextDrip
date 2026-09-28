package com.eveningoutpost.dexdrip.utilitymodels

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Reactive bridge for [ColorCache].
 *
 * [ColorCache] is a static cache with no observers, so this object provides a
 * revision counter that Compose can collect to recompose when a user picks a new
 * color. [ColorCache.invalidateCache] bumps the revision.
 */
object ColorCacheBridge {

    private val _revision = MutableStateFlow(0L)

    /** Increments every time the color cache is invalidated. */
    val revision: StateFlow<Long> = _revision

    @JvmStatic
    fun invalidate() {
        _revision.value += 1
    }
}
