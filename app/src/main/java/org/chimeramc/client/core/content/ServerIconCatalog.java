package org.chimeramc.client.core.content;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Maps a server address to a stable icon key.
 *
 * <p>The launcher can draw the mark of a handful of well-known Bedrock servers, and the mapping from
 * an address to that mark is a rule, not a resource: it is a pure string comparison, so it lives
 * here (Android-free and unit-tested) and the UI layer maps the key to a drawable. An address that
 * matches nothing yields the empty key, and the caller draws its generic tile rather than a wrong
 * logo.
 *
 * <p>Matching is on the host, not the port, and on a substring so a regional or proxy hostname
 * ({@code geo.hivebedrock.network}, {@code eu.play.galaxite.net}) still resolves to the same server.
 */
public final class ServerIconCatalog {

    /** The keys the UI knows how to draw. Adding one means adding a drawable beside it. */
    public static final List<String> KEYS = Collections.unmodifiableList(Arrays.asList(
            "hive", "cubecraft", "galaxite", "inpvp", "lbsg", "nethergames"));

    private ServerIconCatalog() {
    }

    /**
     * The icon key for {@code address}, or the empty string when nothing matches.
     *
     * <p>An empty or null address, or one without a host, returns the empty string so the caller
     * falls back to its generic tile.
     */
    public static String keyFor(String address) {
        String host = host(address);
        if (host.isEmpty()) return "";
        if (host.contains("hivebedrock") || host.contains("playhive")) return "hive";
        if (host.contains("cubecraft")) return "cubecraft";
        if (host.contains("galaxite")) return "galaxite";
        if (host.contains("inpvp")) return "inpvp";
        if (host.contains("lbsg") || host.contains("lifeboat")) return "lbsg";
        if (host.contains("nethergames")) return "nethergames";
        return "";
    }

    /** True when {@link #keyFor} can draw a real mark for {@code address}. */
    public static boolean hasIcon(String address) {
        return !keyFor(address).isEmpty();
    }

    /**
     * The bare host of an address, lower-cased and stripped of a port, a path and a {@code www.}
     * prefix. Returns the empty string when there is no host to speak of.
     */
    static String host(String address) {
        if (address == null) return "";
        String value = address.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) return "";
        int scheme = value.indexOf("://");
        if (scheme >= 0) value = value.substring(scheme + 3);
        int slash = value.indexOf('/');
        if (slash >= 0) value = value.substring(0, slash);
        int colon = value.indexOf(':');
        if (colon >= 0) value = value.substring(0, colon);
        if (value.startsWith("www.")) value = value.substring(4);
        while (value.endsWith(".")) value = value.substring(0, value.length() - 1);
        return value;
    }
}
