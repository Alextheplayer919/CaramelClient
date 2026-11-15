package org.chimeramc.client.core.content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The default Bedrock servers the Servers tab offers before the user saves any of their own.
 *
 * <p>The tab was empty on a fresh install: it read {@code external_servers.txt}, which the game only
 * writes once a player adds a server in-game, so a new user saw "No servers saved" and no way to
 * fill it. These are the well-known public servers the launcher ships as a starting list. They are
 * marked as featured (not saved) so the UI never implies the user added them, and joining one still
 * goes through the game's own server file rather than inventing a join path.
 *
 * <p>The catalogue is pure data so it is unit-tested; the addresses are the servers' own published
 * Bedrock endpoints, and the icon key resolves through {@link ServerIconCatalog}.
 */
public final class FeaturedServers {

    /** One default server: the name, the Bedrock endpoint, and the icon key for its mark. */
    public static final class Featured {
        public final String name;
        public final String address;
        public final int port;
        public final String iconKey;

        public Featured(String name, String address, int port) {
            this.name = name;
            this.address = address;
            this.port = port;
            this.iconKey = ServerIconCatalog.keyFor(address);
        }
    }

    /** The default Bedrock port every one of these servers listens on. */
    public static final int DEFAULT_PORT = 19132;

    private static final List<Featured> SERVERS = Collections.unmodifiableList(
            new ArrayList<>(java.util.Arrays.asList(
                    new Featured("The Hive", "geo.hivebedrock.network", DEFAULT_PORT),
                    new Featured("CubeCraft", "mco.cubecraft.net", DEFAULT_PORT),
                    new Featured("Galaxite", "play.galaxite.net", DEFAULT_PORT),
                    new Featured("InPvP", "play.inpvp.net", DEFAULT_PORT),
                    new Featured("Lifeboat", "play.lbsg.net", DEFAULT_PORT),
                    new Featured("NetherGames", "play.nethergames.org", DEFAULT_PORT))));

    private FeaturedServers() {
    }

    /** The default servers, in the order the Servers tab shows them. */
    public static List<Featured> all() {
        return SERVERS;
    }
}
