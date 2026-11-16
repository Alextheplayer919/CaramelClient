package org.chimeramc.client.launcher.util;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import org.chimeramc.client.R;
import org.chimeramc.client.launcher.settings.SettingsRegistry;
import org.chimeramc.client.launcher.ui.activities.DiagnosticsActivity;
import org.chimeramc.client.ui.activities.MainActivity;
import org.chimeramc.client.settings.FeatureSettings;
import org.chimeramc.client.util.GithubReleaseUpdater;
import org.chimeramc.client.util.LauncherFonts;
import org.chimeramc.client.util.LanguageManager;
import org.chimeramc.client.util.PersonalizationManager;
import org.chimeramc.client.util.ThemeManager;
import org.chimeramc.client.ui.compose.caramel.CaramelSettingFlag;
import org.chimeramc.client.ui.compose.caramel.CaramelSettingsActions;
import org.chimeramc.client.ui.compose.caramel.CaramelSettingsProvider;
import org.chimeramc.client.ui.compose.caramel.CaramelSettingsState;

import java.util.ArrayList;
import java.util.List;

/**
 * The settings panel's action side: one place where a settings control writes to the store that
 * owns the value.
 *
 * <p>Both surfaces that show the panel — the launcher's settings modal and the full-screen
 * {@code SettingsActivity} — hand their presses here, so a slider cannot be live on one surface and
 * dead on the other. The class owns the {@link CaramelSettingsState} it publishes, so a change is
 * written to the store and the panel's copy is rebuilt from the store rather than from the press.
 */
public final class LauncherSettingsController implements CaramelSettingsActions, CaramelSettingsProvider {

    private final Activity activity;
    private final PersonalizationManager personalization;
    private final ThemeManager themeManager;
    private final LanguageManager languageManager;
    private final CaramelSettingsState state = new CaramelSettingsState();
    private final List<String> fontKeys = new ArrayList<>();
    private final List<String> fontLabels = new ArrayList<>();
    private GithubReleaseUpdater updater;

    public LauncherSettingsController(Activity activity) {
        this.activity = activity;
        this.personalization = new PersonalizationManager(activity);
        this.themeManager = new ThemeManager(activity);
        this.languageManager = new LanguageManager(activity);
        for (LauncherFonts.Entry entry : LauncherFonts.entries()) {
            fontKeys.add(entry.key);
            fontLabels.add(activity.getString(entry.labelRes));
        }
        refresh();
    }

    /** The panel's state, owned here so a change survives the composition that made it. */
    public CaramelSettingsState state() {
        return state;
    }

    @Override
    public CaramelSettingsState settingsState() {
        return state;
    }

    @Override
    public CaramelSettingsActions settingsActions() {
        return this;
    }

    public List<String> fontKeys() {
        return fontKeys;
    }

    public List<String> fontLabels() {
        return fontLabels;
    }

    /** Rebuilds the panel's state from the stores. */
    public void refresh() {
        state.setThemeMode(themeManager.getCurrentMode());
        state.setDynamicColor(personalization.isDynamicColorEnabled());
        state.setFontFamilyKey(personalization.getFontFamily());
        state.setFontScale(personalization.getFontScale());
        state.setCardRounding(personalization.getCardRounding());
        state.setIconSize(personalization.getIconSize());
        state.setReduceMotion(personalization.isReduceMotion());
        state.setHaptics(personalization.isHapticFeedbackEnabled());
        state.setBlurIntensity(personalization.getBlurIntensity());
        state.setLanguageTag(languageManager.getCurrentLanguage());
        state.setFlags(buildFlags());
        state.setUpdateAvailable(GithubReleaseUpdater.isUpdateAvailable(activity));
        state.setVersionName(installedVersionName());
    }

    /**
     * Every client switch, in registry order, with its translated label.
     *
     * <p>An action (a setting with no stored value) is skipped: it has nothing to toggle, and the
     * panel would draw a switch that cannot move.
     */
    private List<CaramelSettingFlag> buildFlags() {
        FeatureSettings features = FeatureSettings.getInstance();
        List<CaramelSettingFlag> flags = new ArrayList<>();
        for (SettingsRegistry.Setting setting : SettingsRegistry.all()) {
            if (setting.kind == SettingsRegistry.Kind.ACTION) continue;
            flags.add(new CaramelSettingFlag(
                    setting.id,
                    activity.getString(setting.titleRes),
                    activity.getString(setting.descriptionRes),
                    setting.value(features)));
        }
        return flags;
    }

    private String installedVersionName() {
        try {
            return activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public void setThemeMode(int mode) {
        themeManager.setThemeMode(mode);
        state.setThemeMode(themeManager.getCurrentMode());
    }

    @Override
    public void setDynamicColor(boolean enabled) {
        personalization.setDynamicColorEnabled(enabled);
        state.setDynamicColor(personalization.isDynamicColorEnabled());
    }

    @Override
    public void setFontFamilyKey(String key) {
        personalization.setFontFamily(key);
        state.setFontFamilyKey(personalization.getFontFamily());
    }

    @Override
    public void setFontScale(float scale) {
        personalization.setFontScale(scale);
        state.setFontScale(personalization.getFontScale());
    }

    @Override
    public void setCardRounding(int dp) {
        personalization.setCardRounding(dp);
        state.setCardRounding(personalization.getCardRounding());
    }

    @Override
    public void setIconSize(int percent) {
        personalization.setIconSize(percent);
        state.setIconSize(personalization.getIconSize());
    }

    @Override
    public void setReduceMotion(boolean reduce) {
        personalization.setReduceMotion(reduce);
        state.setReduceMotion(personalization.isReduceMotion());
    }

    @Override
    public void setHaptics(boolean enabled) {
        personalization.setHapticFeedbackEnabled(enabled);
        state.setHaptics(personalization.isHapticFeedbackEnabled());
    }

    @Override
    public void setBlurIntensity(int percent) {
        personalization.setBlurIntensity(percent);
        state.setBlurIntensity(personalization.getBlurIntensity());
    }

    @Override
    public void setLanguageTag(String tag) {
        languageManager.setAppLanguage(tag);
        state.setLanguageTag(tag);
    }

    @Override
    public void setFlag(String id, boolean value) {
        SettingsRegistry.Setting setting = SettingsRegistry.find(id);
        if (setting == null) return;
        setting.apply(FeatureSettings.getInstance(), value);
        state.setFlags(buildFlags());
    }

    @Override
    public void checkForUpdates() {
        if (updater == null) {
            updater = new GithubReleaseUpdater(activity, GithubReleaseUpdater.REPO_OWNER,
                    GithubReleaseUpdater.REPO_NAME, null);
        }
        // A manual press must always answer: the button used to look dead because a failed request
        // only reached the log. The panel now shows a line for the duration and the result.
        state.setBusyMessage(activity.getString(R.string.caramel_checking_updates));
        updater.checkUpdate((outcome, latestVersion, downloadUrl, error) -> {
            switch (outcome) {
                case UPDATE_AVAILABLE:
                    state.setUpdateAvailable(true);
                    state.setBusyMessage(activity.getString(R.string.caramel_update_available_version, latestVersion));
                    break;
                case UP_TO_DATE:
                    state.setUpdateAvailable(false);
                    state.setBusyMessage(activity.getString(R.string.already_latest_version, latestVersion));
                    break;
                default:
                    state.setBusyMessage(activity.getString(R.string.caramel_update_check_failed));
                    break;
            }
        });
    }

    @Override
    public void openDiagnostics() {
        activity.startActivity(new Intent(activity, DiagnosticsActivity.class));
    }

    @Override
    public void openChangelog() {
        // The changelog is the launcher's News tab; opening it there keeps one renderer for it
        // rather than a second copy of the feed.
        Intent intent = new Intent(activity, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_INITIAL_TAB, "News");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
    }

    @Override
    public void resetSettings() {
        personalization.resetAllCustomizations();
        SettingsRegistry.resetToDefaults(FeatureSettings.getInstance());
        refresh();
        Toast.makeText(activity, R.string.caramel_settings_reset_done, Toast.LENGTH_SHORT).show();
    }
}
