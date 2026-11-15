package org.chimeramc.client.ui.compose.caramel.launcher

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.chimeramc.client.R
import org.chimeramc.client.ui.compose.caramel.C
import org.chimeramc.client.ui.compose.caramel.T
import org.chimeramc.client.ui.compose.caramel.sp_

/**
 * The cold-start splash, drawn in Compose on the pack's backdrop.
 *
 * <p>The old splash was a View screen with its own layout, timeline and background registry; all of
 * that was deleted with the legacy shell. This is the replacement: the Caramel backdrop, the logo
 * and a warm-up spinner. It is deliberately small — the splash exists only to cover the launcher's
 * first warm-up, and the activity navigates on a real milestone rather than a fixed delay.
 */
@Composable
fun CaramelSplashScreen(preparing: String) {
    val transition = rememberInfiniteTransition(label = "splash")
    val breathe by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "breathe",
    )
    Box(Modifier.fillMaxSize().background(C.Abyss), Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                Brush.radialGradient(
                    listOf(C.Blue.copy(0.22f), Color.Transparent),
                    Offset(size.width * 0.5f, size.height * 0.45f),
                    size.minDimension * 0.7f,
                ),
                center = Offset(size.width * 0.5f, size.height * 0.45f),
                radius = size.minDimension * 0.7f,
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Image(
                painterResource(R.drawable.ic_caramel_logo),
                contentDescription = null,
                modifier = Modifier.size(148.dp).scale(breathe),
            )
            Text(stringResource(R.string.app_name), style = T.Display.copy(fontSize = 30.sp_))
            Text(preparing, style = T.Body.copy(color = C.Text2))
            CircularProgressIndicator(
                color = C.Caramel,
                strokeWidth = 3.dp,
                modifier = Modifier.size(34.dp).alpha(0.9f),
            )
        }
    }
}
