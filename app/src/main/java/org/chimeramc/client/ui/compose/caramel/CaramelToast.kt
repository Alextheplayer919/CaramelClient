package org.chimeramc.client.ui.compose.caramel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
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
 * The launcher's toast: a transient notice that slides in from the right edge and slides back out.
 *
 * <p>The notice raises itself when [CaramelLauncherState.notice] is set and clears it once [millis]
 * have passed, so a caller never has to schedule the dismissal. The enter/exit are a horizontal
 * slide with a spring plus a fade, and both scale with the user's animation-speed setting — the
 * earlier version dropped in vertically by half its height and popped out the same way, which read
 * as a jump rather than a slide.
 *
 * <p>A thin accent rail on the leading edge carries the meaning (a red rail for something that did
 * not work, a blue one for something that did), and the whole surface is tappable so a long notice
 * can be dismissed early.
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
    // A single spring drives both the slide and the fade, so the surface decelerates into place
    // instead of stopping dead. Its bounciness is the professional detail the flat version lacked.
    val slide = spring<androidx.compose.ui.unit.IntOffset>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
    val fade = tween<Float>(durationMillis = caramelMotionMs(180))
    Box(modifier, Alignment.TopEnd) {
        AnimatedVisibility(
            current != null,
            enter = slideInHorizontally(animationSpec = slide) { it } + fadeIn(fade),
            exit = slideOutHorizontally(animationSpec = tween(caramelMotionMs(200))) { it } + fadeOut(fade),
        ) {
            val n = current
            if (n != null) {
                val tint = if (n.warning) C.Red else C.Blue
                val dismiss = { state.notice = null; shown = null }
                Row(
                    Modifier.padding(top = 58.dp, end = 18.dp)
                        .shadow(28.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(C.Navy.copy(0.98f))
                        .border(1.dp, tint.copy(0.45f), RoundedCornerShape(16.dp))
                        .clickable(remember { MutableInteractionSource() }, null, onClick = dismiss)
                        .height(52.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(4.dp).fillMaxHeight().background(tint))
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
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
                        Icon(
                            Icons.Rounded.Close, null,
                            Modifier.size(15.dp).clickable(
                                remember { MutableInteractionSource() }, null, onClick = dismiss,
                            ),
                            C.Text3,
                        )
                    }
                }
            }
        }
    }
}
