package org.chimeramc.client.ui.compose.caramel.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.chimeramc.client.ui.compose.caramel.C
import org.chimeramc.client.ui.compose.caramel.CaramelLauncherState
import org.chimeramc.client.ui.compose.caramel.CaramelModInfo
import org.chimeramc.client.ui.compose.caramel.CaramelServerInfo
import org.chimeramc.client.ui.compose.caramel.CaramelToggle
import org.chimeramc.client.ui.compose.caramel.FilterChip
import org.chimeramc.client.ui.compose.caramel.GlassButton
import org.chimeramc.client.ui.compose.caramel.GlassPanel
import org.chimeramc.client.ui.compose.caramel.PrimaryButton
import org.chimeramc.client.ui.compose.caramel.SearchField
import org.chimeramc.client.ui.compose.caramel.ServerIcon
import org.chimeramc.client.ui.compose.caramel.StatusLabel
import org.chimeramc.client.ui.compose.caramel.T
import org.chimeramc.client.ui.compose.caramel.VersionRangeLabel
import org.chimeramc.client.ui.compose.caramel.sp_

private val accents = listOf(C.Blue, C.Caramel, C.Green, Color(0xFF966EFF))

/** The pack's `Page` header: a display title, a subtitle and optional trailing actions. */
@Composable
internal fun CaramelPage(
    title: String,
    sub: String,
    actions: @Composable () -> Unit = {},
    body: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(title, style = T.Display)
                Text(sub, style = T.Body.copy(fontSize = 12.5.sp_))
            }
            actions()
        }
        body()
    }
}

@Composable
private fun IconTile(icon: ImageVector, color: Color, size: Int = 48) =
    Box(
        Modifier.size(size.dp).background(color.copy(0.2f), RoundedCornerShape(14.dp))
            .border(1.dp, color.copy(0.55f), RoundedCornerShape(14.dp)),
        Alignment.Center,
    ) { Icon(icon, null, Modifier.size((size / 2).dp), color) }

/** The Mods screen: search, category chips and a card grid. Tapping a card opens the detail modal. */
@Composable
fun CaramelModsScreen(
    state: CaramelLauncherState,
    onToggle: (CaramelModInfo, Boolean) -> Unit,
    onOpenFolder: () -> Unit,
) {
    var q by remember { mutableStateOf("") }
    var chip by remember { mutableStateOf(0) }
    CaramelPage(
        "Mods",
        "Browse, install and toggle mods for this instance",
        {
            SearchField(q, { q = it }, "Search mods", Modifier.width(300.dp))
            Spacer(Modifier.width(10.dp))
            GlassButton("Open mods folder", onOpenFolder, icon = Icons.Rounded.Folder)
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Installed", "Performance", "Visual", "Utility", "Fun").forEachIndexed { i, t ->
                FilterChip(t, chip == i, { chip = i })
            }
        }
        val list = remember(q, state.mods) {
            state.mods.filter { q.trim().isEmpty() || it.name.contains(q.trim(), ignoreCase = true) }
        }
        if (list.isEmpty()) {
            EmptyState("No mods installed", "Mods you add to this instance appear here.", Modifier.fillMaxSize())
        } else {
            LazyVerticalGrid(
                GridCells.Adaptive(270.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(list, key = { it.name }) { m ->
                    GlassPanel(
                        Modifier.fillMaxWidth().height(218.dp)
                            .clickable(remember { MutableInteractionSource() }, null) { state.selectedMod = m },
                        RoundedCornerShape(20.dp),
                        elevation = 8.dp,
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconTile(modIcon(m), accents[modIconIndex(m)], 48)
                                Column {
                                    Text(m.name, style = T.Subtitle)
                                    Text("by ${m.author}", style = T.Body)
                                }
                            }
                            Text(m.description, style = T.Body, maxLines = 3)
                            StatusLabel(if (m.enabled) "Enabled" else "Disabled", if (m.enabled) C.Green else C.Text2)
                            Spacer(Modifier.weight(1f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("v${m.version}", style = T.Caption, modifier = Modifier.weight(1f))
                                CaramelToggle(m.enabled, { onToggle(m, it) })
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun modIcon(m: CaramelModInfo): ImageVector = listOf(
    Icons.Rounded.Extension, Icons.Rounded.Bolt, Icons.Rounded.Place, Icons.Rounded.Dashboard,
)[modIconIndex(m)]

private fun modIconIndex(m: CaramelModInfo): Int = (m.name.hashCode() and 0x7fffffff) % 4

/** The Servers screen: a featured banner over the saved-server rows. */
@Composable
fun CaramelServersScreen(state: CaramelLauncherState, onJoin: (CaramelServerInfo) -> Unit) {
    CaramelPage("Servers", "Featured and saved servers") {
        val featured = state.servers.firstOrNull()
        if (featured != null) {
            GlassPanel(Modifier.fillMaxWidth().height(180.dp), RoundedCornerShape(24.dp)) {
                Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(C.Blue.copy(0.28f), C.Caramel.copy(0.16f)))))
                Row(Modifier.fillMaxSize().padding(36.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(featured.name, style = T.Display.copy(fontSize = 26.sp_))
                        Text(featured.address, style = T.Body.copy(fontSize = 13.sp_))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            featured.online?.let { StatusLabel("$it playing", C.Green) }
                            VersionRangeLabel(featured.minVersion, featured.maxVersion)
                        }
                    }
                    PrimaryButton("Join server", { onJoin(featured) }, Modifier.width(176.dp), icon = Icons.Rounded.PlayArrow)
                }
            }
        }
        if (state.servers.isEmpty()) {
            EmptyState("No servers saved", "Add a server and it appears here.", Modifier.fillMaxWidth().weight(1f))
        } else {
            Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.servers.forEachIndexed { i, s ->
                    GlassPanel(Modifier.fillMaxWidth().height(80.dp), RoundedCornerShape(20.dp), elevation = 6.dp) {
                        Row(
                            Modifier.fillMaxSize().padding(horizontal = 22.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            ServerIcon(accents[i % 4], 52)
                            Column(Modifier.weight(1f)) {
                                Text(s.name, style = T.Subtitle.copy(fontSize = 15.sp_))
                                Text(s.address, style = T.Body)
                            }
                            s.online?.let { Text("$it online", style = T.Label) }
                            if (s.online != null) Icon(Icons.Rounded.Wifi, null, Modifier.size(18.dp), C.Green)
                            s.ping?.let { Text("$it ms", style = T.Body) }
                            VersionRangeLabel(s.minVersion, s.maxVersion)
                            GlassButton("Join", { onJoin(s) }, icon = Icons.Rounded.PlayArrow, height = 36)
                        }
                    }
                }
            }
        }
    }
}

/** The shared empty state, so a screen never draws a fabricated row in place of real data. */
@Composable
internal fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    GlassPanel(modifier, RoundedCornerShape(20.dp), elevation = 6.dp) {
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = T.Subtitle)
            Spacer(Modifier.height(6.dp))
            Text(body, style = T.Body, textAlign = TextAlign.Center)
        }
    }
}
