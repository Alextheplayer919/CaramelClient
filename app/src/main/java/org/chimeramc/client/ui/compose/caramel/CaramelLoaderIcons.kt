package org.chimeramc.client.ui.compose.caramel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * The three loader glyphs, ported from the pack's `components/LoaderIcons.kt`: the Bedrock block,
 * the Rust cog and the Lightning bolt. Each is drawn rather than shipped as an asset so it scales
 * and recolours without a density bucket.
 */
@Composable
fun LoaderGlyph(loader: CaramelLoader, modifier: Modifier = Modifier.size(28.dp)) {
    when (loader) {
        CaramelLoader.Bedrock -> Canvas(modifier) {
            val n = 12
            val px = size.width / n
            val rnd = Random(7)
            for (i in 0 until n) {
                for (j in 0 until n) {
                    val v = if (rnd.nextFloat() > 0.15f) listOf(52, 64, 78, 92, 108, 124).random(rnd) else listOf(28, 34).random(rnd)
                    drawRect(Color(v, v, v + 6), Offset(i * px, j * px), Size(px + 0.5f, px + 0.5f))
                }
            }
            drawRoundRect(Color(0xFF14141A), Offset.Zero, size, CornerRadius(2.dp.toPx()), Stroke(1.5.dp.toPx()))
        }
        CaramelLoader.Rust -> Box(modifier, Alignment.Center) {
            Canvas(Modifier.size(28.dp)) {
                val c = Offset(size.width / 2, size.height / 2)
                val r = size.width * 0.36f
                val rust = Color(0xFFDEA584)
                for (i in 0 until 12) {
                    val a = i * PI / 6
                    drawCircle(
                        rust,
                        size.width * 0.085f,
                        Offset(c.x + (r + size.width * 0.1f) * cos(a).toFloat(), c.y + (r + size.width * 0.1f) * sin(a).toFloat()),
                    )
                }
                drawCircle(rust, r + size.width * 0.04f, c)
                drawCircle(Color(0xFF221612), r * 0.72f, c)
            }
            Text("R", style = T.Title.copy(color = Color(0xFFDEA584), fontSize = 12.sp_))
        }
        CaramelLoader.Lightning -> Canvas(modifier) {
            val w = size.width
            val h = size.height
            val p = Path().apply {
                moveTo(0.56f * w, 0.06f * h)
                lineTo(0.19f * w, 0.56f * h)
                lineTo(0.46f * w, 0.56f * h)
                lineTo(0.40f * w, 0.94f * h)
                lineTo(0.81f * w, 0.40f * h)
                lineTo(0.54f * w, 0.40f * h)
                close()
            }
            drawPath(p, C.Yellow)
        }
    }
}

/** A selectable loader tile: the glyph in a rounded square, with a green tick when selected. */
@Composable
fun LoaderTile(loader: CaramelLoader, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(CaramelDimens.RadiusControl)
    Box(modifier.size(44.dp)) {
        Box(
            Modifier
                .size(44.dp)
                .background(Color.White.copy(if (selected) 0.12f else 0.07f), shape)
                .border(if (selected) 1.6.dp else 1.dp, if (selected) C.Green else Color.White.copy(0.14f), shape)
                .clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
            Alignment.Center,
        ) { LoaderGlyph(loader, Modifier.size(28.dp)) }
        if (selected) {
            Box(
                Modifier
                    .offset((-5).dp, (-5).dp)
                    .size(14.dp)
                    .background(C.Green, CircleShape)
                    .border(1.5.dp, C.Navy, CircleShape),
                Alignment.Center,
            ) { Icon(Icons.Rounded.Check, null, Modifier.size(9.dp), C.OnGreen) }
        }
    }
}
