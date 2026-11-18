package org.chimeramc.client.ui.components;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The game activity's window must not flash white.
 *
 * <p>{@code MinecraftActivity} is themed {@code AppFullScreenTheme}, and that theme parented
 * {@code Theme.AppCompat.NoActionBar} — not a DayNight theme — with {@code @color/background} for
 * both {@code windowBackground} and {@code colorBackground}. {@code background} is the near-white
 * {@code #F7F5FF}, and {@code windowDisablePreview="false"} let the system paint it as the starting
 * window, so the user saw a white screen after the launch screen on every launch. The launcher's own
 * window had already been fixed the same way; this pins the game's.
 *
 * <p>{@code MinecraftActivity} also paints its window from {@code colorBackground} at runtime, so a
 * fix to only one of the two attributes would still leave the white frame behind.
 */
public class GameWindowThemeTest {

    /** A colour light enough to read as "white" behind a full-screen surface. */
    private static final double LUMINANCE_CEILING = 0.5;

    @Test
    public void theGameWindowIsTheCaramelNavyNotTheNearWhiteBackground() throws IOException {
        String theme = style(read(stylesFile()), "AppFullScreenTheme.Base");
        assertTrue("the game window background must be the Caramel navy, not @color/background, or "
                        + "the launch shows a white frame. Theme: " + theme,
                theme.contains("android:windowBackground\">@color/launcher_backdrop"));
        assertTrue("MinecraftActivity paints its window from colorBackground at runtime, so it must "
                        + "be the navy too, not @color/background. Theme: " + theme,
                theme.contains("android:colorBackground\">@color/launcher_backdrop"));
        assertFalse("the near-white background must not remain on either attribute. Theme: " + theme,
                theme.contains("@color/background"));
    }

    @Test
    public void theGameThemeExtendsTheBaseAndTheActivityUsesIt() throws IOException {
        String styles = read(stylesFile());
        String theme = style(styles, "AppFullScreenTheme");
        assertTrue("AppFullScreenTheme must extend AppFullScreenTheme.Base so the API-specific "
                        + "splash overrides can add the splash colour without restating the style",
                theme.contains("parent=\"AppFullScreenTheme.Base\""));

        String manifest = read(resolve("app/src/main/AndroidManifest.xml"));
        int activity = manifest.indexOf("android:name=\".core.minecraft.MinecraftActivity\"");
        assertTrue("the game activity is no longer declared", activity >= 0);
        String block = manifest.substring(activity, Math.min(manifest.length(), activity + 600));
        assertTrue("the game activity must be themed AppFullScreenTheme, which this test pins",
                block.contains("@style/AppFullScreenTheme"));
    }

    @Test
    public void theSystemStartingWindowIsDisabled() throws IOException {
        String theme = style(read(stylesFile()), "AppFullScreenTheme.Base");
        assertTrue("windowDisablePreview must be true, or the system paints its own (light) starting "
                        + "window over the game's dark one. Theme: " + theme,
                theme.contains("android:windowDisablePreview\">true"));
    }

    @Test
    public void theApi31SplashUsesTheNavyNotTheNearWhite() throws IOException {
        for (String variant : new String[]{"values-v31", "values-v35"}) {
            String theme = style(read(resolve("app/src/main/res/" + variant + "/theme.xml")),
                    "AppFullScreenTheme");
            assertTrue(variant + " must set the game window's splash background to the Caramel navy",
                    theme != null && theme.contains(
                            "android:windowSplashScreenBackground\">@color/launcher_backdrop"));
            assertFalse(variant + " must not flash the near-white background as the splash",
                    theme != null && theme.contains("@color/background"));
        }
    }

    @Test
    public void theNavyIsActuallyDark() throws IOException {
        String colors = read(resolve("app/src/main/res/values/colors.xml"));
        String navy = valueOf(colors, "launcher_backdrop");
        assertTrue("launcher_backdrop must be defined", navy != null);
        assertTrue("launcher_backdrop must be a dark colour, not a near-white: " + navy,
                relativeLuminance(navy) < LUMINANCE_CEILING);
    }

    @Test
    public void theGameActivityStaysSoftwareComposited() throws IOException {
        // hardwareAccelerated="false" is deliberate: the activity hosts the engine's EGL surface and
        // the overlays draw through the software Canvas. Flipping it on composites a second GL
        // context over the engine's. Pinned so a "modernise the manifest" change cannot regress it.
        String manifest = read(resolve("app/src/main/AndroidManifest.xml"));
        for (String activity : new String[]{".core.minecraft.MinecraftActivity",
                ".core.minecraft.MinecraftLoadingActivity"}) {
            int at = manifest.indexOf("android:name=\"" + activity + "\"");
            assertTrue(activity + " is no longer declared", at >= 0);
            String block = manifest.substring(at, Math.min(manifest.length(), at + 400));
            assertTrue(activity + " must keep hardwareAccelerated=\"false\"",
                    block.contains("android:hardwareAccelerated=\"false\""));
        }
    }

    private static Path stylesFile() throws IOException {
        return resolve("app/src/main/res/values/styles.xml");
    }

    /**
     * The body of one {@code <style>} block, so an assertion cannot read a neighbouring style.
     * A self-closing {@code <style ... />} returns just its tag.
     */
    private static String style(String xml, String name) {
        String open = "<style name=\"" + name + "\"";
        int start = xml.indexOf(open);
        if (start < 0) return null;
        int gt = xml.indexOf('>', start);
        if (gt < 0) return xml.substring(start);
        String tag = xml.substring(start, gt + 1);
        if (tag.endsWith("/>")) return tag;
        int end = xml.indexOf("</style>", gt);
        return xml.substring(start, end < 0 ? xml.length() : end);
    }

    private static String valueOf(String xml, String name) {
        String marker = "<color name=\"" + name + "\">";
        int at = xml.indexOf(marker);
        if (at < 0) return null;
        int end = xml.indexOf('<', at + marker.length());
        return xml.substring(at + marker.length(), end);
    }

    /** WCAG relative luminance of a {@code #RRGGBB} (or {@code #AARRGGBB}) colour, 0 dark to 1 light. */
    private static double relativeLuminance(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        if (h.length() == 8) h = h.substring(2);
        int r = Integer.parseInt(h.substring(0, 2), 16);
        int g = Integer.parseInt(h.substring(2, 4), 16);
        int b = Integer.parseInt(h.substring(4, 6), 16);
        return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b);
    }

    private static double channel(int c) {
        double s = c / 255.0;
        return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    }

    private static Path resolve(String relativePath) throws IOException {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            Path candidate = dir.resolve(relativePath);
            if (Files.exists(candidate)) return candidate;
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find " + relativePath);
    }

    private static String read(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }
}
