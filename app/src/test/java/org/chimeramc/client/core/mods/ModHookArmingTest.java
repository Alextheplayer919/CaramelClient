package org.chimeramc.client.core.mods;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The native hook install arms even with no mods enabled.
 *
 * <p>{@code enableLoadedMods()} used to return early when no mod was enabled, but the native side of
 * that call also arms the deferred game-hook install, which is what captures the player's own skin
 * and drives the native cosmetics. A user with zero enabled mods therefore had both dark, with no mod
 * to blame. The call now always reaches the native arm.
 */
public class ModHookArmingTest {

    @Test
    public void enableLoadedModsDoesNotSkipOnAnEmptyModSet() throws IOException {
        String source = read("app/src/main/java/org/chimeramc/client/launcher/core/mods/"
                + "ModManager.java");
        int start = source.indexOf("public static void enableLoadedMods()");
        assertTrue("enableLoadedMods must exist", start >= 0);
        int end = source.indexOf("public static boolean pumpGameHookInstall()", start);
        String body = source.substring(start, end);
        assertFalse("the hook install must arm even with no enabled mods",
                body.contains("hasEnabledMods"));
        assertFalse("no early return that skips the native arm",
                body.contains("skipping sweetloader init"));
        assertTrue("the native enable (which arms the hook install) must be called",
                body.contains("nativeEnableLoadedMods()"));
    }

    @Test
    public void theNativeEnableArmsTheDeferredInstall() throws IOException {
        // If the native side stopped arming, removing the Java guard would have bought nothing.
        String cpp = read("app/src/main/cpp/sweetloader/src/pl/SweetLoader.cpp");
        int start = cpp.indexOf("Java_org_chimeramc_client_core_mods_ModManager_nativeEnableLoadedMods");
        assertTrue("the native enable entry point must exist", start >= 0);
        int end = cpp.indexOf("nativePumpGameHookInstall", start);
        String body = cpp.substring(start, end);
        assertTrue("the native enable must arm the deferred hook install",
                body.contains("ArmGameHookInstall"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(root().resolve(relativePath)), StandardCharsets.UTF_8);
    }

    private static Path root() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.exists(dir.resolve("app/src/main/res/layout"))) return dir;
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find the project root");
    }
}
