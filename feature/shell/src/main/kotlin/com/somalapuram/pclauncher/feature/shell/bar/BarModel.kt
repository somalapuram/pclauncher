package com.somalapuram.pclauncher.feature.shell.bar

import android.graphics.drawable.Drawable

/** One pinned or running app in the dock. */
data class DockItem(
    val id: String,
    val label: String,
    val icon: Drawable?,
    val isRunning: Boolean = false,
    val isFocused: Boolean = false,
    /** Quiet-mode, suspended, or on detached storage: shown greyed, not dropped. */
    val isAvailable: Boolean = true,
)

/**
 * One open window in the taskbar strip.
 *
 * Below T2 there is no real window list (SRS §5.4), so this is populated from our own launch
 * bookkeeping and the strip is honestly a *recent windows* list. `capability-tiers.md` replaces the
 * source; this model and the UI over it do not change.
 */
data class WindowChip(
    val id: String,
    val title: String,
    val appLabel: String,
    val icon: Drawable?,
    val isFocused: Boolean = false,
)

/** Everything the bar draws, in one value so no zone owns state another reads (SRS §9). */
data class BarState(
    val dockItems: List<DockItem> = emptyList(),
    val windows: List<WindowChip> = emptyList(),
    val isStartOpen: Boolean = false,
    /** Off in touch-first mode and on a software renderer (SRS §4.3). */
    val magnificationEnabled: Boolean = true,
)
