package com.somalapuram.pclauncher.prompts

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.somalapuram.pclauncher.core.data.prompts.AskedPrompts
import com.somalapuram.pclauncher.core.data.prompts.Prompt
import com.somalapuram.pclauncher.core.data.prompts.PromptStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * The desktop's one-time permission card, if one is due.
 *
 * Owns the whole decision so the home activity does not: which prompt, whether it has been answered
 * already, and what happens to each button. The activity supplies only the two things it is in a
 * position to know — whether each permission is currently held — because those must be read live
 * from the platform on every resume rather than cached (usage-access-ask.md).
 */
@Composable
fun FirstRunPrompt(
    store: PromptStore?,
    canDrawOverlay: Boolean,
    hasUsageAccess: Boolean,
    /** Runs the prompt's own Settings intent. Given by the activity, which can start activities. */
    onOpenSettings: (android.content.Intent) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // No store means no memory of an answer, so nothing is asked: a card whose dismissal cannot be
    // recorded would come back on every launch.
    val asked by (store?.asked ?: NothingToAsk)
        .collectAsState(initial = AskedPrompts(Prompt.entries.toSet()))

    // One card per *launch*, not per answer. Without this the next prompt appears the instant the
    // first is dismissed, which is the queue of dialogs the sequencing exists to avoid — the user
    // learns to dismiss cards without reading them (requirement 1).
    var answeredThisRun by remember { mutableStateOf(false) }

    val prompt = promptToShow(canDrawOverlay, hasUsageAccess, asked)?.takeIf { !answeredThisRun }
        ?: return
    val copy = copyFor(prompt)

    fun answer() {
        answeredThisRun = true
        scope.launch { store?.markAsked(prompt) }
    }

    PermissionCard(
        title = copy.title,
        body = copy.body,
        onAllow = { answer(); onOpenSettings(copy.intent(context)) },
        // Dismissing *is* the answer, and it is remembered. The consequence was stated in the card,
        // so it has been said once and is not said again.
        onNotNow = { answer() },
    )
}

/** Everything already asked, so a shell with no store shows no card. */
private val NothingToAsk = MutableStateFlow(AskedPrompts(Prompt.entries.toSet()))
