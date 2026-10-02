package com.eveningoutpost.dexdrip.ui.secondary

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.eveningoutpost.dexdrip.ui.theme.XdripPreview
import com.eveningoutpost.dexdrip.ui.theme.XdripTheme

private tailrec fun Context.findHostActivity(): AppCompatActivity? = when (this) {
    is AppCompatActivity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}

/**
 * Removes the legacy AppCompat `ActionBar` left over from the pre-Compose theme so the Compose
 * `TopAppBar` is the only bar. No-op under a NoActionBar theme or in `@Preview`.
 */
@Composable
private fun HideLegacyActionBar() {
    val context = LocalContext.current
    DisposableEffect(context) {
        context.findHostActivity()?.supportActionBar?.hide()
        onDispose { }
    }
}

/**
 * Shared scaffold for the migrated secondary views (Track V).
 *
 * Replaces the legacy action bar / nav-drawer shell (`JoH.fixActionBar`, `ActivityWithMenu`,
 * `NavigationDrawerFragment`) with a Material 3 `TopAppBar` + back arrow and a scrollable content
 * column. These screens are pushed from settings, so a back affordance is the expected navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecondaryScreen(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    XdripTheme {
        HideLegacyActionBar()
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(title) },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                )
                            }
                        },
                    )
                },
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState()),
                    content = content,
                )
            }
        }
    }
}

/**
 * Like [SecondaryScreen] but with a non-scrolling [Column] content area, for screens that host
 * their own scrollable list (`LazyColumn`) so it can claim the remaining height with `weight`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecondaryScreenList(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    XdripTheme {
        HideLegacyActionBar()
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(title) },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                )
                            }
                        },
                    )
                },
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    content = content,
                )
            }
        }
    }
}

/**
 * Like [SecondaryScreen] but with a non-scrolling [Box] content area, for screens that render a
 * full-size surface (e.g. a preview bitmap).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecondaryScreenFill(
    title: String,
    onBack: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    XdripTheme {
        HideLegacyActionBar()
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(title) },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                )
                            }
                        },
                    )
                },
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    content = content,
                )
            }
        }
    }
}

// region Previews

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun SecondaryScreenPreview() {
    XdripPreview {
        SecondaryScreen(title = "Screen title", onBack = {}) {
            Text("Screen content")
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 400, heightDp = 800)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun SecondaryScreenFillPreview() {
    XdripPreview {
        SecondaryScreenFill(title = "Screen title", onBack = {}) {
            Text("Full-size content")
        }
    }
}

// endregion
