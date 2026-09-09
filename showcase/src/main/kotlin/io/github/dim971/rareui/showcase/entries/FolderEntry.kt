package io.github.dim971.rareui.showcase.entries

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dim971.rareui.components.display.FolderColor
import io.github.dim971.rareui.components.display.FolderComponent
import io.github.dim971.rareui.components.display.FolderSize
import io.github.dim971.rareui.showcase.CatalogEntry
import io.github.dim971.rareui.showcase.Demo

val folderEntry: CatalogEntry =
    CatalogEntry(
        name = "Folder",
        summary = "A folder whose flap tips back and whose contents fan out of it.",
        demos =
            listOf(
                Demo(
                    title = "Open it",
                    note =
                        "The flap is a real rotation about its own bottom edge, with the camera " +
                            "set where upstream's perspective puts it, and the cards behind it " +
                            "are blurred through its outline.",
                    code = "FolderComponent(color = FolderColor.BLACK)",
                ) {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        FolderComponent(color = FolderColor.BLACK, size = FolderSize.SMALL)
                    }
                },
                Demo(
                    title = "Three folders",
                    note =
                        "Each is a complete set of colours rather than a tint: the black folder " +
                            "holds pale cards and the white one holds dark ones.",
                    code = "FolderComponent(color = FolderColor.BLUE, size = FolderSize.SMALL)",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        FolderColor.entries.forEach { colour ->
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                FolderComponent(color = colour, size = FolderSize.SMALL)
                            }
                        }
                    }
                },
            ),
    ) {
        FolderComponent(color = FolderColor.BLUE, size = FolderSize.SMALL)
    }
