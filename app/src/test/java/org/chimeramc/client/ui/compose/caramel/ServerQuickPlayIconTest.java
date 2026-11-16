package org.chimeramc.client.ui.compose.caramel;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A server's mark reaches both the Servers list and the Quick play strip, in its own rounded square.
 *
 * <p>The defect this covers: the quick-play tiles drew an accent-coloured square with no server mark,
 * so the row said nothing about which server was which. The address-to-key rule is pure and already
 * tested; what this pins is the two joins the rule depends on and that a plain rule test cannot see:
 * every key the catalogue names resolves to a drawable the APK actually ships, and the quick-play
 * strip hands the tile the key rather than a bare colour.
 */
public class ServerQuickPlayIconTest {

    private static final List<String> KEYS = java.util.Arrays.asList(
            "hive", "cubecraft", "galaxite", "inpvp", "lbsg", "nethergames");

    @Test
    public void everyCatalogueKeyResolvesToAShippedDrawable() throws IOException {
        String icons = read("app/src/main/java/org/chimeramc/client/ui/compose/caramel/"
                + "ServerIcons.kt");
        for (String key : KEYS) {
            Pattern p = Pattern.compile("\"" + key + "\"\\s*->\\s*R\\.drawable\\.(\\w+)");
            Matcher m = p.matcher(icons);
            assertTrue("the key " + key + " must map to a drawable", m.find());
            String drawable = m.group(1);
            assertTrue("the drawable " + drawable + " must ship in the APK",
                    Files.exists(drawablePath(drawable)));
        }
    }

    @Test
    public void theQuickPlayStripDrawsEachServersOwnMark() throws IOException {
        String shell = read("app/src/main/java/org/chimeramc/client/ui/compose/caramel/launcher/"
                + "CaramelShell.kt");
        assertTrue("the quick-play tile must carry its server's icon key",
                shell.contains("iconKey = s.iconKey"));
        assertTrue("the tile must draw through ServerIcon, which rounds its own square",
                shell.contains("ServerIcon("));
    }

    private static Path drawablePath(String drawable) {
        Path res = projectRoot().resolve("app/src/main/res");
        for (String dir : new String[]{"drawable-nodpi", "drawable", "drawable-hdpi",
                "drawable-xhdpi", "drawable-xxhdpi", "drawable-xxxhdpi", "drawable-mdpi"}) {
            for (String ext : new String[]{".png", ".xml", ".webp", ".jpg"}) {
                Path candidate = res.resolve(dir).resolve(drawable + ext);
                if (Files.exists(candidate)) return candidate;
            }
        }
        return res.resolve("drawable-nodpi").resolve(drawable + ".png");
    }

    private static Path projectRoot() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.exists(dir.resolve("app/src/main"))) return dir;
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find the project root");
    }

    private static String read(String relativePath) throws IOException {
        Path candidate = projectRoot().resolve(relativePath);
        if (!Files.exists(candidate)) {
            throw new IllegalStateException("could not find " + relativePath);
        }
        return new String(Files.readAllBytes(candidate), StandardCharsets.UTF_8);
    }
}
