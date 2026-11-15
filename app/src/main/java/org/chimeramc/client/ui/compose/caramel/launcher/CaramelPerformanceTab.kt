package org.chimeramc.client.ui.compose.caramel.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.chimeramc.client.R
import org.chimeramc.client.core.minecraft.PerfFeatureCatalog
import org.chimeramc.client.ui.compose.caramel.CaramelLauncherState
import org.chimeramc.client.ui.compose.caramel.CaramelSimpleDropdown
import org.chimeramc.client.ui.compose.caramel.CaramelToggle
import org.chimeramc.client.ui.compose.caramel.GlassButton
import org.chimeramc.client.ui.compose.caramel.SettingRow
import org.chimeramc.client.ui.compose.caramel.SliderRow
import org.chimeramc.client.ui.compose.caramel.T

/**
 * The instance Performance tab: every entry is a real Bedrock option.
 *
 * <p>The tab used to draw nothing. It now renders [PerfFeatureCatalog] in full — over fifty game
 * settings across frames, distance, lighting, particles, camera, upscaling and the interface — and
 * each control writes its key into the instance's own `minecraftpe/options.txt` through the
 * activity's actions. The value the control draws is the value read back from that file, so a
 * setting changed inside the game shows up here rather than a launcher-side copy.
 *
 * <p>A feature whose key the instance's file does not carry yet falls back to the catalogue's own
 * default, which is the game's documented default for that key.
 */
@Composable
fun CaramelPerformanceTab(
    state: CaramelLauncherState,
    actions: CaramelLauncherHost.Actions,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(stringResource(R.string.caramel_perf_sub), style = T.Body)
        if (state.version == null) {
            StatusBanner(stringResource(R.string.caramel_perf_no_instance))
            return@Column
        }
        PerfFeatureCatalog.groups().forEach { group ->
            Text(group, style = T.Title)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PerfFeatureCatalog.inGroup(group).forEach { feature ->
                    val stored = state.perfValues[feature.id] ?: feature.defaultValue
                    when (feature.kind) {
                        PerfFeatureCatalog.Kind.TOGGLE -> SettingRow(
                            feature.name,
                            feature.description,
                        ) {
                            CaramelToggle(stored == feature.onValue(), { on ->
                                actions.setPerfFeature(
                                    feature.id,
                                    if (on) feature.onValue() else feature.offValue(),
                                )
                            })
                        }
                        PerfFeatureCatalog.Kind.CHOICE -> SettingRow(
                            feature.name,
                            feature.description,
                        ) {
                            val labels = feature.values.map { feature.labelFor(it) }
                            CaramelSimpleDropdown(
                                value = feature.labelFor(stored),
                                options = labels,
                                onSelect = { label ->
                                    val index = labels.indexOf(label)
                                    if (index >= 0) {
                                        actions.setPerfFeature(feature.id, feature.values[index])
                                    }
                                },
                                width = 210,
                            )
                        }
                        PerfFeatureCatalog.Kind.RANGE -> SliderRow(
                            title = feature.name,
                            valueText = stored,
                            value = PerfFeatureCatalog.rangeFraction(feature, stored),
                            onChange = { fraction ->
                                actions.setPerfFeature(
                                    feature.id,
                                    PerfFeatureCatalog.rangeValue(feature, fraction),
                                )
                            },
                        )
                    }
                }
            }
        }
        GlassButton(stringResource(R.string.caramel_perf_reset), actions::resetPerfFeatures, height = 38)
        if (state.perfStatus.isNotEmpty()) {
            Text(state.perfStatus, style = T.Caption)
        }
    }
}

/** A one-line, low-emphasis status strip used for the tab's own guard messages. */
@Composable
private fun StatusBanner(text: String) {
    Text(text, style = T.Body)
}
