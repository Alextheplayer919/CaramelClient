package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The slim ("Alex") arm model.
 *
 * <p>A slim skin carries 3-px-wide arms; drawing it with the classic 4-px arms stretches the arm
 * texture across a wider box, so an Alex skin renders as a fat-armed stranger. The two models differ
 * only in the arms, and the slim UV block is 3 px wide throughout, which is the detail that is easy
 * to get wrong (a 4-px-wide top face on a 3-px arm reads the next region's pixels).
 */
public class SlimArmModelTest {

    @Test
    public void theSlimArmsAreThreePixelsWideAndTheClassicAreFour() {
        assertEquals(4f, box(SkinModel.boxes(false), "arm_r").w, 0.001f);
        assertEquals(4f, box(SkinModel.boxes(false), "arm_l").w, 0.001f);
        assertEquals(3f, box(SkinModel.boxes(true), "arm_r").w, 0.001f);
        assertEquals(3f, box(SkinModel.boxes(true), "arm_l").w, 0.001f);
    }

    @Test
    public void theSlimArmsStayFlushAgainstTheBody() {
        // The inner face of each arm must sit on the body's edge (|x| = 4) in both models, so only the
        // outer edge moves. A slim arm placed at the classic centre would leave a gap to the torso.
        for (boolean slim : new boolean[]{false, true}) {
            SkinModel.Box armR = box(SkinModel.boxes(slim), "arm_r");
            SkinModel.Box armL = box(SkinModel.boxes(slim), "arm_l");
            assertEquals("the right arm's inner edge must touch the body",
                    -4f, armR.cx + armR.w / 2f, 0.001f);
            assertEquals("the left arm's inner edge must touch the body",
                    4f, armL.cx - armL.w / 2f, 0.001f);
        }
    }

    @Test
    public void onlyTheArmsDifferBetweenTheTwoModels() {
        // The head, body and legs are shared, so a slim change must not move them.
        for (String id : new String[]{"head", "body", "leg_r", "leg_l"}) {
            SkinModel.Box classic = box(SkinModel.boxes(false), id);
            SkinModel.Box slim = box(SkinModel.boxes(true), id);
            assertEquals(id + " cx", classic.cx, slim.cx, 0.001f);
            assertEquals(id + " cy", classic.cy, slim.cy, 0.001f);
            assertEquals(id + " w", classic.w, slim.w, 0.001f);
            assertEquals(id + " h", classic.h, slim.h, 0.001f);
            assertEquals(id + " d", classic.d, slim.d, 0.001f);
        }
    }

    @Test
    public void theSlimArmUvBlocksAreThreePixelsWide() {
        for (SkinModel.Box box : SkinModel.boxes(true)) {
            if (!box.id.startsWith("arm")) continue;
            for (SkinModel.Face face : SkinModel.Face.values()) {
                SkinModel.Uv base = box.baseUv(face);
                assertNotNull(base);
                assertEquals(box.id + " " + face + " base width", 3, base.w);
                assertTrue(box.id + " " + face + " base UV out of atlas",
                        SkinModel.uvWithinAtlas(base));
                SkinModel.Uv over = box.overlayUv(face);
                if (over != null) {
                    assertEquals(box.id + " " + face + " overlay width", 3, over.w);
                    assertTrue(box.id + " " + face + " overlay UV out of atlas",
                            SkinModel.uvWithinAtlas(over));
                }
            }
        }
    }

    @Test
    public void theClassicAndSlimArmBlocksAreNotTheSame() {
        // A guard against the slim branch silently returning the classic table.
        SkinModel.Uv classicFront = box(SkinModel.boxes(false), "arm_r")
                .baseUv(SkinModel.Face.FRONT);
        SkinModel.Uv slimFront = box(SkinModel.boxes(true), "arm_r")
                .baseUv(SkinModel.Face.FRONT);
        assertNotEquals(classicFront.u, slimFront.u);
    }

    private static SkinModel.Box box(java.util.List<SkinModel.Box> boxes, String id) {
        for (SkinModel.Box box : boxes) {
            if (id.equals(box.id)) return box;
        }
        throw new AssertionError("no box " + id);
    }
}
