package org.chimeramc.client.ui.compose.caramel.launcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The Cosmetics grid must draw each cosmetic's real art, not a generic coloured rectangle.
 *
 * <p>The client ships the renderers that draw a cape's cloth ({@code CapeThumbnail}) and a hat, back
 * piece or pet ({@code CosmeticThumbnail}), but nothing called them: the grid painted the same
 * coloured block for every tile, so a player could not see the cosmetic before equipping it. This
 * pins the wiring — the tile delegates to {@code CaramelCosmeticThumbnail}, which delegates to the
 * shipped renderers with the real catalogue entry and authored assets.
 */
public class CaramelCosmeticThumbnailWiringTest {

    @Test
    public void theGridTileRendersTheRealThumbnail() throws IOException {
        String store = read("app/src/main/java/org/chimeramc/client/ui/compose/caramel/launcher/"
                + "CaramelCosmeticsStore.kt");
        assertTrue("the cosmetics tile must render the real thumbnail",
                store.contains("CaramelCosmeticThumbnail("));
        assertFalse("the tile must not fall back to a plain coloured swatch",
                store.contains("Modifier.size(40.dp, 68.dp).background(accent"));
    }

    @Test
    public void theThumbnailDelegatesToTheShippedRenderers() throws IOException {
        String renderer = read("app/src/main/java/org/chimeramc/client/ui/compose/caramel/launcher/"
                + "CaramelCosmeticThumbnail.kt");
        assertTrue("a cape tile must paint the real cloth",
                renderer.contains("CapeThumbnail.render("));
        assertTrue("an authored cape must sample its real texture",
                renderer.contains("CapeAtlas.sample("));
        assertTrue("a hat/back tile must paint the real accessory",
                renderer.contains("CosmeticThumbnail.renderAccessory("));
        assertTrue("a pet tile must paint the real pet",
                renderer.contains("CosmeticThumbnail.renderPet("));
        assertTrue("an authored mesh must be parsed from the pack assets",
                renderer.contains("BedrockGeometryParser.parse("));
    }

    private static String read(String relative) throws IOException {
        return new String(Files.readAllBytes(projectRoot().resolve(relative)),
                StandardCharsets.UTF_8);
    }

    private static Path projectRoot() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.exists(dir.resolve("app/src/main/res/layout"))) return dir;
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find the project root");
    }
}
