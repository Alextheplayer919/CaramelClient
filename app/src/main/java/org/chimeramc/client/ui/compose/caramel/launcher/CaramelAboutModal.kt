package org.chimeramc.client.ui.compose.caramel.launcher

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.chimeramc.client.BuildConfig
import org.chimeramc.client.R
import org.chimeramc.client.launcher.core.about.AboutFacts
import org.chimeramc.client.launcher.core.about.AboutPeople
import org.chimeramc.client.ui.compose.caramel.C
import org.chimeramc.client.ui.compose.caramel.CaramelAvatarPicture
import org.chimeramc.client.ui.compose.caramel.CaramelLauncherState
import org.chimeramc.client.ui.compose.caramel.CaramelModal
import org.chimeramc.client.ui.compose.caramel.GlassButton
import org.chimeramc.client.ui.compose.caramel.GlassPanel
import org.chimeramc.client.ui.compose.caramel.StatusLabel
import org.chimeramc.client.ui.compose.caramel.T
import org.chimeramc.client.ui.compose.caramel.sp_

/**
 * The About popup: who makes the client, its channel, when it started and the stories behind it.
 *
 * <p>The app menu's About item used to open the Accounts modal, so pressing "About" showed the
 * account list. This is the screen it should have shown. Every claim is drawn from a real source —
 * the roster in [AboutPeople] (whose avatars are the contributors' own GitHub pictures), the channel
 * and date in [AboutFacts], the version from the running build — rather than typed into the layout,
 * so the popup cannot drift from the project it describes.
 */
@Composable
fun CaramelAboutModal(state: CaramelLauncherState) {
    CaramelModal(state.aboutOpen, { state.aboutOpen = false }, Modifier.size(720.dp, 680.dp)) {
        Column(Modifier.fillMaxSize()) {
            CaramelModalHeader(
                stringResource(R.string.caramel_menu_about),
                stringResource(R.string.caramel_about_sub),
                { state.aboutOpen = false },
            )
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                CaramelAboutIdentity()
                CaramelAboutLinks(state)
                CaramelAboutPeople()
                CaramelAboutFunFacts()
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

/** The mark, the version and the date the client started. */
@Composable
private fun CaramelAboutIdentity() {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Image(
            painterResource(R.drawable.ic_caramel_logo),
            null,
            Modifier.size(84.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.app_name), style = T.Display.copy(fontSize = 22.sp_))
            if (BuildConfig.VERSION_NAME.isNotEmpty()) {
                StatusLabel(stringResource(R.string.changelog_version, BuildConfig.VERSION_NAME), C.Caramel)
            }
            Text(stringResource(R.string.caramel_about_made_on, AboutFacts.MADE_ON), style = T.Body)
        }
    }
}

/** The channel and repository buttons: both open inside the launcher's own browser. */
@Composable
private fun CaramelAboutLinks(state: CaramelLauncherState) {
    val channelTitle = stringResource(R.string.caramel_about_channel)
    val repositoryTitle = stringResource(R.string.caramel_about_repository)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassButton(
            channelTitle,
            {
                state.webTitle = channelTitle
                state.webUrl = AboutFacts.YOUTUBE_URL
            },
            icon = Icons.Rounded.Public,
            height = 38,
        )
        GlassButton(
            repositoryTitle,
            {
                state.webTitle = repositoryTitle
                state.webUrl = AboutFacts.REPOSITORY_URL
            },
            icon = Icons.Rounded.Code,
            height = 38,
        )
    }
}

/** The roster: each person's own GitHub picture, their badge and their role. */
@Composable
private fun CaramelAboutPeople() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.caramel_about_people_title), style = T.Title)
        for (person in AboutPeople.all()) {
            GlassPanel(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), elevation = 6.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CaramelAvatarPicture(person.avatarUrl, 52)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                person.name,
                                style = T.Subtitle.copy(fontWeight = FontWeight.SemiBold),
                            )
                            StatusLabel(
                                if (person.role == AboutPeople.Role.OWNER) {
                                    stringResource(R.string.about_owner_badge)
                                } else {
                                    stringResource(R.string.about_lead_badge)
                                },
                                if (person.role == AboutPeople.Role.OWNER) C.Caramel else C.Blue,
                            )
                        }
                        Text(
                            if (person.role == AboutPeople.Role.OWNER) {
                                stringResource(R.string.about_owner_role)
                            } else {
                                stringResource(R.string.about_lead_role)
                            },
                            style = T.Body,
                        )
                    }
                }
            }
        }
    }
}

/** The stories: a caption, then one bulleted line each. */
@Composable
private fun CaramelAboutFunFacts() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.caramel_about_fun_facts), style = T.Title)
        for (fact in AboutFacts.funFactResIds()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.padding(top = 7.dp).size(5.dp)
                        .background(C.Caramel, CircleShape),
                )
                Text(stringResource(fact), style = T.Body, modifier = Modifier.weight(1f))
            }
        }
    }
}
