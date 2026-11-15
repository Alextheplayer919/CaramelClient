package org.chimeramc.client.core.xbox;

import org.chimeramc.client.util.AppLog;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;

/**
 * Resolves a typed gamertag and sends a real Xbox Live follow request.
 *
 * <p>The "Add friend" modal used to be a shell: the field accepted text, a green "found" banner sat
 * under it whether or not anything had been found, and "Send request" only closed the modal. This
 * does the two real calls behind that flow — the profile lookup that turns a name into an account,
 * and the social follow that actually adds it — using the account's own {@code http://xboxlive.com}
 * XSTS token, so no extra consent is needed.
 *
 * <p>Both calls run on one shared executor and report through a callback on that thread; the caller
 * posts to the UI thread. Failures are reported as a reason string rather than thrown, because a
 * lookup that cannot complete must degrade to an explanation, not a crash.
 */
public final class XboxFriendRequestClient {

    /** Where a search's result goes. {@code user} is null when nothing matched or a call failed. */
    public interface SearchCallback {
        void onResult(XboxSocial.FoundUser user, String error);
    }

    /** Where a follow's result goes. */
    public interface AddCallback {
        void onResult(boolean success, String error);
    }

    private static final int TIMEOUT_MS = 12_000;

    private final ExecutorService executor = org.chimeramc.client.util.AppExecutors.single("xbox-friend-request");

    /**
     * Resolves {@code query} to a real account. A blank query is answered immediately, so the caller
     * never issues a call for an empty field.
     */
    public void search(String userHash, String token, String query, SearchCallback callback) {
        String url = XboxSocial.searchUrl(query);
        if (url == null || isBlank(token) || isBlank(userHash)) {
            callback.onResult(null, null);
            return;
        }
        executor.execute(() -> {
            XboxSocial.FoundUser user = null;
            String error = null;
            HttpURLConnection connection = null;
            try {
                connection = open(url, "GET", userHash, token, XboxSocial.profileContractVersion());
                int code = connection.getResponseCode();
                String body = read(code >= 400 ? connection.getErrorStream() : connection.getInputStream());
                if (code >= 200 && code < 300) {
                    user = XboxSocial.parseSearch(body);
                } else if (code == 400 || code == 404) {
                    // The profile service answers a name with no account as 400/404; that is "not
                    // found", not an error worth showing as a failure.
                    user = null;
                } else {
                    error = "Xbox Live returned " + code + ".";
                }
            } catch (Throwable t) {
                error = "Could not reach Xbox Live.";
                AppLog.warnOnce("CaramelClient", "XboxFriendRequestClient:search",
                        "The gamertag lookup failed", t);
            } finally {
                if (connection != null) connection.disconnect();
            }
            callback.onResult(user, error);
        });
    }

    /** Sends a follow (friend) request from the signed-in account to {@code targetXuid}. */
    public void addFriend(String userHash, String token, String targetXuid, AddCallback callback) {
        String body = XboxSocial.addFriendBody(targetXuid);
        if (body.isEmpty() || isBlank(token) || isBlank(userHash)) {
            callback.onResult(false, "There is nothing to send.");
            return;
        }
        executor.execute(() -> {
            boolean success = false;
            String error = null;
            HttpURLConnection connection = null;
            try {
                connection = open(XboxSocial.addFriendUrl(), "POST", userHash, token,
                        XboxSocial.socialContractVersion());
                connection.setDoOutput(true);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(body.getBytes(StandardCharsets.UTF_8));
                }
                int code = connection.getResponseCode();
                if (code >= 200 && code < 300) {
                    success = true;
                } else {
                    String parsed = XboxPeopleHub.parseError(read(connection.getErrorStream()));
                    error = parsed != null ? parsed : ("Xbox Live returned " + code + ".");
                }
            } catch (Throwable t) {
                error = "Could not reach Xbox Live.";
                AppLog.warnOnce("CaramelClient", "XboxFriendRequestClient:addFriend",
                        "The follow request failed", t);
            } finally {
                if (connection != null) connection.disconnect();
            }
            callback.onResult(success, error);
        });
    }

    /** Releases the executor. */
    public void close() {
        executor.shutdownNow();
    }

    private static HttpURLConnection open(String url, String method, String userHash, String token,
                                          String contractVersion) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("Authorization", XboxSocial.authorizationHeader(userHash, token));
        connection.setRequestProperty("x-xbl-contract-version", contractVersion);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        return connection;
    }

    private static String read(java.io.InputStream stream) {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        } catch (Throwable t) {
            return sb.toString();
        }
        return sb.toString();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
