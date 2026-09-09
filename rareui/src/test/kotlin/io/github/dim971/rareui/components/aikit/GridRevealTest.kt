/*
 * GridRevealTest.kt
 * The subdivision and the pacing, checked against `components/ui/grid-reveal.tsx`. The same
 * assertions run on iOS.
 */

package io.github.dim971.rareui.components.aikit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class GridRevealTreeTest {
    private fun leaves(cell: GridRevealCell): List<GridRevealCell> {
        val first = cell.first
        val second = cell.second
        return if (first == null || second == null) listOf(cell) else leaves(first) + leaves(second)
    }

    @Test
    fun `the tree ends up with the number of cells it was asked for`() {
        // Every split turns one cell into two, so a tree of n leaves has n minus one
        // branches, and the count rises one at a time rather than doubling.
        assertEquals(GRID_REVEAL_CELLS - 1, gridRevealBuildTree(1.0).branches.size)
    }

    @Test
    fun `the cells stay roughly square, whatever shape the frame is`() {
        for (aspect in listOf(0.5, 1.0, 1.78, 3.0)) {
            val tree = gridRevealBuildTree(aspect)
            for (leaf in leaves(tree.root)) {
                val ratio = (leaf.width * aspect) / leaf.height
                assertTrue("a cell came out at $ratio to one", ratio > 0.4 && ratio < 2.6)
            }
        }
    }

    @Test
    fun `the first few cells are already apart before the reveal starts`() {
        val tree = gridRevealBuildTree(1.0)
        assertTrue(tree.branches.take(GRID_REVEAL_OPENING_CELLS - 1).all { it.splitAt < 0 })
        // So the reveal never begins as a single rectangle sitting still.
        assertTrue(tree.branches[GRID_REVEAL_OPENING_CELLS - 1].splitAt > 0)
    }

    @Test
    fun `the splits are spread out and finish short of the end`() {
        val scheduled =
            gridRevealBuildTree(1.0)
                .branches
                .map { it.splitAt }
                .filter { it > 0 }
                .sorted()
        assertTrue(scheduled.first() > 0)
        assertTrue(scheduled.last() <= GRID_REVEAL_LAST_SPLIT + 1e-9)
        // Short of the end on purpose: the picture needs somewhere to arrive.
        assertTrue(GRID_REVEAL_LAST_SPLIT < 1)
    }

    @Test
    fun `every cell covers exactly the space its parent gave it`() {
        fun check(cell: GridRevealCell) {
            val first = cell.first ?: return
            val second = cell.second ?: return
            val area = first.width * first.height + second.width * second.height
            assertTrue(abs(area - cell.width * cell.height) < 1e-9)
            check(first)
            check(second)
        }
        check(gridRevealBuildTree(1.0).root)
    }

    @Test
    fun `reordering by detail keeps the pacing and only changes the order`() {
        val tree = gridRevealBuildTree(1.0)
        val before = tree.branches.map { it.splitAt }.sorted()

        // Giving alternate cells something to be interested in.
        tree.branches.forEachIndexed { index, cell -> cell.detail = (index % 7).toDouble() }
        gridRevealOrderByDetail(tree.branches, 0.0)

        assertEquals(
            "the same moments, handed to different cells",
            before,
            tree.branches.map { it.splitAt }.sorted(),
        )
    }

    @Test
    fun `a cell never splits before its parent has`() {
        val tree = gridRevealBuildTree(1.0)
        tree.branches.forEachIndexed { index, cell -> cell.detail = ((index * 37) % 11).toDouble() }
        gridRevealOrderByDetail(tree.branches, 0.0)

        for (cell in tree.branches) {
            val parent = cell.parent ?: continue
            if (!parent.hasChildren) continue
            assertTrue(
                "a cell was told to split before it existed",
                parent.splitAt <= cell.splitAt,
            )
        }
    }
}

class GridRevealPacingTest {
    @Test
    fun `a self-paced reveal creeps toward its ceiling and never reaches it`() {
        var previous = -1.0
        var seconds = 0.0
        while (seconds <= 60.0) {
            val progress = gridRevealSelfPaced(seconds, 6.0)
            assertTrue("it should always be moving", progress > previous)
            assertTrue("and never arrive on its own", progress < GRID_REVEAL_HOLD)
            previous = progress
            seconds += 0.5
        }
    }

    @Test
    fun `it holds short of the end, so the picture has somewhere to arrive`() {
        assertTrue(GRID_REVEAL_HOLD < 1)
        assertTrue(GRID_REVEAL_WAIT_CAP < GRID_REVEAL_HOLD)
    }

    @Test
    fun `a duration of nothing does not divide by nothing`() {
        assertTrue(gridRevealSelfPaced(1.0, 0.0).isFinite())
        assertTrue(gridRevealSelfPaced(1.0, -5.0).isFinite())
    }

    @Test
    fun `the smooth step eases at both ends and is flat outside them`() {
        assertEquals(0.0, gridRevealSmoothstep(0.35, 0.75, 0.2), 0.0)
        assertEquals(1.0, gridRevealSmoothstep(0.35, 0.75, 0.9), 0.0)
        assertEquals(0.5, gridRevealSmoothstep(0.35, 0.75, 0.55), 1e-9)
    }

    @Test
    fun `the placeholder grey breathes rather than sitting still`() {
        val still = gridRevealGrey(0.5, dark = false, clock = 0.0)
        val later = gridRevealGrey(0.5, dark = false, clock = 1.0)
        assertTrue("a grid of flat rectangles reads as a broken image", still != later)
        // But only just: three levels either way out of two hundred and fifty-five.
        assertTrue(abs(still - later) <= 6.0)
    }

    @Test
    fun `a dark appearance starts from a dark grey and a light one from a pale grey`() {
        assertTrue(gridRevealGrey(0.5, dark = true, clock = 0.0) < 60)
        assertTrue(gridRevealGrey(0.5, dark = false, clock = 0.0) > 200)
    }

    @Test
    fun `the same cell always gets the same tone, so the grid does not shimmer`() {
        assertEquals(gridRevealHash(1.5, 2.5, 3.5), gridRevealHash(1.5, 2.5, 3.5), 0.0)
        assertTrue(gridRevealHash(1.5, 2.5, 3.5) in 0.0..1.0)
    }
}
