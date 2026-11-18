package org.chimeramc.client.launcher.core.mods;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The bundled BerryAPI runtime is actually started during a session.
 *
 * <p>{@code BerryApi.ensureAttached()} and {@code BerryApi.bindWorldSeam()} existed and were tested,
 * but nothing in production called them: a whole build could ship with the bundled runtime never
 * attached and every BerryAPI-backed feature silently dark, which is exactly the "it is integrated"
 * claim the code did not back. This pins the wiring: the session must pump the runtime from the
 * game's own frame tick (the same deferred point the native hooks install from, because attaching
 * in {@code onCreate} sees a partially mapped module) and bind the world seam once attached.
 */
public class BerryRuntimeWiringTest {

    @Test
    public void theSessionPumpsTheBerryRuntime() throws IOException {
        String modManager = read("app/src/main/java/org/chimeramc/client/launcher/core/mods/"
                + "ModManager.java");
        assertTrue("ModManager must own a one-shot BerryAPI attach pump",
                modManager.contains("pumpBerryRuntime"));
        assertTrue("the pump must attach BerryAPI",
                modManager.contains("BerryApi.ensureAttached()"));
        assertTrue("the pump must bind the world seam once attached",
                modManager.contains("BerryApi.bindWorldSeam()"));

        String activity = read("app/src/main/java/org/chimeramc/client/launcher/core/minecraft/"
                + "MinecraftActivity.kt");
        assertTrue("the game frame tick must pump the BerryAPI runtime",
                activity.contains("ModManager.pumpBerryRuntime()"));
    }

    /**
     * The attach must not run from {@code onCreate}.
     *
     * <p>Attaching there runs {@code BERRY_Inject} on the main thread while
     * {@code libminecraftpe.so} is only partly mapped. The attach is once-only, so an early
     * attempt both froze the game window before its first frame and could not be retried,
     * defeating the deferred pump. This pins the single call site: the frame tick.
     */
    @Test
    public void theAttachDoesNotRunFromOnCreate() throws IOException {
        String activity = read("app/src/main/java/org/chimeramc/client/launcher/core/minecraft/"
                + "MinecraftActivity.kt");
        int onCreate = activity.indexOf("override fun onCreate(");
        assertTrue("MinecraftActivity must still have an onCreate", onCreate >= 0);
        int onCreateEnd = activity.indexOf("\n    private fun applyHighRefreshRateMode", onCreate);
        assertTrue("could not delimit onCreate", onCreateEnd > onCreate);
        String body = activity.substring(onCreate, onCreateEnd);
        assertTrue("onCreate must not attach BerryAPI: attaching before the engine is up freezes "
                        + "the main thread and cannot be retried",
                !body.contains("BerryApi.ensureAttached()"));
        assertTrue("onCreate must not bind the world seam",
                !body.contains("BerryApi.bindWorldSeam()"));
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
