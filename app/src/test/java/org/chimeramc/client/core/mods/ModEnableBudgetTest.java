package org.chimeramc.client.core.mods;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The native mod enable is bounded so a blocking mod cannot hang the launch.
 *
 * <p>{@code enableLoadedMods()} runs on the game activity's main thread and the native side runs
 * each mod's initialiser. A mod that blocks there hung the launch with no way out, because native
 * code cannot be interrupted once it is running. The call now runs on its own thread and the main
 * thread waits at most {@link ModManager#ENABLE_BUDGET_MS}.
 */
public class ModEnableBudgetTest {

    @Test
    public void theEnableIsBoundedAndRunsOffTheCallingThread() throws IOException {
        String source = read("app/src/main/java/org/chimeramc/client/launcher/core/mods/"
                + "ModManager.java");
        int start = source.indexOf("public static void enableLoadedMods()");
        assertTrue("enableLoadedMods must exist", start >= 0);
        int end = source.indexOf("public static boolean lastEnableTimedOut()", start);
        assertTrue("the budget guard must follow the enable", end > start);
        String body = source.substring(start, end);
        assertTrue("the native enable must run on a background thread",
                body.contains("AppExecutors.thread("));
        assertTrue("the caller must wait with a timeout",
                body.contains("done.await(ENABLE_BUDGET_MS"));
        assertTrue("a timeout must be recorded rather than thrown",
                body.contains("recordEnableTimeout()"));
        assertTrue("the budget must be a positive constant", ModManager.ENABLE_BUDGET_MS > 0L);
    }

    @Test
    public void theTimeoutIsObservableForDiagnostics() throws IOException {
        String source = read("app/src/main/java/org/chimeramc/client/launcher/core/mods/"
                + "ModManager.java");
        assertTrue("the timeout state must be readable",
                source.contains("public static boolean lastEnableTimedOut()"));
        assertTrue("the timeout state must be volatile",
                source.contains("private static volatile boolean enableTimedOut;"));
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
