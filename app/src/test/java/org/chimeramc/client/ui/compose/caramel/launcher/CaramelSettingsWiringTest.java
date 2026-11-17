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
 * Every control the settings panel draws is backed by a store.
 *
 * <p>The defect this covers: the settings screen listed a control whose only effect was a local
 * `mutableStateOf` — the slider moved, the number changed, and nothing was written, so the setting
 * was gone at the next launch. The reverse also shipped: values the store held (the animation
 * speed, the UI transparency, the background image) had no control at all. Both halves are pinned
 * here: each row the panel renders names an action, and each action is implemented by the
 * controller against a real store rather than a composition-local copy.
 */
public class CaramelSettingsWiringTest {

    private static final String PANEL =
            "app/src/main/java/org/chimeramc/client/ui/compose/caramel/CaramelSettings.kt";
    private static final String CONTROLLER =
            "app/src/main/java/org/chimeramc/client/launcher/util/LauncherSettingsController.java";
    private static final String THEME =
            "app/src/main/java/org/chimeramc/client/ui/compose/caramel/CaramelTheme.kt";

    @Test
    public void thePanelRendersTheRestoredControls() throws IOException {
        String panel = read(PANEL);
        for (String action : new String[]{
                "setAnimationSpeed",
                "setUiTransparency",
                "pickBackgroundImage",
                "clearBackgroundImage",
                "setBackgroundImageBlur",
                "setBackgroundImageBrightness",
                "setBackupRetention",
                "setPerformancePreset",
                "setCosmeticSync",
                "setSharedStorageLayout",
                "setWorldBackupEnabled",
                "setWorldBackupIntervalHours",
                "setWorldBackupRetention",
                "pickWorldBackupDestination",
                "runWorldBackupNow",
                "refreshRuntimeData",
                "editCurseForgeKey",
                "cleanHistoricalDirectory",
                "exportSettingsBackup",
                "importSettingsBackup",
        }) {
            assertTrue("the panel must offer " + action, panel.contains("actions." + action)
                    || panel.contains("actions::" + action));
        }
    }

    @Test
    public void theControllerWritesEachControlToItsStore() throws IOException {
        String controller = read(CONTROLLER);
        // Each restored control calls a persisted store, not a field on the state object.
        String[][] expectations = {
                {"setAnimationSpeed", "personalization.setAnimationSpeed("},
                {"setUiTransparency", "personalization.setUiTransparency("},
                {"setBackgroundImageBlur", "personalization.setBackgroundImageBlur("},
                {"setBackgroundImageBrightness", "personalization.setBackgroundImageBrightness("},
                {"clearBackgroundImage", "personalization.clearBackgroundImage("},
                {"setBackupRetention", "FeatureSettings.getInstance().setBackupRetentionKeep("},
                {"setPerformancePreset", "PerformancePresetManager.apply("},
                {"setCosmeticSync", "InbuiltModManager.getInstance(activity).setCosmeticSyncEnabled("},
                {"setSharedStorageLayout", "LauncherStorage.setUseNewSharedStorage("},
                {"setWorldBackupEnabled", "new WorldBackupConfig(activity).setEnabled("},
                {"setWorldBackupIntervalHours", "new WorldBackupConfig(activity).setIntervalHours("},
                {"setWorldBackupRetention", "new WorldBackupConfig(activity).setRetention("},
                {"pickWorldBackupDestination", "worldBackupDestinationLauncher.launch("},
                {"runWorldBackupNow", "WorldBackupScheduler.runNow("},
                {"refreshRuntimeData", "SweetLoaderSignatureRulesManager.refreshNow("},
                {"editCurseForgeKey", "CurseForgeKeyDialog.show("},
                {"cleanHistoricalDirectory", "LauncherStorage.cleanupLegacyRoot("},
                {"exportSettingsBackup", "settingsExportLauncher.launch("},
                {"importSettingsBackup", "settingsImportLauncher.launch("},
        };
        for (String[] pair : expectations) {
            int at = controller.indexOf("void " + pair[0] + "(");
            assertTrue(pair[0] + " must be implemented", at >= 0);
            int end = controller.indexOf("\n    }", at);
            assertTrue(pair[0] + " must write through to the store",
                    controller.substring(at, end).contains(pair[1]));
        }
    }

    @Test
    public void theAnimationSpeedReachesTheComposeTransitions() throws IOException {
        String theme = read(THEME);
        assertTrue("the prefs must carry the animation speed",
                theme.contains("val animationSpeed: Float"));
        assertTrue("the prefs must read it from the store",
                theme.contains("pm.animationSpeed"));
        String glass = read("app/src/main/java/org/chimeramc/client/ui/compose/caramel/CaramelGlass.kt");
        assertTrue("a transition must scale its duration by the setting",
                glass.contains("caramelMotionMs("));
    }

    @Test
    public void reduceMotionIsHonouredByTheComposeTransitions() throws IOException {
        // The switch was stored and never read, so "Reduce motion" did nothing on the Compose
        // launcher. It must collapse a transition's duration and snap the toggle thumb.
        String glass = read("app/src/main/java/org/chimeramc/client/ui/compose/caramel/CaramelGlass.kt");
        assertTrue("the duration helper must read the reduce-motion scale",
                glass.contains("caramelPrefs.motionScale"));
        String controls = read("app/src/main/java/org/chimeramc/client/ui/compose/caramel/CaramelControls.kt");
        assertTrue("the toggle must snap when reduce motion is on",
                controls.contains("snap()"));
        assertTrue("the toggle must read the reduce-motion scale",
                controls.contains("caramelPrefs.motionScale"));
    }

    @Test
    public void thePanelIsNotDrivenByCompositionLocalCopies() throws IOException {
        String panel = read(PANEL);
        assertFalse("a control whose only effect is a local copy is the bug",
                panel.contains("var animationSpeed by remember { mutableStateOf"));
        assertFalse("a control whose only effect is a local copy is the bug",
                panel.contains("var backupRetention by remember { mutableStateOf"));
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
