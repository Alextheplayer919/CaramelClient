package org.chimeramc.client.core.cosmetics;

import org.chimeramc.client.core.voice.VoiceChatModule;
import org.chimeramc.client.core.voice.VoicePeer;
import org.chimeramc.client.util.AppLog;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The set of players this client has confirmed are running CaramelClient.
 *
 * <p>Only two protocols speak CaramelClient: proximity voice and cosmetic sync. So a player heard on
 * either is by definition a Caramel user, and that is the only evidence this directory accepts. It
 * deliberately does not guess from a gamertag, a world list or anything the user typed, because a
 * badge beside a name is a claim about another person and must be true.
 *
 * <p>The "add friend" flow needs exactly this: when a typed gamertag resolves to an account, the
 * modal shows the Caramel badge only if that account's xuid is one we have actually heard on the
 * wire. Otherwise it shows the plain account with no badge, which is the honest answer.
 *
 * <p>Pure and JVM-testable: it is a set of identifiers, not a network call.
 */
public final class CaramelUserDirectory {

    private static final String TAG = "CaramelUserDirectory";

    private CaramelUserDirectory() {
    }

    /** The lower-cased ids of every peer confirmed on a Caramel-only protocol. */
    public static Set<String> knownXuids() {
        Set<String> out = new LinkedHashSet<>();
        collectCosmeticPeers(out);
        collectVoicePeers(out);
        return out;
    }

    /**
     * Whether {@code xuid} has been seen on a Caramel-only protocol.
     *
     * <p>Matching is case-insensitive on the trimmed id; a null or blank xuid is never a Caramel
     * user, so a half-resolved account cannot accidentally earn a badge.
     */
    public static boolean isCaramelUser(String xuid) {
        if (xuid == null || xuid.trim().isEmpty()) return false;
        return knownXuids().contains(xuid.trim().toLowerCase(java.util.Locale.ROOT));
    }

    /**
     * The pure core: the ids confirmed by a given set of adverts and voice peers.
     *
     * <p>Separated from the singletons so the badge rule itself is unit-testable — it is the rule
     * that matters ("a badge means we heard this account on a Caramel-only channel"), and it must
     * not need a device to be exercised.
     */
    public static Set<String> confirmedIds(List<CosmeticSyncProtocol.Advert> adverts,
                                           List<VoicePeer> voicePeers) {
        Set<String> out = new LinkedHashSet<>();
        if (adverts != null) {
            for (CosmeticSyncProtocol.Advert advert : adverts) {
                if (advert == null) continue;
                // The xuid is the real identity; the peer id is a per-install id and the name may be
                // a device model, so both are kept only as a weak fallback.
                add(out, advert.xuid);
                add(out, advert.peerId);
                add(out, advert.name);
            }
        }
        if (voicePeers != null) {
            for (VoicePeer peer : voicePeers) {
                if (peer == null) continue;
                add(out, peer.deviceId);
                add(out, peer.id);
                add(out, peer.name);
            }
        }
        return out;
    }

    private static void collectCosmeticPeers(Set<String> out) {
        try {
            CosmeticSyncModule module = CosmeticSyncModule.peek();
            if (module == null) return;
            out.addAll(confirmedIds(module.registry().snapshot(), null));
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "directory:cosmetic", "Could not read the cosmetic peer list", t);
        }
    }

    private static void collectVoicePeers(Set<String> out) {
        try {
            VoiceChatModule module = VoiceChatModule.peek();
            if (module == null) return;
            out.addAll(confirmedIds(null, module.audiblePeers()));
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "directory:voice", "Could not read the voice peer list", t);
        }
    }

    private static void add(Set<String> out, String value) {
        if (value == null) return;
        String trimmed = value.trim();
        if (!trimmed.isEmpty()) out.add(trimmed.toLowerCase(java.util.Locale.ROOT));
    }
}
