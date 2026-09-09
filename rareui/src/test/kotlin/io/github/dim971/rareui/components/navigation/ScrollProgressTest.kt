/*
 * ScrollProgressTest.kt
 * Which name the pill shows, from the `activeSection` fallback in
 * `components/ui/scroll-progress.tsx`.
 */

package io.github.dim971.rareui.components.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class ScrollProgressTest {
    private val sections =
        listOf(
            ScrollProgressSection("intro", "Introduction"),
            ScrollProgressSection("install", "Installing"),
            ScrollProgressSection("theme", "Theming"),
        )

    @Test
    fun `the pill names the section being read`() {
        assertEquals("Installing", scrollProgressLabel(sections, "install"))
    }

    @Test
    fun `nothing being read yet names the first section`() {
        // Which is the state on the way into a page, before anything has scrolled. An
        // empty pill would read as a fault rather than as a beginning.
        assertEquals("Introduction", scrollProgressLabel(sections, null))
    }

    @Test
    fun `a selection naming nothing in the list falls back rather than emptying the pill`() {
        assertEquals("Introduction", scrollProgressLabel(sections, "gone"))
    }

    @Test
    fun `a page with no sections shows nothing rather than throwing`() {
        assertEquals("", scrollProgressLabel(emptyList(), "intro"))
        assertEquals("", scrollProgressLabel(emptyList(), null))
    }
}
