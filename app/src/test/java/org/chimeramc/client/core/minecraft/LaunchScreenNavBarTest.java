package org.chimeramc.client.core.minecraft;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The launch screen keeps the system bars.
 *
 * <p>The launch screen used to hide the status and navigation bars (immersive sticky). When the
 * launch then hung on the main thread, the user had no Back, Home or Recents to escape with and had
 * to power-cycle the device. The bars are the only reliable way out of a screen whose main thread is
 * blocked, so the launch screen no longer hides them.
 */
public class LaunchScreenNavBarTest {

    @Test
    public void theLaunchScreenDoesNotHideTheSystemBars() throws IOException {
        String activity = read("app/src/main/java/org/chimeramc/client/launcher/core/minecraft/"
                + "MinecraftLoadingActivity.kt");
        assertTrue("the launch screen must opt in to the system bars so a hang cannot trap the user",
                activity.contains("shouldSkipNavBar(): Boolean = false"));
    }

    @Test
    public void baseActivityHonoursTheFlag() throws IOException {
        String base = read("app/src/main/java/org/chimeramc/client/launcher/ui/activities/"
                + "BaseActivity.java");
        assertTrue("BaseActivity must consult shouldSkipNavBar() before hiding the bars",
                base.contains("if (shouldSkipNavBar())"));
        assertTrue("BaseActivity must offer a way to show the bars again",
                base.contains("protected void showSystemUI()"));
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
