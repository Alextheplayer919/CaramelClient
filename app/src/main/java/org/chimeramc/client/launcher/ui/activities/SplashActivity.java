package org.chimeramc.client.ui.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.compose.ui.platform.ComposeView;
import androidx.compose.ui.platform.ViewCompositionStrategy;

import org.chimeramc.client.R;
import org.chimeramc.client.core.versions.VersionManager;
import org.chimeramc.client.ui.compose.caramel.launcher.CaramelSplashHost;
import org.chimeramc.client.util.AppLog;
import org.chimeramc.client.util.MainThread;

/**
 * The cold-start splash: it warms the launcher up, then hands over to {@link MainActivity}.
 *
 * <p>It is the only {@code LAUNCHER} entry, so if it does not navigate the app never leaves it —
 * which is exactly what happened while it was a bare stub that drew nothing and started nothing.
 * The warm-up is best-effort: a version-store failure is reported and the launcher is still opened,
 * so a broken install lands the user on the launcher (where the readiness state is shown) rather
 * than stranding them on a splash.
 *
 * <p>The screen itself is Compose ({@code CaramelSplashScreen}), on the pack's backdrop. The window
 * background is the same navy (see {@code LauncherTheme}), so the first frame does not flash white.
 */
public class SplashActivity extends BaseActivity {

    /** Intent extra marking a cold start, used to gate the launcher's arrival animation. */
    public static final String EXTRA_FROM_SPLASH = "from_splash";

    /**
     * A splash is a cover, not a destination. If the warm-up stalls (a slow disk, a locked store),
     * hand over anyway rather than hold the user here.
     */
    private static final long MAX_WARMUP_MS = 4000L;

    private final MainThread.Scope posts = MainThread.scope();
    private boolean navigated;

    private final Runnable backstop = this::navigateToMain;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildSplash());
        posts.postDelayed(backstop, MAX_WARMUP_MS);
        warmUpThenNavigate();
    }

    private ComposeView buildSplash() {
        ComposeView view = new ComposeView(this);
        view.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool.INSTANCE);
        CaramelSplashHost.setContent(view, getString(R.string.launch_preparing));
        return view;
    }

    private void warmUpThenNavigate() {
        VersionManager.initializeAsync(this, new VersionManager.InitCallback() {
            @Override
            public void onReady(VersionManager manager) {
                navigateToMain();
            }

            @Override
            public void onFailed(Throwable error) {
                AppLog.warnOnce("SplashActivity", "warmup", "Version warm-up failed", error);
                navigateToMain();
            }
        });
    }

    private void navigateToMain() {
        if (navigated || isFinishing()) return;
        navigated = true;
        posts.remove(backstop);
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_FROM_SPLASH, true);
        // Forward a share that arrived on the splash so MainActivity still sees it.
        if (getIntent() != null && getIntent().getData() != null) {
            intent.setData(getIntent().getData());
        }
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        posts.close();
    }
}
