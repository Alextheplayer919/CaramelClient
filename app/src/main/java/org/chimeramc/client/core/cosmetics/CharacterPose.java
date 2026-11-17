package org.chimeramc.client.core.cosmetics;

/**
 * The character's pose: the rotation of each limb about its own joint.
 *
 * <p>The preview drew the player in the model's rest pose — arms straight down, legs together — which
 * is the T-pose's quiet cousin and reads as "vibe coded": nothing about it says a person is standing
 * there. A hero pose is what makes the figure look alive: the arms hang a little out and back, one
 * leg takes the weight, and the whole figure turns its shoulders slightly.
 *
 * <p>The pose is per-limb pitch (forward/back), yaw (out to the side) and roll (twist), in degrees,
 * plus a body lean. It is pure data with a {@link #relaxedHero()} preset and a
 * {@link #apply(String, float[], float[])} that rotates a model-space point about the limb's joint —
 * so the maths is unit-tested without a device and a new pose is a new constant, not new geometry.
 *
 * <p>Rotation is about the joint at the top of the limb, so the shoulder or hip stays put and only
 * the limb swings, which is how a real arm moves.
 */
public final class CharacterPose {

    /** A limb's joint rotation, in degrees. */
    public static final class Limb {
        /** Forward/back swing (about X). Positive swings the far end forward. */
        public final float pitch;
        /**
         * Out to the side (about Z). The sign is the raw rotation, so it is negative for the right
         * limb and positive for the left to swing both away from the torso: a point below the joint
         * moves toward +x under a positive yaw.
         */
        public final float yaw;
        /** Twist about the limb's own length (about Y). */
        public final float roll;

        public Limb(float pitch, float yaw, float roll) {
            this.pitch = pitch;
            this.yaw = yaw;
            this.roll = roll;
        }

        /** No rotation: the model's rest pose. */
        public static final Limb REST = new Limb(0f, 0f, 0f);
    }

    // --- The hero pose ---------------------------------------------------------------------------
    //
    // Angles chosen to read as a relaxed, confident stance rather than a mannequin: the arms are
    // clear of the torso (yaw) so they do not z-fight with it, the right arm hangs a touch further
    // back than the left (asymmetry is what makes a stance look natural), one leg carries the weight
    // and the other is relaxed forward.

    /** The right arm, hanging a little out and back. Negative yaw swings it away from the torso. */
    public static final Limb ARM_R = new Limb(-6f, -10f, 0f);
    /** The left arm, hanging a little out and slightly forward, so the two are not a mirror. */
    public static final Limb ARM_L = new Limb(4f, 9f, 0f);
    /** The right leg: the weight-bearing one, straight. */
    public static final Limb LEG_R = new Limb(0f, 1.5f, 0f);
    /** The left leg: relaxed, eased forward and out. */
    public static final Limb LEG_L = new Limb(5f, -2.5f, 0f);

    /** The body's own lean: a slight forward lean reads as alert rather than slumped. */
    public static final float BODY_LEAN_PITCH = 1.5f;
    /** The shoulders turned a touch, so the figure is not a flat plane facing the camera. */
    public static final float BODY_TWIST_YAW = 6f;

    /**
     * The hip pivot the whole-body lean and shoulder twist turn about, in model pixels.
     *
     * <p>The torso leans from the hips, not the feet: turning the figure about its base would slide
     * the feet sideways, which reads as a stumble. The hip is where a real spine meets the pelvis, so
     * the legs stay planted and only the torso and head carry the lean.
     */
    public static final float BODY_PIVOT_Y = 12f;

    private final Limb armR;
    private final Limb armL;
    private final Limb legR;
    private final Limb legL;
    private final float bodyLeanPitch;
    private final float bodyTwistYaw;

    private CharacterPose(Limb armR, Limb armL, Limb legR, Limb legL, float bodyLeanPitch,
                          float bodyTwistYaw) {
        this.armR = armR;
        this.armL = armL;
        this.legR = legR;
        this.legL = legL;
        this.bodyLeanPitch = bodyLeanPitch;
        this.bodyTwistYaw = bodyTwistYaw;
    }

    /** The relaxed hero stance, the default the preview uses. */
    public static CharacterPose relaxedHero() {
        return new CharacterPose(ARM_R, ARM_L, LEG_R, LEG_L, BODY_LEAN_PITCH, BODY_TWIST_YAW);
    }

    /** The model's rest pose: every limb at zero. Used by tests and the radial hub. */
    public static CharacterPose rest() {
        return new CharacterPose(Limb.REST, Limb.REST, Limb.REST, Limb.REST, 0f, 0f);
    }

    /** The rotation for a box id, or {@link Limb#REST} for a box that is not a limb. */
    public Limb limbFor(String boxId) {
        if (boxId == null) return Limb.REST;
        switch (boxId) {
            case "arm_r": return armR;
            case "arm_l": return armL;
            case "leg_r": return legR;
            case "leg_l": return legL;
            default: return Limb.REST;
        }
    }

    public float bodyLeanPitch() {
        return bodyLeanPitch;
    }

    public float bodyTwistYaw() {
        return bodyTwistYaw;
    }

    /** Whether a box rides the torso, so the whole-body lean and twist move it with the chest. */
    public static boolean isTorso(String boxId) {
        return "body".equals(boxId) || "head".equals(boxId)
                || "arm_r".equals(boxId) || "arm_l".equals(boxId);
    }

    /**
     * Rotates a model-space point by the whole-body lean (pitch) then shoulder twist (yaw) about the
     * hip. Applied to the torso and everything hanging from it — the chest, head and arms — but not
     * the legs, so the feet stay planted while the upper body carries the stance.
     */
    public void applyBody(float[] point) {
        if (bodyLeanPitch == 0f && bodyTwistYaw == 0f) return;
        point[1] -= BODY_PIVOT_Y;
        rotateBody(point);
        point[1] += BODY_PIVOT_Y;
    }

    /** Rotates a direction vector by the whole-body lean then twist, with no pivot translation. */
    public void applyBodyVector(float[] v) {
        if (bodyLeanPitch == 0f && bodyTwistYaw == 0f) return;
        rotateBody(v);
    }

    /** The pitch-then-yaw rotation about the origin, shared by the point and vector forms. */
    private void rotateBody(float[] v) {
        float x = v[0];
        float y = v[1];
        float z = v[2];

        double pitch = Math.toRadians(bodyLeanPitch);
        double cosP = Math.cos(pitch), sinP = Math.sin(pitch);
        float y1 = (float) (y * cosP - z * sinP);
        float z1 = (float) (y * sinP + z * cosP);

        double yaw = Math.toRadians(bodyTwistYaw);
        double cosY = Math.cos(yaw), sinY = Math.sin(yaw);
        v[0] = (float) (x * cosY + z1 * sinY);
        v[1] = y1;
        v[2] = (float) (-x * sinY + z1 * cosY);
    }

    /**
     * Rotates a model-space point about a limb's joint.
     *
     * <p>The joint is the top of the limb (the shoulder or the hip), so the limb swings from there
     * rather than about its centre. The point is moved into joint-local space, rotated pitch then yaw
     * then roll, and moved back.
     *
     * @param boxId the limb's box id
     * @param jointY the y of the limb's joint (top), in model pixels
     * @param point the point to rotate in place, {x, y, z}
     */
    public void apply(String boxId, float jointY, float[] point) {
        Limb limb = limbFor(boxId);
        if (limb == Limb.REST) return;
        float lx = point[0];
        float ly = point[1] - jointY;
        float lz = point[2];

        // Pitch about X (forward/back).
        double p = Math.toRadians(limb.pitch);
        double cp = Math.cos(p), sp = Math.sin(p);
        float y1 = (float) (ly * cp - lz * sp);
        float z1 = (float) (ly * sp + lz * cp);

        // Yaw about Z (out to the side).
        double y = Math.toRadians(limb.yaw);
        double cy = Math.cos(y), sy = Math.sin(y);
        float x2 = (float) (lx * cy - y1 * sy);
        float y2 = (float) (lx * sy + y1 * cy);

        // Roll about Y (twist about the limb's length).
        double r = Math.toRadians(limb.roll);
        double cr = Math.cos(r), sr = Math.sin(r);
        float x3 = (float) (x2 * cr + z1 * sr);
        float z3 = (float) (-x2 * sr + z1 * cr);

        point[0] = x3;
        point[1] = y2 + jointY;
        point[2] = z3;
    }

    /** Rotates a direction (a face normal) by the same limb rotation, without the joint offset. */
    public void applyVector(String boxId, float[] vector) {
        Limb limb = limbFor(boxId);
        if (limb == Limb.REST) return;
        float[] origin = {0f, 0f, 0f};
        // Same rotation, applied to a vector from the joint: reuse apply with a zero joint.
        float saveX = vector[0], saveY = vector[1], saveZ = vector[2];
        float[] p = {saveX, saveY, saveZ};
        apply(boxId, 0f, p);
        vector[0] = p[0]; vector[1] = p[1]; vector[2] = p[2];
    }
}
