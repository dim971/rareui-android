/*
 * CodeBlock.kt
 * A port of upstream's `components/ui/code-block.tsx`.
 *
 * A framed panel of source code with a header, a line-number gutter and a copy button. The
 * whole syntax theme is built from one colour: see CodeBlockTheme.
 */

package io.github.dim971.rareui.components.display

import android.content.ClipData
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.rareui.theme.rememberRareUiReduceMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Which appearance a [CodeBlock] draws in. */
public enum class CodeBlockMode {
    /** Follow the system. */
    AUTOMATIC,

    /** Always dark. */
    DARK,

    /** Always light. */
    LIGHT,
}

/** How long the button says it has copied, before going back to offering to. */
private const val COPY_RESET_MILLIS = 1_800L

/** Upstream's default accent. */
private val DefaultCodeAccent = Color(0xFFF75001)

/**
 * A panel of source code, coloured from a single accent.
 *
 * ```kotlin
 * CodeBlock(code = source, language = CodeLanguage.KOTLIN, filename = "Main.kt")
 * ```
 *
 * With animations turned off on the device the copy button swaps its icon without
 * springing.
 *
 * @param code the source.
 * @param modifier the modifier to apply.
 * @param language what it is written in.
 * @param accent the colour the whole theme is built from.
 * @param mode which appearance to draw in.
 * @param filename the name in the header. Falls back to the language's own.
 * @param showsFrame whether to draw the panel, its border and its header at all.
 * @param showsHeader whether to draw the header. Ignored when the frame is off.
 * @param showsLineNumbers whether to draw the gutter.
 * @param showsCopyButton whether to offer to copy.
 * @param highlightedLines one-based line numbers to wash with the accent.
 */
@Composable
@Suppress("LongParameterList")
public fun CodeBlock(
    code: String,
    modifier: Modifier = Modifier,
    language: CodeLanguage = CodeLanguage.KOTLIN,
    accent: Color = DefaultCodeAccent,
    mode: CodeBlockMode = CodeBlockMode.AUTOMATIC,
    filename: String? = null,
    showsFrame: Boolean = true,
    showsHeader: Boolean = true,
    showsLineNumbers: Boolean = true,
    showsCopyButton: Boolean = true,
    highlightedLines: Set<Int> = emptySet(),
) {
    val systemDark = isSystemInDarkTheme()
    val dark =
        when (mode) {
            CodeBlockMode.DARK -> true
            CodeBlockMode.LIGHT -> false
            CodeBlockMode.AUTOMATIC -> systemDark
        }

    val theme = remember(accent, dark) { codeBlockTheme(accent, dark) }
    val lines = remember(code, language) { codeTokenise(code, language) }
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier =
            modifier
                .then(if (showsFrame) Modifier.background(theme.background, shape) else Modifier)
                .then(if (showsFrame) Modifier.border(1.dp, theme.border, shape) else Modifier)
                .clip(shape),
    ) {
        if (showsFrame && showsHeader) {
            CodeBlockHeader(
                title = filename ?: language.displayName,
                theme = theme,
                showsCopyButton = showsCopyButton,
                code = code,
            )
        }

        Box {
            Column(
                modifier =
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                lines.forEachIndexed { index, tokens ->
                    CodeLine(
                        number = index + 1,
                        tokens = tokens,
                        theme = theme,
                        showsLineNumbers = showsLineNumbers,
                        highlighted = highlightedLines.contains(index + 1),
                    )
                }
            }

            // With no header there is nowhere to put the button, so it floats over the code.
            if (showsCopyButton && (!showsFrame || !showsHeader)) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(theme.headerBackground, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                ) {
                    CopyButton(code, theme)
                }
            }
        }
    }
}

/** The bar above the code, naming the file and offering to copy it. */
@Composable
private fun CodeBlockHeader(
    title: String,
    theme: CodeBlockTheme,
    showsCopyButton: Boolean,
    code: String,
) {
    Column {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(theme.headerBackground)
                    .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = title,
                style =
                    TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        color = theme.muted,
                    ),
                modifier = Modifier.weight(1f),
            )
            if (showsCopyButton) CopyButton(code, theme)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(theme.border))
    }
}

/** One line: its number, and the coloured runs it is made of. */
@Composable
private fun CodeLine(
    number: Int,
    tokens: List<CodeToken>,
    theme: CodeBlockTheme,
    showsLineNumbers: Boolean,
    highlighted: Boolean,
) {
    val body = TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace)

    Row(
        modifier =
            Modifier
                .then(if (highlighted) Modifier.background(theme.lineWash) else Modifier)
                .padding(horizontal = 14.dp, vertical = 1.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (showsLineNumbers) {
            BasicText(
                text = number.toString(),
                style =
                    body.copy(
                        fontSize = 12.sp,
                        color = theme.gutter,
                        textAlign = TextAlign.End,
                    ),
                modifier = Modifier.width(34.dp).padding(end = 12.dp),
            )
        }

        BasicText(text = codeLineText(tokens, theme), style = body)
        Spacer(Modifier.width(14.dp))
    }
}

/**
 * One line, coloured a run at a time.
 *
 * @param tokens the runs.
 * @param theme the palette to colour them from.
 * @return the line, ready to draw.
 */
internal fun codeLineText(
    tokens: List<CodeToken>,
    theme: CodeBlockTheme,
): AnnotatedString {
    // An empty line still needs a height, so it gets a space rather than nothing.
    if (tokens.isEmpty()) return AnnotatedString(" ")

    return buildAnnotatedString {
        tokens.forEach { token ->
            withStyle(
                SpanStyle(
                    color = theme.color(token.kind),
                    fontStyle = if (token.kind.isItalic) FontStyle.Italic else FontStyle.Normal,
                ),
            ) {
                append(token.text)
            }
        }
    }
}

/** The copy button, which becomes a tick for a moment once it has copied. */
@Composable
private fun CopyButton(
    code: String,
    theme: CodeBlockTheme,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val reduceMotion = rememberRareUiReduceMotion()
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (!copied) return@LaunchedEffect
        delay(COPY_RESET_MILLIS)
        copied = false
    }

    val swap by animateFloatAsState(
        targetValue = if (copied) 1f else 0f,
        animationSpec = if (reduceMotion) snap() else tween(300),
        label = "code-copy",
    )

    Canvas(
        modifier =
            Modifier
                .size(20.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        scope.launch {
                            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("code", code)))
                        }
                        copied = true
                    },
                ).semantics {
                    role = Role.Button
                    contentDescription = if (copied) "Copied" else "Copy code"
                },
    ) {
        val stroke = 1.6.dp.toPx()
        if (swap < 1f) {
            // Two overlapping sheets, which is what a copy glyph is.
            val inset = 3.dp.toPx()
            val sheet = size.minDimension - inset * 2 - 3.dp.toPx()
            drawRoundRect(
                color = theme.muted.copy(alpha = theme.muted.alpha * (1f - swap)),
                topLeft = Offset(inset + 3.dp.toPx(), inset),
                size = Size(sheet, sheet),
                cornerRadius = CornerRadius(2.dp.toPx()),
                style = Stroke(width = stroke),
            )
            drawRoundRect(
                color = theme.muted.copy(alpha = theme.muted.alpha * (1f - swap)),
                topLeft = Offset(inset, inset + 3.dp.toPx()),
                size = Size(sheet, sheet),
                cornerRadius = CornerRadius(2.dp.toPx()),
                style = Stroke(width = stroke),
            )
        }
        if (swap > 0f) {
            val left = size.width * 0.22f
            val middle = size.width * 0.42f
            val right = size.width * 0.8f
            drawLine(
                color = theme.accent.copy(alpha = swap),
                start = Offset(left, size.height * 0.52f),
                end = Offset(middle, size.height * 0.74f),
                strokeWidth = stroke * 1.4f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = theme.accent.copy(alpha = swap),
                start = Offset(middle, size.height * 0.74f),
                end = Offset(right, size.height * 0.28f),
                strokeWidth = stroke * 1.4f,
                cap = StrokeCap.Round,
            )
        }
    }
}
