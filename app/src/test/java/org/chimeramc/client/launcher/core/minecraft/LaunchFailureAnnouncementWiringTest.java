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
 * A launch failure must actually reach the user, from both ends of a launch.
 *
 * <p>Two failure paths existed but neither spoke to the user:
 *
 * <ul>
 *   <li>A failure <em>before</em> the game activity started only reset the Play button and logged.
 *       The raw system {@code Toast} in {@code MinecraftLauncher} bypassed the launcher's own toast,
 *       so a refusal read as the button doing nothing.</li>
 *   <li>A failure <em>inside</em> the game activity wrote a {@code LastLaunchFailure} report
 *       ({@code announced=false}) for the relaunched launcher to show, but nothing ever called
 *       {@link LastLaunchFailure#consumePending}, so the report sat on disk unread and the app simply
 *       reappeared with no explanation.</li>
 * </ul>
 *
 * <p>This pins both readers: the launcher's {@code onLaunchFailed} names the classified cause on its
 * own toast, and {@code MainActivity} consumes the pending report on start.
 */
public class LaunchFailureAnnouncementWiringTest {

    private static final String MAIN_ACTIVITY =
            "app/src/main/java/org/chimeramc/client/launcher/ui/activities/MainActivity.java";
    private static final String LAUNCHER =
            "app/src/main/java/org/chimeramc/client/launcher/core/minecraft/MinecraftLauncher.java";

    @Test
    public void theLauncherConsumesAPendingLaunchFailureOnStart() throws IOException {
        String activity = read(MAIN_ACTIVITY);
        assertTrue("the launcher must read the report a game-activity crash wrote",
                activity.contains("LastLaunchFailure.consumePending("));
        assertTrue("the consumed report must name its classified cause",
                activity.contains(".messageRes(cause)"));
        assertTrue("the report must be raised on the launcher's own toast",
                activity.contains("announcePendingLaunchFailure"));
    }

    @Test
    public void aFailedLaunchNamesItsCauseOnTheLauncherToast() throws IOException {
        String activity = read(MAIN_ACTIVITY);
        int at = activity.indexOf("public void onLaunchFailed(Throwable e)");
        assertTrue("the launch callback must be present", at >= 0);
        int end = activity.indexOf("\n            }", at);
        String body = activity.substring(at, end);
        assertTrue("a failed launch must name its cause on the launcher toast",
                (body.contains("toast(") || body.contains("toastWarning("))
                        && body.contains("LaunchErrorDiagnosis.classify(e)"));
    }

    @Test
    public void theLauncherDoesNotDrawASecondSystemToast() throws IOException {
        // Two bubbles for one failure is the bug the Caramel notice replaced; the cause is named on
        // the launcher's toast, so the raw system Toast must be gone.
        String launcher = read(LAUNCHER);
        assertFalse("the raw system Toast must not duplicate the launcher's own toast",
                launcher.contains("Toast.makeText"));
    }

    private static String read(String relative) throws IOException {
        return new String(Files.readAllBytes(projectRoot().resolve(relative)),
                StandardCharsets.UTF_8);
    }

    private static Path projectRoot() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            Path candidate = dir.resolve("app/src/main/java");
            if (Files.exists(candidate)) {
                return dir;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find the project root");
    }
}
