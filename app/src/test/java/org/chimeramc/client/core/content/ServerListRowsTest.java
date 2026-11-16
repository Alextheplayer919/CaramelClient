package org.chimeramc.client.core.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Servers tab's rows come from four inputs, combined by one rule.
 *
 * <p>The tab used to read {@code external_servers.txt} alone, so a fresh install was empty, a star
 * did nothing and a player count had no source. This pins the combination: the user's servers first
 * (starred ahead of the rest, stored order kept inside each group), then the shipped defaults, a
 * saved default dropped rather than listed twice, and a measured value carried only when the ping
 * actually returned one.
 */
public class ServerListRowsTest {

    private static ServerItem item(String name, String ip, int port) {
        return new ServerItem(name, ip, port);
    }

    @Test
    public void theDefaultsFillAnEmptyTab() {
        List<ServerListRows.Row> rows = ServerListRows.build(
                Collections.emptyList(), Collections.emptySet(), Collections.emptyMap());
        assertEquals(FeaturedServers.all().size(), rows.size());
        for (ServerListRows.Row row : rows) {
            assertTrue("a shipped default must be flagged featured", row.featured);
            assertNotNull("a shipped default must have a name", row.name);
            assertFalse("a default must carry an icon key", row.iconKey.isEmpty());
        }
    }

    @Test
    public void theUsersServersComeFirstAndStarredOnesLead() {
        List<ServerItem> saved = Arrays.asList(
                item("Mine A", "10.0.0.1", 19132),
                item("Mine B", "10.0.0.2", 19132),
                item("Mine C", "10.0.0.3", 19132));
        Set<String> favourites = new HashSet<>(Collections.singletonList("10.0.0.3:19132"));
        List<ServerListRows.Row> rows = ServerListRows.build(saved, favourites, Collections.emptyMap());

        assertEquals("Mine C", rows.get(0).name);
        assertTrue("the starred server must be flagged", rows.get(0).favourite);
        assertEquals("Mine A", rows.get(1).name);
        assertEquals("Mine B", rows.get(2).name);
        assertFalse(rows.get(1).featured);
    }

    @Test
    public void aSavedDefaultIsNotListedTwice() {
        List<ServerItem> saved = Collections.singletonList(
                item("Hive (mine)", "geo.hivebedrock.network", 19132));
        List<ServerListRows.Row> rows = ServerListRows.build(saved, Collections.emptySet(), Collections.emptyMap());

        int hiveRows = 0;
        for (ServerListRows.Row row : rows) {
            if ("geo.hivebedrock.network".equals(row.address)) hiveRows++;
        }
        assertEquals("the saved Hive must replace the default one, not double it", 1, hiveRows);
        assertEquals("Hive (mine)", rows.get(0).name);
        assertFalse(rows.get(0).featured);
    }

    @Test
    public void aMeasuredValueIsCarriedAndAnUnmeasuredOneIsNot() {
        Map<String, ServerListRows.Status> statuses = new HashMap<>();
        statuses.put(ServerListRows.key("geo.hivebedrock.network", 19132),
                new ServerListRows.Status(16000, 42, "1.21.60"));

        List<ServerListRows.Row> rows = ServerListRows.build(
                Collections.emptyList(), Collections.emptySet(), statuses);
        ServerListRows.Row hive = rows.get(0);
        assertEquals("geo.hivebedrock.network", hive.address);
        assertEquals(Integer.valueOf(16000), hive.online);
        assertEquals(Integer.valueOf(42), hive.ping);
        assertEquals("1.21.60", hive.version);

        ServerListRows.Row unreached = rows.get(1);
        assertEquals("an unreached server must not report a zero",
                null, unreached.online);
        assertEquals(null, unreached.ping);
        assertEquals("", unreached.version);
        assertFalse("a server with no measurement has not been checked yet", unreached.checked);
    }

    @Test
    public void aTriedButUnansweredServerIsCheckedAndOffline() {
        Map<String, ServerListRows.Status> statuses = new HashMap<>();
        statuses.put(ServerListRows.key("geo.hivebedrock.network", 19132),
                ServerListRows.Status.offline());

        List<ServerListRows.Row> rows = ServerListRows.build(
                Collections.emptyList(), Collections.emptySet(), statuses);
        ServerListRows.Row hive = rows.get(0);
        assertEquals("geo.hivebedrock.network", hive.address);
        assertTrue("a tried server is checked, so the row can say offline", hive.checked);
        assertEquals(null, hive.online);
        assertEquals(null, hive.ping);
        assertFalse("the server after it was never tried, so it is still checking",
                rows.get(1).checked);
    }

    @Test
    public void everySavedAndDefaultServerIsMeasuredOnce() {
        List<ServerItem> saved = Arrays.asList(
                item("Mine", "10.0.0.1", 19132),
                item("Hive (mine)", "geo.hivebedrock.network", 19132));
        Map<String, Integer> targets = ServerListRows.pingTargets(saved);

        assertTrue(targets.containsKey("10.0.0.1:19132"));
        assertTrue(targets.containsKey("geo.hivebedrock.network:19132"));
        assertEquals("a saved default must not add a second target",
                saved.size() + FeaturedServers.all().size() - 1, targets.size());
        assertEquals(Integer.valueOf(19132), targets.get("10.0.0.1:19132"));
    }
}
