package org.chimeramc.client.core.cosmetics;

/**
 * The preview turntable's idle motion, as pure arithmetic.
 *
 * <p>The turntable used to be a single constant-rate yaw, which reads as a motor: the same turn every
 * second, no life in it. A cinematic turn adds three gentle, slow oscillations on top of the yaw —
 * a pitch sway, a small vertical float and a slow yaw speed wobble — all phase-locked to the same
 * clock, so the model breathes instead of spinning like a wheel. The amplitudes are small on purpose:
 * the point is a hand-held feel, not a ride.
 *
 * <p>Pure and Android-free, so the motion is unit-tested and the view only applies it.
 */
public final class PreviewMotion {

    /** Peak pitch sway, in degrees, added to the resting pitch. */
    public static final float PITCH_SWAY_DEG = 3.5f;
    /** Peak vertical float, in model pixels, added to the camera's orbit height. */
    public static final float FLOAT_PIXELS = 1.6f;
    /** Peak yaw-speed wobble, as a fraction of the base idle speed (0.2 = ±20%). */
    public static final float YAW_SPEED_WOBBLE = 0.25f;

    private PreviewMotion() {
    }

    /** The pitch, in degrees, for a turntable at {@code basePitchDeg} at time {@code tSeconds}. */
    public static float idlePitchDeg(float basePitchDeg, float tSeconds) {
        return basePitchDeg + (float) Math.sin(tSeconds * 0.55) * PITCH_SWAY_DEG;
    }

    /** The camera orbit height, in model pixels, for a base height at time {@code tSeconds}. */
    public static float idleFloatPixels(float baseOrbitY, float tSeconds) {
        return baseOrbitY + (float) Math.sin(tSeconds * 0.8 + 0.7) * FLOAT_PIXELS;
    }

    /**
     * The yaw speed multiplier at time {@code tSeconds}: a slow wobble around 1, so the turn eases in
     * and out rather than holding one constant rate.
     */
    public static float yawSpeedScale(float tSeconds) {
        return 1f + (float) Math.sin(tSeconds * 0.37) * YAW_SPEED_WOBBLE;
    }
}
