package io.github.dim971.rareui.showcase.entries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.dim971.rareui.components.display.CodeBlock
import io.github.dim971.rareui.components.display.CodeBlockMode
import io.github.dim971.rareui.components.display.CodeLanguage
import io.github.dim971.rareui.showcase.CatalogEntry
import io.github.dim971.rareui.showcase.Demo

private val KotlinSample =
    """
    @Composable
    fun Total(amount: Double) {
        // The counter rolls to whatever it is given.
        val formatted = "%.2f".format(amount)
        AnimatedCounter(value = amount, decimals = 2, prefix = "$")
    }
    """.trimIndent()

private val JsonSample =
    """
    {
      "name": "rareui-android",
      "version": "0.1.0",
      "minSdk": 26,
      "open": true
    }
    """.trimIndent()

val codeBlockEntry: CatalogEntry =
    CatalogEntry(
        name = "Code Block",
        summary = "A framed panel of source code whose whole syntax theme is built from one colour.",
        demos =
            listOf(
                Demo(
                    title = "One accent, twelve tones",
                    note =
                        "Every colour in the theme keeps the accent's hue and saturation and takes " +
                            "a lightness of its own, so changing the accent changes the whole " +
                            "panel rather than one word in it.",
                    code = "CodeBlock(code = source, language = CodeLanguage.KOTLIN, filename = \"Total.kt\")",
                ) {
                    CodeBlock(
                        code = KotlinSample,
                        language = CodeLanguage.KOTLIN,
                        filename = "Total.kt",
                        highlightedLines = setOf(4),
                    )
                },
                Demo(
                    title = "Another language, another accent",
                    code =
                        "CodeBlock(code = source, language = CodeLanguage.JSON,\n" +
                            "    accent = Color(0xFF50B1FD), mode = CodeBlockMode.LIGHT)",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CodeBlock(
                            code = JsonSample,
                            language = CodeLanguage.JSON,
                            accent = Color(0xFF50B1FD),
                            mode = CodeBlockMode.LIGHT,
                        )
                        CodeBlock(
                            code = JsonSample,
                            language = CodeLanguage.JSON,
                            accent = Color(0xFF39D353),
                            mode = CodeBlockMode.DARK,
                            showsLineNumbers = false,
                        )
                    }
                },
                Demo(
                    title = "No frame at all",
                    note = "With no header there is nowhere to put the copy button, so it floats.",
                    code = "CodeBlock(code = source, showsFrame = false)",
                ) {
                    CodeBlock(
                        code = KotlinSample,
                        language = CodeLanguage.KOTLIN,
                        showsFrame = false,
                        showsLineNumbers = false,
                    )
                },
            ),
    ) {
        CodeBlock(
            code = "val a = 1",
            language = CodeLanguage.KOTLIN,
            showsHeader = false,
            showsCopyButton = false,
        )
    }
