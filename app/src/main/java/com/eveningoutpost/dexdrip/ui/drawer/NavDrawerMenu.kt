package com.eveningoutpost.dexdrip.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.eveningoutpost.dexdrip.ui.theme.XdripTheme

/** Java-friendly listener for a drawer item selection. */
fun interface NavDrawerItemListener {
    fun onItemSelected(position: Int)
}

/**
 * Holds the Compose drawer menu state and is updated from the legacy
 * [com.eveningoutpost.dexdrip.NavigationDrawerFragment] in Java.
 */
class NavDrawerMenuState {
    var items: List<String> by mutableStateOf(emptyList())
        private set
    var selectedPosition: Int by mutableStateOf(0)
        private set
    var onItemSelected: NavDrawerItemListener? = null
        private set

    fun update(newItems: List<String>, newSelectedPosition: Int, listener: NavDrawerItemListener) {
        items = newItems
        selectedPosition = newSelectedPosition
        onItemSelected = listener
    }

    fun setSelected(position: Int) {
        selectedPosition = position
    }

    fun install(view: ComposeView) {
        val state = this
        view.setContent {
            XdripTheme {
                NavDrawerMenu(state)
            }
        }
    }
}

@Composable
fun NavDrawerMenu(state: NavDrawerMenuState) {
    val items = state.items
    val selected = state.selectedPosition
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        itemsIndexed(items) { index, label ->
            NavDrawerMenuRow(
                label = label,
                selected = index == selected,
                onClick = { state.onItemSelected?.onItemSelected(index) },
            )
        }
    }
}

@Composable
private fun NavDrawerMenuRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
