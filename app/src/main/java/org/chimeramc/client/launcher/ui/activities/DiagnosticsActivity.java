package org.chimeramc.client.launcher.ui.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;

import org.chimeramc.client.ui.activities.BaseActivity;

/**
 * Placeholder diagnostics screen.
 *
 * <p>The legacy UI was removed in one sweep so a new one can be dropped in. This class survives
 * only because {@code MinecraftLoadingActivity} still launches it; it draws nothing.
 */
public class DiagnosticsActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
