package io.github.dim971.rareui.showcase.entries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dim971.rareui.components.feedback.EmojiReaction
import io.github.dim971.rareui.components.feedback.EmojiReactionAlign
import io.github.dim971.rareui.components.feedback.EmojiReactionSize
import io.github.dim971.rareui.components.feedback.RareUiDefaultEmojis
import io.github.dim971.rareui.showcase.CatalogEntry
import io.github.dim971.rareui.showcase.Demo

val emojiReactionEntry: CatalogEntry =
    CatalogEntry(
        name = "Emoji Reaction",
        summary = "A bar of emoji, and copies of the one you pick drifting up the screen.",
        demos =
            listOf(
                Demo(
                    title = "Pick one",
                    note =
                        "Five copies leave a quarter of a second apart, each with its own lane, " +
                            "tilt, blur and pace. Holding one down keeps sending them, a little " +
                            "under twice a second.",
                    code = "EmojiReaction(onReact = { emoji -> post(emoji) })",
                ) { EmojiReactionDemo() },
                Demo(
                    title = "Alignment and size",
                    note = "The bar lines up with whichever edge of the trigger you name.",
                    code =
                        "EmojiReaction(size = EmojiReactionSize.LARGE,\n" +
                            "    align = EmojiReactionAlign.LEADING)",
                ) {
                    Column {
                        Spacer(Modifier.height(120.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            EmojiReaction(size = EmojiReactionSize.SMALL, align = EmojiReactionAlign.LEADING)
                            EmojiReaction(size = EmojiReactionSize.MEDIUM)
                            EmojiReaction(size = EmojiReactionSize.LARGE, align = EmojiReactionAlign.TRAILING)
                        }
                    }
                },
                Demo(
                    title = "Your own emoji",
                    code = "EmojiReaction(emojis = listOf(\"🔥\", \"💯\", \"🎉\"))",
                ) { EmojiReactionDemo(listOf("🔥", "💯", "🎉", "👀")) },
            ),
    ) {
        EmojiReaction(size = EmojiReactionSize.SMALL)
    }

@Composable
private fun EmojiReactionDemo(emojis: List<String> = RareUiDefaultEmojis) {
    val reactions = remember { mutableStateListOf<String>() }

    Column(verticalArrangement = Arrangement.spacedBy(40.dp)) {
        Spacer(Modifier.height(60.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EmojiReaction(
                emojis = emojis,
                align = EmojiReactionAlign.LEADING,
                onReact = { reactions += it },
            )
            Text(
                text = if (reactions.isEmpty()) "No reactions yet" else reactions.takeLast(12).joinToString(""),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
