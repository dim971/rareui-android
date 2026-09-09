/*
 * RareUiTest.kt
 * The suites in this module cover the parts of the port that are pure maths and therefore
 * checkable exactly: colour ramps, easing, layout arithmetic and the physics solvers.
 * Motion that can only be judged by eye is checked in the showcase against rareui.com,
 * not here.
 */

package io.github.dim971.rareui

import org.junit.Assert.assertEquals
import org.junit.Test

class RareUiTest {
    @Test
    fun `the reported version matches the tag this library ships under`() {
        assertEquals("0.1.0", RareUi.VERSION)
    }
}
