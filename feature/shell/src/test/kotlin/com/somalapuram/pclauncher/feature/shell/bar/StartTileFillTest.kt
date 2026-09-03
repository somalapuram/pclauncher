package com.somalapuram.pclauncher.feature.shell.bar

import androidx.compose.ui.graphics.Color
import com.somalapuram.pclauncher.core.design.surfaceSheen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Start button's tile fill (start-button-gloss.md).
 *
 * The point of pulling this out of the composable is that the *states* are the part worth
 * protecting: gloss is allowed to change, but "open still looks different from hover" is not.
 */
class StartTileFillTest {

    @Test
    fun `at rest the tile is filled, not transparent`() {
        // The regression this exists for: rest used to be Color.Transparent, which is what made the
        // button an outline around nothing next to a row of glossy icons.
        val fill = startTileFill(isOpen = false, pressed = false, hovered = false)
        assertEquals(StartTileRole.Surface, fill.role)
        assertTrue("rest must carry a fill, got ${fill.alpha}", fill.alpha > 0f)
    }

    @Test
    fun `hover is stronger than rest, and both are the surface role`() {
        val rest = startTileFill(isOpen = false, pressed = false, hovered = false)
        val hover = startTileFill(isOpen = false, pressed = false, hovered = true)
        assertEquals(StartTileRole.Surface, hover.role)
        assertTrue("hover must read stronger than rest", hover.alpha > rest.alpha)
    }

    @Test
    fun `pressing takes the accent`() {
        val fill = startTileFill(isOpen = false, pressed = true, hovered = false)
        assertEquals(StartTileRole.Accent, fill.role)
    }

    @Test
    fun `open takes the accent at full strength`() {
        val fill = startTileFill(isOpen = true, pressed = false, hovered = false)
        assertEquals(StartTileRole.Accent, fill.role)
        assertEquals(1f, fill.alpha, 0f)
    }

    @Test
    fun `open outranks hover`() {
        // While the menu is up the button owns it, even once the pointer has moved into the menu.
        assertEquals(
            startTileFill(isOpen = true, pressed = false, hovered = false),
            startTileFill(isOpen = true, pressed = false, hovered = true),
        )
    }

    @Test
    fun `open outranks press`() {
        assertEquals(
            startTileFill(isOpen = true, pressed = false, hovered = false),
            startTileFill(isOpen = true, pressed = true, hovered = true),
        )
    }

    @Test
    fun `press outranks hover`() {
        val pressed = startTileFill(isOpen = false, pressed = true, hovered = true)
        assertEquals(StartTileRole.Accent, pressed.role)
    }

    @Test
    fun `the four states are all distinguishable`() {
        val states = listOf(
            startTileFill(isOpen = false, pressed = false, hovered = false),
            startTileFill(isOpen = false, pressed = false, hovered = true),
            startTileFill(isOpen = false, pressed = true, hovered = false),
            startTileFill(isOpen = true, pressed = false, hovered = false),
        )
        // Gloss must not flatten the states into each other (requirement 4).
        assertEquals("two states render identically", states.size, states.toSet().size)
    }

    @Test
    fun `every state's alpha is a legal opacity`() {
        for (open in listOf(false, true)) {
            for (press in listOf(false, true)) {
                for (hover in listOf(false, true)) {
                    val a = startTileFill(open, press, hover).alpha
                    assertTrue("alpha out of range: $a", a in 0f..1f)
                }
            }
        }
    }

    @Test
    fun `the sheen keeps every state inside a legal opacity`() {
        // The composable spreads the fill's alpha into three stops; a state whose alpha sat near
        // the top of the range must not produce a stop above 1.
        for (open in listOf(false, true)) {
            for (press in listOf(false, true)) {
                for (hover in listOf(false, true)) {
                    val fill = startTileFill(open, press, hover)
                    surfaceSheen(fill.alpha, lift = StartSheenLift).forEach {
                        assertTrue("sheen stop out of range: $it", it in 0f..1f)
                    }
                }
            }
        }
    }

    @Test
    fun `the specular is a band over the top of the tile, not the whole of it`() {
        // A highlight covering the tile is a tint, not a specular — it is the fall-off that reads
        // as curvature.
        assertTrue(StartSpecularStop > 0f && StartSpecularStop < 1f)
        assertTrue(StartSpecularAlpha > 0f && StartSpecularAlpha < 1f)
    }
}

/**
 * The gloss profiles (start-button-gloss.md).
 *
 * The open state used to be flat — not because the gloss was too weak, but because it was the
 * wrong *kind*: an opaque fill has no alpha to vary. These pin the two characters apart.
 */
class StartTileGlossTest {

    @Test
    fun `the accent tile is lit on top and shaded underneath`() {
        val gloss = startTileGloss(StartTileRole.Accent)
        assertTrue("top must lift toward white", gloss.top > 0f)
        assertTrue("bottom must shade toward black", gloss.bottom < 0f)
    }

    @Test
    fun `the translucent tile leaves its colour alone`() {
        // Its dimension comes from the alpha sheen letting more or less of the bar through.
        val gloss = startTileGloss(StartTileRole.Surface)
        assertEquals(0f, gloss.top, 0f)
        assertEquals(0f, gloss.middle, 0f)
        assertEquals(0f, gloss.bottom, 0f)
    }

    @Test
    fun `the two roles are glossed differently, not merely by strength`() {
        // The user-visible requirement: clicked is glossy *and different*, not a stronger copy.
        assertTrue(startTileGloss(StartTileRole.Accent) != startTileGloss(StartTileRole.Surface))
    }

    @Test
    fun `the accent tile has a real top-to-bottom range`() {
        val gloss = startTileGloss(StartTileRole.Accent)
        assertTrue(
            "a flat accent is the bug this exists to prevent",
            gloss.top - gloss.bottom > 0.25f,
        )
    }

    @Test
    fun `shading toward white lightens every channel`() {
        val base = Color(0.40f, 0.45f, 0.60f, 1f)
        val lit = base.shadedBy(0.34f)
        assertTrue(lit.red > base.red && lit.green > base.green && lit.blue > base.blue)
    }

    @Test
    fun `shading toward black darkens every channel`() {
        val base = Color(0.40f, 0.45f, 0.60f, 1f)
        val shaded = base.shadedBy(-0.24f)
        assertTrue(shaded.red < base.red && shaded.green < base.green && shaded.blue < base.blue)
    }

    @Test
    fun `shading keeps the alpha it was given`() {
        // Mixing with opaque white must not make a translucent tile opaque — that would blank out
        // the bar showing through the top of the rest-state tile.
        val translucent = Color(0.9f, 0.9f, 0.95f, 0.12f)
        // Against the colour's *stored* alpha, not the 0.12f literal: an sRGB Color holds 8 bits a
        // channel, so 0.12 is kept as 31/255 = 0.1215686. Asserting the literal with a tolerance
        // would hide a real drift; asserting the representable value is exact.
        assertEquals(translucent.alpha, translucent.shadedBy(0.34f).alpha, 0f)
        assertEquals(translucent.alpha, translucent.shadedBy(-0.24f).alpha, 0f)
    }

    @Test
    fun `a zero mix is the identity`() {
        val base = Color(0.4f, 0.45f, 0.6f, 0.5f)
        assertEquals(base, base.shadedBy(0f))
    }
}

/**
 * The press dim (start-press-dim.md).
 *
 * The press used to be the brightest thing the button ever did, which fought the 92% shrink
 * happening at the same instant. It is now a scrim over the finished tile rather than a shift in
 * the gloss profile — these pin both the direction and that independence.
 */
class StartPressDimTest {

    @Test
    fun `pressing lays a scrim over the tile`() {
        assertTrue(pressScrimAlpha(pressed = true) > 0f)
    }

    @Test
    fun `not pressing lays no scrim at all`() {
        // Exactly zero, so the unpressed tile is the gloss profile untouched.
        assertEquals(0f, pressScrimAlpha(pressed = false), 0f)
    }

    @Test
    fun `the scrim darkens without hiding the tile`() {
        // A scrim at 1 would be a black hole where the button was; the accent must still read.
        assertTrue(pressScrimAlpha(pressed = true) < 1f)
    }

    @Test
    fun `the press does not touch the gloss profile`() {
        // The regression this guards: when the dim was a shift in the profile, deepening the open
        // state's gloss raised the pressed tile straight back above rest. The two must be tunable
        // apart.
        val accent = startTileGloss(StartTileRole.Accent)
        assertEquals(AccentGlossLift, accent.top, 0f)
        assertEquals(-AccentInnerShade, accent.bottom, 0f)
    }

    @Test
    fun `the specular dims with the tile`() {
        // Left at full strength it becomes the brightest thing on a tile that just went dark.
        assertTrue(startSpecularAlpha(pressed = true) < startSpecularAlpha(pressed = false))
    }

    @Test
    fun `the pressed specular is still visible`() {
        // Dimmed, not removed — the tile is still glass while held.
        assertTrue(startSpecularAlpha(pressed = true) > 0f)
    }

    @Test
    fun `the press changes brightness, not opacity`() {
        // Alpha would read as dimmer over this dark bar but lighter over a pale one, and the shell
        // is themed from the wallpaper.
        assertEquals(
            startTileFill(isOpen = false, pressed = true, hovered = false).alpha,
            0.45f,
            1e-6f,
        )
    }

    @Test
    fun `the scrim is strong enough to overcome the accent the press switches to`() {
        // Pressing does not merely shade the rest tile — it switches from a 12%-alpha wash to a
        // 45%-alpha accent. A scrim that only softened that would still leave press the brightest
        // state, which is what a 0.22 shift did.
        val restAlpha = startTileFill(isOpen = false, pressed = false, hovered = false).alpha
        val pressAlpha = startTileFill(isOpen = false, pressed = true, hovered = false).alpha
        assertTrue("press starts from a brighter base", pressAlpha > restAlpha)
        assertTrue("so the scrim must be substantial", pressScrimAlpha(pressed = true) >= 0.35f)
    }
}
