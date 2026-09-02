package com.somalapuram.pclauncher.feature.shell.input

import androidx.compose.ui.input.key.Key

/**
 * What a shortcut does. Only actions the user can already perform by pointer appear here — a
 * shortcut bound to a no-op teaches the user the key does not work (shell-shortcuts.md).
 */
enum class ShellAction {
    /** Open the Start menu, or close it if it is already open. */
    ToggleStart,

    /** Close whatever menu is open, launching nothing. */
    CloseMenu,

    /** The system Settings app, the same target as the Start menu's Settings button. */
    OpenSettings,
}

/** The modifiers held when a key went down. */
data class Modifiers(
    val ctrl: Boolean = false,
    val meta: Boolean = false,
    val alt: Boolean = false,
    val shift: Boolean = false,
) {
    /** True when nothing that changes a key's meaning is held. Shift alone still types. */
    val none: Boolean get() = !ctrl && !meta && !alt
}

/**
 * The shortcut for a key press, or null if it is not one.
 *
 * A pure table rather than a `when` inside a key handler. Two chords that overlap are invisible in a
 * handler — one branch simply wins and the other key appears dead — and are a test here
 * (shell-shortcuts.md requirement 6).
 *
 * [menuOpen] matters because the same key means different things either side of it: `Esc` closes an
 * open menu and does nothing on a bare desktop, where swallowing it would be taking a key from
 * whatever the user actually meant it for.
 */
fun shortcutFor(key: Key, modifiers: Modifiers, menuOpen: Boolean): ShellAction? = when {
    // The documented fallback for Meta, which the platform frequently swallows (SRS §5.4).
    key == Key.Escape && modifiers.ctrl && !modifiers.alt -> ShellAction.ToggleStart

    key == Key.Escape && modifiers.none && menuOpen -> ShellAction.CloseMenu

    // Both, and for the reason SRS §5.4 gives: Meta chords are frequently swallowed before they
    // reach an app — measured here, where Meta+, never arrives — so every Meta binding needs a
    // Ctrl fallback that does. The same reasoning already made Ctrl+Esc the Start key.
    key == Key.Comma && (modifiers.meta || modifiers.ctrl) -> ShellAction.OpenSettings

    else -> null
}

/**
 * The query after a key, or null when the key was not typing.
 *
 * Kept apart from [shortcutFor] because typing is not a shortcut: it is every key that is *not* one,
 * which is a different question and a different failure if they are confused. Only while a menu is
 * open, and only with no modifier that would make the key mean something else — `Ctrl+A` is not the
 * letter A (shell-shortcuts.md requirement 4).
 */
fun queryAfterKey(
    query: String,
    key: Key,
    codePoint: Int,
    modifiers: Modifiers,
    menuOpen: Boolean,
): String? = when {
    // A key that is a shortcut is never also typing. Enforced here rather than by calling the two
    // in the right order, because the right order is a convention and this is a guarantee: a
    // shortcut key that happens to carry a printable code point would otherwise do both.
    shortcutFor(key, modifiers, menuOpen) != null -> null

    !modifiers.none -> null

    key == Key.Backspace -> query.dropLast(1).takeIf { query.isNotEmpty() }

    // Printable only: control characters would otherwise be appended as invisible query text that
    // matches nothing and cannot be seen to be deleted.
    codePoint in PrintableRange -> query + codePoint.toChar()

    else -> null
}

/** Space through tilde: the characters an app label can be searched by. */
private val PrintableRange = 0x20..0x7E
