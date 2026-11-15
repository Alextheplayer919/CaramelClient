package org.chimeramc.client.ui.compose.caramel.launcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The Cosmetics tab's 3D skin slot must host the real renderer, not the empty white box.
 *
 * <p>The slot's default was {@code EmptySkinSlot} — a white rounded square with a label — because the
 * real preview ({@code CapePreviewView}) is a plain Android {@code View} and nothing bridged it into
 * the Compose launcher. When the Compose launcher replaced the XML launch tab, the preview lost its
 * host, so the Cosmetics tab showed a white box where the player's character belongs. This pins the
 * bridge: {@code CaramelSkinPreview} hosts {@code CapePreviewView} through {@code AndroidView} and
 * binds the equipped cosmetics, and the launcher screen defaults to it.
 */
public class CaramelSkinPreviewWiringTest {

    @Test
    public void theSlotHostsTheRealRenderer() throws IOException {
        String preview = read("app/src/main/java/org/chimeramc/client/ui/compose/caramel/"
                + "launcher/CaramelSkinPreview.kt");
        assertTrue("the skin slot must host the real renderer through AndroidView",
                preview.contains("AndroidView("));
        assertTrue("the host view must be the real software-rendered preview",
                preview.contains("CapePreviewView"));
        assertTrue("the slot must bind the equipped cape from the store",
                preview.contains("getEquippedCapeForDisplay()"));
        assertTrue("the slot must bind the equipped accessory from the store",
                preview.contains("getEquippedAccessory()"));
        assertTrue("the slot must bind the equipped pet from the store",
                preview.contains("getEquippedPet()"));
        assertTrue("the released preview must free its bitmaps",
                preview.contains("release()"));
    }

    @Test
    public void theLauncherDefaultsToTheRealPreviewNotTheEmptySlot() throws IOException {
        String screen = read("app/src/main/java/org/chimeramc/client/ui/compose/caramel/"
                + "launcher/CaramelLauncherScreen.kt");
        assertTrue("the launcher screen must default the Cosmetics slot to the real preview",
                screen.contains("CaramelSkinPreview(state"));
        assertFalse("the Cosmetics slot must not fall back to the empty white box",
                screen.contains("EmptySkinSlot"));
    }

    private static String read(String relative) throws IOException {
        return new String(Files.readAllBytes(projectRoot().resolve(relative)),
                StandardCharsets.UTF_8);
    }

    private static Path projectRoot() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.exists(dir.resolve("app/src/main/res/layout"))) return dir;
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find the project root");
    }
}
