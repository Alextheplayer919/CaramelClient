package org.chimeramc.client.core.auth;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The sign-in button must reach a flow that can actually finish.
 *
 * <p>It used to do nothing visible, and the reason was structural rather than cosmetic: the button
 * ran a flow whose code was never shown, whose browser was never opened, and whose every failure was
 * written to the log and discarded. On top of that the repo carried a second, unreferenced MSAL
 * integration that could never have worked with this app's client id — the Bedrock title id is an
 * MSA title, so the Azure/MSAL endpoint answers {@code AADSTS700016} for it and the MSA endpoint
 * rejects the {@code msauth://} redirect. A later change that wires the button back to that path, or
 * that drops the code, the browser or the error report, would silently restore the dead button.
 *
 * <p>Device-only behaviour cannot be exercised here (there is no emulator), so these cases pin the
 * wiring that a device test would otherwise have to observe: the flow is the device-code one, the
 * code is surfaced, the browser is opened at the pre-filled link, and a failure is reported rather
 * than swallowed.
 */
public class SignInFlowTest {

    private static final Path ROOT = findRoot();

    @Test
    public void theButtonRunsTheDeviceCodeFlow() throws IOException {
        String body = startSignInBody();
        assertTrue("startSignIn must run the device-code flow, which is the only interactive flow "
                + "the Bedrock title id supports", body.contains("loginWithDeviceCode"));
        assertFalse("startSignIn must not fall back to an authorization-code flow: the MSA endpoint "
                + "rejects the msauth:// redirect for this client id", body.contains("loginWithCode"));
    }

    @Test
    public void theCodeAndThePreFilledLinkAreShown() throws IOException {
        String body = startSignInBody();
        assertTrue("the device code must reach the modal, or the user has nothing to enter",
                body.contains("setDeviceCode("));
        assertTrue("the pre-filled verification link must be stored, so the modal can open it again",
                body.contains("setDeviceCodeUrl("));
    }

    @Test
    public void theSignInPageOpensInApp() throws IOException {
        String body = startSignInBody();
        assertTrue("the verification page must open in the launcher's own WebView, so the user does "
                + "not leave the app to finish signing in", body.contains("setSignInWebOpen(true)"));
    }

    @Test
    public void theBrowserFallbackIsStillWired() throws IOException {
        String source = read(ROOT.resolve(
                "app/src/main/java/org/chimeramc/client/launcher/ui/activities/MainActivity.java"));
        int start = source.indexOf("public void openDeviceCodePage()");
        assertTrue("the deliberate system-browser fallback must stay for sign-in steps a WebView "
                + "cannot finish", start >= 0);
        int end = source.indexOf("@Override", start);
        String body = end < 0 ? source.substring(start) : source.substring(start, end);
        assertTrue("the fallback must still open the browser", body.contains("UrlOpener.open("));
    }

    @Test
    public void aFailureIsReportedRatherThanSwallowed() throws IOException {
        String body = startSignInBody();
        assertTrue("a failed sign-in must surface a reason, not close silently",
                body.contains("describeError("));
        assertTrue("the reason must reach the modal", body.contains("setSignInError("));
    }

    @Test
    public void theImpossibleMsalPathIsGone() throws IOException {
        String build = read(ROOT.resolve("app/build.gradle"));
        assertFalse("the MSAL dependency is back; it cannot authenticate the Bedrock title id, so it "
                + "would only re-add a button that can never complete", build.contains("microsoft.identity"));
        String manifest = read(ROOT.resolve("app/src/main/AndroidManifest.xml"));
        assertFalse("the MSAL browser-tab activity is back", manifest.contains("BrowserTabActivity"));
    }

    @Test
    public void aCompletedSignInTurnsOnTheAccountSkinLookup() throws IOException {
        // The lookup is default-off, and the sign-in is the one-line consent. Without this the
        // preview can never resolve the player's own skin, which is the "3D skin is not my actual
        // Minecraft skin" report.
        String auth = read(ROOT.resolve(
                "app/src/main/java/org/chimeramc/client/launcher/core/auth/MsftAuthManager.java"));
        int start = auth.indexOf("public static MsftAccountStore.MsftAccount saveAccountAndActivate(");
        assertTrue("saveAccountAndActivate no longer exists", start >= 0);
        int end = auth.indexOf("public static MsftAccountStore.MsftAccount saveAccountOrThrow(", start);
        String body = auth.substring(start, end);
        assertTrue("sign-in must enable the account skin lookup",
                body.contains("AccountSkinService.setEnabled(context, true)"));
    }

    /** The body of {@code MainActivity.startSignIn()}, from its declaration to the next override. */
    private static String startSignInBody() throws IOException {
        String source = read(ROOT.resolve(
                "app/src/main/java/org/chimeramc/client/launcher/ui/activities/MainActivity.java"));
        int start = source.indexOf("public void startSignIn()");
        assertTrue("MainActivity.startSignIn no longer exists", start >= 0);
        int end = source.indexOf("@Override", start);
        return end < 0 ? source.substring(start) : source.substring(start, end);
    }

    private static Path findRoot() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.isRegularFile(dir.resolve("app/src/main/AndroidManifest.xml"))) return dir;
            dir = dir.getParent();
        }
        throw new IllegalStateException("cannot locate the repository root from "
                + Paths.get("").toAbsolutePath());
    }

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
