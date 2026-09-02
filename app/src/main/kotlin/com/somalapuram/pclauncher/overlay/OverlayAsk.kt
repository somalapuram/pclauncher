package com.somalapuram.pclauncher.overlay

import com.somalapuram.pclauncher.core.data.prompts.AskedPrompts
import com.somalapuram.pclauncher.core.data.prompts.Prompt

/**
 * Whether to explain "Display over other apps" now.
 *
 * Two conditions, and the order they are written in is the order they matter. The permission is
 * read live from the platform rather than remembered, because the user can grant or revoke it in
 * Settings at any time and a cached answer would either nag someone who already said yes or hide
 * the bar from someone who has since said no. What *is* remembered is only that we asked
 * (overlay-permission-ask.md).
 */
fun shouldAskForOverlay(hasPermission: Boolean, asked: AskedPrompts): Boolean =
    !hasPermission && !asked.contains(Prompt.OverlayPermission)
