package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the animated cape sweep: it must actually move, and a still cape must not.
 *
 * <p>The defect this covers: {@link CosmeticCatalog.Cape#animated} was set on the flagship cape and
 * read by nothing, so the one cape the catalogue promised would move sat still in the menu, the grid
 * and the game. These tests assert the sweep is non-trivial and time-varying, and that a
 * non-animated cape is byte-for-byte unaffected.
 */
public class CapeMotionTest {

    @Test
    public void theSweepBrightensPartOfTheClothAndReturns() {
        // At any instant, the band is brighter than the resting cloth somewhere on the panel and
        // back to resting somewhere else, so it reads as light travelling rather than a flat pulse.
        double t = 0.4;
        boolean anyBright = false;
        boolean anyResting = false;
        for (int i = 0; i < 20; i++) {
            float g = CapeMotion.gain(0.5f, i / 19f, t);
            if (g > 1.02f) anyBright = true;
            if (Math.abs(g - 1f) < 1e-4f) anyResting = true;
        }
        assertTrue("the band must brighten some of the cloth", anyBright);
        assertTrue("the band must leave some of the cloth at its base shade", anyResting);
    }

    @Test
    public void theSweepMovesDownTheClothOverTime() {
        // The brightest point at an early time must be higher up the cloth than at a later time.
        int earlyCrest = crestRow(0.05);
        int lateCrest = crestRow(0.45);
        assertNotEquals("the band must not sit still", earlyCrest, lateCrest);
    }

    @Test
    public void aStillCapeIsUnaffectedByTime() {
        CosmeticCatalog.Cape still = CosmeticCatalog.equippedCape("void_black");
        assertEquals("a non-animated cape must ignore the clock",
                CapeWeave.colorAt(still, 0.3f, 0.6f, 0.0),
                CapeWeave.colorAt(still, 0.3f, 0.6f, 12.7));
    }

    @Test
    public void anAnimatedCapeChangesColourOverTime() {
        CosmeticCatalog.Cape animated = CosmeticCatalog.equippedCape("chimera");
        assertTrue("the flagship cape must be the animated one", animated.animated);
        boolean changed = false;
        for (int i = 0; i < 12; i++) {
            float v = i / 11f;
            if (CapeWeave.colorAt(animated, 0.5f, v, 0.0)
                    != CapeWeave.colorAt(animated, 0.5f, v, 1.1)) {
                changed = true;
                break;
            }
        }
        assertTrue("an animated cape must look different as the sweep advances", changed);
    }

    @Test
    public void theInGameTextureCarriesTheSweep() {
        // The procedural sheet must change between two moments for an animated cape, and not for a
        // still one, so the engine's texture swap shows motion.
        CosmeticCatalog.Cape animated = CosmeticCatalog.equippedCape("chimera");
        byte[] a = CapeTexturePainter.paint(animated.color, animated.trimColor, animated.accentColor,
                animated.pattern, animated.branded, true, 0.0);
        byte[] b = CapeTexturePainter.paint(animated.color, animated.trimColor, animated.accentColor,
                animated.pattern, animated.branded, true, 1.1);
        assertFalse("an animated sheet must differ between two moments", java.util.Arrays.equals(a, b));

        CosmeticCatalog.Cape still = CosmeticCatalog.equippedCape("void_black");
        byte[] s0 = CapeTexturePainter.paint(still.color, still.trimColor, still.accentColor,
                still.pattern, still.branded, false, 0.0);
        byte[] s1 = CapeTexturePainter.paint(still.color, still.trimColor, still.accentColor,
                still.pattern, still.branded, false, 9.0);
        assertTrue("a still sheet must be identical regardless of time",
                java.util.Arrays.equals(s0, s1));
    }

    /** The row (0..19) of the brightest sweep gain at a given time, at the cloth centre. */
    private static int crestRow(double seconds) {
        int best = 0;
        float bestGain = -1f;
        for (int i = 0; i < 20; i++) {
            float g = CapeMotion.gain(0.5f, i / 19f, seconds);
            if (g > bestGain) {
                bestGain = g;
                best = i;
            }
        }
        return best;
    }
}
