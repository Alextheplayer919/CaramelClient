package org.chimeramc.client.core.cosmetics;

/**
 * The single answer to "what colour is the cloth here?", shared by every cape renderer.
 *
 * <p>The preview mesh, the grid thumbnail and the in-game 64x32 texture all need the same colour at
 * a normalised cloth point. Keeping the rule in one place is what stops the three from drifting —
 * and it is where the animated sweep ({@link CapeMotion}) is applied, so a cape the catalogue calls
 * animated moves in the menu, in the grid and on the character rather than only in one of them.
 *
 * <p>Pure and Android-free, so the rule is unit-tested without a device.
 */
public final class CapeWeave {

    private CapeWeave() {
    }

    /** The resting colour at a point; identical to {@link #colorAt(CosmeticCatalog.Cape, float,
     *  float, double)} at zero elapsed time. */
    public static int colorAt(CosmeticCatalog.Cape cape, float u, float v) {
        return colorAt(cape, u, v, 0.0);
    }

    /**
     * The colour at a normalised cloth point, including the animated sweep when the cape is animated.
     *
     * @param cape    the cape (null yields transparent)
     * @param u       0..1 across the cloth, left to right
     * @param v       0..1 down the cloth, top to bottom
     * @param seconds elapsed time, used only by an animated cape
     * @return the ARGB colour
     */
    public static int colorAt(CosmeticCatalog.Cape cape, float u, float v, double seconds) {
        if (cape == null) return 0;
        int color = CapePatterns.colorAt(cape.pattern, u, v, cape.color, cape.accentColor);
        if (cape.pattern == CosmeticCatalog.CapePattern.OPTIFINE
                && CapePatterns.optifineMonogramAt(u, v)) {
            color = cape.accentColor;
        }
        if (cape.animated) {
            color = CapeMotion.apply(color, u, v, seconds);
        }
        return color;
    }
}
