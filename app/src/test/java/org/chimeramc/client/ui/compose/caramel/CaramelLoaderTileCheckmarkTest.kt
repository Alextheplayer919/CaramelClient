package org.chimeramc.client.ui.compose.caramel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * The selected-loader tick must read as a selection mark on the glyph, not a small badge.
 *
 * <p>The tile is 44dp; the tick used to be a 20dp disc with a 13dp glyph, which the user read as a
 * tiny notification dot parked on the icon rather than "this loader is chosen". It is now a larger
 * disc centred on the glyph, so this pins the size so a later restyle cannot silently shrink it back.
 */
class CaramelLoaderTileCheckmarkTest {

    @Test
    fun theSelectionTickIsLargeAndCentredOnTheGlyph() {
        val source = source("app/src/main/java/org/chimeramc/client/ui/compose/caramel/CaramelLoaderIcons.kt")
        // The tick disc covers most of the 44dp tile.
        assertTrue("the tick disc should be 30dp", source.contains("size(30.dp)"))
        // The check glyph inside it is larger than the old 13dp.
        assertTrue("the check glyph should be 20dp", source.contains("Modifier.size(20.dp)"))
        // The tile itself is unchanged.
        assertTrue("the loader tile should stay 44dp", source.contains(".size(44.dp)"))
        assertFalse("the old 20dp tick disc must be gone", source.contains("size(20.dp)\n                    .background(C.Green"))
    }

    private fun source(relative: String): String {
        var dir = File("").absoluteFile
        while (!File(dir, "settings.gradle").exists()) {
            dir = dir.parentFile ?: throw IllegalStateException("repo root not found")
        }
        return File(dir, relative).readText(StandardCharsets.UTF_8)
    }
}
