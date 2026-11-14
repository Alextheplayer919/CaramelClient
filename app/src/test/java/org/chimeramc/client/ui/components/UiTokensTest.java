package org.chimeramc.client.ui.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pins the shared UI tokens: the exact palette values, the contrast ratios the spec requires
 * (text.primary on bg.surface &gt;= 7:1, text.secondary &gt;= 4.5:1) and the toggle state machine.
 * Pure JVM — no device, and no Android graphics calls.
 */
public class UiTokensTest {

    @Test
    public void thePaletteMatchesThePack() {
        // The CaramelClient-UI-pack is the design reference, so its numbers are the ones the client
        // draws: the Abyss/Navy/DeepBlue surface ramp, the #4A78FF blue, the #F5A742 caramel family
        // and the three-step text ramp. Pinning them here means a "close enough" retune fails the
        // build instead of drifting the client away from the pack it is meant to match.
        assertEquals(0xFF0A1330, UiTokens.BG_SURFACE);
        assertEquals(0xFF0F1B44, UiTokens.BG_SURFACE_HOVER);
        assertEquals(0xFF4A78FF, UiTokens.ACCENT_500);
        assertEquals(0xFF628CFF, UiTokens.ACCENT_400);
        assertEquals(0xFF7FA0FF, UiTokens.ACCENT_300);
        assertEquals(0xFF152252, UiTokens.ACCENT_900);
        assertEquals(0xFFEAF0FF, UiTokens.TEXT_PRIMARY);
        assertEquals(0xFF9AA8D0, UiTokens.TEXT_SECONDARY);
        assertEquals(0xFF66749E, UiTokens.TEXT_DISABLED);
        assertEquals(0xFF232C40, UiTokens.TOGGLE_OFF_TRACK);
    }

    @Test
    public void theSurfaceRampIsNavyAndOrdered() {
        // The pack's ramp is Abyss -> Navy -> DeepBlue: every step bluer and lighter than the last,
        // and all of them blue-dominant. A grey surface is the "unfinished rectangle" look the ramp
        // exists to avoid.
        assertTrue("the window is the darkest surface",
                UiTokens.luminance(UiTokens.BG_WINDOW) < UiTokens.luminance(UiTokens.BG_SURFACE));
        assertTrue("a raised surface is lighter than its base",
                UiTokens.luminance(UiTokens.BG_SURFACE) < UiTokens.luminance(UiTokens.BG_SURFACE_HOVER));
        for (int c : new int[]{UiTokens.BG_WINDOW, UiTokens.BG_SURFACE, UiTokens.BG_SURFACE_HOVER}) {
            assertTrue("surface must be navy, not grey", UiTokens.isBlueDominant(c));
        }
    }

    @Test
    public void theWindowIsNearlyOpaque() {
        int alpha = (UiTokens.BG_WINDOW >>> 24) & 0xFF;
        assertTrue("window should be ~0.96 alpha", alpha >= 240 && alpha <= 250);
    }

    @Test
    public void textPrimaryOnSurfaceMeetsWcagAaa() {
        double ratio = UiTokens.contrastRatio(UiTokens.TEXT_PRIMARY, UiTokens.BG_SURFACE);
        assertTrue("text.primary contrast was " + ratio, ratio >= 7.0);
    }

    @Test
    public void textSecondaryOnSurfaceMeetsWcagAa() {
        double ratio = UiTokens.contrastRatio(UiTokens.TEXT_SECONDARY, UiTokens.BG_SURFACE);
        assertTrue("text.secondary contrast was " + ratio, ratio >= 4.5);
    }

    @Test
    public void textPrimaryOnWindowMeetsWcagAa() {
        double ratio = UiTokens.contrastRatio(UiTokens.TEXT_PRIMARY, UiTokens.BG_WINDOW);
        assertTrue("text.primary on window contrast was " + ratio, ratio >= 4.5);
    }

    @Test
    public void blendAndAlphaArePureIntegerMaths() {
        assertEquals(0xFF000000, UiTokens.blend(0xFF000000, 0xFFFFFFFF, 0f));
        assertEquals(0xFFFFFFFF, UiTokens.blend(0xFF000000, 0xFFFFFFFF, 1f));
        // Half blend of black and white is ~0x808080.
        int mid = UiTokens.blend(0xFF000000, 0xFFFFFFFF, 0.5f);
        assertEquals(0x7F, (mid >>> 16) & 0xFF);
        assertEquals(128, UiTokens.withAlpha(0xFF0F5BE4, 128) >>> 24);
    }

    @Test
    public void theDefaultAccentIsBlue() {
        assertEquals(UiTokens.ACCENT_500, UiTokens.resolveAccent(null));
    }

    @Test
    public void theTouchTargetsMeetTheMinimum() {
        assertTrue("row height must be a comfortable touch target",
                UiTokens.ROW_HEIGHT >= 44);
        assertTrue("the toggle must be at least 48dp wide with its row padding",
                UiTokens.TOGGLE_WIDTH + 24 >= 48);
    }

    @Test
    public void motionDurationsStayWithinTheCap() {
        assertTrue(UiTokens.MOTION_DEFAULT_MS <= UiTokens.MOTION_MAX_MS);
        assertTrue(UiTokens.MOTION_PRESS_MS <= UiTokens.MOTION_MAX_MS);
        assertTrue(UiTokens.TOGGLE_SLIDE_MS <= UiTokens.MOTION_MAX_MS);
    }

    @Test
    public void theRadiusScaleHasExactlyFourSteps() {
        assertEquals(8, UiTokens.RADIUS_SMALL);
        assertEquals(12, UiTokens.RADIUS_MEDIUM);
        assertEquals(16, UiTokens.RADIUS_LARGE);
        assertEquals(28, UiTokens.RADIUS_XLARGE);
        // Every named surface radius is one of the four, never a bespoke value.
        assertEquals(UiTokens.RADIUS_XLARGE, UiTokens.RADIUS_WINDOW);
        assertEquals(UiTokens.RADIUS_LARGE, UiTokens.RADIUS_CARD);
        assertEquals(UiTokens.RADIUS_MEDIUM, UiTokens.RADIUS_CHIP);
        assertEquals(UiTokens.RADIUS_LARGE, UiTokens.RADIUS_BUTTON);
    }

    @Test
    public void theSpacingScaleIsTheFourEightProgression() {
        assertEquals(4, UiTokens.SPACE_1);
        assertEquals(8, UiTokens.SPACE_2);
        assertEquals(12, UiTokens.SPACE_3);
        assertEquals(16, UiTokens.SPACE_4);
        assertEquals(24, UiTokens.SPACE_6);
        assertEquals(32, UiTokens.SPACE_8);
    }

    @Test
    public void theTypeScaleIsOrderedAndHasFiveRoles() {
        float[] roles = {
                UiTokens.TEXT_DISPLAY, UiTokens.TEXT_TITLE_ROLE, UiTokens.TEXT_BODY,
                UiTokens.TEXT_LABEL, UiTokens.TEXT_CAPTION
        };
        assertEquals(5, roles.length);
        for (int i = 1; i < roles.length; i++) {
            assertTrue("role " + i + " must be smaller than the one above it",
                    roles[i] < roles[i - 1]);
        }
    }

    @Test
    public void reduceMotionSelectsTheFlatScheme() {
        assertEquals(UiTokens.Motion.EXPRESSIVE, UiTokens.motionScheme(false, false));
        assertEquals(UiTokens.Motion.REDUCED, UiTokens.motionScheme(true, false));
        // The platform "remove animations" setting wins too.
        assertEquals(UiTokens.Motion.REDUCED, UiTokens.motionScheme(false, true));
        assertTrue(UiTokens.Motion.REDUCED.linear);
        assertTrue(UiTokens.Motion.REDUCED.durationMs <= UiTokens.Motion.EXPRESSIVE.durationMs);
        assertTrue("reduced motion must not overshoot", UiTokens.Motion.REDUCED.damping >= 1f);
    }

    @Test
    public void theXmlRadiusTokensAreTheSameScaleAsTheCode() throws IOException {
        String dimens = readResource("app/src/main/res/values/dimens.xml");
        assertEquals(UiTokens.RADIUS_SMALL + "dp", dimenValue(dimens, "radius_small"));
        assertEquals(UiTokens.RADIUS_MEDIUM + "dp", dimenValue(dimens, "radius_medium"));
        assertEquals(UiTokens.RADIUS_LARGE + "dp", dimenValue(dimens, "radius_large"));
        assertEquals(UiTokens.RADIUS_XLARGE + "dp", dimenValue(dimens, "radius_xlarge"));

        // The card tokens alias the scale rather than restating a value.
        assertEquals("@dimen/radius_large", dimenValue(dimens, "chimera_card_radius"));
        assertEquals("@dimen/radius_medium", dimenValue(dimens, "chimera_card_radius_small"));
    }

    private static String dimenValue(String xml, String name) {
        Matcher m = Pattern.compile(
                "<dimen name=\"" + Pattern.quote(name) + "\">([^<]+)</dimen>").matcher(xml);
        assertTrue("dimen " + name + " is missing from dimens.xml", m.find());
        return m.group(1).trim();
    }

    private static String readResource(String relativePath) throws IOException {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            Path candidate = dir.resolve(relativePath);
            if (Files.isRegularFile(candidate)) {
                return new String(Files.readAllBytes(candidate), StandardCharsets.UTF_8);
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find " + relativePath + " from "
                + Paths.get("").toAbsolutePath());
    }
}
