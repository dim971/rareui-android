/*
 * OtpInput.kt
 * A port of upstream's `components/ui/otp-input.tsx`.
 *
 * A row of boxes for a one time code. Characters roll in from below as they are typed and
 * roll back out the way they came when they are deleted, a caret slides from box to box,
 * and the row shakes or draws itself a green outline depending on how the code was
 * received.
 *
 * One deliberate departure, recorded in docs/fidelity.md. Upstream gives every box its own
 * text input, because on the web that is the only way to put a real caret in a particular
 * box. Here the row is backed by a single field instead. That is what makes the platform
 * behave: the code from a message is offered by autofill, paste works, and the keyboard's
 * own delete key does the right thing. The price is upstream's arrow key editing in the
 * middle of a code, which has no equivalent on a touch keyboard anyway.
 *
 * A second, smaller one: the boxes have plain rounded corners rather than the continuous
 * ones upstream and the iOS port draw. Compose has no smooth corner shape of its own, and
 * one written here belongs with the components that need it most rather than with the
 * first component that would like it.
 */

package io.github.dim971.rareui.components.inputs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.dim971.rareui.theme.RareUiTheme
import io.github.dim971.rareui.theme.rareUiSpring
import io.github.dim971.rareui.theme.rememberRareUiReduceMotion
import kotlinx.coroutines.delay

/** Upstream's `ROLL_SPRING`. */
private const val ROLL_STIFFNESS = 500f
private const val ROLL_DAMPING = 34f

/** Upstream's `CARET_SPRING`. */
private const val CARET_STIFFNESS = 500f
private const val CARET_DAMPING = 40f

/** Half of upstream's 1.1 second blink, which is a square wave rather than a fade. */
private const val BLINK_HALF_PERIOD_MILLIS = 550L

/** How far out of the box a character starts and finishes, as a fraction of its height. */
private const val ROLL_DISTANCE = 1.1f

/** Upstream's `SHAKE`, over its 0.32 seconds. */
private const val SHAKE_MILLIS = 320

/** How long a box takes to draw its success outline, and when it starts. */
private const val RING_MILLIS = 450
private const val RING_DELAY_MILLIS = 150
private const val RING_STAGGER_MILLIS = 50

/**
 * A row of boxes for a one time code.
 *
 * ```kotlin
 * OtpInput(code = code, onCodeChange = { code = it }, status = status, onComplete = ::verify)
 * ```
 *
 * With animations turned off on the device the characters appear without rolling, the caret
 * does not blink, and the error shake is dropped, leaving the red outline to carry the
 * message.
 *
 * @param code the code typed so far. Anything in it that does not belong is dropped.
 * @param onCodeChange called with the code after it has been filtered and cut to length.
 * @param modifier the modifier to apply.
 * @param length how many characters the code has.
 * @param characterSet what may be typed.
 * @param size how large to draw the boxes.
 * @param status what the field is saying about the code.
 * @param mask whether to show bullets instead of the characters.
 * @param enabled whether the field can be typed into.
 * @param autoFocus whether to take the keyboard as soon as the row appears.
 * @param onComplete called with the code once the last box is filled.
 */
@Composable
public fun OtpInput(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    characterSet: OtpCharacterSet = OtpCharacterSet.NUMBERS,
    size: OtpSize = OtpSize.MEDIUM,
    status: OtpStatus = OtpStatus.IDLE,
    mask: Boolean = false,
    enabled: Boolean = true,
    autoFocus: Boolean = false,
    onComplete: ((String) -> Unit)? = null,
) {
    val colors = RareUiTheme.colors
    val reduceMotion = rememberRareUiReduceMotion()
    val boxes = maxOf(1, length)

    val kept = otpAccepted(code, boxes, characterSet)
    val characters = kept.toCharArray()
    val caretIndex = minOf(characters.size, boxes - 1)

    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(autoFocus, enabled) {
        if (autoFocus && enabled) focusRequester.requestFocus()
    }
    val showsCaret = focused && characters.size < boxes

    // Which way a character leaves: back down the way it came in when it was deleted, up
    // and out of the top when it was replaced, so the two never look like the same event.
    var cleared by remember { mutableStateOf(false) }
    var previousLength by remember { mutableStateOf(characters.size) }
    LaunchedEffect(characters.size) {
        cleared = characters.size < previousLength
        previousLength = characters.size
        if (characters.size == boxes) onComplete?.invoke(kept)
    }

    val shake = remember { Animatable(0f) }
    LaunchedEffect(status, reduceMotion) {
        if (status != OtpStatus.ERROR || reduceMotion) return@LaunchedEffect
        // Four equal steps, since Motion spaces a keyframe array evenly unless told
        // otherwise.
        shake.animateTo(
            targetValue = 0f,
            animationSpec =
                keyframes {
                    durationMillis = SHAKE_MILLIS
                    -5f at SHAKE_MILLIS / 4
                    4f at SHAKE_MILLIS / 2
                    -2f at SHAKE_MILLIS * 3 / 4
                    0f at SHAKE_MILLIS
                },
        )
    }

    var blinkOn by remember { mutableStateOf(true) }
    LaunchedEffect(caretIndex, reduceMotion, showsCaret) {
        // Restarted whenever it moves, so the caret is always solid at the moment it
        // arrives in a new box.
        blinkOn = true
        if (reduceMotion) return@LaunchedEffect
        while (true) {
            delay(BLINK_HALF_PERIOD_MILLIS)
            blinkOn = !blinkOn
        }
    }

    val caretOffset by animateDpAsState(
        targetValue = size.box * caretIndex + size.gap * caretIndex + size.box / 2 - 1.dp,
        animationSpec =
            if (reduceMotion) {
                snap()
            } else {
                rareUiSpring(
                    stiffness = CARET_STIFFNESS,
                    damping = CARET_DAMPING,
                )
            },
        label = "otp-caret",
    )

    BasicTextField(
        value = kept,
        onValueChange = { onCodeChange(otpAccepted(it, boxes, characterSet)) },
        modifier =
            modifier
                .focusRequester(focusRequester)
                .offset { IntOffset(shake.value.dp.roundToPx(), 0) }
                .alpha(if (enabled) 1f else 0.5f)
                .clearAndSetSemantics {
                    // The row is a stack of drawn boxes, which a screen reader would
                    // otherwise read one glyph at a time with no sense of the whole.
                    contentDescription =
                        "One time code, $boxes characters. " +
                        if (kept.isEmpty()) "Empty" else kept.toCharArray().joinToString(" ")
                },
        enabled = enabled,
        // The field's own text and caret are invisible: the row draws both itself.
        textStyle = TextStyle(color = Color.Transparent),
        cursorBrush = SolidColor(Color.Transparent),
        keyboardOptions =
            KeyboardOptions(
                keyboardType = if (characterSet == OtpCharacterSet.NUMBERS) KeyboardType.Number else KeyboardType.Ascii,
                capitalization =
                    if (characterSet == OtpCharacterSet.NUMBERS) {
                        KeyboardCapitalization.None
                    } else {
                        KeyboardCapitalization.Characters
                    },
                autoCorrectEnabled = false,
            ),
        singleLine = true,
        interactionSource = interaction,
    ) {
        Box {
            Row(horizontalArrangement = Arrangement.spacedBy(size.gap)) {
                repeat(boxes) { index ->
                    OtpBox(
                        character = characters.getOrNull(index),
                        index = index,
                        size = size,
                        status = status,
                        mask = mask,
                        cleared = cleared,
                        ringed = focused && index == caretIndex,
                        reduceMotion = reduceMotion,
                    )
                }
            }

            if (showsCaret) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = caretOffset)
                            .size(width = 2.dp, height = size.caretHeight)
                            .alpha(if (blinkOn) 1f else 0f)
                            .background(colors.foreground, CircleShape),
                )
            }
        }
    }
}

/** One box of the row: the character in it, the ring around it and the outline it draws. */
@Composable
@Suppress("LongParameterList")
private fun OtpBox(
    character: Char?,
    index: Int,
    size: OtpSize,
    status: OtpStatus,
    mask: Boolean,
    cleared: Boolean,
    ringed: Boolean,
    reduceMotion: Boolean,
) {
    val colors = RareUiTheme.colors
    val shape = RoundedCornerShape(size.radius)

    // Idle focus and error both draw a ring; success draws its own, stroke by stroke, so it
    // must not have a second one underneath.
    val ring =
        when {
            status == OtpStatus.ERROR -> colors.red.copy(alpha = 0.7f)
            status == OtpStatus.SUCCESS -> Color.Transparent
            ringed -> colors.glyph.copy(alpha = 0.5f)
            else -> Color.Transparent
        }

    // Each box starts a twentieth of a second after the one before, so the outline runs
    // along the row rather than appearing all at once.
    val drawn by animateFloatAsState(
        targetValue = if (status == OtpStatus.SUCCESS) 1f else 0f,
        animationSpec =
            if (reduceMotion || status != OtpStatus.SUCCESS) {
                snap()
            } else {
                tween(
                    durationMillis = RING_MILLIS,
                    delayMillis = RING_DELAY_MILLIS + index * RING_STAGGER_MILLIS,
                )
            },
        label = "otp-success-ring",
    )

    Box(
        modifier =
            Modifier
                .size(size.box)
                .background(colors.surface, shape)
                .border(2.dp, ring, shape)
                // A character on its way in or out is riding past the edge of the box, and
                // the box is what it should disappear behind.
                .clip(shape),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = character,
            transitionSpec = {
                if (reduceMotion) {
                    fadeIn(snap()) togetherWith fadeOut(snap())
                } else {
                    val roll = rareUiSpring<IntOffset>(stiffness = ROLL_STIFFNESS, damping = ROLL_DAMPING)
                    slideInVertically(roll) { (it * ROLL_DISTANCE).toInt() } togetherWith
                        slideOutVertically(roll) {
                            if (cleared) (it * ROLL_DISTANCE).toInt() else (-it * ROLL_DISTANCE).toInt()
                        }
                }
            },
            label = "otp-character",
        ) { glyph ->
            if (glyph != null) {
                BasicText(
                    text = if (mask) "•" else glyph.toString(),
                    style =
                        TextStyle(
                            fontSize = size.fontSize,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.foreground,
                            textAlign = TextAlign.Center,
                        ),
                )
            } else {
                Box(Modifier.size(size.box))
            }
        }

        if (drawn > 0f) {
            Box(
                modifier =
                    Modifier
                        .size(size.box)
                        .padding(1.dp)
                        .drawWithContent {
                            // Revealing the stroke behind a growing rectangle draws the
                            // outline from the left, which is what a trimmed path does on a
                            // rounded rectangle without any of the corner cases a trim has
                            // at the corners themselves.
                            clipRect(right = this.size.width * drawn) { this@drawWithContent.drawContent() }
                        }.border(2.dp, colors.green, RoundedCornerShape(size.radius - 1.dp)),
            )
        }
    }
}
