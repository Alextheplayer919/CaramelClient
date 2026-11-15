package org.chimeramc.client.ui.compose.caramel.launcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.chimeramc.client.core.cosmetics.CosmeticStore
import org.chimeramc.client.core.mods.inbuilt.overlay.CapePreviewView
import org.chimeramc.client.ui.compose.caramel.CaramelLauncherState

/**
 * The Cosmetics tab's 3D skin slot: the player's own character, rendered by [CapePreviewView].
 *
 * <p>The slot used to show [EmptySkinSlot] — a white square with a label — because the real
 * software renderer is a plain Android [android.view.View] and nothing bridged it into Compose. This
 * hosts it through [AndroidView], so the character the player sees is the same textured, animated
 * render the launch hero and the radial wheel use, wearing whatever is equipped.
 *
 * <p>The equipped set is read from [CosmeticStore] rather than derived from the tile flags, so the
 * Optifine default cape is honoured exactly as it is at launch. Rebinding is skipped unless the
 * equipped set actually changed, so an unrelated recomposition does not rebuild the scene or reload
 * the cape atlas.
 */
@Composable
fun CaramelSkinPreview(state: CaramelLauncherState, modifier: Modifier = Modifier) {
    val equippedKey = state.cosmetics.filter { it.equipped }.joinToString("|") { it.id }
    val binder = remember { SkinPreviewBinder() }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            CapePreviewView(context).apply {
                setAlwaysAnimate(true)
                startPreview()
            }
        },
        update = { view -> binder.bind(view, equippedKey) },
        onRelease = { it.release() },
    )
}

/** Binds the equipped cosmetics into a [CapePreviewView] once per distinct equipped set. */
private class SkinPreviewBinder {
    private var boundKey: String? = null

    fun bind(view: CapePreviewView, key: String) {
        if (key == boundKey) return
        boundKey = key
        val store = CosmeticStore(view.context)
        view.setCape(store.getEquippedCapeForDisplay())
        view.setAccessory(store.getEquippedAccessory())
        view.setPet(store.getEquippedPet())
    }
}
