package org.chimeramc.client.ui.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;

/**
 * Placeholder screen.
 *
 * <p>The legacy UI was removed in one sweep so a new one can be dropped in. This class survives
 * only because non-UI code (the manifest, an Intent target, or a nav destination) still names it;
 * it draws nothing. Replace it with the real MainActivity screen.
 */
public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Refreshes the on-screen Minecraft version label. Kept as a no-op so
     * {@code VersionManager.attemptRepairLibs} still compiles; the new Main screen should override
     * the label it draws.
     */
    public void setTextMinecraftVersion() {
    }
}
