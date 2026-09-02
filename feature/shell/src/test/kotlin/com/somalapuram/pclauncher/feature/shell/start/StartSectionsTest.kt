package com.somalapuram.pclauncher.feature.shell.start

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The caret crossing between Recent and All apps (recent-apps.md requirement 7).
 *
 * The interesting cases are all at the seam: a short Recent row leaves holes in the flattened list,
 * and a move that lands in one must not strand the caret on nothing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StartSectionsTest {

    private val columns = 5
    private fun apps(vararg labels: String) = labels.map { app(it) }

    private fun sections(recent: Int, all: Int) = StartSections(
        recent = (1..recent).map { app("R$it") },
        all = (1..all).map { app("A$it") },
        columns = columns,
    )

    @Test
    fun `no recent row means the list is just the apps`() {
        val s = StartSections(recent = emptyList(), all = apps("A", "B"), columns = columns)
        assertEquals(listOf("A", "B"), s.navigable.map { it?.label })
        assertEquals(0, s.recentSlots)
    }

    @Test
    fun `a short recent row is padded to a whole row`() {
        val s = sections(recent = 2, all = 3)
        assertEquals(columns, s.recentSlots)
        assertEquals(
            listOf("R1", "R2", null, null, null, "A1", "A2", "A3"),
            s.navigable.map { it?.label },
        )
    }

    @Test
    fun `a full recent row needs no padding`() {
        val s = sections(recent = 5, all = 1)
        assertEquals(listOf("R1", "R2", "R3", "R4", "R5", "A1"), s.navigable.map { it?.label })
    }

    /** The grid draws only All apps, so its indices are offset by the whole recent row. */
    @Test
    fun `grid index skips the recent row`() {
        val s = sections(recent = 2, all = 3)
        assertEquals(0, s.gridIndexFor(columns))
        assertEquals(2, s.gridIndexFor(columns + 2))
    }

    /** The recent row is always in view; scrolling the grid to it would yank the menu about. */
    @Test
    fun `a selection on the recent row has no grid index`() {
        val s = sections(recent = 2, all = 3)
        assertNull(s.gridIndexFor(0))
        assertNull(s.gridIndexFor(1))
        assertNull(s.gridIndexFor(null))
    }

    @Test
    fun `right from the last recent app stays put rather than jumping to all apps`() {
        val s = sections(recent = 2, all = 6)
        assertEquals(1, selectionAfterMoveInSections(1, GridMove.Right, s))
    }

    @Test
    fun `right within a full recent row moves along it`() {
        val s = sections(recent = 5, all = 6)
        assertEquals(2, selectionAfterMoveInSections(1, GridMove.Right, s))
    }

    @Test
    fun `down from a recent app reaches all apps`() {
        val s = sections(recent = 2, all = 6)
        assertEquals(columns, selectionAfterMoveInSections(0, GridMove.Down, s))
    }

    /**
     * The hole case. Below R2 is padding, and stopping there would put the caret on an empty slot;
     * it has to carry on into the grid.
     */
    @Test
    fun `down through a padding slot lands on a real app`() {
        val s = sections(recent = 2, all = 6)
        val landed = selectionAfterMoveInSections(1, GridMove.Down, s)
        assertEquals("A2", s.entryAt(landed)?.label)
    }

    @Test
    fun `up from all apps reaches the recent row`() {
        val s = sections(recent = 5, all = 6)
        assertEquals(0, selectionAfterMoveInSections(columns, GridMove.Up, s))
    }

    /**
     * Up from a column with no recent app above it. There is nothing further up to reach, so the
     * caret stays where it is rather than settling on the empty slot.
     */
    @Test
    fun `up into a padding slot with nothing beyond it stays put`() {
        val s = sections(recent = 2, all = 6)
        val from = columns + 3

        val landed = selectionAfterMoveInSections(from, GridMove.Up, s)

        assertEquals(from, landed)
        assertEquals("A4", s.entryAt(landed)?.label)
    }

    @Test
    fun `the first press selects rather than moves`() {
        val s = sections(recent = 2, all = 6)
        assertEquals(0, selectionAfterMoveInSections(null, GridMove.Down, s))
    }

    @Test
    fun `an empty menu has nothing to select`() {
        val s = StartSections(recent = emptyList(), all = emptyList(), columns = columns)
        assertNull(selectionAfterMoveInSections(null, GridMove.Down, s))
    }

    @Test
    fun `a search hides the recent row`() {
        val recent = apps("Chrome", "Clock")
        assertEquals(emptyList<String>(), recentFor(recent, "ca").map { it.label })
    }

    @Test
    fun `an empty query shows the recent row`() {
        val recent = apps("Chrome", "Clock")
        assertEquals(listOf("Chrome", "Clock"), recentFor(recent, "").map { it.label })
    }

    /** Whitespace is not a search. Clearing back to spaces must bring the row back. */
    @Test
    fun `a blank query shows the recent row`() {
        assertEquals(1, recentFor(apps("Chrome"), "   ").size)
    }

    @Test
    fun `entryAt is null outside the list`() {
        val s = sections(recent = 1, all = 1)
        assertNull(s.entryAt(99))
        assertNull(s.entryAt(null))
    }
}
