/*
 * OtpAcceptanceTest.kt
 * What a code field will accept, checked against the `PATTERNS` table and the paste and
 * autofill handling in `components/ui/otp-input.tsx`. The same assertions run on iOS.
 */

package io.github.dim971.rareui.components.inputs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpCharacterSetTest {
    @Test
    fun `digits take digits and nothing else`() {
        assertTrue(OtpCharacterSet.NUMBERS.accepts('0'))
        assertTrue(OtpCharacterSet.NUMBERS.accepts('9'))
        assertFalse(OtpCharacterSet.NUMBERS.accepts('a'))
        assertFalse(OtpCharacterSet.NUMBERS.accepts('-'))
    }

    @Test
    fun `letters take either case and no digits`() {
        assertTrue(OtpCharacterSet.LETTERS.accepts('a'))
        assertTrue(OtpCharacterSet.LETTERS.accepts('Z'))
        assertFalse(OtpCharacterSet.LETTERS.accepts('4'))
    }

    @Test
    fun `the mixed set takes both`() {
        assertTrue(OtpCharacterSet.ALPHANUMERIC.accepts('a'))
        assertTrue(OtpCharacterSet.ALPHANUMERIC.accepts('7'))
        assertFalse(OtpCharacterSet.ALPHANUMERIC.accepts('_'))
    }

    @Test
    fun `only plain ASCII counts, so a digit from another script is not a digit here`() {
        // Upstream's patterns are [0-9] and [a-zA-Z], which these are. Accepting an Arabic
        // Indic digit would put a character in the box that the code will never match.
        assertFalse(OtpCharacterSet.NUMBERS.accepts('٠'))
        assertFalse(OtpCharacterSet.LETTERS.accepts('é'))
    }
}

class OtpAcceptanceTest {
    @Test
    fun `anything that does not belong is dropped rather than refused`() {
        // This is what lets a pasted or autofilled code arrive with its own punctuation and
        // still land in the boxes.
        assertEquals("123456", otpAccepted("123-456", 6, OtpCharacterSet.NUMBERS))
        assertEquals("4821", otpAccepted("Code: 4821", 6, OtpCharacterSet.NUMBERS))
        assertEquals("ab", otpAccepted("a1b2", 6, OtpCharacterSet.LETTERS))
    }

    @Test
    fun `the code is cut to the length of the row`() {
        assertEquals("123456", otpAccepted("123456789", 6, OtpCharacterSet.NUMBERS))
        assertEquals("12", otpAccepted("12", 6, OtpCharacterSet.NUMBERS))
    }

    @Test
    fun `nothing usable leaves nothing`() {
        assertEquals("", otpAccepted("", 6, OtpCharacterSet.NUMBERS))
        assertEquals("", otpAccepted("hello", 6, OtpCharacterSet.NUMBERS))
    }

    @Test
    fun `a nonsensical length yields nothing rather than throwing`() {
        assertEquals("", otpAccepted("123", 0, OtpCharacterSet.NUMBERS))
        assertEquals("", otpAccepted("123", -4, OtpCharacterSet.NUMBERS))
    }
}

class OtpSizeTest {
    @Test
    fun `everything about a box grows together`() {
        var previous: OtpSize? = null
        for (size in OtpSize.entries) {
            previous?.let {
                assertTrue(size.box > it.box)
                assertTrue(size.radius > it.radius)
                assertTrue(size.fontSize.value > it.fontSize.value)
                assertTrue(size.caretHeight > it.caretHeight)
                assertTrue(size.gap > it.gap)
            }
            previous = size
        }
    }

    @Test
    fun `the caret is shorter than the box it sits in`() {
        for (size in OtpSize.entries) {
            assertTrue(size.caretHeight < size.box)
        }
    }
}
