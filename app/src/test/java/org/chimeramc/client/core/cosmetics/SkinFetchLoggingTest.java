package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A failed skin fetch names why it failed.
 *
 * <p>The fetch methods used to return null with no logging, so a support report could not tell "no
 * token" from "the service refused" from "no skin for this account". Each of those is now a named
 * outcome, so the three are distinguishable from the log alone.
 */
public class SkinFetchLoggingTest {

    @Test
    public void theProfileFetchNamesItsFailureModes() throws IOException {
        String source = read("app/src/main/java/org/chimeramc/client/core/cosmetics/"
                + "MinecraftSkinFetcher.java");
        assertTrue("a missing bearer must be logged as its own outcome",
                source.contains("No Minecraft Services bearer"));
        assertTrue("a non-200 profile response must be logged with its code",
                source.contains("returned HTTP \" + code"));
        assertTrue("an over-budget response must be logged",
                source.contains("exceeded \" + MAX_BYTES"));
        assertTrue("a thrown fetch must be logged",
                source.contains("The profile fetch failed"));
    }

    @Test
    public void theSkinServiceRequestNamesItsFailureModes() throws IOException {
        String source = read("app/src/main/java/org/chimeramc/client/core/cosmetics/"
                + "AccountSkinService.java");
        assertTrue("a non-200 skin-service response must be logged with its code",
                source.contains("returned HTTP \" + code"));
        assertTrue("an over-budget response must be logged",
                source.contains("response exceeded \" + MAX_BYTES"));
        assertTrue("a thrown request must be logged",
                source.contains("The skin service request failed"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(root().resolve(relativePath)), StandardCharsets.UTF_8);
    }

    private static Path root() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.exists(dir.resolve("app/src/main/res/layout"))) return dir;
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find the project root");
    }
}
