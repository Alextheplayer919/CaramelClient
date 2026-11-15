package org.chimeramc.client.core.content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the Servers tab's rows from the saved servers, the shipped defaults, the starred keys and
 * whatever the last ping measured.
 *
 * <p>Four inputs that used to be nowhere: the tab read {@code external_servers.txt} alone, so a
 * fresh install was empty, a star had no effect, and a player count had no source. This is the one
 * place they are combined, and it is Android-free so the combination is unit-tested rather than
 * only visible on a device.
 *
 * <p>The rules: the user's own servers come first (starred ones before the rest, in the order the
 * game file stores them), then the shipped defaults; a default whose address the user has already
 * saved is dropped so the same server is never listed twice; and a row shows a measured value only
 * when the ping actually returned one, so nothing is fabricated.
 */
public final class ServerListRows {

    /** One measured value for a server address, all null/empty until the ping answers. */
    public static final class Status {
        public final Integer online;
        public final Integer ping;
        public final String gameVersion;

        /**
         * True once a ping has been attempted for this address. A checked server with no values was
         * tried and did not answer, which the UI states plainly; an unchecked one has not been
         * measured yet, which the UI shows as still checking.
         */
        public final boolean checked;

        public Status(Integer online, Integer ping, String gameVersion) {
            this(online, ping, gameVersion, true);
        }

        private Status(Integer online, Integer ping, String gameVersion, boolean checked) {
            this.online = online;
            this.ping = ping;
            this.gameVersion = gameVersion == null ? "" : gameVersion;
            this.checked = checked;
        }

        /** A server the ping has not been attempted for: no values and not yet checked. */
        public static Status pending() {
            return new Status(null, null, "", false);
        }

        /** A server the ping tried and could not reach: checked, but with no values. */
        public static Status offline() {
            return new Status(null, null, "", true);
        }
    }

    /** One row of the Servers tab. */
    public static final class Row {
        public final String name;
        public final String address;
        public final int port;
        public final Integer online;
        public final Integer ping;
        public final String version;
        public final boolean favourite;
        public final String iconKey;
        public final boolean featured;
        /** True once a ping has been attempted, so the row can tell "checking" from "offline". */
        public final boolean checked;

        Row(String name, String address, int port, Integer online, Integer ping, String version,
            boolean favourite, String iconKey, boolean featured, boolean checked) {
            this.name = name;
            this.address = address;
            this.port = port;
            this.online = online;
            this.ping = ping;
            this.version = version;
            this.favourite = favourite;
            this.iconKey = iconKey;
            this.featured = featured;
            this.checked = checked;
        }
    }

    private ServerListRows() {
    }

    /** The key a saved server and a measured status share: the address and port. */
    public static String key(String address, int port) {
        return (address == null ? "" : address) + ":" + port;
    }

    /**
     * The rows for the Servers tab, in display order.
     *
     * @param saved      the user's own servers, in the game file's order
     * @param favourites the starred {@code address:port} keys
     * @param statuses   the last measured status per {@code address:port} key, never null
     */
    public static List<Row> build(List<ServerItem> saved, Set<String> favourites,
                                  Map<String, Status> statuses) {
        List<Row> rows = new ArrayList<>();
        List<ServerItem> own = saved == null ? Collections.emptyList() : saved;
        Set<String> stars = favourites == null ? Collections.emptySet() : favourites;
        Map<String, Status> measured = statuses == null ? Collections.emptyMap() : statuses;

        List<ServerItem> starred = new ArrayList<>();
        List<ServerItem> rest = new ArrayList<>();
        for (ServerItem s : own) {
            if (s == null) continue;
            if (stars.contains(key(s.ip, s.port))) starred.add(s);
            else rest.add(s);
        }
        for (ServerItem s : starred) rows.add(row(s.name, s.ip, s.port, true, measured, false));
        for (ServerItem s : rest) rows.add(row(s.name, s.ip, s.port, false, measured, false));

        for (FeaturedServers.Featured f : FeaturedServers.all()) {
            if (containsAddress(own, f.address)) continue;
            rows.add(row(f.name, f.address, f.port, stars.contains(key(f.address, f.port)),
                    measured, true));
        }
        return rows;
    }

    /**
     * The addresses to ping: every saved server and every shipped default, once each, mapped from
     * {@code address:port} key to port.
     *
     * <p>A key that appears twice (a saved default) is measured once, because the row it feeds is
     * the same row.
     */
    public static Map<String, Integer> pingTargets(List<ServerItem> saved) {
        Map<String, Integer> targets = new LinkedHashMap<>();
        if (saved != null) {
            for (ServerItem s : saved) {
                if (s != null && s.ip != null && !s.ip.isEmpty()) targets.put(key(s.ip, s.port), s.port);
            }
        }
        for (FeaturedServers.Featured f : FeaturedServers.all()) {
            targets.put(key(f.address, f.port), f.port);
        }
        return targets;
    }

    private static Row row(String name, String address, int port, boolean favourite,
                           Map<String, Status> measured, boolean featured) {
        Status status = measured.get(key(address, port));
        if (status == null) status = Status.pending();
        // Only a value the ping actually returned is carried; a version the pong did not advertise
        // stays blank rather than a fabricated range.
        return new Row(name, address, port, status.online, status.ping, status.gameVersion,
                favourite, ServerIconCatalog.keyFor(address), featured, status.checked);
    }

    private static boolean containsAddress(List<ServerItem> saved, String address) {
        for (ServerItem s : saved) {
            if (s != null && s.ip != null && s.ip.equalsIgnoreCase(address)) return true;
        }
        return false;
    }
}
