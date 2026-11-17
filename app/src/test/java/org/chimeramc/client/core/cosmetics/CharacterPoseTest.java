package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The character pose, pinned without a device.
 *
 * <p>The pose exists to stop the preview looking like a rest-pose mannequin. What matters: the rest
 * pose moves nothing, a limb rotates about its joint (the shoulder or hip stays put), the hero pose
 * actually differs from rest on every limb, and the arms are held clear of the torso so they cannot
 * z-fight with it.
 */
public class CharacterPoseTest {

    @Test
    public void theRestPoseMovesNothing() {
        CharacterPose rest = CharacterPose.rest();
        float[] p = {5f, 10f, -3f};
        rest.apply("arm_r", 24f, p);
        assertEquals(5f, p[0], 1e-4f);
        assertEquals(10f, p[1], 1e-4f);
        assertEquals(-3f, p[2], 1e-4f);
    }

    @Test
    public void aLimbRotatesAboutItsJoint() {
        // A point at the joint itself must not move, whatever the rotation: that is what "rotate
        // about the joint" means, and it is what keeps the shoulder attached to the body.
        CharacterPose pose = CharacterPose.relaxedHero();
        float jointY = 24f;
        float[] p = {0f, jointY, 0f};
        pose.apply("arm_r", jointY, p);
        assertEquals(0f, p[0], 1e-3f);
        assertEquals(jointY, p[1], 1e-3f);
        assertEquals(0f, p[2], 1e-3f);
    }

    @Test
    public void theHeroPoseDiffersFromRestOnEveryLimb() {
        CharacterPose hero = CharacterPose.relaxedHero();
        assertNotEquals("the right arm must not be in the rest pose",
                CharacterPose.Limb.REST.pitch, hero.limbFor("arm_r").pitch, 1e-4f);
        assertNotEquals("the left arm must not be in the rest pose",
                CharacterPose.Limb.REST.pitch, hero.limbFor("arm_l").pitch, 1e-4f);
        assertTrue("at least one leg must carry a different angle than the other, so the stance is "
                        + "not a mirror",
                hero.limbFor("leg_r").pitch != hero.limbFor("leg_l").pitch);
        assertTrue("the body must lean", hero.bodyLeanPitch() != 0f);
        assertTrue("the shoulders must be turned", hero.bodyTwistYaw() != 0f);
    }

    @Test
    public void theArmsAreHeldClearOfTheTorso() {
        // The body is 4 deep (z in [-2,2]) and the arm is 4 wide, so a zero yaw leaves the arm
        // touching the torso. Both arms must swing outward, in opposite directions.
        CharacterPose hero = CharacterPose.relaxedHero();
        // A point below the joint moves toward +x under a positive yaw, so the right arm (on the
        // -x side) swings out with a negative yaw and the left arm with a positive one.
        assertTrue("the right arm must swing out (negative yaw)", hero.limbFor("arm_r").yaw < 0f);
        assertTrue("the left arm must swing out (positive yaw)", hero.limbFor("arm_l").yaw > 0f);
        // And they must not be a mirror: a symmetric stance reads as a mannequin.
        assertNotEquals("the arms must not be a perfect mirror",
                hero.limbFor("arm_r").pitch, -hero.limbFor("arm_l").pitch, 1e-4f);
    }

    @Test
    public void aNonLimbBoxIsLeftAlone() {
        CharacterPose hero = CharacterPose.relaxedHero();
        assertSame("the head is not a limb and must not rotate here",
                CharacterPose.Limb.REST, hero.limbFor("head"));
        float[] p = {1f, 2f, 3f};
        hero.apply("head", 24f, p);
        assertEquals(1f, p[0], 1e-4f);
        assertEquals(2f, p[1], 1e-4f);
        assertEquals(3f, p[2], 1e-4f);
        hero.apply(null, 24f, p);
        assertEquals(1f, p[0], 1e-4f);
    }

    @Test
    public void aYawSwingsTheFarEndOfTheArmOutward() {
        // A point below the shoulder on the right arm (x negative side) must move further out
        // (more negative x) under a positive yaw, which is "away from the torso".
        CharacterPose hero = CharacterPose.relaxedHero();
        float jointY = 24f;
        float[] p = {-6f, 18f, 0f}; // mid-arm on the right side
        hero.apply("arm_r", jointY, p);
        assertTrue("the right arm's yaw must swing it outward (more negative x). got x="
                + p[0], p[0] < -6f);
    }

    @Test
    public void theBodyLeanAndTwistActuallyMoveTheTorso() {
        // The lean and twist were defined but never applied, so the figure was a flat plane facing
        // the camera. A point high on the torso must move under the hero pose's lean/twist, and the
        // legs must not (they stay planted).
        CharacterPose hero = CharacterPose.relaxedHero();
        float[] torso = {0f, 26f, 0f};
        float[] moved = {torso[0], torso[1], torso[2]};
        hero.applyBody(moved);
        assertTrue("the torso must move under the body lean/twist",
                Math.abs(moved[0] - torso[0]) > 1e-3f || Math.abs(moved[2] - torso[2]) > 1e-3f);

        // The hip pivot itself is the fixed point of the lean.
        float[] hip = {0f, CharacterPose.BODY_PIVOT_Y, 0f};
        hero.applyBody(hip);
        assertEquals(0f, hip[0], 1e-3f);
        assertEquals(CharacterPose.BODY_PIVOT_Y, hip[1], 1e-3f);
        assertEquals(0f, hip[2], 1e-3f);

        // Legs are not torso, so the body transform must leave them alone.
        assertTrue("a leg must not ride the body lean", !CharacterPose.isTorso("leg_r"));
        assertTrue("the chest must ride the body lean", CharacterPose.isTorso("body"));
    }

    @Test
    public void theRestPoseLeansNothing() {
        CharacterPose rest = CharacterPose.rest();
        float[] p = {3f, 30f, -2f};
        rest.applyBody(p);
        assertEquals(3f, p[0], 1e-4f);
        assertEquals(30f, p[1], 1e-4f);
        assertEquals(-2f, p[2], 1e-4f);
    }
}
