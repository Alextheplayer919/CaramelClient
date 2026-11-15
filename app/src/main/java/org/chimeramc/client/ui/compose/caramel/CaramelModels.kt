package org.chimeramc.client.ui.compose.caramel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * The launcher's plain-data shapes, ported from the pack's `data/Models.kt`.
 *
 * <p>The pack ships its own `Versions`, `ExampleMods` and `ExampleServers` here. Those are
 * stand-ins for a demo and are not carried over: the honesty rule forbids drawing a number or a
 * name the client has not actually measured or fetched, so every list this file describes is
 * filled by the activity from a real manager. What stays is the shape and the enumerations, so the
 * screens are a pure function of a small immutable value and can be tested on the JVM.
 */

/** A version's release channel, which is what the green/orange chip on a version row shows. */
enum class CaramelChannel(val label: String, val color: Color) {
    Release("Release", CaramelColors.Green),
    Beta("Beta", CaramelColors.Orange),
}

/** One selectable game version. [beta] drives the channel chip. */
data class CaramelVersion(val id: String, val beta: Boolean) {
    val channel: CaramelChannel get() = if (beta) CaramelChannel.Beta else CaramelChannel.Release
}

/** The three loaders the pack's launch menu offers. */
enum class CaramelLoader(val title: String) {
    Bedrock("Minecraft Bedrock"),
    Rust("Rust"),
    Lightning("Lightning"),
}

/** The instance profile mode: a shared "Open" profile or an isolated one. */
enum class CaramelProfileMode(val title: String, val desc: String, val label: String, val locked: Boolean) {
    Open("Open Instance Profile", "Shares worlds and settings", "Unlocked", false),
    Isolated("Isolated Instance Profile", "Separate files and mods", "Locked", true),
}

/** The six launcher tabs the pack's sidebar carries. */
enum class CaramelTab { Home, Mods, Servers, Cosmetics, News, Store }

/** One mod row: the fields the Mods grid and the instance-settings list draw. */
data class CaramelModInfo(
    val name: String,
    val author: String,
    val version: String,
    val description: String,
    val enabled: Boolean,
)

/** One saved server: what the Servers tab draws. [online] and [ping] are only set when measured. */
data class CaramelServerInfo(
    val name: String,
    val address: String,
    val port: Int,
    val online: Int?,
    val ping: Int?,
    /** The game version the server advertised, empty until a ping measured one. */
    val version: String,
    val favourite: Boolean,
    /** The icon key from [org.chimeramc.client.core.content.ServerIconCatalog], empty when none. */
    val iconKey: String,
    /** True for a shipped default server, false for one the user saved. */
    val featured: Boolean,
    /** True once a ping has been attempted, so the row can tell "checking" from "offline". */
    val checked: Boolean = false,
)

/** What kind of cosmetic a tile is, which is the Cosmetics screen's filter chip. */
enum class CaramelCosmeticKind(val chip: String) {
    Cape("Capes"),
    Wings("Wings"),
    Hat("Hats"),
    Pet("Pets"),
}

/** One cosmetic tile: the Cosmetics grid draws these from the real catalogue. */
data class CaramelCosmeticInfo(
    val id: String,
    val name: String,
    val kind: CaramelCosmeticKind,
    val equipped: Boolean,
    val owned: Boolean,
    val favourite: Boolean,
)

/** One news/changelog entry: the News tab draws these from the real changelog. */
data class CaramelNewsInfo(
    val title: String,
    val summary: String,
    val meta: String,
    val featured: Boolean,
)

/** One captured screenshot: the Screenshots modal draws these from the instance's folder. */
data class CaramelScreenshotInfo(
    val name: String,
    val path: String,
)

/** One Xbox Live friend: the Friends card and modal draw these from PeopleHub. */
data class CaramelFriendInfo(
    val xuid: String,
    val gamertag: String,
    val avatarUrl: String,
    val online: Boolean,
    val presence: String,
)

/**
 * The launcher's whole mutable state, as the pack's `LauncherState` is.
 *
 * <p>Every list defaults to empty and every count to zero: the screens then draw their empty state
 * rather than a fabricated row. The activity fills them from the live managers.
 */
class CaramelLauncherState {
    var tab by mutableStateOf(CaramelTab.Home)

    var versions by mutableStateOf<List<CaramelVersion>>(emptyList())
    var version by mutableStateOf<CaramelVersion?>(null)
    var loader by mutableStateOf(CaramelLoader.Bedrock)
    var profile by mutableStateOf(CaramelProfileMode.Isolated)

    var instanceName by mutableStateOf("")
    var accountName by mutableStateOf("")
    var signedIn by mutableStateOf(false)

    /** The signed-in account's profile picture URL (Xbox), empty when unknown or signed out. */
    var accountAvatarUrl by mutableStateOf("")
    var runningInstances by mutableStateOf(0)
    var launchEnabled by mutableStateOf(false)
    var launching by mutableStateOf(false)

    var mods by mutableStateOf<List<CaramelModInfo>>(emptyList())
    var servers by mutableStateOf<List<CaramelServerInfo>>(emptyList())
    var cosmetics by mutableStateOf<List<CaramelCosmeticInfo>>(emptyList())
    var news by mutableStateOf<List<CaramelNewsInfo>>(emptyList())
    var screenshots by mutableStateOf<List<CaramelScreenshotInfo>>(emptyList())
    var friends by mutableStateOf<List<CaramelFriendInfo>>(emptyList())

    /** A status line for the friends list: "loading", an error reason, or empty when loaded. */
    var friendsStatus by mutableStateOf("")

    var launchMenu by mutableStateOf(false)
    var instanceSettings by mutableStateOf(false)
    var accountMenu by mutableStateOf(false)
    var addFriend by mutableStateOf(false)
    var launcherSettings by mutableStateOf(false)
    var appMenu by mutableStateOf(false)
    var screenshotsModal by mutableStateOf(false)
    var friendsAll by mutableStateOf(false)

    /** Whether the "View all news" modal is open. */
    var newsAll by mutableStateOf(false)
    var addAccount by mutableStateOf(false)
    var accountSettings by mutableStateOf(false)

    /** The selected mod row, or null when the detail modal is closed. */
    var selectedMod by mutableStateOf<CaramelModInfo?>(null)

    /** The selected screenshot row, or null when the Screenshots modal is closed. */
    var selectedScreenshot by mutableStateOf<CaramelScreenshotInfo?>(null)

    /** Whether the instance's files can be repaired or deleted; false for the built-in version. */
    var instanceEditable by mutableStateOf(false)

    /** A status line shown while a long action (sign-in, repair) is running; empty when idle. */
    var busyMessage by mutableStateOf("")

    /** The Microsoft device code shown in the add-account modal, empty when no sign-in is running. */
    var deviceCode by mutableStateOf("")

    /** The page the code is entered on, with the code pre-filled; empty until a code is issued. */
    var deviceCodeUrl by mutableStateOf("")

    /** Whether a sign-in is in flight, so the modal can show progress rather than a dead button. */
    var signInRunning by mutableStateOf(false)

    /** Why the last sign-in attempt failed, empty when it has not failed. */
    var signInError by mutableStateOf("")

    /** The gamertag typed into the Add friend modal. */
    var friendQuery by mutableStateOf("")

    /** Whether a gamertag lookup is in flight, so the modal can show progress. */
    var friendSearchRunning by mutableStateOf(false)

    /** The account the last lookup resolved, or null when nothing has matched yet. */
    var friendFound by mutableStateOf<CaramelFoundUser?>(null)

    /** Whether the found account is a confirmed Caramel user, so the badge is honest. */
    var friendFoundIsCaramel by mutableStateOf(false)

    /** Why the last lookup found nothing, empty when it has not failed. */
    var friendSearchError by mutableStateOf("")

    /** Whether the follow request for [friendFound] is in flight. */
    var friendRequestRunning by mutableStateOf(false)

    /** The result line shown after a follow attempt, empty when idle. */
    var friendRequestResult by mutableStateOf("")

    /** Whether the follow request succeeded, so the result line can be tinted. */
    var friendRequestSucceeded by mutableStateOf(false)

    /** Whether the in-app mods-folder browser is open. */
    var modsFolderOpen by mutableStateOf(false)

    /** Whether the Add server modal is open. */
    var addServer by mutableStateOf(false)

    /** The name typed into the Add server modal. */
    var serverDraftName by mutableStateOf("")

    /** The address typed into the Add server modal. */
    var serverDraftAddress by mutableStateOf("")

    /** The port typed into the Add server modal, as text so the field can be cleared. */
    var serverDraftPort by mutableStateOf("")

    /** Whether an add-server write is in flight, so the button shows progress. */
    var addServerRunning by mutableStateOf(false)

    /** Why the last add-server attempt failed, empty when it has not failed. */
    var addServerError by mutableStateOf("")

    /** The folder the browser is currently showing, as a path relative to the mods root. */
    var modsFolderPath by mutableStateOf("/")

    /** The entries in the folder the browser is showing. */
    var modsFolderEntries by mutableStateOf<List<CaramelFileEntry>>(emptyList())

    /** Whether the browser can go up a level (false at the mods root). */
    var modsFolderCanGoUp by mutableStateOf(false)

    /**
     * The instance's performance settings, keyed by [org.chimeramc.client.core.minecraft.PerfFeatureCatalog]
     * feature id. A missing entry means the instance's options file has no such key yet, so the tab
     * draws the feature's own default rather than a fabricated reading.
     */
    var perfValues by mutableStateOf<Map<String, String>>(emptyMap())

    /** A status line for the last performance write; empty when idle. */
    var perfStatus by mutableStateOf("")

    /** Whether this instance runs the MB shader loader (the mtbinloader2 compatibility fixer). */
    var instanceShaderCompat by mutableStateOf(false)

    /** Whether this instance keeps its own game data rather than the shared root. */
    var instanceIsolation by mutableStateOf(false)

    /** Whether this instance launches in portrait. */
    var instanceLaunchVertical by mutableStateOf(false)

    /** Whether this instance exposes the in-game resource-pack changer. */
    var instancePackChanger by mutableStateOf(false)
}

/** One row in the mods-folder browser: a file or a sub-directory. */
data class CaramelFileEntry(
    val name: String,
    val path: String,
    val directory: Boolean,
    val sizeLabel: String,
)

/** One account the Add friend modal resolved from a typed gamertag. */
data class CaramelFoundUser(
    val xuid: String,
    val gamertag: String,
    val avatarUrl: String,
)
