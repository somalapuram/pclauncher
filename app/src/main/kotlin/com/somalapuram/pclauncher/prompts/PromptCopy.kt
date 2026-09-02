package com.somalapuram.pclauncher.prompts

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.somalapuram.pclauncher.core.data.prompts.Prompt

/**
 * What each card says, and where its Allow button goes.
 *
 * Copy and intent together, because they have to agree: the card names the toggle the user is about
 * to be shown, and a card whose wording drifts from the screen it opens is worse than no card — the
 * user arrives somewhere that looks unrelated to what they just agreed to.
 */
data class PromptCopy(
    val title: String,
    val body: String,
    val intent: (Context) -> Intent,
)

fun copyFor(prompt: Prompt): PromptCopy = when (prompt) {
    Prompt.OverlayPermission -> PromptCopy(
        title = "Keep the taskbar on top",
        body = "Turn on \"Display over other apps\" and the taskbar, Start menu and system tray " +
            "stay visible while you use apps.\n\n" +
            "Without it they show only on the desktop, and anything you open covers them. You can " +
            "change this later in Settings.",
        intent = ::overlayPermissionIntent,
    )

    // Careful not to overclaim: Recent works without this. What the permission adds is history the
    // shell could not have seen for itself, so that is what the card offers.
    Prompt.UsageAccess -> PromptCopy(
        title = "Show apps you already use",
        body = "Turn on \"App usage data\" and the Start menu's Recent row also knows about apps " +
            "you opened outside pclauncher.\n\n" +
            "Without it, Recent lists only what you launched from here — which works, but starts " +
            "out empty. You can change this later in Settings.",
        intent = { Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
    )
}

/** The overlay Settings screen, scoped to this app rather than the whole list. */
fun overlayPermissionIntent(context: Context): Intent =
    Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.fromParts("package", context.packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
