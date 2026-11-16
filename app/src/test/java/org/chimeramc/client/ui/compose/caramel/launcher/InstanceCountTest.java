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
 * The title bar's instance count is the number of instances, not a mid-game flag.
 *
 * <p>The defect this covers: the label read {@code runningInstances}, which the activity set from
 * {@code MinecraftActivityState.isRunning(...)} — a boolean about whether the game process is up.
 * That is false for every user who is not mid-game, which is everyone almost all of the time, so a
 * user with a full list of instances saw "0 instances". The count now comes from the version list.
 */
public class InstanceCountTest {

    @Test
    public void theCountComesFromTheInstanceList() throws IOException {
        String activity = read("app/src/main/java/org/chimeramc/client/launcher/ui/activities/"
                + "MainActivity.java");
        assertTrue("the count must be the size of the instance list",
                activity.contains("setInstancesCount(s.getVersions().size())"));
        assertFalse("the count must not be the running-game flag",
                activity.contains("setRunningInstances("));
    }

    @Test
    public void theTitleBarDrawsTheCount() throws IOException {
        String shell = read("app/src/main/java/org/chimeramc/client/ui/compose/caramel/launcher/"
                + "CaramelShell.kt");
        assertTrue("the title bar must draw the instance count",
                shell.contains("caramel_instances_running, state.instancesCount"));
        assertFalse("the title bar must not draw the removed running flag",
                shell.contains("state.runningInstances"));
    }

    private static String read(String relativePath) throws IOException {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            Path candidate = dir.resolve(relativePath);
            if (Files.exists(candidate)) {
                return new String(Files.readAllBytes(candidate), StandardCharsets.UTF_8);
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find " + relativePath);
    }
}
