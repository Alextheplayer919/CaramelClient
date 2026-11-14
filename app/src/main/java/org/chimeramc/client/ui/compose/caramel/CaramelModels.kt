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
    val online: Int?,
    val ping: Int?,
    val minVersion: String,
    val maxVersion: String,
)

/** One cosmetic tile: the Cosmetics grid draws these from the real catalogue. */
data class CaramelCosmeticInfo(
    val name: String,
    val kind: String,
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
    var runningInstances by mutableStateOf(0)
    var launchEnabled by mutableStateOf(false)
    var launching by mutableStateOf(false)

    var mods by mutableStateOf<List<CaramelModInfo>>(emptyList())
    var servers by mutableStateOf<List<CaramelServerInfo>>(emptyList())
    var cosmetics by mutableStateOf<List<CaramelCosmeticInfo>>(emptyList())
    var news by mutableStateOf<List<CaramelNewsInfo>>(emptyList())

    var launchMenu by mutableStateOf(false)
    var instanceSettings by mutableStateOf(false)
    var accountMenu by mutableStateOf(false)
    var addFriend by mutableStateOf(false)
    var launcherSettings by mutableStateOf(false)
    var appMenu by mutableStateOf(false)
    var screenshots by mutableStateOf(false)
    var friendsAll by mutableStateOf(false)
    var addAccount by mutableStateOf(false)
    var accountSettings by mutableStateOf(false)

    /** The selected mod row, or null when the detail modal is closed. */
    var selectedMod by mutableStateOf<CaramelModInfo?>(null)
}
