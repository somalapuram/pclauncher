package com.somalapuram.pclauncher.icons

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.somalapuram.pclauncher.core.apps.IconCacheKey
import com.somalapuram.pclauncher.core.apps.IconLoader
import com.somalapuram.pclauncher.core.design.icon.IconCompositor
import com.somalapuram.pclauncher.core.design.icon.IconStyle

/**
 * Wraps a raw [IconLoader] and applies the icon treatment to whatever it returns.
 *
 * This lives in `:app` rather than in `core:apps` on purpose: the compositor is a design concern
 * and pulls in `compose-ui-graphics`, while `core:apps` is required to stay headless. `:app` is the
 * only module that already sees both, so it is where the two are joined.
 *
 * Because it sits *inside* the loader, the treatment is applied once and the composited bitmap is
 * what the cache stores — drawing an icon anywhere in the shell stays a single blit.
 */
class TreatedIconLoader(
    private val delegate: IconLoader,
    private val styleFor: (IconCacheKey) -> IconStyle?,
    private val sizePx: Int = DEFAULT_SIZE_PX,
) : IconLoader {

    override fun load(key: IconCacheKey): Drawable? {
        val raw = delegate.load(key) ?: return null

        // No style means the treatment is switched off; hand back exactly what the app ships.
        val style = styleFor(key) ?: return raw

        return runCatching {
            BitmapDrawable(null, IconCompositor(style).composite(raw, sizePx))
        }.getOrElse {
            // A treatment failure must never cost the user their icon — fall back to the original.
            raw
        }
    }

    companion object {
        /** Large enough for the biggest surface that draws one (the desktop grid at 96 dp). */
        const val DEFAULT_SIZE_PX = 192
    }
}
