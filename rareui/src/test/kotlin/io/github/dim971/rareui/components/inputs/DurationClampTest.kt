/*
 * DurationClampTest.kt
 * What the two fields will accept, from the clamping in
 * `components/ui/duration-picker.tsx`. The same assertions run on iOS.
 */

package io.github.dim971.rareui.components.inputs

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationClampTest {
    @Test
    fun `a number inside the range is left alone`() {
        assertEquals(3, durationClamp(3, 24))
        assertEquals(0, durationClamp(0, 24))
        assertEquals(24, durationClamp(24, 24))
    }

    @Test
    fun `a number outside it is brought back in`() {
        assertEquals(24, durationClamp(99, 24))
        assertEquals(0, durationClamp(-5, 24))
    }

    @Test
    fun `the two fields have their own limits`() {
        assertEquals(24, durationClamp(45, 24))
        assertEquals(45, durationClamp(45, 60))
    }
}
