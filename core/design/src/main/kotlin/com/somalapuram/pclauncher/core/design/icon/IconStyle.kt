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
            // Clay carries more of the app's colour than glass does — that is what makes the light
            // set read as pastel rather than as a grid of identical white squares.
            tileTint = 0.26f,
            gloss = Color(0x73FFFFFF),
            glossStop = 0.56f,
            rim = Color(0x40FFFFFF),
            rimWidthFraction = 0.014f,
            // Clay is matte, so its specular is a hint rather than a band — pushing it as hard as
            // glass would turn the light set plastic.
            specular = Color(0x4DFFFFFF),
            specularStop = 0.14f,
            glowAlpha = 0.30f,
            glowRadiusFraction = 0.10f,
            shadow = Color(0x2E000000),
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
        const val TREATMENT_VERSION = 3
    }
}

/** Follows the theme, so light and dark stay first-class (SRS §6.1 principle 6). */
fun iconStyleFor(darkTheme: Boolean): IconStyle =
    if (darkTheme) IconStyle.DarkGlass else IconStyle.SoftClay
