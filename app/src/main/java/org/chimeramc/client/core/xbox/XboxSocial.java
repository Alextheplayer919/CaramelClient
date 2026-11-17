package org.chimeramc.client.core.xbox;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The Xbox Live profile and social-graph routes the "add a friend" flow uses.
 *
 * <p>Two first-party routes, the same ones the Xbox app itself calls, so no undocumented endpoint
 * is invented:
 *
 * <ul>
 *   <li>the profile service's gamertag lookup ({@code profile.xboxlive.com/users/gt(...)}), which
 *       turns a typed name into a real account — its xuid, gamertag and picture;</li>
 *   <li>the social service's follow list ({@code social.xboxlive.com/users/me/people/xuids}), which
 *       is how one account follows another, i.e. how a friend request is actually sent.</li>
 * </ul>
 *
 * <p>Pure URL building and parsing, so the shapes are unit-tested without a device or a network: a
 * response the API reshapes must fail to parse here, not silently draw an invented row.
 */
public final class XboxSocial {

    private XboxSocial() {
    }

    /** One account the profile service resolved from a typed name. */
    public static final class FoundUser {
        public final String xuid;
        public final String gamertag;
        public final String displayName;
        public final String avatarUrl;
        public final String gamerscore;

        public FoundUser(String xuid, String gamertag, String displayName, String avatarUrl,
                         String gamerscore) {
            this.xuid = nz(xuid);
            this.gamertag = nz(gamertag);
            this.displayName = nz(displayName);
            this.avatarUrl = nz(avatarUrl);
            this.gamerscore = nz(gamerscore);
        }

        /** The name to show: the gamertag, or the display name when there is no gamertag. */
        public String label() {
            return gamertag.isEmpty() ? displayName : gamertag;
        }

        private static String nz(String s) {
            return s == null ? "" : s;
        }
    }

    /**
     * The profile settings the lookup asks for, matching the Xbox app's own request.
     *
     * <p>Only documented setting names are requested. An unknown one makes the profile service
     * reject the whole lookup with HTTP 400, and the caller reads a 400 as "no such player" — so a
     * single bad name silently turned every search into "player not found". {@code DisplayPic} was
     * exactly that: the service knows {@code DisplayPicRaw}, not {@code DisplayPic}.
     */
    private static final String SETTINGS =
            "AppDisplayName,AppDisplayPicRaw,DisplayPicRaw,GameDisplayPicRaw,Gamerscore,Gamertag,"
                    + "PublicGamerpic,XboxOneRep";

    /**
     * The profile-service URL that resolves one gamertag.
     *
     * <p>The gamertag is lower-cased and percent-encoded exactly as the service expects; an
     * unencodable name yields null so the caller reports a bad name rather than calling a broken URL.
     */
    public static String searchUrl(String gamertag) {
        if (gamertag == null || gamertag.trim().isEmpty()) return null;
        try {
            String encoded = URLEncoder.encode(gamertag.trim().toLowerCase(java.util.Locale.ROOT),
                    "UTF-8");
            return "https://profile.xboxlive.com/users/gt(" + encoded + ")/profile/settings"
                    + "?settings=" + SETTINGS;
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    /**
     * The social-service URL that follows one account, which is what "add friend" means on Xbox.
     *
     * <p>The body is {@code {"xuids":["..."]}}; the {@code method=add} query selects the follow
     * direction, matching the Xbox app's own follow call.
     */
    public static String addFriendUrl() {
        return "https://social.xboxlive.com/users/me/people/xuids?method=add";
    }

    /** The JSON body a follow call sends for one xuid. */
    public static String addFriendBody(String xuid) {
        if (xuid == null || xuid.trim().isEmpty()) return "";
        JSONObject root = new JSONObject();
        try {
            JSONArray xuids = new JSONArray();
            xuids.put(xuid.trim());
            root.put("xuids", xuids);
        } catch (JSONException e) {
            return "";
        }
        return root.toString();
    }

    /**
     * Parses a profile-service lookup into the resolved account, or null when nothing matched.
     *
     * <p>The response is {@code {"profileUsers":[{"id":..., "settings":[{"id":..,"value":..}]}]}}.
     * A malformed body or an empty list reads as "not found", which is the honest outcome: the
     * caller shows "no player with that name", never an invented account.
     */
    public static FoundUser parseSearch(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            JSONObject root = new JSONObject(json);
            JSONArray users = root.optJSONArray("profileUsers");
            if (users == null || users.length() == 0) return null;
            JSONObject user = users.optJSONObject(0);
            if (user == null) return null;
            String xuid = user.optString("id", "");
            if (xuid.isEmpty()) return null;
            JSONArray settings = user.optJSONArray("settings");
            String gamertag = "";
            String displayName = "";
            String avatar = "";
            String gamerscore = "";
            if (settings != null) {
                for (int i = 0; i < settings.length(); i++) {
                    JSONObject setting = settings.optJSONObject(i);
                    if (setting == null) continue;
                    String key = setting.optString("id", "");
                    String value = setting.optString("value", "");
                    if ("Gamertag".equals(key)) gamertag = value;
                    else if ("AppDisplayName".equals(key)) displayName = value;
                    else if ("AppDisplayPicRaw".equals(key) && avatar.isEmpty()) avatar = value;
                    else if ("GameDisplayPicRaw".equals(key) && avatar.isEmpty()) avatar = value;
                    else if ("Gamerscore".equals(key)) gamerscore = value;
                }
            }
            return new FoundUser(xuid, gamertag, displayName, avatar, gamerscore);
        } catch (JSONException e) {
            return null;
        }
    }

    /** The {@code x-xbl-contract-version} the profile lookup expects. */
    public static String profileContractVersion() {
        return "3";
    }

    /** The {@code x-xbl-contract-version} the social follow call expects. */
    public static String socialContractVersion() {
        return "1";
    }

    /** The {@code Authorization} header value an XSTS token turns into. */
    public static String authorizationHeader(String userHash, String token) {
        return "XBL3.0 x=" + userHash + ";" + token;
    }

    /** Every xuid in a {@code {"xuids":[...]}} body, for tests and diagnostics. */
    public static List<String> parseXuids(String json) {
        List<String> out = new ArrayList<>();
        if (json == null || json.isEmpty()) return out;
        try {
            JSONArray xuids = new JSONObject(json).optJSONArray("xuids");
            if (xuids == null) return out;
            for (int i = 0; i < xuids.length(); i++) {
                String value = xuids.optString(i, "");
                if (!value.isEmpty()) out.add(value);
            }
        } catch (JSONException e) {
            return new ArrayList<>();
        }
        return out;
    }
}
