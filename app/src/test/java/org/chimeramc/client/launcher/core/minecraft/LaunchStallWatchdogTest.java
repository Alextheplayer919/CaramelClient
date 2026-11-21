package org.chimeramc.client.launcher.core.minecraft;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Pins the launch-stall watchdog.
 *
 * <p>The defect it fixes: the loading screen's watchdog runs on the main thread, so when the game
 * activity blocked that thread the app froze with no recovery at all. The recovery has to run off
 * the main thread, and it has to be armed before the block can begin and settled by a real frame.
 */
public class LaunchStallWatchdogTest {

    private static final long T0 = 1_000_000L;

    @Test
    public void aLaunchInsideTheBudgetIsNotRecovered() {
        assertFalse(LaunchStallWatchdog.shouldRecover(T0, false, T0 + 1_000L,
                LaunchStallWatchdog.BUDGET_MS));
    }

    @Test
    public void aLaunchThatSettledIsNeverRecovered() {
        assertFalse(LaunchStallWatchdog.shouldRecover(T0, true,
                T0 + LaunchStallWatchdog.BUDGET_MS * 10, LaunchStallWatchdog.BUDGET_MS));
    }

    @Test
    public void aLaunchPastTheBudgetIsRecovered() {
        assertTrue(LaunchStallWatchdog.shouldRecover(T0, false,
                T0 + LaunchStallWatchdog.BUDGET_MS, LaunchStallWatchdog.BUDGET_MS));
    }

    @Test
    public void theBoundaryIsExact() {
        assertFalse(LaunchStallWatchdog.shouldRecover(T0, false,
                T0 + LaunchStallWatchdog.BUDGET_MS - 1, LaunchStallWatchdog.BUDGET_MS));
    }

    @Test
    public void aClockThatMovedBackwardsIsNotAStall() {
        assertFalse(LaunchStallWatchdog.shouldRecover(T0, false, T0 - 5_000L,
                LaunchStallWatchdog.BUDGET_MS));
    }

    @Test
    public void theWatchdogRunsOffTheMainThreadAndIsArmedBeforeTheBlock() throws IOException {
        String watchdog = read("app/src/main/java/org/chimeramc/client/launcher/core/minecraft/"
                + "LaunchStallWatchdog.java");
        assertTrue("the recovery must run on its own thread, not the main thread a hang blocks",
                watchdog.contains("AppExecutors.thread("));

        String activity = read("app/src/main/java/org/chimeramc/client/launcher/core/minecraft/"
                + "MinecraftActivity.kt");
        assertTrue("the watchdog must be armed in onCreate, before the launch can block",
                activity.contains("LaunchStallWatchdog.arm(this)"));
        assertTrue("the game's frame tick must settle the watchdog",
                activity.contains("LaunchStallWatchdog.settle()"));
        assertTrue("the watchdog must be released when the session ends",
                activity.contains("LaunchStallWatchdog.disarm()"));

        String loading = read("app/src/main/java/org/chimeramc/client/launcher/core/minecraft/"
                + "MinecraftLoadingActivity.kt");
        assertTrue("the off-thread watchdog must cover the startActivity handoff too: the loading "
                        + "screen's watchdog stands down at enteringGame and the game activity has "
                        + "not armed its own yet",
                loading.contains("LaunchStallWatchdog.arm(this)"));
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
