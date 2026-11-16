package org.chimeramc.client.ui.compose.caramel.launcher

import org.chimeramc.client.ui.compose.caramel.CaramelLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * The loader picker must offer exactly one preloader that can actually start a game.
 *
 * <p>The three tiles used to be interchangeable, so picking "Rust" or "Lightning" looked like a
 * real choice and launched the same SweetLoader path anyway. Only SweetLoader exists, so only it is
 * selectable; these tests pin that, the names the user asked for, and that both places that draw the
 * picker use the shared one (so the greyed tiles cannot be shown without the note that explains
 * them).
 */
class CaramelLoaderAvailabilityTest {

    @Test
    fun sweetLoaderIsTheOnlySelectablePreloader() {
        assertEquals(1, CaramelLoader.entries.count { it.available })
        assertTrue(CaramelLoader.Bedrock.available)
        assertFalse(CaramelLoader.Rust.available)
        assertFalse(CaramelLoader.Lightning.available)
    }

    @Test
    fun thePreloadersCarryTheNamesTheUserChose() {
        assertEquals("SweetLoader", CaramelLoader.Bedrock.title)
        assertEquals("RustyLoader", CaramelLoader.Rust.title)
        assertEquals("LightningLoader", CaramelLoader.Lightning.title)
    }

    @Test
    fun bothPlacesThatOfferTheChoiceUseTheSharedPicker() {
        val overlay = source("app/src/main/java/org/chimeramc/client/ui/compose/caramel/launcher/CaramelOverlays.kt")
        val modals = source("app/src/main/java/org/chimeramc/client/ui/compose/caramel/launcher/CaramelModals.kt")
        assertTrue("the launch menu must draw the shared picker", overlay.contains("CaramelLoaderPicker("))
        assertTrue("instance settings must draw the shared picker", modals.contains("CaramelLoaderPicker("))
        assertFalse("a bare LoaderTile loop would skip the unavailable note", overlay.contains("LoaderTile("))
        assertFalse("a bare LoaderTile loop would skip the unavailable note", modals.contains("LoaderTile("))
    }

    private fun source(relative: String): String {
        var dir = File("").absoluteFile
        while (!File(dir, "settings.gradle").exists()) {
            dir = dir.parentFile ?: throw IllegalStateException("repo root not found")
        }
        return File(dir, relative).readText(StandardCharsets.UTF_8)
    }
}
