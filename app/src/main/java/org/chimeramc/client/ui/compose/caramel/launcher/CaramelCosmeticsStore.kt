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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import org.chimeramc.client.R
import androidx.compose.ui.unit.dp
import org.chimeramc.client.ui.compose.caramel.C
import org.chimeramc.client.ui.compose.caramel.CaramelNotice
import org.chimeramc.client.ui.compose.caramel.Tone
import org.chimeramc.client.ui.compose.caramel.CaramelCosmeticInfo
import org.chimeramc.client.ui.compose.caramel.CaramelCosmeticKind
import org.chimeramc.client.ui.compose.caramel.CaramelLauncherState
import org.chimeramc.client.ui.compose.caramel.FilterChip
import org.chimeramc.client.ui.compose.caramel.GlassButton
import org.chimeramc.client.ui.compose.caramel.GlassPanel
import org.chimeramc.client.ui.compose.caramel.PrimaryButton
import org.chimeramc.client.ui.compose.caramel.StatusLabel
import org.chimeramc.client.ui.compose.caramel.T
import org.chimeramc.client.ui.compose.caramel.sp_

private val cosAccents = listOf(C.Blue, C.Caramel, C.Green, Color(0xFF966EFF))

/** The cosmetic kinds, in the order the filter chips show them (index 1..4). */
private val cosFilterKinds = listOf(
    CaramelCosmeticKind.Cape,
    CaramelCosmeticKind.Wings,
    CaramelCosmeticKind.Hat,
    CaramelCosmeticKind.Pet,
)

/**
 * The tiles a chip selects: 0 is "All", 1..4 are the kinds above, 5 is the starred set.
 *
 * <p>Pure and index-based so the filter the chips promise is the filter the grid applies; before
 * this the chip only moved its own highlight and the grid kept showing everything.
 */
internal fun filterCosmetics(all: List<CaramelCosmeticInfo>, chip: Int): List<CaramelCosmeticInfo> =
    when {
        chip == 5 -> all.filter { it.favourite }
        chip in 1..cosFilterKinds.size -> all.filter { it.kind == cosFilterKinds[chip - 1] }
        else -> all
    }

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
    CaramelPage(stringResource(R.string.mod_menu_tab_cosmetics), stringResource(R.string.caramel_cosmetics_sub)) {
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
                    GlassButton(stringResource(R.string.caramel_save_outfit), {}, Modifier.fillMaxWidth(), icon = Icons.Rounded.Check)
                    GlassButton(stringResource(R.string.caramel_reset_view), onResetView, Modifier.fillMaxWidth(), icon = Icons.Rounded.SwapHoriz)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(R.string.caramel_filter_all, R.string.caramel_filter_capes, R.string.caramel_filter_wings, R.string.caramel_filter_hats, R.string.caramel_filter_pets).forEachIndexed { i, t -> FilterChip(stringResource(t), chip == i, { chip = i }) }
                    FilterChip(stringResource(R.string.caramel_filter_favorites), chip == 5, { chip = 5 })
                }
                val shown = remember(state.cosmetics, chip) { filterCosmetics(state.cosmetics, chip) }
                if (state.cosmetics.isEmpty()) {
                    EmptyState(stringResource(R.string.caramel_no_cosmetics_title), stringResource(R.string.caramel_no_cosmetics_body), Modifier.fillMaxWidth().weight(1f))
                } else if (shown.isEmpty()) {
                    EmptyState(stringResource(R.string.caramel_no_favourite_cosmetics_title), stringResource(R.string.caramel_no_favourite_cosmetics_body), Modifier.fillMaxWidth().weight(1f))
                } else {
                    LazyVerticalGrid(
                        GridCells.Adaptive(190.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(shown, key = { it.name }) { c ->
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
            stringResource(R.string.caramel_skin_preview),
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
                CaramelCosmeticThumbnail(c, Modifier.fillMaxWidth().height(104.dp).padding(6.dp), 192)
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
                    StatusLabel(stringResource(R.string.caramel_equipped), C.Green)
                } else if (c.owned) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.caramel_owned), style = T.Body, modifier = Modifier.weight(1f))
                        GlassButton(stringResource(R.string.caramel_equip), onEquip, height = 28)
                    }
                } else {
                    StatusLabel(stringResource(R.string.caramel_locked), C.Caramel)
                }
            }
        }
    }
}

/** The News screen: a featured post over a side list. */
@Composable
fun CaramelNewsScreen(state: CaramelLauncherState) {
    CaramelPage(stringResource(R.string.caramel_news_title), stringResource(R.string.caramel_news_sub)) {
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            val featured = state.news.firstOrNull { it.featured } ?: state.news.firstOrNull()
            if (featured == null) {
                EmptyState(stringResource(R.string.caramel_no_news_title), stringResource(R.string.caramel_no_news_body), Modifier.fillMaxSize())
            } else {
                GlassPanel(Modifier.weight(1.3f).fillMaxHeight(), RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(6.dp)) {
                        Box(
                            Modifier.fillMaxWidth().height(260.dp)
                                .background(Brush.verticalGradient(listOf(Color(0xFF5A2818), Color(0xFFF5963C))), RoundedCornerShape(18.dp)),
                        )
                        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatusLabel(stringResource(R.string.caramel_featured), C.Caramel)
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
    CaramelPage(stringResource(R.string.caramel_store_title), stringResource(R.string.caramel_store_sub)) {
        GlassPanel(Modifier.fillMaxWidth().height(160.dp), RoundedCornerShape(24.dp)) {
            Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(C.Caramel.copy(0.3f), C.Blue.copy(0.08f)))))
            Row(Modifier.fillMaxSize().padding(horizontal = 36.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.caramel_plus), style = T.Display.copy(fontSize = 28.sp_))
                    Text(
                        stringResource(R.string.caramel_plus_blurb),
                        style = T.Body.copy(fontSize = 13.5.sp_),
                    )
                }
                val noPayment = stringResource(R.string.caramel_plus_no_payment)
                PrimaryButton(
                    stringResource(R.string.caramel_get_plus),
                    { state.notice = CaramelNotice(noPayment, warning = true) },
                    Modifier.width(200.dp),
                    Tone.Caramel,
                    muted = true,
                )
            }
        }
        EmptyState(
            stringResource(R.string.caramel_store_soon_title),
            stringResource(R.string.caramel_store_soon_body),
            Modifier.fillMaxWidth().weight(1f),
        )
    }
}
