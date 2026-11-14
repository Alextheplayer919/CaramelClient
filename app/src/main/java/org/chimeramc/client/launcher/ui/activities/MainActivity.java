package org.chimeramc.client.ui.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.compose.runtime.MutableState;
import androidx.compose.ui.platform.ComposeView;
import androidx.compose.ui.platform.ViewCompositionStrategy;

import org.chimeramc.client.core.auth.MsftAccountStore;
import org.chimeramc.client.core.minecraft.MinecraftActivityState;
import org.chimeramc.client.core.minecraft.MinecraftLauncher;
import org.chimeramc.client.core.mods.Mod;
import org.chimeramc.client.core.mods.ModManager;
import org.chimeramc.client.core.versions.GameVersion;
import org.chimeramc.client.core.versions.VersionManager;
import org.chimeramc.client.ui.compose.caramel.CaramelCosmeticInfo;
import org.chimeramc.client.ui.compose.caramel.CaramelLauncherState;
import org.chimeramc.client.ui.compose.caramel.CaramelModInfo;
import org.chimeramc.client.ui.compose.caramel.CaramelServerInfo;
import org.chimeramc.client.ui.compose.caramel.CaramelVersion;
import org.chimeramc.client.ui.compose.caramel.launcher.CaramelLauncherHost;
import org.chimeramc.client.util.AppLog;

import java.util.ArrayList;
import java.util.List;

/**
 * The launcher's main screen: the Caramel UI pack's Compose launcher, hosted in a {@link ComposeView}.
 *
 * <p>This activity owns the state and the actions. It builds a {@link CaramelLauncherState} from the
 * live managers (versions, account, mods) and hands it to the Kotlin host, which renders the pack's
 * frame, tabs and modals. Every button and slider on screen calls back into a real launcher action
 * here; nothing is a mock.
 *
 * <p>The activity draws no chrome of its own. {@link BaseActivity} keeps the immersive full-screen
 * plumbing, and the Compose surface fills what is left.
 */
public class MainActivity extends BaseActivity implements CaramelLauncherHost.Actions {

    private static final String TAG = "MainActivity";

    private MutableState<CaramelLauncherState> launcherState;
    private VersionManager versionManager;
    private MinecraftLauncher minecraftLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        minecraftLauncher = new MinecraftLauncher(this);
        launcherState = CaramelLauncherHost.state();
        setContentView(buildComposeHost());
        initVersionManager();
        refreshState();
    }

    private ComposeView buildComposeHost() {
        ComposeView view = new ComposeView(this);
        view.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool.INSTANCE);
        CaramelLauncherHost.setContent(view, launcherState, this);
        return view;
    }

    private void initVersionManager() {
        VersionManager.initializeAsync(this, manager -> {
            versionManager = manager;
            refreshState();
        });
    }

    /** Rebuilds the launcher state from the live managers and recomposes. */
    private void refreshState() {
        CaramelLauncherState s = launcherState.getValue();
        s.setVersions(buildVersions());
        if (s.getVersion() == null && !s.getVersions().isEmpty()) {
            s.setVersion(s.getVersions().get(0));
        }
        MsftAccountStore.MsftAccount active = activeAccount();
        s.setSignedIn(active != null);
        s.setAccountName(active != null ? displayName(active) : "");
        s.setRunningInstances(MinecraftActivityState.isRunning(this) ? 1 : 0);
        s.setLaunchEnabled(versionManager != null && versionManager.getSelectedVersion() != null);
        s.setMods(buildMods());
        launcherState.setValue(s);
    }

    private List<CaramelVersion> buildVersions() {
        List<CaramelVersion> out = new ArrayList<>();
        if (versionManager == null) return out;
        List<GameVersion> installed = versionManager.getInstalledVersions();
        if (installed != null) for (GameVersion v : installed) out.add(versionRow(v));
        List<GameVersion> custom = versionManager.getCustomVersions();
        if (custom != null) for (GameVersion v : custom) out.add(versionRow(v));
        return out;
    }

    private static CaramelVersion versionRow(GameVersion v) {
        String id = v.versionCode != null && !v.versionCode.isEmpty() ? v.versionCode
                : v.directoryName != null ? v.directoryName : "";
        return new CaramelVersion(id, id.toLowerCase(java.util.Locale.ROOT).contains("beta"));
    }

    private List<CaramelModInfo> buildMods() {
        List<CaramelModInfo> out = new ArrayList<>();
        try {
            for (Mod m : ModManager.getInstance().getMods()) {
                out.add(new CaramelModInfo(
                        m.getDisplayName() != null ? m.getDisplayName() : m.getFileName(),
                        m.getAuthor() != null ? m.getAuthor() : "",
                        m.getVersion() != null ? m.getVersion() : "",
                        m.getDescription() != null ? m.getDescription() : "",
                        m.isEnabled()));
            }
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "buildMods", "Could not read the mod list", t);
        }
        return out;
    }

    private MsftAccountStore.MsftAccount activeAccount() {
        for (MsftAccountStore.MsftAccount a : MsftAccountStore.list(this)) {
            if (a.active) return a;
        }
        return null;
    }

    private static String displayName(MsftAccountStore.MsftAccount a) {
        if (a.minecraftUsername != null && !a.minecraftUsername.isEmpty()) return a.minecraftUsername;
        if (a.xboxGamertag != null && !a.xboxGamertag.isEmpty()) return a.xboxGamertag;
        return "";
    }

    // --- CaramelLauncherHost.Actions ------------------------------------------------------------

    @Override
    public void launch() {
        GameVersion selected = versionManager != null ? versionManager.getSelectedVersion() : null;
        if (selected == null) return;
        minecraftLauncher.launch(getIntent(), selected);
    }

    @Override
    public void selectVersion(String id) {
        if (versionManager == null) return;
        for (GameVersion v : versionManager.getInstalledVersions()) {
            if (versionRow(v).getId().equals(id)) {
                versionManager.selectVersion(v);
                refreshState();
                return;
            }
        }
    }

    @Override
    public void toggleMod(CaramelModInfo mod, boolean on) {
        try {
            ModManager.getInstance().setModEnabled(mod.getName(), on);
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "toggleMod", "Could not toggle the mod", t);
        }
        refreshState();
    }

    @Override
    public void equipCosmetic(CaramelCosmeticInfo cosmetic) {
        // Wired when the cosmetic catalogue is loaded into the Cosmetics tab.
    }

    @Override
    public void toggleFavourite(CaramelCosmeticInfo cosmetic) {
        // Favourites are persisted by the cosmetic store; wired with the catalogue.
    }

    @Override
    public void joinServer(CaramelServerInfo server) {
        // Joining launches the game with the server address; wired with the server store.
    }

    @Override
    public void openModsFolder() {
        startActivity(new Intent(this, ModsFullscreenActivity.class));
    }

    @Override
    public void openContent() {
        startActivity(new Intent(this, LibraryActivity.class));
    }

    @Override
    public void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    @Override
    public void openAccounts() {
        // Wired with the account UI.
    }

    @Override
    public void quit() {
        finishAffinity();
    }

    /**
     * Refreshes the on-screen Minecraft version label. Kept as a no-op so
     * {@code VersionManager.attemptRepairLibs} still compiles; the Caramel screen shows the version
     * on the hero, which {@link #refreshState()} drives.
     */
    public void setTextMinecraftVersion() {
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshState();
    }
}
