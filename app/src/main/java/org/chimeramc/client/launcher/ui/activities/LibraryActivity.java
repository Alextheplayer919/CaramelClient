package org.chimeramc.client.ui.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;

/**
 * Placeholder screen.
 *
 * <p>The legacy UI was removed in one sweep so a new one can be dropped in. This class survives
 * only because non-UI code (the manifest, an Intent target, or a nav destination) still names it;
 * it draws nothing. Replace it with the real LibraryActivity screen.
 */
public class LibraryActivity extends BaseActivity {

    /** Initial inner tab extra. Retained so deep-link handling keeps compiling. */
    public static final String EXTRA_INITIAL_TAB = "library_initial_tab";
    public static final int TAB_INSTANCES = 0;
    public static final int TAB_INSTALL = 1;
    public static final int TAB_REPLAY = 2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
