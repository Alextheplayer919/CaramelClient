package org.chimeramc.client.ui.compose.caramel.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.chimeramc.client.R
import org.chimeramc.client.ui.compose.caramel.C
import org.chimeramc.client.ui.compose.caramel.CaramelAvatar
import org.chimeramc.client.ui.compose.caramel.CaramelLauncherState
import org.chimeramc.client.ui.compose.caramel.CaramelModal
import org.chimeramc.client.ui.compose.caramel.CaramelProfileDropdown
import org.chimeramc.client.ui.compose.caramel.CaramelScreenshotInfo
import org.chimeramc.client.ui.compose.caramel.CaramelTab
import org.chimeramc.client.ui.compose.caramel.CaramelTextField
import org.chimeramc.client.ui.compose.caramel.CaramelToggle
import org.chimeramc.client.ui.compose.caramel.CaramelVersionDropdown
import org.chimeramc.client.ui.compose.caramel.FilterChip
import org.chimeramc.client.ui.compose.caramel.GlassButton
import org.chimeramc.client.ui.compose.caramel.GlassPanel
import org.chimeramc.client.ui.compose.caramel.LoaderTile
import org.chimeramc.client.ui.compose.caramel.PrimaryButton
import org.chimeramc.client.ui.compose.caramel.SearchField
import org.chimeramc.client.ui.compose.caramel.SettingRow
import org.chimeramc.client.ui.compose.caramel.SliderRow
import org.chimeramc.client.ui.compose.caramel.StatusLabel
import org.chimeramc.client.ui.compose.caramel.T
import org.chimeramc.client.ui.compose.caramel.Tone
import org.chimeramc.client.ui.compose.caramel.sp_

/**
 * The launcher's modals and popups, ported from the pack's `launcher/Modals.kt`, `Popups.kt` and
 * `InstanceSettings.kt`.
 *
 * <p>The pack fills these with example rows. Here every list, name and count comes from
 * [CaramelLauncherState], which the activity fills from the live managers, and every action calls
 * back into the activity through [CaramelLauncherHost.Actions]. Where the pack drew a value the
 * client cannot measure (a ping, a "2 days ago"), the row is left out rather than invented.
 */

/** The Caramel Client app menu: the dropdown by the title bar's arrow. Quit lives here. */
@Composable
fun CaramelAppMenu(state: CaramelLauncherState, actions: CaramelLauncherHost.Actions) {
    if (!state.appMenu) return
    CaramelPopup(Alignment.TopStart, 14, 42, onDismiss = { state.appMenu = false }) {
        GlassPanel(Modifier.width(262.dp), RoundedCornerShape(18.dp), popup = true, elevation = 36.dp) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CaramelMenuItem(stringResource(R.string.caramel_menu_settings), Icons.Rounded.Settings, true) {
                    state.appMenu = false; actions.openSettings()
                }
                CaramelMenuItem(stringResource(R.string.caramel_menu_instance_settings), Icons.Rounded.Tune) {
                    state.appMenu = false; state.instanceSettings = true
                }
                CaramelMenuItem(stringResource(R.string.caramel_menu_release_notes), Icons.Rounded.Article) {
                    state.appMenu = false; state.tab = CaramelTab.News
                }
                CaramelMenuItem(stringResource(R.string.caramel_menu_about), Icons.Rounded.Info) {
                    state.appMenu = false; actions.openAccounts()
                }
                Box(Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 6.dp).height(1.dp).background(Color.White.copy(0.1f)))
                CaramelMenuItem(stringResource(R.string.caramel_menu_quit), Icons.Rounded.Close, danger = true) {
                    state.appMenu = false; actions.quit()
                }
            }
        }
    }
}

/** The account menu under the title-bar pill: the active account and its actions. */
@Composable
fun CaramelAccountMenu(state: CaramelLauncherState, actions: CaramelLauncherHost.Actions) {
    if (!state.accountMenu) return
    CaramelPopup(Alignment.TopEnd, -150, 44, onDismiss = { state.accountMenu = false }) {
        GlassPanel(Modifier.width(238.dp), RoundedCornerShape(20.dp), popup = true, elevation = 36.dp) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    Modifier.padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CaramelAvatar(44, online = state.signedIn)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (state.signedIn) state.accountName else stringResource(R.string.not_signed_in),
                            style = T.Subtitle,
                        )
                        if (state.signedIn) StatusLabel(stringResource(R.string.caramel_account_online), C.Green)
                    }
                }
                if (state.signedIn) {
                    CaramelMenuItem(stringResource(R.string.caramel_skin_cosmetics), Icons.Rounded.ViewInAr) {
                        state.accountMenu = false; state.tab = CaramelTab.Cosmetics
                    }
                    CaramelMenuItem(stringResource(R.string.caramel_account_settings_title), Icons.Rounded.Settings) {
                        state.accountMenu = false; state.accountSettings = true
                    }
                    CaramelMenuItem(stringResource(R.string.caramel_sign_out), Icons.Rounded.SwapHoriz, danger = true) {
                        state.accountMenu = false; actions.signOut()
                    }
                } else {
                    CaramelMenuItem(stringResource(R.string.caramel_add_account), Icons.Rounded.Add) {
                        state.accountMenu = false; state.addAccount = true
                    }
                    CaramelMenuItem(stringResource(R.string.caramel_menu_instance_settings), Icons.Rounded.Tune) {
                        state.accountMenu = false; state.instanceSettings = true
                    }
                }
            }
        }
    }
}

/** The Screenshots modal: a grid over the instance's real screenshot folder. */
@Composable
fun CaramelScreenshotsModal(state: CaramelLauncherState, actions: CaramelLauncherHost.Actions) {
    CaramelModal(state.screenshotsModal, { state.screenshotsModal = false }, Modifier.size(980.dp, 700.dp)) {
        var chip by remember { mutableStateOf(0) }
        val shots = state.screenshots
        Column(Modifier.fillMaxSize()) {
            CaramelModalHeader(
                stringResource(R.string.caramel_screenshots_title),
                stringResource(R.string.caramel_screenshots_sub, shots.size, state.profile.title),
                { state.screenshotsModal = false },
            ) {
                GlassButton(stringResource(R.string.caramel_open_folder), actions::openScreenshots, icon = Icons.Rounded.Folder, height = 32)
            }
            Row(Modifier.padding(start = 32.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(R.string.caramel_filter_all, R.string.caramel_filter_today, R.string.caramel_filter_this_week)
                    .forEachIndexed { i, res -> FilterChip(stringResource(res), chip == i, { chip = i }) }
            }
            if (shots.isEmpty()) {
                EmptyState(
                    stringResource(R.string.caramel_no_screenshots_title),
                    stringResource(R.string.caramel_no_screenshots_body),
                    Modifier.fillMaxWidth().weight(1f).padding(horizontal = 32.dp),
                )
            } else {
                val tints = listOf(C.Blue, C.Caramel, C.Green, Color(0xFF966EFF), C.Red, Color(0xFF4AC8E6))
                LazyVerticalGrid(
                    GridCells.Fixed(4),
                    Modifier.weight(1f).padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(shots, key = { it.path }) { s ->
                        val selected = state.selectedScreenshot?.path == s.path
                        Column(
                            Modifier.clickable(remember { MutableInteractionSource() }, null) { state.selectedScreenshot = s },
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            val tint = tints[(s.name.hashCode() and 0x7fffffff) % 6]
                            Box(
                                Modifier.fillMaxWidth().height(118.dp).clip(RoundedCornerShape(14.dp))
                                    .background(Brush.verticalGradient(listOf(tint.copy(0.55f), C.Navy)))
                                    .border(if (selected) 2.dp else 1.dp, if (selected) C.Blue else Color.White.copy(0.16f), RoundedCornerShape(14.dp)),
                            )
                            Text(s.name, style = T.Caption, maxLines = 1)
                        }
                    }
                }
            }
            CaramelRule()
            Row(
                Modifier.fillMaxWidth().padding(24.dp, 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val sel = state.selectedScreenshot
                Text(
                    stringResource(R.string.caramel_selected_count, if (sel != null) 1 else 0),
                    style = T.Label,
                    modifier = Modifier.weight(1f),
                )
                GlassButton(stringResource(R.string.caramel_open_folder), actions::openScreenshots, icon = Icons.Rounded.Folder)
                GlassButton(
                    stringResource(R.string.delete),
                    { sel?.let(actions::deleteScreenshot) },
                    icon = Icons.Rounded.Delete,
                    danger = true,
                )
            }
        }
    }
}

/**
 * The "all news" modal.
 *
 * <p>"View all news" used to be a label with no click, so the press did nothing. It now opens this
 * list of every changelog entry the Home row truncates to three; an unreadable feed leaves the list
 * empty and the modal shows its empty state rather than a fabricated post.
 */
@Composable
fun CaramelNewsModal(state: CaramelLauncherState, actions: CaramelLauncherHost.Actions) {
    CaramelModal(state.newsAll, { state.newsAll = false }, Modifier.size(760.dp, 700.dp)) {
        val posts = state.news
        Column(Modifier.fillMaxSize()) {
            CaramelModalHeader(
                stringResource(R.string.caramel_news_title),
                stringResource(R.string.caramel_news_sub),
                { state.newsAll = false },
            ) {
                GlassButton(
                    stringResource(R.string.caramel_view_all),
                    { state.newsAll = false; state.tab = CaramelTab.News },
                    icon = Icons.Rounded.Article,
                    height = 32,
                )
            }
            if (posts.isEmpty()) {
                EmptyState(
                    stringResource(R.string.caramel_no_news_title),
                    stringResource(R.string.caramel_no_news_body),
                    Modifier.fillMaxWidth().weight(1f).padding(horizontal = 32.dp),
                )
            } else {
                val tints = listOf(C.Caramel, C.Blue, C.Green, Color(0xFF966EFF), C.Red, Color(0xFF4AC8E6))
                LazyColumn(
                    Modifier.weight(1f).padding(horizontal = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(posts, key = { it.title + it.meta }) { post ->
                        val tint = tints[(post.title.hashCode() and 0x7fffffff) % tints.size]
                        GlassPanel(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), elevation = 6.dp) {
                            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Box(
                                    Modifier.size(150.dp, 106.dp).background(
                                        Brush.verticalGradient(listOf(tint.copy(0.5f), tint.copy(0.1f))),
                                        RoundedCornerShape(12.dp),
                                    ),
                                )
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (post.featured) StatusLabel(stringResource(R.string.caramel_featured), C.Caramel)
                                    Text(post.title, style = T.Subtitle.copy(fontSize = 15.sp_), maxLines = 2)
                                    Text(post.summary, style = T.Body, maxLines = 3)
                                    Text(post.meta, style = T.Caption)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The Friends modal: the active account's real Xbox Live friends, loaded by the activity. */
@Composable
fun CaramelFriendsModal(state: CaramelLauncherState, actions: CaramelLauncherHost.Actions) {
    CaramelModal(state.friendsAll, { state.friendsAll = false }, Modifier.size(620.dp, 700.dp)) {
        var q by remember { mutableStateOf("") }
        Column(Modifier.fillMaxSize()) {
            CaramelModalHeader(
                stringResource(R.string.caramel_friends),
                stringResource(R.string.caramel_friends_sub),
                { state.friendsAll = false },
            ) {
                PrimaryButton(
                    stringResource(R.string.caramel_add_friend),
                    { state.friendsAll = false; state.addFriend = true },
                    Modifier.width(146.dp),
                    Tone.Blue,
                    Icons.Rounded.Add,
                    34,
                )
            }
            SearchField(q, { q = it }, stringResource(R.string.caramel_search_friends), Modifier.fillMaxWidth().padding(32.dp, 14.dp))
            if (!state.signedIn) {
                EmptyState(
                    stringResource(R.string.caramel_no_friends_title),
                    stringResource(R.string.caramel_no_friends_body),
                    Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp),
                )
            } else {
                LazyColumn(Modifier.weight(1f).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Text(
                            stringResource(R.string.caramel_no_friends_body),
                            style = T.Body,
                            modifier = Modifier.padding(start = 8.dp, top = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * The Microsoft sign-in modal.
 *
 * <p>It carries the whole device-code flow: the button starts it, the code and the pre-filled link
 * appear as soon as Microsoft issues them, and a failure shows why instead of closing silently.
 * Before this the modal only ever showed a code the flow never had time to hand over, so the button
 * looked dead and the modal closed on its own.
 */
@Composable
fun CaramelAddAccountModal(state: CaramelLauncherState, actions: CaramelLauncherHost.Actions) {
    CaramelModal(state.addAccount, { state.addAccount = false }, Modifier.size(460.dp, 560.dp)) {
        Column(Modifier.fillMaxSize()) {
            CaramelModalHeader(
                stringResource(R.string.caramel_add_account_title),
                stringResource(R.string.caramel_add_account_sub),
                { state.addAccount = false },
            )
            Column(
                Modifier.weight(1f).padding(horizontal = 32.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp))
                        .background(if (state.signInRunning) Color.White.copy(0.6f) else Color.White)
                        .clickable(
                            remember { MutableInteractionSource() }, null,
                            enabled = !state.signInRunning,
                            onClick = actions::startSignIn,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        if (state.signInRunning) stringResource(R.string.caramel_sign_in_starting)
                        else stringResource(R.string.caramel_sign_in_microsoft),
                        style = T.Label.copy(color = Color(0xFF1C2446), fontSize = 14.5.sp_),
                    )
                }
                Text(
                    stringResource(R.string.caramel_or_device_code),
                    style = T.Caption,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0x78040A1A))
                        .border(1.dp, Color.White.copy(0.16f), RoundedCornerShape(18.dp)).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val code = state.deviceCode.ifEmpty { "\u2014\u2014\u2014\u2014" }
                    Text(code, style = T.Display.copy(fontSize = 30.sp_, letterSpacing = 3.sp_))
                    if (state.busyMessage.isNotEmpty()) Text(state.busyMessage, style = T.Body)
                    if (state.deviceCode.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GlassButton(
                                stringResource(R.string.ms_device_code_open_browser),
                                actions::openDeviceCodePage,
                                icon = Icons.Rounded.Public,
                                height = 34,
                            )
                            GlassButton(
                                stringResource(R.string.ms_device_code_copy),
                                actions::copyDeviceCode,
                                icon = Icons.Rounded.ContentCopy,
                                height = 34,
                            )
                        }
                    }
                }
                if (state.signInError.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(C.Red.copy(0.14f))
                            .border(1.dp, C.Red.copy(0.45f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Rounded.ErrorOutline, null, Modifier.size(16.dp), C.Red)
                        Text(
                            stringResource(R.string.ms_login_failed_detail, state.signInError),
                            style = T.Body.copy(color = C.Red),
                        )
                    }
                }
                Text(
                    stringResource(R.string.caramel_device_code_hint),
                    style = T.Body.copy(fontSize = 12.5.sp_, textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            CaramelRule()
            Row(Modifier.fillMaxWidth().padding(24.dp, 18.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassButton(stringResource(R.string.cancel), { state.addAccount = false })
                Spacer(Modifier.weight(1f))
                Text(
                    stringResource(
                        if (state.signInRunning) R.string.caramel_waiting_sign_in
                        else R.string.caramel_sign_in_idle
                    ),
                    style = T.Body,
                )
            }
        }
    }
}

/** The account settings modal: privacy toggles over the signed-in account. */
@Composable
fun CaramelAccountSettingsModal(state: CaramelLauncherState, actions: CaramelLauncherHost.Actions) {
    CaramelModal(state.accountSettings, { state.accountSettings = false }, Modifier.size(720.dp, 680.dp)) {
        var status by remember { mutableStateOf(true) }
        var requests by remember { mutableStateOf(true) }
        var activity by remember { mutableStateOf(false) }
        Column(Modifier.fillMaxSize()) {
            CaramelModalHeader(
                stringResource(R.string.caramel_account_settings_title),
                null,
                { state.accountSettings = false },
            )
            CaramelRule()
            Column(Modifier.weight(1f).padding(32.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    CaramelAvatar(88)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (state.signedIn) state.accountName else stringResource(R.string.not_signed_in),
                            style = T.Display.copy(fontSize = 19.sp_),
                        )
                        if (state.signedIn) StatusLabel(stringResource(R.string.caramel_microsoft_linked), C.Green)
                    }
                    GlassButton(stringResource(R.string.caramel_skin_cosmetics), { state.tab = CaramelTab.Cosmetics; state.accountSettings = false }, icon = Icons.Rounded.Public)
                }
                Text(stringResource(R.string.caramel_privacy), style = T.Subtitle.copy(color = C.Text2))
                SettingRow(stringResource(R.string.caramel_show_online_status), stringResource(R.string.caramel_show_online_status_desc)) {
                    CaramelToggle(status, { status = it })
                }
                SettingRow(stringResource(R.string.caramel_allow_requests), stringResource(R.string.caramel_allow_requests_desc)) {
                    CaramelToggle(requests, { requests = it })
                }
                SettingRow(stringResource(R.string.caramel_share_activity), stringResource(R.string.caramel_share_activity_desc)) {
                    CaramelToggle(activity, { activity = it })
                }
            }
            CaramelRule()
            Row(Modifier.fillMaxWidth().padding(24.dp, 18.dp)) {
                GlassButton(stringResource(R.string.caramel_sign_out), actions::signOut, icon = Icons.Rounded.SwapHoriz, danger = true)
                Spacer(Modifier.weight(1f))
                PrimaryButton(stringResource(R.string.save), { state.accountSettings = false }, Modifier.width(148.dp), height = 40)
            }
        }
    }
}

/** The add-friend modal. Xbox Live has no client-side add, so the request is a link-out. */
@Composable
fun CaramelAddFriendModal(state: CaramelLauncherState) {
    CaramelModal(state.addFriend, { state.addFriend = false }, Modifier.size(400.dp, 300.dp)) {
        var user by remember { mutableStateOf("") }
        Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.caramel_add_friend_title), style = T.Title.copy(fontSize = 18.sp_))
                    Text(stringResource(R.string.caramel_add_friend_sub), style = T.Body)
                }
                GlassButton(null, { state.addFriend = false }, icon = Icons.Rounded.Close, height = 32)
            }
            CaramelTextField(user, { user = it }, Modifier.fillMaxWidth(), stringResource(R.string.caramel_username), Icons.Rounded.Person)
            Row(
                Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(12.dp)).background(C.Green.copy(0.11f))
                    .border(1.dp, C.Green.copy(0.4f), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Rounded.Check, null, Modifier.size(15.dp), C.Green)
                Text(
                    stringResource(R.string.caramel_friend_found, user),
                    style = T.Label.copy(fontWeight = FontWeight.Medium, fontSize = 12.5.sp_),
                )
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                GlassButton(stringResource(R.string.cancel), { state.addFriend = false }, Modifier.width(140.dp), height = 38)
                PrimaryButton(stringResource(R.string.caramel_send_request), { state.addFriend = false }, Modifier.weight(1f), height = 38)
            }
        }
    }
}

/** The launcher settings panel: the real preferences the activity reads back. */
@Composable
fun CaramelLauncherSettingsPanel(state: CaramelLauncherState, actions: CaramelLauncherHost.Actions, close: () -> Unit) {
    var reduceMotion by remember { mutableStateOf(false) }
    var checkUpdates by remember { mutableStateOf(true) }
    var blur by remember { mutableStateOf(0.6f) }
    var scale by remember { mutableStateOf(0.4f) }
    var accent by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.caramel_launcher_settings_title), style = T.Display, modifier = Modifier.weight(1f))
            GlassButton(null, close, icon = Icons.Rounded.Close, height = 32)
        }
        Text(stringResource(R.string.caramel_appearance), style = T.Title)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            listOf(C.Caramel, C.Blue, C.Green, Color(0xFF966EFF), C.Red, Color(0xFF4AC8E6)).forEachIndexed { i, col ->
                Box(
                    Modifier.size(34.dp).background(col, CircleShape)
                        .border(2.dp, if (i == accent) Color.White else Color.White.copy(0.15f), CircleShape)
                        .clickable(remember { MutableInteractionSource() }, null) { accent = i },
                )
            }
        }
        SliderRow(stringResource(R.string.caramel_glass_blur), "${(blur * 40).toInt()}", blur, { blur = it })
        SliderRow(stringResource(R.string.caramel_interface_scale), "${(80 + scale * 50).toInt()}%", scale, { scale = it })
        SettingRow(stringResource(R.string.caramel_reduce_motion), stringResource(R.string.caramel_reduce_motion_desc)) {
            CaramelToggle(reduceMotion, { reduceMotion = it })
        }
        SettingRow(stringResource(R.string.caramel_menu_check_updates), stringResource(R.string.caramel_check_updates_desc)) {
            CaramelToggle(checkUpdates, { checkUpdates = it })
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton(stringResource(R.string.caramel_menu_check_updates), actions::openSettings, icon = Icons.Rounded.RocketLaunch)
            Spacer(Modifier.weight(1f))
            PrimaryButton(stringResource(R.string.save), close, Modifier.width(140.dp), height = 40)
        }
    }
}

/** The instance settings modal: a section rail over the real instance's options. */
@Composable
fun CaramelInstanceSettingsModal(state: CaramelLauncherState, actions: CaramelLauncherHost.Actions) {
    CaramelModal(state.instanceSettings, { state.instanceSettings = false }, Modifier.size(900.dp, 670.dp)) {
        var section by remember { mutableStateOf(0) }
        var name by remember(state.instanceName) { mutableStateOf(state.instanceName) }
        val sections = listOf(
            R.string.caramel_section_general to Icons.Rounded.Tune,
            R.string.caramel_section_game to Icons.Rounded.SportsEsports,
            R.string.caramel_section_performance to Icons.Rounded.Bolt,
            R.string.caramel_section_mods to Icons.Rounded.Extension,
            R.string.caramel_section_advanced to Icons.Rounded.Settings,
        )
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 32.dp, end = 24.dp, top = 24.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.caramel_menu_instance_settings), style = T.Display.copy(fontSize = 20.sp_, fontWeight = FontWeight.Medium))
                    Text(
                        stringResource(R.string.caramel_instance_settings_sub, state.instanceName, state.profile.title),
                        style = T.Body,
                    )
                }
                GlassButton(null, { state.instanceSettings = false }, icon = Icons.Rounded.Close, height = 32)
            }
            CaramelRule()
            Row(Modifier.weight(1f)) {
                Column(Modifier.width(216.dp).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sections.forEachIndexed { i, (titleRes, icon) ->
                        CaramelSideItem(stringResource(titleRes), icon, i == section) { section = i }
                    }
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(0.08f)))
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    when (section) {
                        0 -> {
                            CaramelField(stringResource(R.string.caramel_instance_name)) {
                                CaramelTextField(name, { name = it }, Modifier.fillMaxWidth())
                            }
                            SettingRow(stringResource(R.string.caramel_game_version)) {
                                CaramelVersionDropdown(state.versions, state.version, onSelect = { state.version = it })
                            }
                            SettingRow(stringResource(R.string.caramel_loader)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    org.chimeramc.client.ui.compose.caramel.CaramelLoader.entries.forEach {
                                        LoaderTile(it, state.loader == it, { state.loader = it })
                                    }
                                }
                            }
                            SettingRow(stringResource(R.string.caramel_profile_mode), stringResource(R.string.caramel_profile_open_desc)) {
                                CaramelProfileDropdown(state.profile, { state.profile = it }, width = 240)
                            }
                        }
                        3 -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                GlassButton(stringResource(R.string.caramel_open_mods_folder), actions::openModsFolder, icon = Icons.Rounded.Folder)
                            }
                            if (state.mods.isEmpty()) {
                                Text(stringResource(R.string.caramel_no_mods_title), style = T.Body)
                            } else {
                                state.mods.take(8).forEach { m ->
                                    Row(
                                        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Color.White.copy(0.06f))
                                            .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(13.dp)).padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Box(Modifier.size(28.dp).background(C.Blue.copy(0.25f), RoundedCornerShape(8.dp)), Alignment.Center) {
                                            Icon(Icons.Rounded.Extension, null, Modifier.size(15.dp), C.Blue)
                                        }
                                        Column(Modifier.weight(1f)) {
                                            Text(m.name, style = T.Label)
                                            Text(stringResource(R.string.caramel_mod_version, m.version), style = T.Caption)
                                        }
                                        CaramelToggle(m.enabled, { actions.toggleMod(m, it) })
                                    }
                                }
                            }
                        }
                        else -> {
                            Text(stringResource(R.string.caramel_section_advanced), style = T.Title)
                            Text(stringResource(R.string.caramel_verbose_logging_desc), style = T.Body)
                        }
                    }
                }
            }
            CaramelRule()
            Row(
                Modifier.fillMaxWidth().padding(24.dp, 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassButton(stringResource(R.string.caramel_open_folder), actions::openInstanceFolder, icon = Icons.Rounded.Folder)
                if (state.instanceEditable) {
                    GlassButton(stringResource(R.string.caramel_repair), actions::repairInstance, icon = Icons.Rounded.Build)
                    GlassButton(stringResource(R.string.delete), actions::deleteInstance, icon = Icons.Rounded.Delete, danger = true)
                }
                Spacer(Modifier.weight(1f))
                PrimaryButton(stringResource(R.string.caramel_save_changes), {
                    if (name.isNotBlank() && name != state.instanceName) actions.renameInstance(name)
                    state.instanceSettings = false
                }, Modifier.width(148.dp), height = 40)
            }
        }
    }
}

/** A glass popup anchored at [x]/[y], from the pack's `Popup` helper. */
@Composable
private fun CaramelPopup(
    alignment: Alignment,
    x: Int,
    y: Int,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    androidx.compose.ui.window.Popup(
        alignment = alignment,
        offset = androidx.compose.ui.unit.IntOffset(x, y),
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.PopupProperties(focusable = true),
        content = { content() },
    )
}
