package org.chimeramc.client.launcher.core.splash;

import org.chimeramc.client.R;

/**
 * The bundled key art the splash can show.
 *
 * <p>The art is the Minecraft key-art wallpapers, from The Garden Awakens onward, re-encoded to
 * 1280x720 JPEG and drawn as a full-bleed cover. They are {@code drawable-nodpi} because they are
 * photographic and scale by aspect, not by density.
 *
 * <p>This is the one list: adding a picture is one entry here, and the splash picks from whatever
 * length this array has. The order is the order the source page lists them in, oldest drop last, so
 * the first entry is the newest key art.
 */
public final class SplashArtCatalog {

    private static final int[] DRAWABLES = {
            R.drawable.splash_bg_01,
            R.drawable.splash_bg_02,
            R.drawable.splash_bg_03,
            R.drawable.splash_bg_04,
            R.drawable.splash_bg_05,
            R.drawable.splash_bg_06,
            R.drawable.splash_bg_07,
            R.drawable.splash_bg_08,
            R.drawable.splash_bg_09,
            R.drawable.splash_bg_10,
            R.drawable.splash_bg_11,
            R.drawable.splash_bg_12,
            R.drawable.splash_bg_13,
            R.drawable.splash_bg_14,
            R.drawable.splash_bg_15,
            R.drawable.splash_bg_16,
            R.drawable.splash_bg_17,
            R.drawable.splash_bg_18,
    };

    private SplashArtCatalog() {}

    /** How many pictures are bundled. */
    public static int size() {
        return DRAWABLES.length;
    }

    /** The drawable at [index], wrapping so an out-of-range index cannot crash the splash. */
    public static int drawableAt(int index) {
        if (DRAWABLES.length == 0) {
            throw new IllegalStateException("the splash catalogue is empty");
        }
        int wrapped = ((index % DRAWABLES.length) + DRAWABLES.length) % DRAWABLES.length;
        return DRAWABLES[wrapped];
    }

    /** Every drawable, for a caller that wants to render them all. */
    public static int[] all() {
        return DRAWABLES.clone();
    }
}
