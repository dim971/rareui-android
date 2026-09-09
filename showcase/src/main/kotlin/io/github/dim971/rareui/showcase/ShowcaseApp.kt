package io.github.dim971.rareui.showcase

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.dim971.rareui.theme.RareUiTheme
import io.github.dim971.rareui.theme.rareUiColors

/**
 * The catalog.
 *
 * @param opening the component to open straight to, if one was named at launch.
 */
@Composable
fun ShowcaseApp(opening: String? = null) {
    var accent by remember { mutableStateOf(ShowcaseAccents.first().second) }
    var showing by remember { mutableStateOf(catalogEntries.firstOrNull { it.name == opening }) }

    MaterialTheme {
        RareUiTheme(colors = rareUiColors(dark = false, accent = accent)) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.fillMaxSize()) {
                    val entry = showing
                    if (entry == null) {
                        CatalogHomeScreen(
                            accent = accent,
                            onAccentChange = { accent = it },
                            onOpen = { showing = it },
                        )
                    } else {
                        ComponentScreen(
                            entry = entry,
                            accent = accent,
                            onAccentChange = { accent = it },
                            onBack = { showing = null },
                        )
                    }
                }
            }
        }
    }
}

/**
 * The accent choices offered in the controls. The first is upstream's own.
 *
 * Most of these components take an accent colour, and most of the interesting questions
 * about them are questions about how they look in someone else's palette.
 */
val ShowcaseAccents: List<Pair<String, Color>> =
    listOf(
        "Rare" to Color(0xFFFC4C01),
        "Ember" to Color(0xFFF75001),
        "Grass" to Color(0xFF39D353),
        "Sky" to Color(0xFF1A73F2),
        "Violet" to Color(0xFFAF52DE),
    )
