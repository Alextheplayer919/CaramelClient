package org.chimeramc.client.ui.compose.caramel.launcher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.chimeramc.client.ui.compose.caramel.CaramelLauncherState
import org.chimeramc.client.ui.compose.caramel.CaramelTab

/**
 * The whole Caramel launcher, ported from the pack's `CaramelApp` in `launcher/Shell.kt`.
 *
 * <p>The frame (backdrop, title bar, sidebar) hosts the active tab, and the overlays and modals sit
 * above it. This is the single composable the activity mounts through [CaramelLauncherHost].
 */
@Composable
fun CaramelLauncherScreen(
    state: CaramelLauncherState,
    actions: CaramelLauncherHost.Actions,
    skinViewer: @Composable (Modifier) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        CaramelLauncherFrame(state) { contentModifier ->
            Box(contentModifier) {
                when (state.tab) {
                    CaramelTab.Home -> CaramelHomeScreen(state, actions::launch, actions::openContent)
                    CaramelTab.Mods -> CaramelModsScreen(
                        state,
                        onToggle = actions::toggleMod,
                        onOpenFolder = actions::openModsFolder,
                    )
                    CaramelTab.Servers -> CaramelServersScreen(state, actions::joinServer)
                    CaramelTab.Cosmetics -> CaramelCosmeticsScreen(
                        state,
                        onToggleFavourite = actions::toggleFavourite,
                        onEquip = actions::equipCosmetic,
                        onResetView = {},
                        skinViewer = skinViewer,
                    )
                    CaramelTab.News -> CaramelNewsScreen(state)
                    CaramelTab.Store -> CaramelStoreScreen(state)
                }
            }
        }
        CaramelLaunchMenuOverlay(state, actions::launch)
        CaramelModDetailModal(state, actions::toggleMod)
        CaramelAppMenuOverlay(state, actions::quit)
    }
}
