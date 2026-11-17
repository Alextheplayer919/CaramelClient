package org.chimeramc.client.core.cosmetics;

/**
 * Chooses the preview's internal supersample factor from the device's GPU family.
 *
 * <p>The preview rasterises the whole scene at a multiple of the view size and downscales for
 * antialiasing. On a software rasteriser (the emulator's SwiftShader, Mesa's llvmpipe) that is a
 * real cost per frame, and a phone GPU handles it without noticing; the family is the only signal
 * available without measuring a frame. This maps the family to a factor, so a software rasteriser
 * renders at 1x (no supersample) rather than paying 4x the fill.
 *
 * <p>Pure, so the mapping is pinned without a device. It is deliberately conservative: an unknown
 * or unread family gets the normal factor, because the common case is real hardware and a wrong
 * downgrade would make the preview look worse for no reason.
 */
public final class PreviewQuality {

    /** The supersample factor used on real hardware. */
    public static final int HARDWARE_SUPERSAMPLE = 2;
    /** The supersample factor used on a software rasteriser. */
    public static final int SOFTWARE_SUPERSAMPLE = 1;

    private PreviewQuality() {
    }

    /**
     * The internal supersample factor for a GPU family.
     *
     * @param family the classified family, from {@link GpuFamily#fromNativeCode}
     * @return {@link #SOFTWARE_SUPERSAMPLE} for a software rasteriser, otherwise
     *         {@link #HARDWARE_SUPERSAMPLE}
     */
    public static int supersampleFor(GpuFamily.Kind family) {
        return GpuFamily.isSoftware(family) ? SOFTWARE_SUPERSAMPLE : HARDWARE_SUPERSAMPLE;
    }

    /**
     * The internal supersample factor for a native GPU-family code.
     *
     * @param nativeCode the raw {@code nativeGpuFamily()} reading; a negative "not read" value is
     *                   treated as unknown hardware, not as software
     */
    public static int supersampleForNativeCode(int nativeCode) {
        if (nativeCode < 0) return HARDWARE_SUPERSAMPLE;
        return supersampleFor(GpuFamily.fromNativeCode(nativeCode));
    }

    /**
     * The offscreen raster size for a view, supersampled but capped on its <em>longest</em> edge so
     * the aspect ratio survives.
     *
     * <p>Clamping width and height independently (the earlier code) squashed whichever axis was over
     * the cap while leaving the other alone, so a wide or tall view rendered at the wrong shape and
     * the blit back to the view stretched the character — the "3D skin looks stretched" report. This
     * scales both axes by one factor, so the cap never changes the shape.
     *
     * @param viewW the view's width in pixels
     * @param viewH the view's height in pixels
     * @param supersample the internal supersample factor, from {@link #supersampleForNativeCode}
     * @param maxEdge the cap on the longest edge, so a large view cannot blow the memory budget
     * @return {@code {width, height}}, each at least 1, in the view's own aspect ratio
     */
    public static int[] internalSize(int viewW, int viewH, int supersample, int maxEdge) {
        int factor = Math.max(1, supersample);
        float w = Math.max(1, viewW) * (float) factor;
        float h = Math.max(1, viewH) * (float) factor;
        float longest = Math.max(w, h);
        if (maxEdge > 0 && longest > maxEdge) {
            float scale = maxEdge / longest;
            w *= scale;
            h *= scale;
        }
        return new int[]{Math.max(1, Math.round(w)), Math.max(1, Math.round(h))};
    }
}
