package com.somalapuram.pclauncher.prompts

import com.somalapuram.pclauncher.core.data.prompts.AskedPrompts
import com.somalapuram.pclauncher.core.data.prompts.Prompt

/**
 * Which one-time card the desktop should show, if any.
 *
 * One function returning one prompt, rather than a predicate per card. Two independent predicates
 * can both be true at once, and "never two cards on screen together" is precisely the guarantee a
 * pair of booleans cannot make (usage-access-ask.md requirement 1). Here it is a property of the
 * return type.
 *
 * Both permissions are read live rather than remembered: each can be granted or revoked in Settings
 * at any time, and a cached answer would either nag someone who has already said yes or hide a
 * feature from someone who has since said no. What is remembered is only that we asked.
 */
fun promptToShow(
    canDrawOverlay: Boolean,
    hasUsageAccess: Boolean,
    asked: AskedPrompts,
): Prompt? = when {
    // The overlay first, and alone. It is the one the shell's core promise depends on — the bar
    // staying above app windows — so it gets the user's attention undivided.
    !canDrawOverlay && !asked.contains(Prompt.OverlayPermission) -> Prompt.OverlayPermission

    // Only once the first question has been answered. Asking both on one desktop turns a decision
    // into a queue of dialogs, which is how a user learns to dismiss cards without reading them.
    !hasUsageAccess &&
        !asked.contains(Prompt.UsageAccess) &&
        asked.contains(Prompt.OverlayPermission) -> Prompt.UsageAccess

    else -> null
}
