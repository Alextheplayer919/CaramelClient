package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The profile's skin variant decides the arm model.
 *
 * <p>{@code variant: "SLIM"} means the 3-px ("Alex") arms; {@code "CLASSIC"} or an absent variant
 * means the 4-px default. The variant is read from the same entry {@link
 * MinecraftSkinFetcher#activeSkinUrl} selects, so the URL and the arm model never describe different
 * skins. A wrong read here is the "my Alex skin has Steve arms" defect.
 */
public class SkinVariantTest {

    private static final String SLIM_ACTIVE = "{"
            + "\"id\":\"x\",\"name\":\"n\",\"skins\":["
            + "  {\"id\":\"a1\",\"state\":\"INACTIVE\",\"variant\":\"CLASSIC\","
            + "   \"url\":\"http://textures.minecraft.net/texture/classic\"},"
            + "  {\"id\":\"a2\",\"state\":\"ACTIVE\",\"variant\":\"SLIM\","
            + "   \"url\":\"http://textures.minecraft.net/texture/slim\"}]}";

    private static final String CLASSIC_ACTIVE = "{"
            + "\"id\":\"x\",\"name\":\"n\",\"skins\":["
            + "  {\"id\":\"a1\",\"state\":\"ACTIVE\",\"variant\":\"CLASSIC\","
            + "   \"url\":\"http://textures.minecraft.net/texture/classic\"}]}";

    private static final String NO_VARIANT = "{"
            + "\"id\":\"x\",\"name\":\"n\",\"skins\":["
            + "  {\"id\":\"a1\",\"state\":\"ACTIVE\","
            + "   \"url\":\"http://textures.minecraft.net/texture/plain\"}]}";

    @Test
    public void theActiveSlimVariantIsSlim() {
        assertTrue(MinecraftSkinFetcher.activeSkinIsSlim(SLIM_ACTIVE));
    }

    @Test
    public void aClassicVariantIsNotSlim() {
        assertFalse(MinecraftSkinFetcher.activeSkinIsSlim(CLASSIC_ACTIVE));
    }

    @Test
    public void anAbsentVariantDefaultsToClassic() {
        assertFalse(MinecraftSkinFetcher.activeSkinIsSlim(NO_VARIANT));
    }

    @Test
    public void aMalformedBodyIsNotSlimRatherThanACrash() {
        assertFalse(MinecraftSkinFetcher.activeSkinIsSlim("not json"));
        assertFalse(MinecraftSkinFetcher.activeSkinIsSlim(null));
        assertFalse(MinecraftSkinFetcher.activeSkinIsSlim(""));
    }

    @Test
    public void theVariantFollowsTheSameEntryAsTheUrl() {
        // The active entry is slim, so both the URL and the flag must describe it: the URL is the
        // slim one and the flag is true. A parser that took the variant from the first entry would
        // say CLASSIC while the URL said slim.
        assertTrue(MinecraftSkinFetcher.activeSkinUrl(SLIM_ACTIVE).endsWith("/slim"));
        assertTrue(MinecraftSkinFetcher.activeSkinIsSlim(SLIM_ACTIVE));
    }
}
