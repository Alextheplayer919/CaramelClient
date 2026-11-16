package org.chimeramc.client.core.cosmetics;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Persistence for the equipped cosmetic selection.
 *
 * SharedPreferences is right here rather than the settings backup: the selection is small,
 * changes on every tap, and has nothing to do with instance launch. The keys are plain
 * strings, not package paths, so a rebrand does not strand a player's chosen cape.
 */
public final class CosmeticStore {

    private static final String PREFS_NAME = "cosmetics_state";
    private static final String KEY_CAPE = "equipped_cape";
    private static final String KEY_ACCESSORY = "equipped_accessory";
    private static final String KEY_PET = "equipped_pet";
    private static final String KEY_FAVOURITES = "favourites";

    private final SharedPreferences prefs;

    public CosmeticStore(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** The equipped cape id, or {@link CosmeticCatalog#NONE}. */
    public String getEquippedCapeId() {
        return prefs.getString(KEY_CAPE, CosmeticCatalog.NONE);
    }

    public CosmeticCatalog.Cape getEquippedCape() {
        return CosmeticCatalog.equippedCape(getEquippedCapeId());
    }

    /**
     * The cape to wear/render, honouring the Optifine default.
     *
     * <p>When Optifine Mode is on and the player has not equipped a cape, the classic all-black
     * Optifine cape is returned so the character is not left bare. The launcher reads the setting
     * through the stored feature flag so the preview and the launch-time pack sync agree on the
     * same cape without either re-implementing the rule.
     */
    public CosmeticCatalog.Cape getEquippedCapeForDisplay() {
        return CosmeticCatalog.resolveEquippedCape(getEquippedCapeId(), isOptifineModeEnabled());
    }

    private boolean isOptifineModeEnabled() {
        try {
            return org.chimeramc.client.settings.FeatureSettings.getInstance().isOptifineModeEnabled();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public void setEquippedCape(String capeId) {
        prefs.edit().putString(KEY_CAPE, capeId == null ? CosmeticCatalog.NONE : capeId).apply();
    }

    public String getEquippedAccessoryId() {
        return prefs.getString(KEY_ACCESSORY, CosmeticCatalog.NONE);
    }

    public CosmeticCatalog.Accessory getEquippedAccessory() {
        return CosmeticCatalog.equippedAccessory(getEquippedAccessoryId());
    }

    public void setEquippedAccessory(String accessoryId) {
        prefs.edit().putString(KEY_ACCESSORY,
                accessoryId == null ? CosmeticCatalog.NONE : accessoryId).apply();
    }

    public String getEquippedPetId() {
        return prefs.getString(KEY_PET, CosmeticCatalog.NONE);
    }

    public CosmeticCatalog.Pet getEquippedPet() {
        return CosmeticCatalog.equippedPet(getEquippedPetId());
    }

    public void setEquippedPet(String petId) {
        prefs.edit().putString(KEY_PET, petId == null ? CosmeticCatalog.NONE : petId).apply();
    }

    /** The ids the player has starred, in the order they were starred. */
    public Set<String> getFavourites() {
        Set<String> stored = prefs.getStringSet(KEY_FAVOURITES, null);
        return stored == null ? Collections.<String>emptySet()
                : Collections.unmodifiableSet(new LinkedHashSet<>(stored));
    }

    public boolean isFavourite(String cosmeticId) {
        return cosmeticId != null && getFavourites().contains(cosmeticId);
    }

    /** Adds or removes a star, returning the new state so the caller does not re-read the store. */
    public boolean toggleFavourite(String cosmeticId) {
        if (cosmeticId == null || cosmeticId.isEmpty()) return false;
        Set<String> next = toggledFavourite(getFavourites(), cosmeticId);
        prefs.edit().putStringSet(KEY_FAVOURITES, next).apply();
        return next.contains(cosmeticId);
    }

    /**
     * The starred set after tapping [cosmeticId]: the pure rule behind [toggleFavourite].
     *
     * <p>Split out because the persistence is a one-line write while the rule — an id that is
     * present leaves, one that is absent joins — is what the star's behaviour depends on, and this
     * part can be tested without a device.
     */
    public static Set<String> toggledFavourite(Set<String> current, String cosmeticId) {
        Set<String> next = new LinkedHashSet<>(current == null ? Collections.<String>emptySet() : current);
        if (cosmeticId == null || cosmeticId.isEmpty()) return next;
        if (!next.remove(cosmeticId)) next.add(cosmeticId);
        return next;
    }
}
