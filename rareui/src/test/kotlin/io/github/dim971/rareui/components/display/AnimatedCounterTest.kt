/*
 * AnimatedCounterTest.kt
 * The counter is mostly arithmetic wearing an animation, and the arithmetic is where it
 * can be wrong without looking wrong. The same cases are asserted in the iOS twin, which
 * is what keeps the two ports in step.
 */

package io.github.dim971.rareui.components.display

import io.github.dim971.rareui.core.rareUiMod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CounterShapeTest {
    @Test
    fun `a plain integer needs as many columns as it has digits`() {
        val shape = counterShape(1234.0, decimals = 0, padStart = 1, duration = 0.6)
        assertEquals(1234L, shape.scaled)
        assertEquals(0, shape.places)
        assertEquals(4, shape.width)
    }

    @Test
    fun `decimals are scaled into the whole number and counted as columns`() {
        val shape = counterShape(12.34, decimals = 2, padStart = 1, duration = 0.6)
        assertEquals(1234L, shape.scaled)
        assertEquals(2, shape.places)
        assertEquals(4, shape.width)
    }

    @Test
    fun `padStart sets a floor on the width, which is what stops a timer jittering`() {
        assertEquals(6, counterShape(7.0, 0, 6, 0.6).width)
    }

    @Test
    fun `the sign is kept separately, so the columns are the same either way`() {
        assertEquals(counterShape(42.0, 0, 1, 0.6).scaled, counterShape(-42.0, 0, 1, 0.6).scaled)
        assertEquals(-42.0, counterShape(-42.0, 0, 1, 0.6).amount, 0.0)
    }

    @Test
    fun `a value that is not a number becomes zero rather than poisoning every comparison`() {
        assertEquals(0.0, counterShape(Double.NaN, 0, 1, 0.6).amount, 0.0)
        assertEquals(0.0, counterShape(Double.POSITIVE_INFINITY, 0, 1, 0.6).amount, 0.0)
    }

    @Test
    fun `decimals, padding and duration are all confined to what can actually be drawn`() {
        val wild = counterShape(1.0, decimals = 99, padStart = 999, duration = 1e6)
        assertEquals(15, wild.places)
        assertEquals(60.0, wild.pace, 0.0)
        // 15 decimal places plus the 24 place padding ceiling.
        assertEquals(39, wild.width)

        val tiny = counterShape(1.0, decimals = -5, padStart = -5, duration = 0.0)
        assertEquals(0, tiny.places)
        assertEquals(0.01, tiny.pace, 0.0)
        assertEquals(1, tiny.width)
    }
}

class CounterGroupingTest {
    @Test
    fun `western grouping is threes all the way up`() {
        assertEquals("1", counterGroup("1", ",", CounterGrouping.WESTERN))
        assertEquals("123", counterGroup("123", ",", CounterGrouping.WESTERN))
        assertEquals("1,234", counterGroup("1234", ",", CounterGrouping.WESTERN))
        assertEquals("1,234,567", counterGroup("1234567", ",", CounterGrouping.WESTERN))
        assertEquals("1,000,000", counterGroup("1000000", ",", CounterGrouping.WESTERN))
    }

    @Test
    fun `indian grouping is three at the end and pairs above it`() {
        assertEquals("123", counterGroup("123", ",", CounterGrouping.INDIAN))
        assertEquals("1,234", counterGroup("1234", ",", CounterGrouping.INDIAN))
        assertEquals("12,34,567", counterGroup("1234567", ",", CounterGrouping.INDIAN))
        assertEquals("12,34,56,789", counterGroup("123456789", ",", CounterGrouping.INDIAN))
    }

    @Test
    fun `an empty separator leaves the digits alone`() {
        assertEquals("1234567", counterGroup("1234567", "", CounterGrouping.WESTERN))
        assertEquals("1234567", counterGroup("1234567", "", CounterGrouping.INDIAN))
    }

    @Test
    fun `a separator of more than one character is inserted whole`() {
        assertEquals("1 234 567", counterGroup("1234567", " ", CounterGrouping.WESTERN))
    }
}

class CounterFormattingTest {
    private fun format(
        value: Double,
        decimals: Int = 0,
        padStart: Int = 1,
        separator: String = ",",
        decimalSeparator: String = ".",
        grouping: CounterGrouping = CounterGrouping.WESTERN,
    ): String =
        counterFormat(
            counterShape(value, decimals, padStart, 0.6),
            separator,
            decimalSeparator,
            grouping,
        )

    @Test
    fun `whole numbers are grouped and nothing else`() {
        assertEquals("0", format(0.0))
        assertEquals("1,234", format(1234.0))
        // The sign is drawn separately.
        assertEquals("1,234", format(-1234.0))
    }

    @Test
    fun `decimals keep their trailing zeros, which is the point of a fixed width`() {
        assertEquals("12.50", format(12.5, decimals = 2))
        assertEquals("0.50", format(0.5, decimals = 2))
        assertEquals("1,234.50", format(1234.5, decimals = 2))
    }

    @Test
    fun `a value smaller than the smallest shown place rounds into it`() {
        assertEquals("0.00", format(0.004, decimals = 2))
        // Halves round away from zero, as in JavaScript.
        assertEquals("0.01", format(0.005, decimals = 2))
        assertEquals("1.00", format(0.9999, decimals = 2))
    }

    @Test
    fun `padding fills from the left with zeros`() {
        assertEquals("0007", format(7.0, padStart = 4, separator = ""))
        assertEquals("007.5", format(7.5, decimals = 1, padStart = 3, separator = ""))
    }

    @Test
    fun `the decimal separator is independent of the grouping one`() {
        assertEquals("1.234,50", format(1234.5, decimals = 2, separator = ".", decimalSeparator = ","))
    }
}

class CounterCellTest {
    @Test
    fun `digits are keyed by distance from the right, so a new place shifts rather than remounts`() {
        val narrow = counterCells("999", 3).filterIsInstance<CounterCell.Digit>().map { it.place }
        val wide = counterCells("1,000", 4).filterIsInstance<CounterCell.Digit>().map { it.place }
        assertEquals(listOf(3, 2, 1), narrow)
        assertEquals(listOf(4, 3, 2, 1), wide)
    }

    @Test
    fun `a separator is a column too, and does not advance the place`() {
        val cells = counterCells("1,234", 4)
        assertEquals(5, cells.size)
        val mark = cells[1] as CounterCell.Mark
        assertEquals(',', mark.character)
        assertEquals(3, mark.place)
        assertEquals(0, mark.run)
    }

    @Test
    fun `every column has an identity of its own, including repeated separators`() {
        val cells = counterCells("1,234,567.89", 9)
        assertEquals(cells.size, cells.map { it.key }.toSet().size)
    }
}

class DigitWheelTest {
    @Test
    fun `a wheel already heading at its face is left alone`() {
        assertEquals(4.0, wheelGoal(4.0, 4.0, 4, 1.0), 0.0)
        assertEquals(14.0, wheelGoal(14.0, 14.0, 4, -1.0), 0.0)
    }

    @Test
    fun `turning up adds the shortest forward distance to the face`() {
        assertEquals(5.0, wheelGoal(3.0, 3.0, 5, 1.0), 0.0)
        assertEquals(11.0, wheelGoal(8.0, 8.0, 1, 1.0), 0.0)
    }

    @Test
    fun `turning down subtracts the shortest backward distance`() {
        assertEquals(3.0, wheelGoal(5.0, 5.0, 3, -1.0), 0.0)
        assertEquals(-2.0, wheelGoal(1.0, 1.0, 8, -1.0), 0.0)
    }

    @Test
    fun `nine to zero rolls forward into the repeated face rather than back through eight`() {
        // The eleventh face is a second zero, so a single step forward lands on it and the
        // wheel never travels the nine faces backward to reach the same glyph.
        assertEquals(10.0, wheelGoal(9.0, 9.0, 0, 1.0), 0.0)
        assertEquals(9.0, wheelGoal(10.0, 10.0, 9, -1.0), 0.0)
    }

    @Test
    fun `aiming starts from where the wheel is, not from where it was sent`() {
        val goal = wheelGoal(7.0, 5.5, 9, 1.0)
        assertEquals(9.0, goal, 1e-9)
        assertEquals(3.5, goal - 5.5, 1e-9)
    }

    @Test
    fun `the aim always lands on the requested face`() {
        for (digit in 0..9) {
            var start = -20.0
            while (start <= 20.0) {
                for (heading in listOf(1.0, -1.0)) {
                    val goal = wheelGoal(start, start, digit, heading)
                    assertTrue(
                        "aiming at $digit from $start heading $heading landed on $goal",
                        abs(rareUiMod(goal, 10.0) - digit) < 1e-9,
                    )
                }
                start += 0.5
            }
        }
    }

    @Test
    fun `a reversal alone does not send every wheel the long way round`() {
        assertNotEquals(0.0, wheelGoal(3.0, 3.0, 5, -1.0), 0.0)
        assertEquals(3.0, wheelGoal(3.0, 3.0, 3, -1.0), 0.0)
    }
}
