package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the preview turntable's cinematic idle motion.
 *
 * <p>The failure this guards is the "the spin looks like a motor" report: a constant-rate yaw. These
 * tests assert the motion actually varies with time, stays within the advertised amplitude, and is
 * centred on the resting value so it never drifts the view.
 */
public class PreviewMotionTest {

    @Test
    public void thePitchSwaysWithinItsAmplitudeAndReturns() {
        float base = 18f;
        float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
        for (int i = 0; i < 2000; i++) {
            float t = i / 20f;
            float pitch = PreviewMotion.idlePitchDeg(base, t);
            min = Math.min(min, pitch);
            max = Math.max(max, pitch);
        }
        assertTrue("sway exceeds its amplitude", max - base <= PreviewMotion.PITCH_SWAY_DEG + 1e-3f);
        assertTrue("sway exceeds its amplitude", base - min <= PreviewMotion.PITCH_SWAY_DEG + 1e-3f);
        assertTrue("pitch never sways", max - min > PreviewMotion.PITCH_SWAY_DEG);
    }

    @Test
    public void thePitchStartsAtTheRestingValue() {
        assertEquals(18f, PreviewMotion.idlePitchDeg(18f, 0f), 1e-4f);
    }

    @Test
    public void theYawSpeedWobblesAroundOne() {
        float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
        for (int i = 0; i < 2000; i++) {
            float scale = PreviewMotion.yawSpeedScale(i / 20f);
            min = Math.min(min, scale);
            max = Math.max(max, scale);
        }
        assertTrue("wobble exceeds its band",
                max <= 1f + PreviewMotion.YAW_SPEED_WOBBLE + 1e-3f);
        assertTrue("wobble exceeds its band",
                min >= 1f - PreviewMotion.YAW_SPEED_WOBBLE - 1e-3f);
        assertNotEquals("the speed never varies", min, max);
    }

    @Test
    public void theFloatIsBoundedAndCentred() {
        float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
        for (int i = 0; i < 2000; i++) {
            float y = PreviewMotion.idleFloatPixels(18f, i / 20f);
            min = Math.min(min, y);
            max = Math.max(max, y);
        }
        assertTrue(max - 18f <= PreviewMotion.FLOAT_PIXELS + 1e-3f);
        assertTrue(18f - min <= PreviewMotion.FLOAT_PIXELS + 1e-3f);
        assertTrue("the camera never floats", max - min > PreviewMotion.FLOAT_PIXELS);
    }
}
