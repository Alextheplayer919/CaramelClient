package org.chimeramc.client.core.xbox;

/**
 * The signed-in account's Xbox identity, shared with the Caramel-only protocols.
 *
 * <p>Voice and cosmetic sync are the two channels where a Caramel user is identifiable, and both
 * need to say who the sender is. This holds the active account's xuid and gamertag in one place so
 * neither protocol reads the account store itself (which is a launcher-layer type).
 *
 * <p>It is a plain in-memory holder: the launcher sets it when the active account changes, and the
 * protocols read it. Empty means "no account", in which case an advertisement simply carries no
 * xuid and the peer cannot be badged.
 */
public final class CaramelIdentity {

    private static volatile String xuid = "";
    private static volatile String gamertag = "";

    private CaramelIdentity() {
    }

    /** Records the active account's identity. A null value clears that field. */
    public static void set(String newXuid, String newGamertag) {
        xuid = newXuid == null ? "" : newXuid.trim();
        gamertag = newGamertag == null ? "" : newGamertag.trim();
    }

    /** Forgets the identity, e.g. on sign-out. */
    public static void clear() {
        set("", "");
    }

    public static String xuid() {
        return xuid;
    }

    public static String gamertag() {
        return gamertag;
    }
}
