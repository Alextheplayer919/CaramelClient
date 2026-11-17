package org.chimeramc.client.core.cosmetics;

/**
 * The moving light an animated cape carries, shared by the preview, the tile and the in-game texture.
 *
 * <p>{@link CosmeticCatalog.Cape#animated} was set on the flagship cape and read by nothing, so the
 * one cape the catalogue promised would move sat still. This is the motion itself: a soft band that
 * travels down the cloth, brightening the weave as it passes. It is expressed as a brightness
 * multiplier at a point, so every renderer can apply it to whatever colour it already computed
 * without duplicating the weave rules.
 *
 * <p>Pure and Android-free, so the sweep is unit-tested without a device.
 */
public final class CapeMotion {

    /** Seconds for the band to travel the whole length of the cloth once. */
    public static final float PERIOD_SECONDS = 3.2f;

    /** How much the band brightens the cloth at its crest, as a fraction (1.0 = no change). */
    private static final float PEAK_GAIN = 0.35f;

    /** How wide the band is along the cloth, in normalised cloth units. */
    private static final float BAND_WIDTH = 0.28f;

    private CapeMotion() {
    }

    /**
     * The brightness multiplier for an animated point at {@code (u, v)}.
     *
     * <p>The band's leading edge travels from the shoulders (v=0) to the hem (v=1) and repeats. The
     * crest brightens the cloth and the trough returns it to its base colour, so the sweep reads as
     * light moving across fabric rather than the whole cape pulsing.
     *
     * @param u       0..1 across the cloth, left to right
     * @param v       0..1 down the cloth, top to bottom
     * @param seconds elapsed time
     * @return a multiplier around 1.0 (clamped to a sane range)
     */
    public static float gain(float u, float v, double seconds) {
        float phase = (float) ((seconds % PERIOD_SECONDS) / PERIOD_SECONDS);
        // A slight diagonal lean, so the band does not read as a flat horizontal wipe.
        float centre = phase + (u - 0.5f) * 0.12f;
        float d = v - centre;
        // Wrap the distance so the band re-enters at the top as it leaves the hem.
        if (d > 0.5f) d -= 1f;
        if (d < -0.5f) d += 1f;
        float t = 1f - Math.min(1f, Math.abs(d) / (BAND_WIDTH * 0.5f));
        float crest = t * t * (3f - 2f * t);   // smoothstep, so the band has soft edges
        return 1f + PEAK_GAIN * crest;
    }

    /**
     * Applies the sweep to an ARGB colour, preserving alpha.
     *
     * @param argb    the base colour
     * @param u       0..1 across the cloth
     * @param v       0..1 down the cloth
     * @param seconds elapsed time
     * @return the brightened colour
     */
    public static int apply(int argb, float u, float v, double seconds) {
        float g = gain(u, v, seconds);
        if (g == 1f) return argb;
        int a = (argb >>> 24) & 0xFF;
        int r = clamp(Math.round(((argb >>> 16) & 0xFF) * g));
        int gg = clamp(Math.round(((argb >>> 8) & 0xFF) * g));
        int b = clamp(Math.round((argb & 0xFF) * g));
        return (a << 24) | (r << 16) | (gg << 8) | b;
    }

    private static int clamp(int value) {
        return value < 0 ? 0 : Math.min(value, 255);
    }
}
