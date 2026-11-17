package org.chimeramc.client.launcher.core.content;

import android.app.Activity;

import org.chimeramc.client.R;

import java.io.File;

/**
 * The one screenshot entry point.
 *
 * <p>The catalogue's {@code util_screenshot} module and the in-game pause-menu row both take a
 * screenshot, and they must behave identically: same folder, same reporting, same failure message.
 * The catalogue module used to own this logic; it now delegates here so the two callers cannot
 * drift apart.
 */
public final class ScreenshotAction {

    private ScreenshotAction() {
    }

    /**
     * Captures the launcher's own window into the selected instance's {@code Screenshots} folder.
     *
     * <p>Reports through a toast in every case — a missing instance, a device whose compositor
     * excludes the game surface, and success all say something, so a screenshot never appears to do
     * nothing.
     */
    public static void capture(Activity activity) {
        if (activity == null) return;
        File dir = ScreenshotStore.screenshotsDir(activity);
        if (dir == null) {
            org.chimeramc.client.ui.compose.caramel.CaramelNoticeBus.post(activity, R.string.catalog_options_no_instance);
            return;
        }
        ScreenshotCapture.capture(activity, dir, new ScreenshotCapture.Callback() {
            @Override
            public void onSaved(File file) {
                org.chimeramc.client.ui.compose.caramel.CaramelNoticeBus.post(activity, R.string.catalog_screenshot_saved);
            }

            @Override
            public void onFailed() {
                org.chimeramc.client.ui.compose.caramel.CaramelNoticeBus.post(activity, R.string.catalog_screenshot_failed);
            }
        });
    }
}
