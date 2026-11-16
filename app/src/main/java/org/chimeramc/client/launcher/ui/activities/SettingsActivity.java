package org.chimeramc.client.ui.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.compose.ui.platform.ComposeView;
import androidx.compose.ui.platform.ViewCompositionStrategy;

import org.chimeramc.client.launcher.util.LauncherSettingsController;
import org.chimeramc.client.ui.compose.caramel.CaramelSettingsHost;

/**
 * The settings screen: the Caramel settings panel over the real stores.
 *
 * <p>This activity used to be a placeholder that drew nothing — it survived only because the
 * manifest and the app menu named it, so the settings tab opened an empty window. The panel's
 * controls and the stores behind them live in {@link LauncherSettingsController}, which the
 * launcher's own settings modal shares, so a control cannot work on one surface and be dead on the
 * other.
 */
public class SettingsActivity extends BaseActivity {

    private LauncherSettingsController controller;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        controller = new LauncherSettingsController(this);
        setContentView(buildComposeHost());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (controller != null) controller.refresh();
    }

    private ComposeView buildComposeHost() {
        ComposeView view = new ComposeView(this);
        view.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool.INSTANCE);
        CaramelSettingsHost.setContent(view, controller.state(), controller, this::finish);
        return view;
    }
}
