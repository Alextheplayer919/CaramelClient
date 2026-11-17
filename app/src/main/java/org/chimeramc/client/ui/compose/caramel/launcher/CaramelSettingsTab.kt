package org.chimeramc.client.ui.compose.caramel.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.chimeramc.client.R
import org.chimeramc.client.ui.compose.caramel.CaramelSettingsPanel
import org.chimeramc.client.ui.compose.caramel.CaramelSettingsProvider
import org.chimeramc.client.ui.compose.caramel.T

/**
 * The Settings tab: the same panel the gear used to open as a modal, mounted as a real screen.
 *
 * <p>The gear in the sidebar opened a popup over whatever tab was showing, which read as a menu
 * rather than a destination and hid the panel behind a scrim. Settings is a first-class tab now, so
 * the sidebar's gear selects it like every other entry and the panel fills the content area. The
 * panel itself is unchanged — [CaramelSettingsPanel] over the activity's [CaramelSettingsProvider] —
 * so a control cannot work in one host and be dead in the other.
 */
@Composable
fun CaramelSettingsTabScreen(settings: CaramelSettingsProvider?, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_title), style = T.Display)
            Text(stringResource(R.string.caramel_settings_sub), style = T.Body)
        }
        if (settings == null) {
            // A host that mounted the launcher without a settings store: say so rather than drawing
            // controls that cannot write anywhere.
            EmptyState(
                stringResource(R.string.caramel_settings_unavailable_title),
                stringResource(R.string.caramel_settings_unavailable_body),
                Modifier.fillMaxWidth().weight(1f),
            )
            return@Column
        }
        CaramelSettingsPanel(settings.settingsState(), settings.settingsActions(), Modifier.weight(1f))
    }
}
