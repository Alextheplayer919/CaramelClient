package org.chimeramc.client.ui.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.chimeramc.client.util.ApiLevels;

/**
 * Minimal base for the launcher's full-screen activities.
 *
 * <p>Placeholder left after the legacy UI was removed: it keeps only the immersive-mode plumbing
 * the game-host screens ({@code MinecraftLoadingActivity}, {@code LauncherRestartActivity}) rely on.
 * The previous navigation-bar/rail injection lived here and went with the rest of the shell; a
 * screen that wants chrome now provides its own.
 */
public abstract class BaseActivity extends AppCompatActivity {

    /**
     * Whether this screen opts out of launcher chrome. Retained so the game-host screens compile;
     * with the navigation shell removed every screen is effectively chrome-free.
     */
    protected boolean shouldSkipNavBar() {
        return true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(
                visibility -> getWindow().getDecorView().post(this::hideSystemUI));
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    /** Hides the status and navigation bars; transient bars appear on a swipe. */
    protected void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        if (ApiLevels.atLeastQ()) {
            getWindow().setStatusBarContrastEnforced(false);
            getWindow().setNavigationBarContrastEnforced(false);
        }
        if (ApiLevels.atLeastR()) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = decorView.getWindowInsetsController();
            if (controller != null) {
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                controller.hide(WindowInsets.Type.statusBars()
                        | WindowInsets.Type.navigationBars());
            }
        }
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }
}
