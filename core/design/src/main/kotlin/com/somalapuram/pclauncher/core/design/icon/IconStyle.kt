package com.somalapuram.pclauncher.core.design.icon

import androidx.compose.ui.graphics.Color

/**
 * One icon treatment, parameterised.
 *
 * `ref-img/icons2.png` and `ref-img/icons3.png` are the same design at two polarities — same
 * silhouette, same glyph placement, inverted lighting. So they are two instances of this, not two
 * pipelines. A third style must be expressible here without touching the compositor.
 */
data class IconStyle(
    val id: String,
    /** Base tile colour before the app's own colour is mixed in. */
    val tileBase: Color,
    /** How much of the app's colour survives into the tile. 0 = pure [tileBase], 1 = pure app. */
    val tileTint: Float,
    /** Top-left specular highlight. Alpha 0 disables it. */
    val gloss: Color,
    val glossStop: Float,
    /** Lit-edge rim. Drawn along the top and left. */
    val rim: Color,
    val rimWidthFraction: Float,
    /**
     * A second, tighter specular band inside the gloss.
     *
     * One soft gradient reads as a tinted fill; a soft wash *plus* a tight bright band is what the
     * eye interprets as a curved glossy surface catching a light source.
     */
    val specular: Color,
    val specularStop: Float,
    /**
     * Outer glow, taken from the app's own colour.
     *
     * This is what makes the reference tiles feel lit from within rather than merely dark — the
     * green phone icon sits in a faint green halo. Alpha 0 disables it.
     */
    val glowAlpha: Float,
    val glowRadiusFraction: Float,
    /** Ambient shadow under the tile, opposite the light. */
    val shadow: Color,
    val shadowRadiusFraction: Float,
    val shadowOffsetFraction: Float,
    /**
     * A highlight drawn **over** the glyph, clipped to the tile.
     *
     * The gloss and specular below are painted before the artwork, so on a well-filled adaptive
     * icon they survive only in the margin — which is why the tiles measured flat despite having
     * two highlight layers. Glass covers what is under it; this is the layer that makes the tile
     * read as glass rather than as a gradient behind a picture. Alpha 0 disables it.
     */
    val glaze: Color,
    val glazeStop: Float,
    /**
     * A soft darkening along the bottom **inside** the shape.
     *
     * The drop shadow sits beneath the tile and says where the tile is; this says the tile has a
     * body. Without it a tile has a lit top edge and no underside, which reads as a sticker.
     */
    val innerShade: Color,
    val innerShadeStop: Float,
    /** Inset of an adaptive icon's foreground inside the tile. */
    val adaptiveInset: Float,
    /**
     * How much to overscale an adaptive foreground.
     *
     * Adaptive icons are 108dp with the visible content confined to the central 72dp safe zone —
     * the outer third exists only so the system can mask and parallax it. Drawn at face value the
     * glyph occupies barely a third of our tile and looks lost. 108/72 = 1.5 recovers the intended
     * optical size; the tile clips whatever spills.
     */
    val adaptiveForegroundScale: Float,
    /** Legacy icons sit smaller — they carry their own visual padding and often their own corners. */
    val legacyInset: Float,
) {
    companion object {
        /**
         * `ref-img/icons2.png` — near-black glass, saturated glyph, coloured rim bleeding off the
         * edge. The cheapest of the two to composite, which is why it is the dark default on a
         * software renderer.
         */
        val DarkGlass = IconStyle(
            id = "dark-glass",
            tileBase = Color(0xFF0F1116),
            // Kept low on purpose: the reference tiles are near-black with a *hint* of the app's
            // colour. Blending more turns every tile into a different mid-grey and the set stops
            // reading as one design.
            tileTint = 0.13f,
            gloss = Color(0x8FFFFFFF),
            glossStop = 0.44f,
            rim = Color(0xA6FFFFFF),
            rimWidthFraction = 0.020f,
            specular = Color(0x99FFFFFF),
            specularStop = 0.17f,
            glowAlpha = 0.55f,
            glowRadiusFraction = 0.085f,
            // Restrained: near-black glass shows a highlight readily, and matching clay's strength
            // here would grey the tiles out and lose the depth the darkness is buying.
            glaze = Color(0x66FFFFFF),
            glazeStop = 0.62f,
            innerShade = Color(0x8C000000),
            innerShadeStop = 0.55f,
            shadow = Color(0x8C000000),
            shadowRadiusFraction = 0.07f,
            shadowOffsetFraction = 0.04f,
            adaptiveInset = 0.14f,
            adaptiveForegroundScale = 1.5f,
            legacyInset = 0.17f,
        )

        /** `ref-img/icons3.png` — white/pastel clay, matte, soft ambient shadow, no hard specular. */
        val SoftClay = IconStyle(
            id = "soft-clay",
            tileBase = Color(0xFFFDFDFF),
            // Raised hard from 0.26: at that level every tile landed on near-white and the set read
            // as identical squares — the exact failure the tint exists to prevent. The reference is
            // pastel, and pastel needs the app's colour to actually survive into the tile.
            tileTint = 0.52f,
            gloss = Color(0xA6FFFFFF),
            glossStop = 0.58f,
            // The rim sits at the tile edge, outside the glyph's inset, so unlike the gloss it is
            // never covered — which makes it the cheapest lit edge available on a light tile.
            rim = Color(0xBFFFFFFF),
            rimWidthFraction = 0.024f,
            // Was a "hint" on the theory that a hard specular would turn the light set plastic.
            // Against `ref-img/icons3.png` plastic is precisely what it is: glossy, saturated and
            // dimensional. The hint measured as no shading at all.
            specular = Color(0xBFFFFFFF),
            specularStop = 0.20f,
            glowAlpha = 0.30f,
            glowRadiusFraction = 0.10f,
            // The stop is a fraction of the whole bitmap, and the tile is inset inside it to
            // leave room for the shadow and glow — so a stop under ~0.5 dies before the diagonal
            // reaches the tile at all. 0.46 measured as no sheen whatsoever.
            glaze = Color(0xBFFFFFFF),
            glazeStop = 0.72f,
            innerShade = Color(0x4D324055),
            innerShadeStop = 0.55f,
            shadow = Color(0x3D000000),
            shadowRadiusFraction = 0.090f,
            shadowOffsetFraction = 0.050f,
            adaptiveInset = 0.15f,
            adaptiveForegroundScale = 1.5f,
            legacyInset = 0.19f,
        )

        /**
         * Bumped whenever the pipeline changes. Part of the cache key, so a change invalidates
         * every stored icon rather than leaving old and new tiles side by side on the desktop.
         */
        /**
         * Bumped whenever the treatment's *output* changes.
         *
         * The composited bitmap is cached on disk under this, so a style change without a bump
         * serves every existing install the old artwork for good.
         */
        const val TREATMENT_VERSION = 4
    }
}

/** Follows the theme, so light and dark stay first-class (SRS §6.1 principle 6). */
fun iconStyleFor(darkTheme: Boolean): IconStyle =
    if (darkTheme) IconStyle.DarkGlass else IconStyle.SoftClay
