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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.SwapHoriz
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.chimeramc.client.ui.compose.caramel.C
import org.chimeramc.client.ui.compose.caramel.CaramelCosmeticInfo
import org.chimeramc.client.ui.compose.caramel.CaramelLauncherState
import org.chimeramc.client.ui.compose.caramel.FilterChip
import org.chimeramc.client.ui.compose.caramel.GlassButton
import org.chimeramc.client.ui.compose.caramel.GlassPanel
import org.chimeramc.client.ui.compose.caramel.PrimaryButton
import org.chimeramc.client.ui.compose.caramel.StatusLabel
import org.chimeramc.client.ui.compose.caramel.T
import org.chimeramc.client.ui.compose.caramel.sp_

private val cosAccents = listOf(C.Blue, C.Caramel, C.Green, Color(0xFF966EFF))

/**
 * The Cosmetics screen, ported from the pack's `CosmeticsScreen`.
 *
 * <p>The pack reserves a white rounded square for the 3D skin viewer; on Android the client already
 * has a software skin renderer, so the slot is filled by the host through [skinViewer]. When the
 * host passes nothing, the slot shows its empty label rather than a fake model.
 */
@Composable
fun CaramelCosmeticsScreen(
    state: CaramelLauncherState,
    onToggleFavourite: (CaramelCosmeticInfo) -> Unit,
    onEquip: (CaramelCosmeticInfo) -> Unit,
    onResetView: () -> Unit,
    skinViewer: @Composable (Modifier) -> Unit = { EmptySkinSlot(it) },
) {
    var chip by remember { mutableStateOf(0) }
    CaramelPage("Cosmetics", "Capes, wings, hats and more") {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            GlassPanel(Modifier.width(320.dp).fillMaxHeight(), RoundedCornerShape(24.dp)) {
                Column(
                    Modifier.fillMaxSize().padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier.fillMaxWidth().weight(1f).background(Color.White, RoundedCornerShape(22.dp)),
                        Alignment.Center,
                    ) { skinViewer(Modifier.fillMaxSize().padding(8.dp)) }
                    GlassButton("Save outfit", {}, Modifier.fillMaxWidth(), icon = Icons.Rounded.Check)
                    GlassButton("Reset view", onResetView, Modifier.fillMaxWidth(), icon = Icons.Rounded.SwapHoriz)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf("All", "Capes", "Wings", "Hats", "Pets").forEachIndexed { i, t -> FilterChip(t, chip == i, { chip = i }) }
                    FilterChip("Favorites", chip == 5, { chip = 5 })
                }
                if (state.cosmetics.isEmpty()) {
                    EmptyState("No cosmetics yet", "Capes and hats you own appear here.", Modifier.fillMaxWidth().weight(1f))
                } else {
                    LazyVerticalGrid(
                        GridCells.Adaptive(190.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.cosmetics, key = { it.name }) { c ->
                            CosmeticTile(c, { onToggleFavourite(c) }, { onEquip(c) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun EmptySkinSlot(modifier: Modifier) {
    Box(modifier, Alignment.Center) {
        Text(
            "3D skin preview",
            style = T.Title.copy(color = Color(0xFF1C2854), fontSize = 15.sp_, textAlign = TextAlign.Center),
            modifier = Modifier.width(190.dp),
        )
    }
}

@Composable
private fun CosmeticTile(c: CaramelCosmeticInfo, onFav: () -> Unit, onEquip: () -> Unit) {
    val accent = cosAccents[(c.name.hashCode() and 0x7fffffff) % 4]
    GlassPanel(Modifier.fillMaxWidth().height(188.dp), RoundedCornerShape(18.dp), elevation = 6.dp) {
        Column(Modifier.padding(8.dp)) {
            Box(
                Modifier.fillMaxWidth().height(112.dp)
                    .background(Brush.verticalGradient(listOf(accent.copy(0.32f), accent.copy(0.05f))), RoundedCornerShape(12.dp)),
                Alignment.Center,
            ) {
                Box(Modifier.size(40.dp, 68.dp).background(accent, RoundedCornerShape(4.dp)))
                Box(
                    Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp).clip(CircleShape)
                        .background(Color(0x96081024))
                        .border(1.dp, if (c.favourite) C.Caramel else Color.White.copy(0.25f), CircleShape)
                        .clickable(remember { MutableInteractionSource() }, null, onClick = onFav),
                    Alignment.Center,
                ) {
                    Icon(
                        if (c.favourite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        null, Modifier.size(16.dp), if (c.favourite) C.Caramel else C.Text,
                    )
                }
            }
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(c.name, style = T.Label, maxLines = 1)
                if (c.equipped) {
                    StatusLabel("Equipped", C.Green)
                } else if (c.owned) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Owned", style = T.Body, modifier = Modifier.weight(1f))
                        GlassButton("Equip", onEquip, height = 28)
                    }
                } else {
                    StatusLabel("Locked", C.Caramel)
                }
            }
        }
    }
}

/** The News screen: a featured post over a side list. */
@Composable
fun CaramelNewsScreen(state: CaramelLauncherState) {
    CaramelPage("News", "Updates, events and community posts") {
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            val featured = state.news.firstOrNull { it.featured } ?: state.news.firstOrNull()
            if (featured == null) {
                EmptyState("No news yet", "Posts appear here once the changelog loads.", Modifier.fillMaxSize())
            } else {
                GlassPanel(Modifier.weight(1.3f).fillMaxHeight(), RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(6.dp)) {
                        Box(
                            Modifier.fillMaxWidth().height(260.dp)
                                .background(Brush.verticalGradient(listOf(Color(0xFF5A2818), Color(0xFFF5963C))), RoundedCornerShape(18.dp)),
                        )
                        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatusLabel("Featured", C.Caramel)
                            Text(featured.title, style = T.Display)
                            Text(featured.summary, style = T.Body.copy(fontSize = 13.sp_))
                            Text(featured.meta, style = T.Label.copy(color = C.Caramel))
                        }
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.news.drop(1).take(3).forEachIndexed { i, n ->
                        GlassPanel(Modifier.fillMaxWidth().height(130.dp), RoundedCornerShape(20.dp), elevation = 6.dp) {
                            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Box(
                                    Modifier.size(150.dp, 106.dp)
                                        .background(Brush.verticalGradient(listOf(cosAccents[i % 4].copy(0.5f), cosAccents[i % 4].copy(0.1f))), RoundedCornerShape(12.dp)),
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(n.title, style = T.Subtitle.copy(fontSize = 15.sp_), maxLines = 2)
                                    Text(n.summary, style = T.Body, maxLines = 2)
                                    Text(n.meta, style = T.Caption)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The Store screen: a Caramel+ banner over the bundle grid. */
@Composable
fun CaramelStoreScreen(state: CaramelLauncherState) {
    CaramelPage("Store", "Cosmetics, bundles and Caramel+") {
        GlassPanel(Modifier.fillMaxWidth().height(160.dp), RoundedCornerShape(24.dp)) {
            Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(C.Caramel.copy(0.3f), C.Blue.copy(0.08f)))))
            Row(Modifier.fillMaxSize().padding(horizontal = 36.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Caramel+", style = T.Display.copy(fontSize = 28.sp_))
                    Text(
                        "Support the client and unlock cosmetics, themes and early builds.",
                        style = T.Body.copy(fontSize = 13.5.sp_),
                    )
                }
                PrimaryButton("Learn more", {}, Modifier.width(200.dp))
            }
        }
        EmptyState(
            "Store coming soon",
            "Bundles will appear here once the store backend is live.",
            Modifier.fillMaxWidth().weight(1f),
        )
    }
}
