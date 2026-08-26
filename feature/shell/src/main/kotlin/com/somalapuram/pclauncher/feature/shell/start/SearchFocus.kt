package com.somalapuram.pclauncher.feature.shell.start

/**
 * Whether the Start menu should take focus into its search field when it opens.
 *
 * SRS §6.4 asks for typing to go straight into search. Focusing the field is not the way to get
 * there on this device: Android raises the on-screen keyboard over the bottom half of the display,
 * which is precisely where the Start menu sits, so the first thing a user must do with the menu
 * they just opened is dismiss something to see it. Detecting a hardware keyboard did not avoid it —
 * the emulator reports one and the IME appears anyway.
 *
 * So focus is not taken on open. The intent behind §6.4 is that *keystrokes* reach search, and that
 * is a key-routing question rather than a focus one; it belongs with the shortcut work SRS §8
 * describes, where a key event can be delivered to the field without a caret being placed in it.
 * Tapping the field still focuses it, and on a touch device raising the keyboard then is what the
 * user asked for.
 *
 * Kept as a named function rather than a deleted line so the decision is visible at the call site
 * and has somewhere to be reversed.
 */
fun shouldFocusSearchOnOpen(): Boolean = false
