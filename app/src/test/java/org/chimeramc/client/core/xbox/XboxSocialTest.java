package org.chimeramc.client.core.xbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

/**
 * The gamertag lookup and follow-request wire shapes, pinned without a network.
 *
 * <p>The add-friend flow is two real Xbox Live calls; the URL building and the parsing are what
 * break silently when the service reshapes a field, so they are exercised here against the real
 * response shapes.
 */
public class XboxSocialTest {

    private static final String ONE_USER = "{"
            + "\"profileUsers\":[{"
            + "\"id\":\"2533274812345678\","
            + "\"settings\":["
            + "{\"id\":\"Gamertag\",\"value\":\"Steve\"},"
            + "{\"id\":\"AppDisplayName\",\"value\":\"Steve\"},"
            + "{\"id\":\"AppDisplayPicRaw\",\"value\":\"https://img/steve.png\"},"
            + "{\"id\":\"Gamerscore\",\"value\":\"12345\"}"
            + "]}]}";

    @Test
    public void aLookupParsesTheAccount() {
        XboxSocial.FoundUser user = XboxSocial.parseSearch(ONE_USER);
        assertEquals("2533274812345678", user.xuid);
        assertEquals("Steve", user.gamertag);
        assertEquals("Steve", user.displayName);
        assertEquals("https://img/steve.png", user.avatarUrl);
        assertEquals("12345", user.gamerscore);
        assertEquals("Steve", user.label());
    }

    @Test
    public void anEmptyOrMalformedLookupReadsAsNotFound() {
        assertNull(XboxSocial.parseSearch(null));
        assertNull(XboxSocial.parseSearch(""));
        assertNull(XboxSocial.parseSearch("{}"));
        assertNull(XboxSocial.parseSearch("{\"profileUsers\":[]}"));
        assertNull("a malformed body must not throw", XboxSocial.parseSearch("not json"));
        // A user with no xuid cannot be followed, so it is not a result.
        assertNull(XboxSocial.parseSearch("{\"profileUsers\":[{\"settings\":[]}]}"));
    }

    @Test
    public void theGamertagIsLowerCasedAndEncoded() {
        String url = XboxSocial.searchUrl("Steve 123");
        assertTrue(url.startsWith("https://profile.xboxlive.com/users/gt("));
        assertTrue(url.contains("steve+123"));
        assertTrue("the lookup must request only real profile settings; an unknown name makes the "
                + "service reject the whole lookup",
                url.contains("settings=AppDisplayName,AppDisplayPicRaw,DisplayPicRaw,"
                        + "GameDisplayPicRaw,Gamerscore,Gamertag,PublicGamerpic,XboxOneRep"));
        assertFalse("DisplayPic is not a profile setting the service knows", url.contains("DisplayPic,"));
        assertNull("a blank name has no URL", XboxSocial.searchUrl("   "));
        assertNull(XboxSocial.searchUrl(null));
    }

    @Test
    public void theFollowBodyCarriesTheXuid() {
        assertEquals("https://social.xboxlive.com/users/me/people/xuids?method=add",
                XboxSocial.addFriendUrl());
        assertEquals("{\"xuids\":[\"2533274812345678\"]}",
                XboxSocial.addFriendBody("2533274812345678"));
        assertEquals("", XboxSocial.addFriendBody("  "));
        assertEquals("", XboxSocial.addFriendBody(null));
        List<String> xuids = XboxSocial.parseXuids(XboxSocial.addFriendBody("42"));
        assertEquals(1, xuids.size());
        assertEquals("42", xuids.get(0));
    }

    @Test
    public void theHeadersAreBuiltCorrectly() {
        assertEquals("XBL3.0 x=hash;token", XboxSocial.authorizationHeader("hash", "token"));
        assertEquals("3", XboxSocial.profileContractVersion());
        assertEquals("1", XboxSocial.socialContractVersion());
    }
}
