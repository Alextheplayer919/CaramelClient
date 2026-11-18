package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Existing signed-in accounts get the account skin lookup.
 *
 * <p>The pref is default-off and was only flipped on a fresh Microsoft sign-in, so an account that
 * was already signed in (or re-imported) never had it and the preview showed the default skin. The
 * launcher now turns it on at startup when an account is present.
 */
public class AccountSkinOptInTest {

    @Test
    public void startupEnablesTheLookupForAnExistingAccount() throws IOException {
        String app = read("app/src/main/java/org/chimeramc/client/launcher/core/minecraft/"
                + "Application.kt");
        assertTrue("startup must enable the lookup for an account that signed in before it existed",
                app.contains("ensureEnabledForExistingAccount(applicationContext)"));
    }

    @Test
    public void theEnableHelperOnlyActsWhenAnAccountIsPresent() throws IOException {
        String service = read("app/src/main/java/org/chimeramc/client/core/cosmetics/"
                + "AccountSkinService.java");
        int start = service.indexOf("public static void ensureEnabledForExistingAccount(");
        assertTrue("the helper must exist", start >= 0);
        int end = service.indexOf("public static void refreshIfStale(", start);
        String body = service.substring(start, end);
        assertTrue("it must not enable the lookup with no account signed in",
                body.contains("activeXuid(context) == null) return"));
        assertTrue("it must be idempotent", body.contains("if (isEnabled(context)) return"));
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
