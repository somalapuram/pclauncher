package com.somalapuram.pclauncher.feature.shell.input

import androidx.compose.ui.input.key.Key
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The shell's key bindings, and the chords that must not collide (shell-shortcuts.md). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShellShortcutsTest {

    private val none = Modifiers()
    private val ctrl = Modifiers(ctrl = true)
    private val meta = Modifiers(meta = true)

    @Test
    fun `ctrl escape toggles start from the desktop`() {
        assertEquals(
            ShellAction.ToggleStart,
            shortcutFor(Key.Escape, ctrl, menuOpen = false),
        )
    }

    @Test
    fun `ctrl escape toggles start with the menu already open`() {
        assertEquals(
            ShellAction.ToggleStart,
            shortcutFor(Key.Escape, ctrl, menuOpen = true),
        )
    }

    @Test
    fun `escape closes an open menu`() {
        assertEquals(ShellAction.CloseMenu, shortcutFor(Key.Escape, none, menuOpen = true))
    }

    /**
     * On a bare desktop Esc is not ours. Swallowing it takes the key from whatever the user meant
     * it for, and gives nothing back.
     */
    @Test
    fun `escape on the desktop is not a shortcut`() {
        assertNull(shortcutFor(Key.Escape, none, menuOpen = false))
    }

    @Test
    fun `meta comma opens settings`() {
        assertEquals(ShellAction.OpenSettings, shortcutFor(Key.Comma, meta, menuOpen = false))
    }

    /** The fallback that actually arrives; Meta+, is measured not to on this platform. */
    @Test
    fun `ctrl comma opens settings too`() {
        assertEquals(ShellAction.OpenSettings, shortcutFor(Key.Comma, ctrl, menuOpen = false))
    }

    @Test
    fun `a bare comma is not a shortcut`() {
        assertNull(shortcutFor(Key.Comma, none, menuOpen = true))
    }

    @Test
    fun `an unbound key is not a shortcut`() {
        for (key in listOf(Key.A, Key.Tab, Key.F3, Key.Spacebar, Key.D, Key.W)) {
            assertNull(shortcutFor(key, meta, menuOpen = true))
            assertNull(shortcutFor(key, none, menuOpen = true))
        }
    }

    /** Alt+Esc belongs to the platform's window cycling; it must not be taken for Ctrl+Esc. */
    @Test
    fun `alt escape is left alone`() {
        assertNull(shortcutFor(Key.Escape, Modifiers(ctrl = true, alt = true), menuOpen = false))
    }

    // --- typing -------------------------------------------------------------------------------

    @Test
    fun `a printable character extends the query`() {
        assertEquals("ca", queryAfterKey("c", Key.A, 'a'.code, none, menuOpen = true))
    }

    @Test
    fun `backspace shortens the query`() {
        assertEquals("c", queryAfterKey("ca", Key.Backspace, 0, none, menuOpen = true))
    }

    @Test
    fun `backspace on an empty query does nothing`() {
        assertNull(queryAfterKey("", Key.Backspace, 0, none, menuOpen = true))
    }

    /** `Ctrl+A` is not the letter A. Appending it would put invisible text in the field. */
    @Test
    fun `a modified key is not typing`() {
        assertNull(queryAfterKey("", Key.A, 'a'.code, ctrl, menuOpen = true))
        assertNull(queryAfterKey("", Key.A, 'a'.code, meta, menuOpen = true))
    }

    @Test
    fun `shift still types`() {
        assertEquals("A", queryAfterKey("", Key.A, 'A'.code, Modifiers(shift = true), menuOpen = true))
    }

    /** A control character has no glyph: it would sit in the field unseen and match nothing. */
    @Test
    fun `a control character is not typing`() {
        assertNull(queryAfterKey("", Key.Enter, 13, none, menuOpen = true))
        assertNull(queryAfterKey("", Key.Tab, 9, none, menuOpen = true))
    }

    @Test
    fun `a key with no character is not typing`() {
        assertNull(queryAfterKey("", Key.DirectionDown, 0, none, menuOpen = true))
    }

    /**
     * The overlap check requirement 6 exists for: no key press may be both a shortcut and typing,
     * or one of the two silently loses.
     */
    @Test
    fun `no key is both a shortcut and typing`() {
        val keys = listOf(Key.Escape, Key.Comma, Key.A, Key.Backspace, Key.Enter, Key.Spacebar)
        val mods = listOf(none, ctrl, meta, Modifiers(alt = true), Modifiers(shift = true))
        for (key in keys) {
            for (mod in mods) {
                for (open in listOf(true, false)) {
                    val isShortcut = shortcutFor(key, mod, open) != null
                    val isTyping = queryAfterKey("x", key, ','.code, mod, open) != null
                    assertEquals(
                        "key=$key mods=$mod menuOpen=$open is both",
                        false,
                        isShortcut && isTyping,
                    )
                }
            }
        }
    }
}
