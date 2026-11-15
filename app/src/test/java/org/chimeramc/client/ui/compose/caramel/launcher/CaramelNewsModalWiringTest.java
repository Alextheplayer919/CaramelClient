package org.chimeramc.client.ui.compose.caramel.launcher;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The "View all news" label must open a menu.
 *
 * <p>It was plain text with no click modifier, so pressing it did nothing. The wiring is three parts
 * that must all be present: the label carries a click that sets {@code newsAll}, a modal reads that
 * flag, and the screen renders the modal. A device test would observe the menu; without an emulator
 * these cases pin the wiring so a refactor that drops one part fails here.
 */
public class CaramelNewsModalWiringTest {

    private static final Path ROOT = findRoot();

    @Test
    public void theViewAllLabelOpensTheNewsModal() throws IOException {
        String home = read("app/src/main/java/org/chimeramc/client/ui/compose/caramel/launcher/CaramelHome.kt");
        int label = home.indexOf("R.string.caramel_view_all_news");
        assertTrue("the View all news label no longer exists", label >= 0);
        // The label sits inside its Text(...) call; take a window wide enough to cover the call's
        // modifier chain without spilling into the next composable's arguments.
        String region = home.substring(label, Math.min(home.length(), label + 400));
        assertTrue("the View all news label has no clickable, so pressing it does nothing",
                region.contains("clickable"));
        assertTrue("the View all news label no longer sets newsAll",
                region.contains("newsAll = true"));
    }

    @Test
    public void aModalReadsTheNewsAllFlag() throws IOException {
        String modals = read("app/src/main/java/org/chimeramc/client/ui/compose/caramel/launcher/CaramelModals.kt");
        assertTrue("no modal is bound to state.newsAll", modals.contains("state.newsAll"));
        assertTrue("the news modal no longer exists", modals.contains("fun CaramelNewsModal("));
    }

    @Test
    public void theScreenRendersTheNewsModal() throws IOException {
        String screen = read("app/src/main/java/org/chimeramc/client/ui/compose/caramel/launcher/CaramelLauncherScreen.kt");
        assertTrue("the news modal is not rendered, so setting newsAll opens nothing",
                screen.contains("CaramelNewsModal("));
    }

    private static Path findRoot() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.isRegularFile(dir.resolve("app/src/main/AndroidManifest.xml"))) return dir;
            dir = dir.getParent();
        }
        throw new IllegalStateException("cannot locate the repository root from "
                + Paths.get("").toAbsolutePath());
    }

    private static String read(String relative) throws IOException {
        return new String(Files.readAllBytes(ROOT.resolve(relative)), StandardCharsets.UTF_8);
    }
}