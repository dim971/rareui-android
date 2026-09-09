/*
 * HookRailTest.kt
 * The rail's geometry, checked against `components/ui/hook-sidebar.tsx`. The same
 * assertions run on iOS, less the one for the hook's outline: a Compose `Path` delegates
 * to an Android one, which in a plain unit test is a stub that answers every question with
 * zero. What can be checked without a canvas is checked here instead.
 */

package io.github.dim971.rareui.components.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class HookRailTest {
    @Test
    fun `pointing below the selection, the faint rail carries on from where the accent one stops`() {
        // The two together read as one line reaching further, rather than as two lines
        // overlapping each other.
        assertEquals(40.0, hookGhostRailStart(activeY = 40.0, hoverY = 100.0, corner = 6.0), 0.0)
    }

    @Test
    fun `pointing above the selection, only the hook is drawn`() {
        // The accent rail already covers everything above the selection, so the faint one
        // starts a corner short of its own row and draws nothing but the turn.
        assertEquals(34.0, hookGhostRailStart(activeY = 100.0, hoverY = 40.0, corner = 6.0), 0.0)
    }

    @Test
    fun `the faint rail never starts above the top of the list`() {
        assertEquals(0.0, hookGhostRailStart(activeY = 100.0, hoverY = 4.0, corner = 6.0), 0.0)
    }

    @Test
    fun `with nothing to point at there is nothing to start from`() {
        assertEquals(40.0, hookGhostRailStart(activeY = 40.0, hoverY = null, corner = 6.0), 0.0)
        assertEquals(0.0, hookGhostRailStart(activeY = null, hoverY = 40.0, corner = 6.0), 0.0)
        assertEquals(0.0, hookGhostRailStart(activeY = null, hoverY = null, corner = 6.0), 0.0)
    }

    @Test
    fun `pointing at the selection itself puts the faint rail exactly under the accent one`() {
        // Which is why the component hides it rather than drawing it: two identical rails
        // on top of each other only darken the dashes.
        assertEquals(34.0, hookGhostRailStart(activeY = 40.0, hoverY = 40.0, corner = 6.0), 0.0)
    }

    @Test
    fun `the hairline stops a corner short of the row, because the hook covers the rest`() {
        assertEquals(54.0, hookRailLength(from = 0.0, to = 60.0, corner = 6.0), 0.0)
        assertEquals(14.0, hookRailLength(from = 40.0, to = 60.0, corner = 6.0), 0.0)
    }

    @Test
    fun `a rail with nowhere to go draws no line at all`() {
        // A hook alone, which is what the faint rail is above the selection.
        assertEquals(0.0, hookRailLength(from = 34.0, to = 40.0, corner = 6.0), 0.0)
        assertEquals(0.0, hookRailLength(from = 100.0, to = 40.0, corner = 6.0), 0.0)
    }
}
