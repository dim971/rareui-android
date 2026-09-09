package io.github.dim971.rareui.showcase.entries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import io.github.dim971.rareui.components.feedback.NotificationBell
import io.github.dim971.rareui.components.feedback.NotificationBellColor
import io.github.dim971.rareui.components.feedback.NotificationBellVariant
import io.github.dim971.rareui.showcase.CatalogEntry
import io.github.dim971.rareui.showcase.Demo

val notificationBellEntry: CatalogEntry =
    CatalogEntry(
        name = "Notification Bell",
        summary = "A bell that swings when its count goes up, with a clapper that trails behind it.",
        demos =
            listOf(
                Demo(
                    title = "Ring it",
                    note =
                        "Nothing is tapped: the arrival of a notification is the event. The bell " +
                            "is pushed the way it is already moving, so several arriving at once " +
                            "make it swing harder rather than starting the swing again.",
                    code = "NotificationBell(count = unread)",
                ) { BellDemo() },
                Demo(
                    title = "A dot instead of a number",
                    code =
                        "NotificationBell(count = unread, variant = NotificationBellVariant.DOT,\n" +
                            "    color = NotificationBellColor.BLUE)",
                ) { BellDemo(variant = NotificationBellVariant.DOT, color = NotificationBellColor.BLUE) },
                Demo(
                    title = "Colours and sizes",
                    note =
                        "Everything about the bell is a fraction of its size, so it holds " +
                            "together at any of them.",
                    code = "NotificationBell(count = 3, size = 64.dp, color = NotificationBellColor.VIOLET)",
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NotificationBell(count = 3, size = 36.dp, color = NotificationBellColor.GREEN)
                        NotificationBell(count = 12, size = 48.dp, color = NotificationBellColor.ORANGE)
                        NotificationBell(count = 128, size = 64.dp, color = NotificationBellColor.VIOLET)
                    }
                },
            ),
    ) {
        NotificationBell(count = 3, size = 40.dp)
    }

@Composable
private fun BellDemo(
    variant: NotificationBellVariant = NotificationBellVariant.COUNT,
    color: NotificationBellColor = NotificationBellColor.RED,
) {
    var unread by remember { mutableIntStateOf(1) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        NotificationBell(count = unread, variant = variant, color = color)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { unread += 1 }) { Text("One more") }
            OutlinedButton(onClick = { unread += 5 }) { Text("Five at once") }
            OutlinedButton(onClick = { unread = 0 }) { Text("Read them") }
        }
    }
}
