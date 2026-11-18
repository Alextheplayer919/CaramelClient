package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * The skin resolve must not run on the UI thread.
 *
 * <p>{@code CapePreviewView} resolved in its constructor, and the resolve reads the account cache,
 * the applied pack and the imported file and decodes a PNG. All of that is disk work, and it ran
 * while the view was being built on the main thread, so a cold cache (or a slow read) showed as a
 * stall on the frame that opened the preview. The fix is that the view paints the last in-memory
 * resolve and re-resolves through {@link PlayerSkinProvider#resolveAsync}.
 *
 * <p>These are source-level guards: the alternative (a real resolve) needs a Context and a Bitmap,
 * which the JVM unit test does not have.
 */
public class SkinResolveOffMainThreadTest {

    @Test
    public void thePreviewNeverResolvesOnTheUiThread() throws IOException {
        String preview = read("app/src/main/java/org/chimeramc/client/launcher/core/mods/"
                + "inbuilt/overlay/CapePreviewView.java");
        assertFalse("the preview must not call the blocking resolve on the UI thread",
                preview.contains("PlayerSkinProvider.resolve("));
        assertTrue("the preview must resolve off the UI thread",
                preview.contains("PlayerSkinProvider.resolveAsync("));
        assertTrue("the preview must paint the cached resolve immediately",
                preview.contains("PlayerSkinProvider.lastResolved()"));
    }

    @Test
    public void theResolveAsyncRunsOnTheDiskPoolAndDeliversOnTheMainThread() throws IOException {
        String provider = read("app/src/main/java/org/chimeramc/client/core/cosmetics/"
                + "PlayerSkinProvider.java");
        assertTrue("the disk work must run on the disk pool",
                provider.contains("AppExecutors.Kind.DISK"));
        assertTrue("the result must be delivered on the main thread",
                provider.contains("MainThread.post("));
        assertTrue("the resolve must be remembered so a later view paints it without blocking",
                provider.contains("lastResolved"));
    }

    private static String read(String relative) throws IOException {
        return new String(Files.readAllBytes(projectRoot().resolve(relative)),
                StandardCharsets.UTF_8);
    }

    private static java.nio.file.Path projectRoot() {
        java.nio.file.Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.exists(dir.resolve("app/src/main/res/layout"))) return dir;
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find the project root");
    }
}
