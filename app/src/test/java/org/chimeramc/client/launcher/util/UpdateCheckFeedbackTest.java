package org.chimeramc.client.launcher.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * "Check for updates" must answer the press, including when it fails.
 *
 * <p>The defect: the button appeared to do nothing. The check did run, but its only visible
 * reactions were the "new version" dialog and an "already latest" toast — a request that failed or
 * returned no APK reached the log alone. The check now reports every outcome to the caller, and the
 * settings panel shows a line for the duration and the result. Without a device these cases pin that
 * contract.
 */
public class UpdateCheckFeedbackTest {

    private static final Path ROOT = findRoot();

    @Test
    public void theCheckReportsEveryOutcome() throws IOException {
        String updater = read("app/src/main/java/org/chimeramc/client/launcher/util/GithubReleaseUpdater.java");
        assertTrue("the check must expose a result listener", updater.contains("interface ResultListener"));
        assertTrue("the outcomes must name all three endings", updater.contains("UP_TO_DATE")
                && updater.contains("UPDATE_AVAILABLE") && updater.contains("FAILED"));
        // Every path out of the request must report: failure, no APK asset, parse error, and both
        // the newer and the current-version branches.
        int reports = count(updater, "listener.onResult(");
        assertTrue("the check reports only " + reports + " of the five endings",
                reports >= 5);
    }

    @Test
    public void thePanelShowsTheProgressAndTheResult() throws IOException {
        String controller = read("app/src/main/java/org/chimeramc/client/launcher/util/LauncherSettingsController.java");
        int at = controller.indexOf("public void checkForUpdates()");
        assertTrue("checkForUpdates is gone", at >= 0);
        String body = controller.substring(at, Math.min(controller.length(), at + 1600));
        assertTrue("the press must show that a check is running",
                body.contains("caramel_checking_updates"));
        assertTrue("a newer release must be named", body.contains("caramel_update_available_version"));
        assertTrue("a failed check must say so", body.contains("caramel_update_check_failed"));
        assertTrue("the outcome must be handled by case, not ignored",
                body.contains("UPDATE_AVAILABLE") && body.contains("UP_TO_DATE"));

        String settings = read("app/src/main/java/org/chimeramc/client/ui/compose/caramel/CaramelSettings.kt");
        assertTrue("the panel must draw the status line", settings.contains("state.busyMessage"));
    }

    @Test
    public void theUpdateActionIsStillWiredToTheButton() throws IOException {
        String settings = read("app/src/main/java/org/chimeramc/client/ui/compose/caramel/CaramelSettings.kt");
        assertTrue("the Check for updates row must call the action",
                settings.contains("actions::checkForUpdates"));
    }

    @Test
    public void everyLocaleCarriesTheUpdateStrings() throws IOException {
        for (String locale : new String[]{"es", "fr", "hi", "id", "ja", "pt", "ru", "tr", "vi",
                "zh-rCN"}) {
            String strings = read("app/src/main/res/values-" + locale + "/strings.xml");
            for (String key : new String[]{"caramel_checking_updates", "caramel_update_available_version",
                    "caramel_update_check_failed"}) {
                assertTrue(key + " is not translated for " + locale,
                        strings.contains("name=\"" + key + "\""));
            }
        }
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        int at = haystack.indexOf(needle);
        while (at >= 0) {
            n++;
            at = haystack.indexOf(needle, at + needle.length());
        }
        return n;
    }

    private static Path findRoot() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.isRegularFile(dir.resolve("app/src/main/AndroidManifest.xml"))) return dir;
            dir = dir.getParent();
        }
        throw new IllegalStateException("cannot locate the repository root");
    }

    private static String read(String relative) throws IOException {
        return new String(Files.readAllBytes(ROOT.resolve(relative)), StandardCharsets.UTF_8);
    }
}
