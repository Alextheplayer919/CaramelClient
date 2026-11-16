package org.chimeramc.client.ui.compose.caramel.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The launcher's "screenshots" entry points must open the real Screenshots menu, not a blank screen.
 *
 * <p>The Home hero's camera button (labelled "Content") called {@code openContent()}, which started
 * {@code LibraryActivity} — a placeholder that draws nothing — so the press led to a white screen.
 * The Screenshots modal already existed and reads the selected instance's real screenshot folder, so
 * the button now opens it. The modal's own "Open folder" buttons were dead too: they called
 * {@code openScreenshots()} (which only re-opened the modal they were already in) instead of handing
 * the real folder to a file manager.
 */
public class ScreenshotEntryPointsTest {

    @Test
    public void theHomeButtonOpensTheScreenshotsModal() throws IOException {
        String home = read("app/src/main/java/org/chimeramc/client/ui/compose/caramel/launcher/"
                + "CaramelHome.kt");
        assertTrue("the hero's camera button must open the screenshots modal",
                home.contains("R.string.caramel_screenshots_title"));
        assertFalse("the button must not still call the dead Content action",
                home.contains("onOpenContent"));

        String screen = read("app/src/main/java/org/chimeramc/client/ui/compose/caramel/launcher/"
                + "CaramelLauncherScreen.kt");
        assertTrue("the Home tab must route the button to openScreenshots",
                screen.contains("CaramelHomeScreen(state, actions::launch, actions::openScreenshots)"));
    }

    @Test
    public void theContentActionNoLongerStartsTheBlankLibrary() throws IOException {
        String activity = read("app/src/main/java/org/chimeramc/client/launcher/ui/activities/"
                + "MainActivity.java");
        assertFalse("openContent must not start the blank LibraryActivity",
                activity.contains("startActivity(new Intent(this, LibraryActivity.class))"));
        assertTrue("the folder action must open the instance's real Screenshots folder",
                activity.contains("openFolder(this, new File(dataDir, \"Screenshots\"))"));
    }

    @Test
    public void theLibraryActivityCannotFlashWhite() throws IOException {
        String manifest = read("app/src/main/AndroidManifest.xml");
        int at = manifest.indexOf("org.chimeramc.client.ui.activities.LibraryActivity");
        assertTrue(at >= 0);
        String block = manifest.substring(at, Math.min(manifest.length(), at + 400));
        assertTrue("LibraryActivity must use the dark launcher theme, not the default window",
                block.contains("android:theme=\"@style/LauncherTheme\""));
    }

    @Test
    public void theFolderButtonsAreNotDead() throws IOException {
        String modals = read("app/src/main/java/org/chimeramc/client/ui/compose/caramel/launcher/"
                + "CaramelModals.kt");
        assertTrue("the modal's open-folder buttons must call the folder action",
                modals.contains("actions::openScreenshotsFolder"));
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
