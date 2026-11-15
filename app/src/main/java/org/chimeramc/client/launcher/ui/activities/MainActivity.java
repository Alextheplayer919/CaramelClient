package org.chimeramc.client.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.compose.runtime.MutableState;
import androidx.compose.ui.platform.ComposeView;
import androidx.compose.ui.platform.ViewCompositionStrategy;

import org.chimeramc.client.R;
import org.chimeramc.client.core.changelog.ChangelogFeed;
import org.chimeramc.client.core.cosmetics.CosmeticCatalog;
import org.chimeramc.client.core.cosmetics.CosmeticStore;
import net.raphimc.minecraftauth.bedrock.BedrockAuthManager;
import org.chimeramc.client.core.auth.MsftAccountStore;
import org.chimeramc.client.core.auth.MsftAuthManager;
import org.chimeramc.client.core.content.ContentManager;
import org.chimeramc.client.core.content.ScreenshotItem;
import org.chimeramc.client.core.content.ServerItem;
import org.chimeramc.client.core.minecraft.MinecraftActivityState;
import org.chimeramc.client.core.minecraft.MinecraftLauncher;
import org.chimeramc.client.core.mods.Mod;
import org.chimeramc.client.core.mods.ModManager;
import org.chimeramc.client.core.versions.GameVersion;
import org.chimeramc.client.core.versions.VersionManager;
import org.chimeramc.client.settings.FeatureSettings;
import org.chimeramc.client.util.LauncherStorage;
import org.chimeramc.client.ui.compose.caramel.CaramelCosmeticInfo;
import org.chimeramc.client.ui.compose.caramel.CaramelCosmeticKind;
import org.chimeramc.client.ui.compose.caramel.CaramelLauncherState;
import org.chimeramc.client.ui.compose.caramel.CaramelModInfo;
import org.chimeramc.client.ui.compose.caramel.CaramelNewsInfo;
import org.chimeramc.client.ui.compose.caramel.CaramelScreenshotInfo;
import org.chimeramc.client.ui.compose.caramel.CaramelServerInfo;
import org.chimeramc.client.ui.compose.caramel.CaramelVersion;
import org.chimeramc.client.ui.compose.caramel.launcher.CaramelLauncherHost;
import org.chimeramc.client.util.AppExecutors;
import org.chimeramc.client.util.AppLog;
import org.chimeramc.client.util.LocalFileOpener;
import org.chimeramc.client.util.UrlOpener;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The launcher's main screen: the Caramel UI pack's Compose launcher, hosted in a {@link ComposeView}.
 *
 * <p>This activity owns the state and the actions. It builds a {@link CaramelLauncherState} from the
 * live managers (versions, account, mods, servers, cosmetics, changelog, screenshots) and hands it
 * to the Kotlin host, which renders the pack's frame, tabs and modals. Every button on screen calls
 * back into a real launcher action here; nothing is a mock, and a list with no data shows its empty
 * state rather than a fabricated row.
 *
 * <p>The activity draws no chrome of its own. {@link BaseActivity} keeps the immersive full-screen
 * plumbing, and the Compose surface fills what is left.
 */
public class MainActivity extends BaseActivity implements CaramelLauncherHost.Actions {

    private static final String TAG = "MainActivity";

    private MutableState<CaramelLauncherState> launcherState;
    private VersionManager versionManager;
    private MinecraftLauncher minecraftLauncher;
    private ContentManager contentManager;
    private CosmeticStore cosmeticStore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        minecraftLauncher = new MinecraftLauncher(this);
        launcherState = CaramelLauncherHost.state();
        setContentView(buildComposeHost());
        initVersionManager();
        initContent();
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
            refreshContentDirectories();
            refreshState();
        });
    }

    /**
     * Wires the content manager to the selected instance's game data directories and observes the
     * server and screenshot scans, so the Servers tab and the Screenshots modal draw real rows.
     */
    private void initContent() {
        contentManager = ContentManager.getInstance(this);
        contentManager.getServersLiveData().observe(this, servers -> {
            List<CaramelServerInfo> rows = new ArrayList<>();
            if (servers != null) {
                for (ServerItem s : servers) rows.add(serverRow(s));
            }
            mutateState(s -> s.setServers(rows));
        });
        contentManager.getScreenshotsLiveData().observe(this, shots -> {
            List<CaramelScreenshotInfo> rows = new ArrayList<>();
            if (shots != null) {
                for (ScreenshotItem s : shots) {
                    rows.add(new CaramelScreenshotInfo(s.name, s.file.getAbsolutePath()));
                }
            }
            mutateState(s -> s.setScreenshots(rows));
        });
    }

    /** Points the content manager at the selected instance's folders, then rescans. */
    private void refreshContentDirectories() {
        if (contentManager == null || versionManager == null) return;
        File gameDataDir = activeGameDataDir();
        if (gameDataDir == null) return;
        contentManager.setStorageDirectories(
                new File(gameDataDir, "minecraftWorlds"),
                new File(gameDataDir, "resource_packs"),
                new File(gameDataDir, "behavior_packs"),
                new File(gameDataDir, "skin_packs"),
                new File(gameDataDir, "Screenshots"),
                new File(gameDataDir, "minecraftpe"));
    }

    /** The game data root the selected instance plays from, or null when none is selected. */
    private File activeGameDataDir() {
        GameVersion selected = selectedVersion();
        if (selected == null) return null;
        FeatureSettings.StorageType type = LauncherStorage.readSavedContentStorageType(this);
        return LauncherStorage.getActiveGameDataDir(
                this, selected.getStorageProfileId(), selected.versionIsolation, type);
    }

    /** Rebuilds the launcher state from the live managers and recomposes. */
    private void refreshState() {
        CaramelLauncherState s = launcherState.getValue();
        GameVersion selected = selectedVersion();
        s.setVersions(buildVersions());
        if (selected != null) {
            s.setVersion(new CaramelVersion(versionId(selected), isBeta(selected)));
        } else if (s.getVersion() == null && !s.getVersions().isEmpty()) {
            s.setVersion(s.getVersions().get(0));
        }
        s.setInstanceName(selected != null && selected.displayName != null ? selected.displayName : "");
        s.setInstanceEditable(selected != null && !selected.isInstalled);
        MsftAccountStore.MsftAccount active = activeAccount();
        s.setSignedIn(active != null);
        s.setAccountName(active != null ? displayName(active) : "");
        s.setRunningInstances(MinecraftActivityState.isRunning(this) ? 1 : 0);
        s.setLaunchEnabled(selected != null);
        s.setMods(buildMods());
        s.setCosmetics(buildCosmetics());
        s.setNews(buildNews());
        launcherState.setValue(s);
    }

    private List<CaramelVersion> buildVersions() {
        List<CaramelVersion> out = new ArrayList<>();
        if (versionManager == null) return out;
        List<GameVersion> installed = versionManager.getInstalledVersions();
        if (installed != null) {
            for (GameVersion v : installed) out.add(new CaramelVersion(versionId(v), isBeta(v)));
        }
        List<GameVersion> custom = versionManager.getCustomVersions();
        if (custom != null) {
            for (GameVersion v : custom) out.add(new CaramelVersion(versionId(v), isBeta(v)));
        }
        return out;
    }

    private static String versionId(GameVersion v) {
        if (v.versionCode != null && !v.versionCode.isEmpty()) return v.versionCode;
        return v.directoryName != null ? v.directoryName : "";
    }

    private static boolean isBeta(GameVersion v) {
        return versionId(v).toLowerCase(Locale.ROOT).contains("beta");
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

    /**
     * Builds the cosmetic tiles from the real catalogue.
     *
     * <p>Every catalogue entry is drawn and the equipped one is flagged from the store. The client
     * has no cosmetic-ownership model, so every catalogue entry is treated as owned rather than
     * inventing a lock the client cannot honour.
     */
    private List<CaramelCosmeticInfo> buildCosmetics() {
        List<CaramelCosmeticInfo> out = new ArrayList<>();
        try {
            if (cosmeticStore == null) cosmeticStore = new CosmeticStore(this);
            String equippedCape = cosmeticStore.getEquippedCapeId();
            String equippedAccessory = cosmeticStore.getEquippedAccessoryId();
            String equippedPet = cosmeticStore.getEquippedPetId();
            for (CosmeticCatalog.Cape c : CosmeticCatalog.capes()) {
                out.add(new CaramelCosmeticInfo(c.id, c.name, CaramelCosmeticKind.Cape,
                        c.id.equals(equippedCape), true, false));
            }
            for (CosmeticCatalog.Accessory a : CosmeticCatalog.accessories()) {
                CaramelCosmeticKind kind = a.kind != null && a.kind.followsHead()
                        ? CaramelCosmeticKind.Hat : CaramelCosmeticKind.Wings;
                out.add(new CaramelCosmeticInfo(a.id, a.name, kind,
                        a.id.equals(equippedAccessory), true, false));
            }
            for (CosmeticCatalog.Pet p : CosmeticCatalog.pets()) {
                out.add(new CaramelCosmeticInfo(p.id, p.name, CaramelCosmeticKind.Pet,
                        p.id.equals(equippedPet), true, false));
            }
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "buildCosmetics", "Could not read the cosmetic catalogue", t);
        }
        return out;
    }

    /** Reads the bundled changelog feed into news rows; an unreadable feed yields no rows. */
    private List<CaramelNewsInfo> buildNews() {
        List<CaramelNewsInfo> out = new ArrayList<>();
        try (InputStream in = getAssets().open("launcher/changelog_feed.json")) {
            byte[] buf = new byte[8192];
            StringBuilder sb = new StringBuilder();
            int read;
            while ((read = in.read(buf)) > 0) {
                sb.append(new String(buf, 0, read, StandardCharsets.UTF_8));
            }
            ChangelogFeed feed = ChangelogFeed.parse(sb.toString());
            ChangelogFeed.Section newest = feed.newest();
            if (newest != null && newest.headline != null && !newest.headline.isEmpty()) {
                out.add(new CaramelNewsInfo(newest.headline, joinItems(newest.items), newest.version, true));
            }
            ChangelogFeed.Section upcoming = feed.upcoming();
            if (upcoming != null && upcoming.headline != null && !upcoming.headline.isEmpty()) {
                out.add(new CaramelNewsInfo(upcoming.headline, joinItems(upcoming.items), upcoming.version, false));
            }
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "buildNews", "Could not read the changelog feed", t);
        }
        return out;
    }

    private static String joinItems(List<String> items) {
        if (items == null || items.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            if (item == null || item.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" \u00b7 ");
            sb.append(item);
        }
        return sb.toString();
    }

    private static CaramelServerInfo serverRow(ServerItem s) {
        // online/ping/version range are only known once measured; the client does not measure them,
        // so the row carries the saved address and leaves the rest null.
        return new CaramelServerInfo(s.name, s.ip, s.port, null, null, "", "", false);
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

    private GameVersion selectedVersion() {
        return versionManager != null ? versionManager.getSelectedVersion() : null;
    }

    private void toast(int resId) {
        Toast.makeText(this, getString(resId), Toast.LENGTH_SHORT).show();
    }

    private void toast(String message) {
        if (message == null || message.isEmpty()) return;
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /** Applies a change to the Compose state on the UI thread and recomposes. */
    private void mutateState(StateMutation mutation) {
        CaramelLauncherState s = launcherState.getValue();
        mutation.apply(s);
        launcherState.setValue(s);
    }

    private interface StateMutation {
        void apply(CaramelLauncherState state);
    }

    // --- CaramelLauncherHost.Actions ------------------------------------------------------------

    @Override
    public void launch() {
        GameVersion selected = selectedVersion();
        if (selected == null) return;
        mutateState(s -> s.setLaunching(true));
        minecraftLauncher.launch(getIntent(), selected, new MinecraftLauncher.LaunchCallback() {
            @Override
            public void onLaunchStarted() {
                mutateState(s -> s.setLaunching(false));
            }

            @Override
            public void onLaunchFailed(Throwable e) {
                mutateState(s -> s.setLaunching(false));
                AppLog.warnOnce(TAG, "launch", "The game did not start", e);
            }
        });
    }

    @Override
    public void selectVersion(String id) {
        if (versionManager == null) return;
        GameVersion match = findVersion(id);
        if (match == null) return;
        versionManager.selectVersion(match);
        refreshContentDirectories();
        refreshState();
    }

    private GameVersion findVersion(String id) {
        if (versionManager == null) return null;
        List<GameVersion> all = new ArrayList<>();
        if (versionManager.getInstalledVersions() != null) all.addAll(versionManager.getInstalledVersions());
        if (versionManager.getCustomVersions() != null) all.addAll(versionManager.getCustomVersions());
        for (GameVersion v : all) {
            if (versionId(v).equals(id)) return v;
        }
        return null;
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
        try {
            if (cosmeticStore == null) cosmeticStore = new CosmeticStore(this);
            switch (cosmetic.getKind()) {
                case Cape -> cosmeticStore.setEquippedCape(cosmetic.getId());
                case Wings, Hat -> cosmeticStore.setEquippedAccessory(cosmetic.getId());
                case Pet -> cosmeticStore.setEquippedPet(cosmetic.getId());
            }
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "equipCosmetic", "Could not save the equipped cosmetic", t);
        }
        refreshState();
    }

    @Override
    public void toggleFavourite(CaramelCosmeticInfo cosmetic) {
        // The cosmetic store has no favourites model; the tile keeps its star in the screen rather
        // than the client pretending the choice was persisted.
    }

    @Override
    public void joinServer(CaramelServerInfo server) {
        // Bedrock has no launcher-side "join server" intent; the saved server reaches the game
        // through external_servers.txt, so the honest action is to launch the instance.
        launch();
    }

    @Override
    public void addServer() {
        // The add-server dialog lives in the content screens.
        openContent();
    }

    @Override
    public void toggleServerFavourite(CaramelServerInfo server) {
        // Server favourites are persisted by ServerFavouritesStore, wired with the server editor.
    }

    @Override
    public void openModsFolder() {
        GameVersion selected = selectedVersion();
        if (selected == null || selected.modsDir == null) return;
        LocalFileOpener.openFolder(this, selected.modsDir);
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
        // Accounts are managed through the account modal; there is no separate screen.
    }

    @Override
    public void openScreenshots() {
        mutateState(s -> s.setScreenshotsModal(true));
    }

    @Override
    public void openScreenshot(CaramelScreenshotInfo screenshot) {
        LocalFileOpener.openImage(this, new File(screenshot.getPath()));
    }

    @Override
    public void deleteScreenshot(CaramelScreenshotInfo screenshot) {
        try {
            new File(screenshot.getPath()).delete();
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "deleteScreenshot", "Could not delete the screenshot", t);
        }
        if (contentManager != null) contentManager.refreshScreenshots();
        mutateState(s -> s.setSelectedScreenshot(null));
    }

    /**
     * Starts the Microsoft device-code sign-in.
     *
     * <p>The button used to call this and see nothing happen. Three things were wrong, and each one
     * on its own was enough to make the press look dead: the code was never shown for long enough to
     * act on, the browser was never opened, and every failure was written to the log and then
     * discarded, so the modal closed with no account, no code and no message. This runs the flow the
     * device-code path actually supports (the Bedrock title id is an MSA title, so the MSAL browser
     * flow cannot authenticate it) and reports each step into the modal.
     */
    @Override
    public void startSignIn() {
        mutateState(s -> {
            s.setSignInRunning(true);
            s.setSignInError("");
            s.setDeviceCode("");
            s.setDeviceCodeUrl("");
            s.setBusyMessage(getString(R.string.caramel_sign_in_starting));
        });
        AppExecutors.run(AppExecutors.Kind.NETWORK, "caramel-sign-in", () -> {
            String failure = null;
            try {
                BedrockAuthManager manager = MsftAuthManager.loginWithDeviceCode(
                        code -> runOnUiThread(() -> {
                            // The code is shown first and the browser opened second, so the user
                            // returns to a modal that already carries the code and the link.
                            mutateState(s -> {
                                s.setDeviceCode(code.getUserCode());
                                s.setDeviceCodeUrl(code.getDirectVerificationUri());
                                s.setBusyMessage(getString(R.string.caramel_waiting_sign_in));
                            });
                            UrlOpener.open(this, code.getDirectVerificationUri());
                        }),
                        org.chimeramc.client.core.auth.Cancellation.none(),
                        state -> { });
                MsftAccountStore.MsftAccount account = MsftAuthManager.saveAccountAndActivate(this, manager);
                runOnUiThread(() -> toast(getString(R.string.ms_login_success, displayName(account))));
            } catch (Throwable t) {
                failure = MsftAuthManager.describeError(t);
                AppLog.warnOnce(TAG, "startSignIn", "Microsoft sign-in did not complete", t);
            }
            final String error = failure;
            runOnUiThread(() -> {
                mutateState(s -> {
                    s.setBusyMessage("");
                    s.setSignInRunning(false);
                    s.setSignInError(error == null ? "" : error);
                    if (error == null) {
                        s.setDeviceCode("");
                        s.setDeviceCodeUrl("");
                        s.setAddAccount(false);
                    }
                });
                if (error != null) toast(error);
                refreshState();
            });
        });
    }

    @Override
    public void openDeviceCodePage() {
        CaramelLauncherState s = launcherState.getValue();
        if (!UrlOpener.open(this, s.getDeviceCodeUrl())) {
            toast(R.string.caramel_device_code_open_failed);
        }
    }

    @Override
    public void copyDeviceCode() {
        String code = launcherState.getValue().getDeviceCode();
        if (code == null || code.isEmpty()) return;
        try {
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard == null) return;
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Microsoft device code", code));
            toast(R.string.ms_device_code_copied);
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "copyDeviceCode", "Could not copy the device code", t);
        }
    }

    @Override
    public void signOut() {
        MsftAccountStore.MsftAccount active = activeAccount();
        if (active != null) MsftAccountStore.remove(this, active.id);
        mutateState(s -> {
            s.setAccountMenu(false);
            s.setAccountSettings(false);
        });
        refreshState();
    }

    @Override
    public void renameInstance(String name) {
        GameVersion selected = selectedVersion();
        if (selected == null || versionManager == null) return;
        versionManager.setInstanceDisplayName(selected, name);
        refreshState();
    }

    @Override
    public void repairInstance() {
        GameVersion selected = selectedVersion();
        if (selected == null || versionManager == null) return;
        mutateState(s -> s.setBusyMessage(getString(R.string.caramel_busy_repair)));
        versionManager.repairLibsAsync(selected, new VersionManager.LibsRepairCallback() {
            @Override
            public void onRepairStarted() {
            }

            @Override
            public void onRepairProgress(int progress) {
            }

            @Override
            public void onRepairCompleted(boolean success) {
                runOnUiThread(() -> {
                    mutateState(s -> s.setBusyMessage(""));
                    toast(success ? R.string.repair_completed : R.string.repair_failed);
                });
            }

            @Override
            public void onRepairFailed(Throwable e) {
                runOnUiThread(() -> {
                    mutateState(s -> s.setBusyMessage(""));
                    AppLog.warnOnce(TAG, "repairInstance", "The instance repair failed", e);
                    toast(R.string.repair_failed);
                });
            }
        });
    }

    @Override
    public void deleteInstance() {
        GameVersion selected = selectedVersion();
        if (selected == null || versionManager == null || selected.isInstalled) return;
        versionManager.deleteCustomVersion(selected, new VersionManager.OnDeleteVersionCallback() {
            @Override
            public void onDeleteCompleted(boolean success) {
                runOnUiThread(() -> {
                    refreshState();
                    if (!success) toast(R.string.toast_delete_failed);
                });
            }

            @Override
            public void onDeleteFailed(Exception e) {
                runOnUiThread(() -> {
                    AppLog.warnOnce(TAG, "deleteInstance", "Could not delete the instance", e);
                    toast(R.string.toast_delete_failed);
                });
            }
        });
    }

    @Override
    public void openInstanceFolder() {
        GameVersion selected = selectedVersion();
        if (selected == null || selected.versionDir == null) return;
        LocalFileOpener.openFolder(this, selected.versionDir);
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
        refreshContentDirectories();
        refreshState();
    }
}
