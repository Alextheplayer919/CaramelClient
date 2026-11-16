package org.chimeramc.client.ui.compose.caramel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The Xbox profile picture is drawn from the account's stored avatar URL.
 *
 * <p>The URL was already stored (`MsftAccount.xboxAvatarUrl`, read from `AppDisplayPicRaw` at
 * sign-in) but the avatar slot drew a bare ring, so the picture never appeared. [CaramelAvatarLoader]
 * is the loader; its pure parts (URL gating and the cache-name mapping) are pinned here so the wiring
 * cannot silently regress.
 */
class CaramelAvatarLoaderTest {

    @Test
    fun onlyHttpUrlsAreAttempted() {
        assertTrue(CaramelAvatarLoader.isLoadable("https://images-eds.xboxlive.com/image"))
        assertTrue(CaramelAvatarLoader.isLoadable("http://example.com/a.png"))
        assertFalse("a blank URL must fall back to the placeholder", CaramelAvatarLoader.isLoadable(""))
        assertFalse(CaramelAvatarLoader.isLoadable(null))
        assertFalse(CaramelAvatarLoader.isLoadable("file:///data/local/avatar.png"))
    }

    @Test
    fun theCacheNameIsStableAndFilesystemSafe() {
        val name = CaramelAvatarLoader.cacheName("https://images-eds.xboxlive.com/image?x=1")
        assertEquals(name, CaramelAvatarLoader.cacheName("https://images-eds.xboxlive.com/image?x=1"))
        assertTrue("the name must be a bare file name, not a path", name.indexOf('/') < 0)
        assertTrue(name.endsWith(".png"))
        assertTrue(File(name).name == name)
    }

    @Test
    fun differentUrlsGetDifferentNames() {
        val a = CaramelAvatarLoader.cacheName("https://a/1.png")
        val b = CaramelAvatarLoader.cacheName("https://a/2.png")
        assertTrue(a != b)
    }

    @Test
    fun theLoaderIsReferencedByTheAvatarComposable() {
        val labels = read(
            "app/src/main/java/org/chimeramc/client/ui/compose/caramel/CaramelLabels.kt")
        assertTrue("the avatar composable must load through the loader",
                labels.contains("CaramelAvatarLoader.load("))
        assertTrue("the avatar composable must be reachable from the account surfaces",
                labels.contains("fun CaramelAvatarPicture("))
    }

    private fun read(relative: String): String =
        File(projectRoot(), relative).readText()

    private fun projectRoot(): File {
        var dir = File("").absoluteFile
        for (i in 0 until 6) {
            if (File(dir, "app/src/main/res/layout").isDirectory) return dir
            dir = dir.parentFile ?: break
        }
        throw IllegalStateException("could not find the project root")
    }
}
