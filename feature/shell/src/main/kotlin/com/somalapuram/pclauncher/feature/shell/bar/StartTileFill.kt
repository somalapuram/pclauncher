package com.somalapuram.pclauncher.feature.shell.bar

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Mix a colour toward white or black, keeping its alpha.
 *
 * Alpha is preserved deliberately: the mix says how *lit* the surface is, and the sheen has
 * already decided how much of the bar shows through it. Letting `lerp` carry white's opacity in
 * would make the top of a translucent tile opaque and lose the bar behind it.
 */
fun Color.shadedBy(mix: Float): Color = when {
    mix > 0f -> lerp(this, Color.White, mix).copy(alpha = alpha)
    mix < 0f -> lerp(this, Color.Black, -mix).copy(alpha = alpha)
    else -> this
}

/**
 * Which fill the Start button's tile wears (start-button-gloss.md).
 *
 * Separated from the composable so the state machine can be tested without a composition — the
 * appearance is then just "apply the shell's sheen to whatever this decided".
 */
data class StartTileFill(
    /** Which colour role the tile takes; the composable resolves it against the theme. */
    val role: StartTileRole,
    /** Base opacity, before the sheen spreads it into a gradient. */
    val alpha: Float,
)

enum class StartTileRole {
    /** The accent — the button owns the menu that is up, or is being pressed. */
    Accent,

    /** The foreground colour at low opacity — a tile on the bar, not a coloured button. */
    Surface,
}

/**
 * Decide the tile's fill.
 *
 * **Open outranks hover** (and hover outranks rest) so that while the menu is up the button reads
 * as the thing that owns it, even after the pointer has moved off it and into the menu.
 *
 * Rest is deliberately *not* transparent. It was, and that is what made the button an outline
 * around nothing while every icon beside it was a glossy tile.
 */
fun startTileFill(isOpen: Boolean, pressed: Boolean, hovered: Boolean): StartTileFill = when {
    isOpen -> StartTileFill(StartTileRole.Accent, 1f)
    pressed -> StartTileFill(StartTileRole.Accent, 0.45f)
    hovered -> StartTileFill(StartTileRole.Surface, 0.22f)
    else -> StartTileFill(StartTileRole.Surface, 0.12f)
}

/**
 * How far the specular band reaches down the tile.
 *
 * A single soft gradient reads as a tinted fill; it is the tighter bright band near the top that
 * the eye interprets as a curved surface catching a light — the same reasoning `IconStyle.specular`
 * carries for the baked icon tiles.
 */
const val StartSpecularStop: Float = 0.46f

/** Kept below the bar's own lift: this is a 40 dp tile, not a full-width surface. */
const val StartSheenLift: Float = 0.07f

/** Peak opacity of the specular band, at the very top of the tile. */
const val StartSpecularAlpha: Float = 0.30f

/**
 * How a tile is shaded down its height, as fractions mixing toward white (positive) or black
 * (negative) at the top, middle and bottom.
 *
 * `surfaceSheen` varies **alpha**, which is all a translucent tile needs — it lets more or less of
 * the bar through. An *opaque* tile has no such headroom: at alpha 1 the sheen's stops collapse to
 * `0.93, 1.0, 1.0` and the accent reads dead flat, which is exactly how the open state looked. So
 * the opaque state shades its **colour** instead.
 */
data class TileGloss(val top: Float, val middle: Float, val bottom: Float)

/**
 * The gloss profile for a role — deliberately two different characters, not one strength.
 *
 * The translucent tile is a soft wash that fades into the bar. The accent tile is a lit top and a
 * shaded underside: `IconStyle.innerShade` records why that matters — without it a tile has a lit
 * edge and no body, and reads as a sticker rather than an object.
 */
fun startTileGloss(role: StartTileRole): TileGloss = when (role) {
    StartTileRole.Accent -> TileGloss(top = AccentGlossLift, middle = 0f, bottom = -AccentInnerShade)
    // Left alone: the alpha sheen is already doing the work over a bar that shows through.
    StartTileRole.Surface -> TileGloss(top = 0f, middle = 0f, bottom = 0f)
}

/**
 * How much the tile dims while held down, as a black scrim over the finished tile.
 *
 * A control pushed under a finger moves *away* from the light. The button already shrinks to 92%
 * on press; brightening at that moment fought the shrink and the pair read as a flicker rather
 * than a press (start-press-dim.md).
 *
 * **A scrim, not a shift in the gloss profile.** The first version subtracted from each of the
 * profile's stops, which coupled the press to the accent's tuning: deepening the open state's
 * gloss raised the pressed tile straight back above rest, silently. Multiplying the finished tile
 * instead dims whatever the profile produced, by construction, so the two can be tuned apart.
 *
 * It is also the polarity-correct channel: darkening reads as *less light* on a pale bar and a
 * dark one alike, where dropping alpha would read as dimmer on one and lighter on the other.
 */
fun pressScrimAlpha(pressed: Boolean): Float = if (pressed) PressScrim else 0f

/** Enough to put the pressed tile below rest at every depth, measured on device. */
const val PressScrim: Float = 0.62f

/** The specular, dimmed with the tile it sits on. */
fun startSpecularAlpha(pressed: Boolean): Float =
    if (pressed) StartSpecularAlphaPressed else StartSpecularAlpha

/**
 * A full-strength white highlight on a tile that has just gone dark is the brightest thing left on
 * it, and undoes the press entirely.
 */
const val StartSpecularAlphaPressed: Float = 0.14f

/**
 * How far the accent tile's top lifts toward white.
 *
 * Raised from 0.34, which measured `201 -> 137` on device — a gradient, but a shallow one that
 * still read as a flat chip beside the composited icon tiles it shares a bar with.
 */
const val AccentGlossLift: Float = 0.52f

/**
 * How far its underside darkens — the body that stops it reading as a flat chip.
 *
 * Raised with the lift: it is the *range* between the lit top and the shaded underside that reads
 * as curvature, so lifting the top alone would have washed the tile out rather than shaping it.
 */
const val AccentInnerShade: Float = 0.42f

/**
 * Where the gloss profile's middle stop sits down the tile.
 *
 * Below the centre on purpose. The glyph occupies roughly the middle 40% of the tile, and an
 * evenly-spaced gradient put the shaded underside straight through its lower rows — which is where
 * the open state's contrast was measured at its worst. Pushing the midpoint down concentrates the
 * darkening *below* the glyph, so the tile gains depth and the glyph keeps its ground.
 */
const val TileGlossMidStop: Float = 0.62f
