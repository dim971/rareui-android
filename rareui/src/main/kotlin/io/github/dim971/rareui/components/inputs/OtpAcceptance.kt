/*
 * OtpAcceptance.kt
 * What a one time code field will take and how large it draws itself, from the `PATTERNS`
 * table and the paste and autofill handling in upstream's `components/ui/otp-input.tsx`.
 */

package io.github.dim971.rareui.components.inputs

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** What the field is saying about the code in it. */
public enum class OtpStatus {
    /** Nothing to report. */
    IDLE,

    /** The code was accepted. Each box draws itself a green outline in turn. */
    SUCCESS,

    /** The code was refused. The row shakes and turns red. */
    ERROR,
}

/** What may be typed into a one time code. */
public enum class OtpCharacterSet {
    /** Digits only, with a number pad to match. */
    NUMBERS,

    /** Letters only. */
    LETTERS,

    /** Letters and digits. */
    ALPHANUMERIC,
    ;

    /**
     * Whether a character belongs in the code.
     *
     * Upstream's patterns are `[0-9]` and `[a-zA-Z]`, so plain ASCII is the whole of it.
     * Taking an Arabic Indic digit would put a character in the box that the code being
     * checked against will never match.
     *
     * @param character the character to judge.
     */
    internal fun accepts(character: Char): Boolean {
        val ascii = character.code < 128
        return when (this) {
            NUMBERS -> ascii && character.isDigit()
            LETTERS -> ascii && character.isLetter()
            ALPHANUMERIC -> ascii && character.isLetterOrDigit()
        }
    }
}

/** How large an [OtpInput] is drawn. */
public enum class OtpSize(
    /** The width and height of a box. */
    internal val box: Dp,
    /** The corner radius of a box. */
    internal val radius: Dp,
    /** The character's type size. */
    internal val fontSize: TextUnit,
    /** The caret's height. */
    internal val caretHeight: Dp,
    /** The space between two boxes. */
    internal val gap: Dp,
) {
    /** The smallest, at 40 points a box. */
    SMALL(box = 40.dp, radius = 8.dp, fontSize = 16.sp, caretHeight = 20.dp, gap = 6.dp),

    /** The default, at 48. */
    MEDIUM(box = 48.dp, radius = 12.dp, fontSize = 18.sp, caretHeight = 24.dp, gap = 8.dp),

    /** The largest, at 56. */
    LARGE(box = 56.dp, radius = 16.dp, fontSize = 20.sp, caretHeight = 28.dp, gap = 10.dp),
}

/**
 * What survives of a string once it has to be a code.
 *
 * Everything that does not belong in the character set is dropped rather than refused,
 * which is what lets a pasted or autofilled code arrive with its own punctuation and still
 * land in the boxes. What is left is cut to the row's length.
 *
 * @param raw whatever arrived in the field.
 * @param length how many characters the row holds.
 * @param characterSet what belongs in the code.
 * @return the code to show.
 */
internal fun otpAccepted(
    raw: String,
    length: Int,
    characterSet: OtpCharacterSet,
): String = raw.filter(characterSet::accepts).take(maxOf(0, length))
