package org.chimeramc.client.ui.compose.caramel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.chimeramc.client.R

/**
 * The launcher's settings, as one screen and one modal body.
 *
 * <p>The screen the settings tab opened was a placeholder that drew nothing, and the modal the
 * launcher's own gear opened held every control in `remember { mutableStateOf(...) }` — moving a
 * slider changed a number on screen and wrote nothing. Both surfaces now render this panel, and
 * every control calls back into a real store: [PersonalizationManager] for the look,
 * [org.chimeramc.client.util.ThemeManager] for the night mode, [LanguageManager] for the locale and
 * [org.chimeramc.client.settings.FeatureSettings] (through [org.chimeramc.client.launcher.settings.SettingsRegistry])
 * for the client switches.
 *
 * <p>The state lives in [CaramelSettingsState] and is owned by the activity, not the composition, so
 * a value the activity changed (a font the user picked) is the value the panel draws after a
 * recomposition rather than a local copy that drifts.
 */

/** The launcher's persisted settings, as the panel draws them. */
class CaramelSettingsState {
    /** One of [org.chimeramc.client.util.ThemeManager.MODE_FOLLOW_SYSTEM] / LIGHT / DARK. */
    var themeMode by mutableStateOf(0)
    var dynamicColor by mutableStateOf(false)
    /** The persisted font key; the label comes from [org.chimeramc.client.util.LauncherFonts]. */
    var fontFamilyKey by mutableStateOf("")
    var fontScale by mutableStateOf(1f)
    /** Card corner rounding, in dp (the store keeps the same unit and converts for pixels). */
    var cardRounding by mutableStateOf(16)
    var iconSize by mutableStateOf(100)
    var reduceMotion by mutableStateOf(false)
    var haptics by mutableStateOf(true)
    var blurIntensity by mutableStateOf(50)
    /** The persisted BCP-47 language tag. */
    var languageTag by mutableStateOf("")
    /**
     * Every client switch, in registry order, with its translated label and its live value.
     *
     * <p>The labels are resolved by the activity that owns the registry, because the registry holds
     * resource ids and the panel would otherwise need a second copy of the id-to-label mapping.
     */
    var flags by mutableStateOf<List<CaramelSettingFlag>>(emptyList())
    /** True when a newer release has been seen, so the Updates section can say so. */
    var updateAvailable by mutableStateOf(false)
    /** The installed version name, shown in About. */
    var versionName by mutableStateOf("")
    /** A status line for a long action (an update check); empty when idle. */
    var busyMessage by mutableStateOf("")
}

/** One client switch, as the panel draws it: its id, its label, its description and its value. */
class CaramelSettingFlag(
    val id: String,
    val title: String,
    val desc: String,
    val value: Boolean,
)

/** Everything the settings panel can change. */
interface CaramelSettingsActions {
    fun setThemeMode(mode: Int)
    fun setDynamicColor(enabled: Boolean)
    fun setFontFamilyKey(key: String)
    fun setFontScale(scale: Float)
    fun setCardRounding(dp: Int)
    fun setIconSize(percent: Int)
    fun setReduceMotion(reduce: Boolean)
    fun setHaptics(enabled: Boolean)
    fun setBlurIntensity(percent: Int)
    fun setLanguageTag(tag: String)
    fun setFlag(id: String, value: Boolean)
    fun checkForUpdates()
    fun openDiagnostics()
    fun openChangelog()
    fun resetSettings()
}

/** The language choices, in the order the picker shows them. */
private val languages = listOf(
    "en" to R.string.english,
    "zh-CN" to R.string.chinese,
    "fr" to R.string.french,
    "hi" to R.string.hindi,
    "id" to R.string.indonesian,
    "ja" to R.string.japanese,
    "pt" to R.string.portuguese,
    "ru" to R.string.russian,
    "es" to R.string.spanish,
    "tr" to R.string.turkish,
    "vi" to R.string.vietnamese,
)

/** A switch on the panel: its label, its description, its value and the setter that persists it. */
private class SwitchSpec(
    val title: String,
    val desc: String,
    val value: Boolean,
    val set: (Boolean) -> Unit,
)

/** One settings row: a title, its description and a trailing control. */
@Composable
private fun ToggleRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    SettingRow(title, desc) { CaramelToggle(checked, onChange) }
}

@Composable
private fun SectionHeader(title: String) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Text(title, style = T.Title)
        Spacer(Modifier.height(2.dp))
    }
}

/** The font keys and their translated labels, in catalogue order. */
@Composable
private fun fontCatalogue(): Pair<List<String>, List<String>> {
    val context = LocalContext.current
    return remember(context) {
        val keys = mutableListOf<String>()
        val labels = mutableListOf<String>()
        for (entry in org.chimeramc.client.util.LauncherFonts.entries()) {
            keys.add(entry.key)
            labels.add(context.getString(entry.labelRes))
        }
        keys to labels
    }
}

/**
 * The settings body, shared by the full-screen [CaramelSettingsScreen] and the launcher's modal.
 */
@Composable
fun CaramelSettingsPanel(
    state: CaramelSettingsState,
    actions: CaramelSettingsActions,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
) {
    val (fontKeys, fontLabels) = fontCatalogue()
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()

    fun matches(title: String, desc: String): Boolean =
        q.isEmpty() || title.lowercase().contains(q) || desc.lowercase().contains(q)

    val body: @Composable () -> Unit = {
        val general = listOf(
            SwitchSpec(
                stringResource(R.string.reduce_motion),
                stringResource(R.string.reduce_motion_desc),
                state.reduceMotion,
                { v -> actions.setReduceMotion(v) },
            ),
            SwitchSpec(
                stringResource(R.string.haptic_feedback),
                stringResource(R.string.haptic_feedback_desc),
                state.haptics,
                { v -> actions.setHaptics(v) },
            ),
        )

        val themeLabels = listOf(
            stringResource(R.string.theme_follow_system),
            stringResource(R.string.theme_light),
            stringResource(R.string.theme_dark),
        )

        SectionHeader(stringResource(R.string.caramel_section_general))
        general.forEach { spec ->
            if (matches(spec.title, spec.desc)) ToggleRow(spec.title, spec.desc, spec.value, spec.set)
        }

        SectionHeader(stringResource(R.string.caramel_appearance))
        if (matches(stringResource(R.string.theme_title), stringResource(R.string.theme_title))) {
            SettingRow(stringResource(R.string.theme_title), null) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    themeLabels.forEachIndexed { i, label ->
                        FilterChip(label, state.themeMode == i, { actions.setThemeMode(i) })
                    }
                }
            }
        }
        if (matches(stringResource(R.string.dynamic_color), stringResource(R.string.dynamic_color_desc))) {
            ToggleRow(
                stringResource(R.string.dynamic_color),
                stringResource(R.string.dynamic_color_desc),
                state.dynamicColor,
            ) { actions.setDynamicColor(it) }
        }
        if (matches(stringResource(R.string.font_family), stringResource(R.string.font_family))) {
            SettingRow(stringResource(R.string.font_family), null) {
                CaramelSimpleDropdown(
                    state.fontFamilyKey,
                    fontLabels,
                    { label ->
                        val index = fontLabels.indexOf(label)
                        if (index >= 0 && index < fontKeys.size) actions.setFontFamilyKey(fontKeys[index])
                    },
                    width = 200,
                )
            }
        }
        if (matches(stringResource(R.string.font_scale), stringResource(R.string.font_scale))) {
            SliderRow(
                stringResource(R.string.font_scale),
                stringResource(R.string.font_scale_value, state.fontScale),
                (state.fontScale - 0.8f) / 0.7f,
                { actions.setFontScale(0.8f + it * 0.7f) },
            )
        }
        if (matches(stringResource(R.string.card_rounding), stringResource(R.string.card_rounding))) {
            SliderRow(
                stringResource(R.string.card_rounding),
                stringResource(R.string.card_rounding_value, state.cardRounding),
                state.cardRounding / 32f,
                { actions.setCardRounding((it * 32f).toInt()) },
            )
        }
        if (matches(stringResource(R.string.icon_size), stringResource(R.string.icon_size))) {
            SliderRow(
                stringResource(R.string.icon_size),
                stringResource(R.string.icon_size_value, state.iconSize),
                (state.iconSize - 50) / 100f,
                { actions.setIconSize((50 + it * 100f).toInt()) },
            )
        }
        if (matches(stringResource(R.string.blur_intensity), stringResource(R.string.blur_intensity))) {
            SliderRow(
                stringResource(R.string.blur_intensity),
                stringResource(R.string.blur_intensity_value, state.blurIntensity),
                state.blurIntensity / 100f,
                { actions.setBlurIntensity((it * 100f).toInt()) },
            )
        }

        val languageTitle = stringResource(R.string.language)
        if (matches(languageTitle, languageTitle)) {
            SectionHeader(languageTitle)
            SettingRow(languageTitle, null) {
                val labels = languages.map { stringResource(it.second) }
                val current = languages.firstOrNull { it.first == state.languageTag }?.let {
                    stringResource(it.second)
                } ?: labels.first()
                CaramelSimpleDropdown(current, labels, { label ->
                    val index = labels.indexOf(label)
                    if (index >= 0) actions.setLanguageTag(languages[index].first)
                }, width = 200)
            }
        }

        val clientTitle = stringResource(R.string.caramel_section_client)
        val visibleFlags = state.flags.filter { matches(it.title, it.desc) }
        if (visibleFlags.isNotEmpty()) {
            SectionHeader(clientTitle)
            visibleFlags.forEach { flag ->
                ToggleRow(flag.title, flag.desc, flag.value) { actions.setFlag(flag.id, it) }
            }
        }

        SectionHeader(stringResource(R.string.settings_tab_updates))
        if (matches(stringResource(R.string.caramel_menu_check_updates), stringResource(R.string.caramel_check_updates_desc))) {
            SettingRow(
                stringResource(R.string.caramel_menu_check_updates),
                stringResource(
                    if (state.updateAvailable) R.string.caramel_update_available
                    else R.string.caramel_check_updates_desc
                ),
            ) {
                GlassButton(
                    stringResource(R.string.check_update),
                    actions::checkForUpdates,
                    icon = Icons.Rounded.RocketLaunch,
                    height = 34,
                )
            }
        }

        SectionHeader(stringResource(R.string.settings_tab_about))
        if (matches(stringResource(R.string.caramel_version), stringResource(R.string.caramel_version))) {
            SettingRow(stringResource(R.string.caramel_version), null) {
                Text(state.versionName, style = T.Label.copy(color = C.Caramel, fontWeight = FontWeight.Medium))
            }
        }
        if (matches(stringResource(R.string.settings_diagnostics), stringResource(R.string.settings_diagnostics_desc))) {
            SettingRow(
                stringResource(R.string.settings_diagnostics),
                stringResource(R.string.settings_diagnostics_desc),
            ) {
                GlassButton(stringResource(R.string.settings_diagnostics_open), actions::openDiagnostics, icon = Icons.Rounded.Build, height = 34)
            }
        }
        if (matches(stringResource(R.string.settings_open_changelog), stringResource(R.string.settings_open_changelog))) {
            SettingRow(stringResource(R.string.settings_open_changelog), null) {
                GlassButton(stringResource(R.string.settings_open_changelog), actions::openChangelog, icon = Icons.Rounded.Article, height = 34)
            }
        }
        SettingRow(stringResource(R.string.settings_reset), stringResource(R.string.settings_reset_desc)) {
            GlassButton(
                stringResource(R.string.reset),
                actions::resetSettings,
                icon = Icons.Rounded.Refresh,
                danger = true,
                height = 34,
            )
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SearchField(query, { query = it }, stringResource(R.string.caramel_search), Modifier.fillMaxWidth())
        if (state.busyMessage.isNotEmpty()) {
            Text(state.busyMessage, style = T.Body.copy(color = C.Caramel))
        }
        if (scrollable) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) { body() }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) { body() }
        }
    }
}

/** The full-screen settings surface, mounted by `SettingsActivity`. */
@Composable
fun CaramelSettingsScreen(
    state: CaramelSettingsState,
    actions: CaramelSettingsActions,
    onClose: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_title), style = T.Display)
                Text(stringResource(R.string.caramel_settings_sub), style = T.Body)
            }
            GlassButton(null, onClose, icon = Icons.Rounded.Close, height = 34)
        }
        CaramelSettingsPanel(state, actions, Modifier.weight(1f))
    }
}

/**
 * The settings surface a host hands to the panel.
 *
 * <p>The launcher's gear and the full-screen settings activity both need a state and an action
 * receiver, and the action receiver is a Java class, so the pair travels as one object rather than
 * as two nullable parameters that could disagree.
 */
interface CaramelSettingsProvider {
    fun settingsState(): CaramelSettingsState
    fun settingsActions(): CaramelSettingsActions
}

/** The Java-facing mount for the settings screen, mirroring [CaramelLauncherHost]. */
object CaramelSettingsHost {
    @JvmStatic
    fun state(): CaramelSettingsState = CaramelSettingsState()

    @JvmStatic
    fun setContent(
        composeView: ComposeView,
        state: CaramelSettingsState,
        actions: CaramelSettingsActions,
        onClose: Runnable,
    ) {
        composeView.setContent {
            CaramelTheme {
                CaramelSettingsScreen(state, actions) { onClose.run() }
            }
        }
    }
}

