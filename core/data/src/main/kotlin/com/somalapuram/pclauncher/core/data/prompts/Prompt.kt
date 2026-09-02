package com.somalapuram.pclauncher.core.data.prompts

/**
 * A one-time explanation the shell shows and then never shows again.
 *
 * Named rather than boolean-per-feature, because "have we asked yet" is the same question every
 * time and the answers belong in one place. What is stored is that the user was *asked* — never the
 * answer to a permission, which is read live from the platform and can change behind our back.
 */
enum class Prompt {
    /** "Display over other apps", so the bar stays visible above app windows. */
    OverlayPermission,

    /** Usage access, so Recent knows about apps opened outside the shell. */
    UsageAccess,
}

/** Which prompts have been shown. Unknown names are dropped, so a downgrade cannot crash a read. */
@JvmInline
value class AskedPrompts(val shown: Set<Prompt> = emptySet()) {
    fun contains(prompt: Prompt): Boolean = prompt in shown
    fun plus(prompt: Prompt): AskedPrompts = AskedPrompts(shown + prompt)
}

/**
 * Text form for the store: comma-separated enum names.
 *
 * Names, not ordinals, so reordering the enum cannot silently re-target an existing answer — the
 * cost of getting that wrong is a prompt the user already dismissed coming back.
 */
object PromptCodec {

    fun encode(asked: AskedPrompts): String = asked.shown.joinToString(",") { it.name }

    fun decode(raw: String?): AskedPrompts {
        if (raw.isNullOrBlank()) return AskedPrompts()
        val known = raw.split(',')
            .mapNotNull { name -> Prompt.entries.firstOrNull { it.name == name.trim() } }
        return AskedPrompts(known.toSet())
    }
}
