package org.chimeramc.client.ui.compose.caramel.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.chimeramc.client.ui.compose.caramel.C
import org.chimeramc.client.ui.compose.caramel.CaramelLauncherState
import org.chimeramc.client.ui.compose.caramel.CaramelLoader
import org.chimeramc.client.ui.compose.caramel.CaramelModInfo
import org.chimeramc.client.ui.compose.caramel.CaramelModal
import org.chimeramc.client.ui.compose.caramel.CaramelProfileDropdown
import org.chimeramc.client.ui.compose.caramel.CaramelVersionDropdown
import org.chimeramc.client.ui.compose.caramel.GlassButton
import org.chimeramc.client.ui.compose.caramel.GlassPanel
import org.chimeramc.client.ui.compose.caramel.LoaderTile
import org.chimeramc.client.ui.compose.caramel.PrimaryButton
import org.chimeramc.client.ui.compose.caramel.T
import org.chimeramc.client.ui.compose.caramel.Tone
import org.chimeramc.client.ui.compose.caramel.sp_

/**
 * The pack's launch menu, its mod-detail modal and the Caramel Client app menu.
 *
 * <p>The pack anchors the launch menu to a fixed desktop offset. On a phone the surface is a
 * different shape, so the menu is centred as a popup; its contents (version, loaders, profile
 * mode, launch) are the pack's.
 */
@Composable
fun CaramelLaunchMenuOverlay(state: CaramelLauncherState, onLaunch: () -> Unit) {
    if (!state.launchMenu) return
    Popup(
        alignment = Alignment.Center,
        onDismissRequest = { state.launchMenu = false },
        properties = PopupProperties(focusable = true),
    ) {
        GlassPanel(Modifier.width(372.dp), RoundedCornerShape(22.dp), popup = true, elevation = 40.dp) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Version", style = T.Title.copy(fontSize = 15.sp_), modifier = Modifier.weight(1f))
                    CaramelVersionDropdown(state.versions, state.version, onSelect = { state.version = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Loaders", style = T.Title.copy(fontSize = 15.sp_), modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CaramelLoader.entries.forEach { LoaderTile(it, state.loader == it, { state.loader = it }) }
                    }
                }
                CaramelProfileDropdown(state.profile, { state.profile = it }, width = 328)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassButton(null, { state.launchMenu = false; state.instanceSettings = true }, icon = Icons.Rounded.Settings, height = 44)
                    PrimaryButton("Launch Game", { state.launchMenu = false; onLaunch() }, Modifier.weight(1f), icon = Icons.Rounded.RocketLaunch)
                }
            }
        }
    }
}

/** The mod detail modal, opened by tapping a Mods card. */
@Composable
fun CaramelModDetailModal(state: CaramelLauncherState, onToggle: (CaramelModInfo, Boolean) -> Unit) {
    val mod = state.selectedMod
    CaramelModal(mod != null, { state.selectedMod = null }, Modifier.width(520.dp)) {
        if (mod == null) return@CaramelModal
        Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(mod.name, style = T.Display.copy(fontSize = 20.sp_, fontWeight = FontWeight.Medium))
                    Text("by ${mod.author} · v${mod.version}", style = T.Body)
                }
                GlassButton(null, { state.selectedMod = null }, icon = Icons.Rounded.Close, height = 32)
            }
            Text(mod.description, style = T.Body.copy(fontSize = 13.sp_))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (mod.enabled) "Enabled" else "Disabled", style = T.Label, modifier = Modifier.weight(1f))
                CaramelToggleRef(mod.enabled) { onToggle(mod, it) }
            }
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassButton("Open config", {}, Modifier.weight(1f), height = 40)
                PrimaryButton(if (mod.enabled) "Disable" else "Enable", { onToggle(mod, !mod.enabled) }, Modifier.weight(1f), height = 40)
            }
        }
    }
}

/** The Caramel Client app menu: the dropdown by the title bar's arrow, which carries Quit. */
@Composable
fun CaramelAppMenuOverlay(state: CaramelLauncherState, onQuit: () -> Unit) {
    if (!state.appMenu) return
    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(70, 40),
        onDismissRequest = { state.appMenu = false },
        properties = PopupProperties(focusable = true),
    ) {
        GlassPanel(Modifier.width(230.dp), RoundedCornerShape(18.dp), popup = true, elevation = 36.dp) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                CaramelAppMenuRow("Settings", false) { state.appMenu = false; state.launcherSettings = true }
                CaramelAppMenuRow("Instance settings", false) { state.appMenu = false; state.instanceSettings = true }
                CaramelAppMenuRow("About", false) { state.appMenu = false }
                Spacer(Modifier.height(4.dp))
                CaramelAppMenuRow("Quit", true) { state.appMenu = false; onQuit() }
            }
        }
    }
}

@Composable
private fun CaramelAppMenuRow(label: String, danger: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(10.dp))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = T.Label.copy(color = if (danger) C.Red else C.Text, fontWeight = FontWeight.Medium),
        )
    }
}

/** A local alias so this file does not import the control just for one call. */
@Composable
private fun CaramelToggleRef(checked: Boolean, onChange: (Boolean) -> Unit) =
    org.chimeramc.client.ui.compose.caramel.CaramelToggle(checked, onChange)
