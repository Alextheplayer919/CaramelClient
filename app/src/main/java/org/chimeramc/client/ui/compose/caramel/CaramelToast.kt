package org.chimeramc.client.ui.compose.caramel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * The launcher's toast: a transient line over the frame with a leading mark in a rounded square.
 *
 * <p>The mark is the part that carries the meaning, so it is drawn as a real square rather than a
 * bare glyph: a red "!" for something that did not work, a blue check for something that did. The
 * notice raises itself when [CaramelLauncherState.notice] is set and clears it once [millis] have
 * passed, so a caller never has to schedule the dismissal.
 */
@Composable
fun CaramelToast(state: CaramelLauncherState, modifier: Modifier = Modifier, millis: Long = 4200L) {
    val notice = state.notice
    var shown by remember { mutableStateOf<CaramelNotice?>(null) }
    LaunchedEffect(notice) {
        if (notice != null) {
            shown = notice
            delay(millis)
            if (state.notice === notice) state.notice = null
            shown = null
        }
    }
    val current = shown ?: notice
    Box(modifier, Alignment.TopCenter) {
        AnimatedVisibility(
            current != null,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 },
        ) {
            val n = current
            if (n != null) {
                val tint = if (n.warning) C.Red else C.Blue
                Row(
                    Modifier.padding(top = 58.dp)
                        .shadow(24.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(C.Navy.copy(0.98f))
                        .border(1.dp, tint.copy(0.5f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier.size(26.dp).clip(RoundedCornerShape(8.dp))
                            .background(tint.copy(0.18f))
                            .border(1.dp, tint.copy(0.55f), RoundedCornerShape(8.dp)),
                        Alignment.Center,
                    ) {
                        Icon(
                            if (n.warning) Icons.Rounded.PriorityHigh else Icons.Rounded.Check,
                            null, Modifier.size(17.dp), tint,
                        )
                    }
                    Text(
                        n.text,
                        style = T.Label.copy(fontWeight = FontWeight.Medium, color = C.Text),
                    )
                }
            }
        }
    }
}
