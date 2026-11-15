package org.chimeramc.client.core.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * A server address maps to the mark the client can draw, or to nothing.
 *
 * <p>The Servers tab and the Quick play strip both draw a server's own icon, and the mapping from an
 * address to that icon is a pure string rule. These pin the parts a UI test cannot see: a regional
 * hostname still resolves, a port or a scheme does not confuse the match, and an unknown server
 * yields the empty key so the caller draws its generic tile rather than a wrong logo.
 */
public class ServerIconCatalogTest {

    @Test
    public void theKnownServersResolveToTheirKey() {
        assertEquals("hive", ServerIconCatalog.keyFor("geo.hivebedrock.network"));
        assertEquals("cubecraft", ServerIconCatalog.keyFor("mco.cubecraft.net"));
        assertEquals("galaxite", ServerIconCatalog.keyFor("play.galaxite.net"));
        assertEquals("inpvp", ServerIconCatalog.keyFor("play.inpvp.net"));
        assertEquals("lbsg", ServerIconCatalog.keyFor("play.lbsg.net"));
        assertEquals("nethergames", ServerIconCatalog.keyFor("play.nethergames.org"));
    }

    @Test
    public void aRegionalOrDecoratedHostStillResolves() {
        assertEquals("hive", ServerIconCatalog.keyFor("eu.hivebedrock.network"));
        assertEquals("galaxite", ServerIconCatalog.keyFor("https://eu.play.galaxite.net:19132/some/path"));
        assertEquals("cubecraft", ServerIconCatalog.keyFor("  MCO.CubeCraft.NET  "));
        assertEquals("hive", ServerIconCatalog.keyFor("www.playhive.com"));
    }

    @Test
    public void anUnknownOrEmptyAddressHasNoKey() {
        assertEquals("", ServerIconCatalog.keyFor(null));
        assertEquals("", ServerIconCatalog.keyFor(""));
        assertEquals("", ServerIconCatalog.keyFor("   "));
        assertEquals("", ServerIconCatalog.keyFor("my.own.server.example"));
        assertEquals("", ServerIconCatalog.keyFor("https://"));
        assertFalse(ServerIconCatalog.hasIcon("my.own.server.example"));
        assertTrue(ServerIconCatalog.hasIcon("geo.hivebedrock.network"));
    }

    @Test
    public void everyKeyTheCatalogueNamesIsOneTheUiCanDraw() {
        // The keys the UI resolver knows; adding a server here means adding its drawable there.
        for (String key : ServerIconCatalog.KEYS) {
            assertFalse("a key must not be blank", key.isEmpty());
        }
        assertEquals(6, ServerIconCatalog.KEYS.size());
    }
}
