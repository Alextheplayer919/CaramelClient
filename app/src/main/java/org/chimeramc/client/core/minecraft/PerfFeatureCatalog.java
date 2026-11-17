package org.chimeramc.client.core.minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The per-instance performance catalogue: every entry is a real Bedrock setting.
 *
 * <p>These are <em>game</em> settings, not device tweaks. Each entry names a key the game itself
 * writes into its own {@code options.txt} and reads on the next load, so toggling one changes what
 * the game renders rather than what the launcher does. That is the whole point of the tab: a
 * "performance feature" that only altered a launcher process flag would be a device change dressed
 * up as a game one.
 *
 * <p>The keys and their valid values are the game's own option names, taken from the published
 * Bedrock {@code options.txt} table. The game owns this grammar; the catalogue only mirrors it, so
 * a key the game does not know on an older build is simply ignored by the game when it next loads —
 * which is the safe failure, not a broken file.
 *
 * <p>Pure and Android-free: the list, the groups and the value rules are unit-tested, and the
 * writer, the panel and the tests all read this one table so a feature cannot be listed without a
 * key behind it.
 */
public final class PerfFeatureCatalog {

    /** How a feature is presented and edited. */
    public enum Kind {
        /** On/off; written as the two {@link Feature#values}. */
        TOGGLE,
        /** One of a small fixed set; {@link Feature#values} and {@link Feature#labels} run parallel. */
        CHOICE,
        /** A number between {@link Feature#min} and {@link Feature#max}. */
        RANGE,
    }

    /** One game setting. */
    public static final class Feature {
        public final String id;
        /** The {@code options.txt} key. The game's own name; never invented. */
        public final String key;
        public final String group;
        public final String name;
        public final String description;
        public final Kind kind;
        /** TOGGLE: the off/on values. CHOICE: every option value. */
        public final String[] values;
        /** CHOICE: the display label for each value in {@link #values}; null for other kinds. */
        public final String[] labels;
        public final float min;
        public final float max;
        public final float step;
        /** RANGE: how many decimal places the written value carries. */
        public final int decimals;
        /** The value a reset writes back. */
        public final String defaultValue;
        /**
         * CHOICE: for each option in {@link #values}, whether it needs the deferred (Vibrant
         * Visuals) pipeline to run at all. A true entry is dropped from the offered options on a
         * device that cannot run it, rather than shown as a mode the game would silently ignore.
         * Null (or a short array) means every option is always offered.
         */
        public final boolean[] deferredOptions;

        Feature(String id, String key, String group, String name, String description, Kind kind,
                String[] values, String[] labels, float min, float max, float step, int decimals,
                String defaultValue) {
            this(id, key, group, name, description, kind, values, labels, min, max, step, decimals,
                    defaultValue, null);
        }

        Feature(String id, String key, String group, String name, String description, Kind kind,
                String[] values, String[] labels, float min, float max, float step, int decimals,
                String defaultValue, boolean[] deferredOptions) {
            this.id = id;
            this.key = key;
            this.group = group;
            this.name = name;
            this.description = description;
            this.kind = kind;
            this.values = values;
            this.labels = labels;
            this.min = min;
            this.max = max;
            this.step = step;
            this.decimals = decimals;
            this.defaultValue = defaultValue;
            this.deferredOptions = deferredOptions;
        }

        /** Whether option {@code index} of a CHOICE needs the deferred pipeline. */
        public boolean optionNeedsDeferred(int index) {
            return deferredOptions != null && index < deferredOptions.length && deferredOptions[index];
        }

        /** The value written when the switch is turned on (TOGGLE only). */
        public String onValue() {
            return values != null && values.length > 1 ? values[1] : "1";
        }

        /** The value written when the switch is turned off (TOGGLE only). */
        public String offValue() {
            return values != null && values.length > 0 ? values[0] : "0";
        }

        /** The label for a stored value, or the value itself when it is not one of the options. */
        public String labelFor(String value) {
            if (kind == Kind.CHOICE && values != null) {
                for (int i = 0; i < values.length; i++) {
                    if (values[i].equals(value)) {
                        return labels != null && i < labels.length ? labels[i] : value;
                    }
                }
            }
            if (kind == Kind.TOGGLE) {
                return onValue().equals(value) ? "On" : "Off";
            }
            return value == null ? "" : value;
        }
    }

    private static final List<Feature> FEATURES = build();

    private static final Map<String, Feature> BY_ID = buildIndex();

    private PerfFeatureCatalog() {
    }

    /** Every feature, in display order. */
    public static List<Feature> all() {
        return FEATURES;
    }

    /** The feature with [id], or null. */
    public static Feature find(String id) {
        return id == null ? null : BY_ID.get(id);
    }

    /** The group names, in first-seen display order. */
    public static List<String> groups() {
        List<String> out = new ArrayList<>();
        for (Feature f : FEATURES) {
            if (!out.contains(f.group)) out.add(f.group);
        }
        return out;
    }

    /** The features in [group], in display order. */
    public static List<Feature> inGroup(String group) {
        List<Feature> out = new ArrayList<>();
        for (Feature f : FEATURES) {
            if (f.group.equals(group)) out.add(f);
        }
        return out;
    }

    /**
     * Clamps and formats a RANGE value to what the game accepts.
     *
     * <p>The panel hands a 0..1 slider position; this maps it onto the feature's own range, snaps to
     * the step and formats with the feature's decimal count, so a written value is always one the
     * game can parse.
     */
    public static String rangeValue(Feature feature, float fraction) {
        if (feature == null || feature.kind != Kind.RANGE) return null;
        float clamped = Math.max(0f, Math.min(1f, fraction));
        float raw = feature.min + clamped * (feature.max - feature.min);
        if (feature.step > 0f) {
            raw = feature.min + Math.round((raw - feature.min) / feature.step) * feature.step;
        }
        raw = Math.max(feature.min, Math.min(feature.max, raw));
        return format(raw, feature.decimals);
    }

    /**
     * The CHOICE options this device can actually offer, as indices into {@link Feature#values}.
     *
     * <p>A device that cannot run the deferred pipeline never sees the "Vibrant Visuals" option, so
     * a mode the GPU cannot render is not advertised. Non-choice features, and choices with no
     * deferred-only option, return every index.
     *
     * @param deferredSupported whether {@link DeferredRenderSupport#supported} said yes
     */
    public static int[] offeredChoiceIndices(Feature feature, boolean deferredSupported) {
        if (feature == null || feature.kind != Kind.CHOICE || feature.values == null) {
            return new int[0];
        }
        java.util.List<Integer> kept = new ArrayList<>();
        for (int i = 0; i < feature.values.length; i++) {
            if (!feature.optionNeedsDeferred(i) || deferredSupported) kept.add(i);
        }
        int[] out = new int[kept.size()];
        for (int i = 0; i < kept.size(); i++) out[i] = kept.get(i);
        return out;
    }

    /** The 0..1 slider position for a stored RANGE value; 0 when the value is missing or invalid. */
    public static float rangeFraction(Feature feature, String value) {
        if (feature == null || feature.kind != Kind.RANGE || value == null) return 0f;
        float parsed;
        try {
            parsed = Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return 0f;
        }
        float span = feature.max - feature.min;
        if (span <= 0f) return 0f;
        return Math.max(0f, Math.min(1f, (parsed - feature.min) / span));
    }

    private static String format(float value, int decimals) {
        if (decimals <= 0) {
            return Integer.toString(Math.round(value));
        }
        return String.format(java.util.Locale.ROOT, "%." + decimals + "f", value);
    }

    private static Map<String, Feature> buildIndex() {
        Map<String, Feature> map = new LinkedHashMap<>();
        for (Feature f : FEATURES) {
            map.put(f.id, f);
        }
        return Collections.unmodifiableMap(map);
    }

    private static List<Feature> build() {
        List<Feature> out = new ArrayList<>();

        // --- Frames & pacing -------------------------------------------------------------------
        String frames = "Frames & pacing";
        add(out, "max_framerate", "gfx_max_framerate", frames, "Frame rate cap",
                "Caps the game's frame rate; 0 leaves it unlimited.", Kind.RANGE, null, null,
                0f, 250f, 10f, 0, "0");
        add(out, "vsync", "gfx_vsync", frames, "Vertical sync",
                "Synchronises frames to the display to stop tearing, at the cost of latency.",
                Kind.TOGGLE, null, null, 0f, 0f, 0f, 0, "0");
        add(out, "frame_pacing", "frame_pacing_enabled", frames, "Frame pacing",
                "Lets the game smooth the interval between frames instead of presenting them unevenly.",
                Kind.TOGGLE, null, null, 0f, 0f, 0f, 0, "0");
        add(out, "multithreaded_renderer", "gfx_multithreaded_renderer", frames,
                "Multithreaded renderer",
                "Builds the frame across several threads; the biggest single-thread win when it works.",
                Kind.TOGGLE, null, null, 0f, 0f, 0f, 0, "1");
        add(out, "ecomode", "ecomode_toggle", frames, "Eco mode",
                "The game's own low-power mode; trades some visual detail for battery.",
                Kind.TOGGLE, null, null, 0f, 0f, 0f, 0, "1");
        add(out, "msaa", "gfx_msaa", frames, "Multisample anti-aliasing",
                "Smooths jagged edges; each step multiplies the render cost, so 1 is the fast choice.",
                Kind.CHOICE, new String[]{"1", "2", "4", "8", "16"},
                new String[]{"Off (1x)", "2x", "4x", "8x", "16x"}, 0f, 0f, 0f, 0, "1");
        add(out, "texel_aa", "gfx_texel_aa_2", frames, "Texel anti-aliasing",
                "The second texel sampling pass; off is cheaper and slightly harder-edged.",
                Kind.TOGGLE, null, null, 0f, 0f, 0f, 0, "0");
        add(out, "texture_streaming", "gfx_texture_streaming", frames, "Texture streaming",
                "Streams textures in as they are needed instead of holding them all in memory.",
                Kind.TOGGLE, null, null, 0f, 0f, 0f, 0, "1");

        // --- Distance & detail -----------------------------------------------------------------
        String distance = "Distance & detail";
        add(out, "view_distance", "gfx_viewdistance", distance, "View distance",
                "How far the world is drawn. The single largest cost in the frame.",
                Kind.RANGE, null, null, 80f, 1536f, 16f, 0, "320");
        add(out, "particle_distance", "gfx_particleviewdistance", distance, "Particle view distance",
                "A multiplier on how far particles are drawn; lower keeps particle bursts cheap.",
                Kind.RANGE, null, null, 0f, 1f, 0.05f, 2, "0.5");
        add(out, "raytracing_distance", "raytracing_viewdistance", distance, "Ray tracing distance",
                "View distance for the ray-traced pipeline; only used when the ray tracer is on.",
                Kind.RANGE, null, null, 0f, 128f, 4f, 0, "64");
        add(out, "deferred_distance", "deferred_viewdistance", distance, "Deferred render distance",
                "View distance for the deferred (Vibrant Visuals) pipeline.",
                Kind.RANGE, null, null, 0f, 128f, 4f, 0, "64");
        add(out, "graphics_mode", "graphics_mode", distance, "Graphics mode",
                "Simple drops most effects, Fancy keeps them, Vibrant Visuals adds the deferred pipeline.",
                Kind.CHOICE, new String[]{"0", "1", "2"},
                new String[]{"Simple", "Fancy", "Vibrant Visuals"}, 0f, 0f, 0f, 0, "1",
                new boolean[]{false, false, true});
        add(out, "quality_preset", "graphics_quality_preset_mode", distance, "Quality preset",
                "The game's own two-way preset: favour performance or favour visuals.",
                Kind.CHOICE, new String[]{"0", "1"},
                new String[]{"Favour performance", "Favour visuals"}, 0f, 0f, 0f, 0, "0");
        add(out, "graphics_api", "graphics_api", distance, "Graphics API",
                "Selects the rendering backend the game uses on this instance.",
                Kind.CHOICE, new String[]{"0", "1"},
                new String[]{"Default", "Alternate"}, 0f, 0f, 0f, 0, "0");

        // --- Lighting & sky --------------------------------------------------------------------
        String lighting = "Lighting & sky";
        add(out, "smooth_lighting", "gfx_smoothlighting", lighting, "Smooth lighting",
                "Softens the light gradient across blocks; off is a flatter, cheaper look.",
                Kind.TOGGLE, null, null, 0f, 0f, 0f, 0, "1");
        add(out, "fancy_skies", "gfx_fancyskies", lighting, "Fancy skies",
                "Draws the full sky with sun, moon and stars rather than the flat version.",
                Kind.TOGGLE, null, null, 0f, 0f, 0f, 0, "1");
        add(out, "clouds", "gfx_toggleclouds", lighting, "Clouds",
                "Draws clouds at all; turning them off removes a full-screen pass.",
                Kind.TOGGLE, null, null, 0f, 0f, 0f, 0, "1");
        add(out, "cloud_quality", "cloud_quality", lighting, "Cloud quality",
                "How detailed the cloud layer is drawn.", Kind.CHOICE,
                new String[]{"0", "1", "2", "3"},
                new String[]{"Low", "Medium", "High", "Ultra"}, 0f, 0f, 0f, 0, "1");
        add(out, "shadow_quality", "shadow_quality", lighting, "Shadow quality",
                "Resolution of the shadow map; the deferred pipeline reads it.",
                Kind.CHOICE, new String[]{"0", "1", "2", "3"},
                new String[]{"Low", "Medium", "High", "Ultra"}, 0f, 0f, 0f, 0, "1");
        add(out, "block_light_quality", "block_light_quality", lighting, "Block light quality",
                "Standard light, volumetric lights, or the full ray-traced path.",
                Kind.CHOICE, new String[]{"0", "1", "2"},
                new String[]{"Standard", "Volumetric", "Ray traced"}, 0f, 0f, 0f, 0, "1");
        add(out, "fog_quality", "volumetric_fog_quality", lighting, "Volumetric fog",
                "Adds light-scattering fog; off removes the pass entirely.",
                Kind.CHOICE, new String[]{"0", "1", "2", "3"},
                new String[]{"Off", "Low", "Medium", "High"}, 0f, 0f, 0f, 0, "2");
        add(out, "reflections_quality", "reflections_quality", lighting, "Reflections",
                "Screen-space reflections on water and other surfaces.",
                Kind.CHOICE, new String[]{"0", "1", "2", "3"},
                new String[]{"Off", "Low", "Medium", "High"}, 0f, 0f, 0f, 0, "2");
        add(out, "bloom", "bloom_strength", lighting, "Bloom strength",
                "How much bright light spills into the surrounding pixels.",
                Kind.RANGE, null, null, 0f, 1f, 0.05f, 2, "1");
        add(out, "lighting_upscale", "lighting_mixed_res_upscale", lighting,
                "Mixed-resolution lighting",
                "Computes lighting at a lower resolution and upscales it; a large fill-rate saving.",
                Kind.TOGGLE, null, null, 0f, 0f, 0f, 0, "1");
        add(out, "darkness_modifier", "darkness_effect_modifier", lighting, "Darkness effect",
                "Strength of the Warden's darkness and similar screen effects.",
                Kind.RANGE, null, null, 0f, 1f, 0.05f, 2, "1");

        // --- Particles & effects ---------------------------------------------------------------
        String effects = "Particles & effects";
        add(out, "bubble_particles", "gfx_bubble_particles", effects, "Bubble particles",
                "Draws the bubble particles underwater.", Kind.TOGGLE, null, null,
                0f, 0f, 0f, 0, "1");
        add(out, "screen_distortion", "screen_distortion", effects, "Screen distortion",
                "The wobble applied by portals and the nether.", Kind.RANGE, null, null,
                0f, 1f, 0.05f, 2, "1");
        add(out, "glint_strength", "glint_strength", effects, "Enchantment glint strength",
                "Brightness of the enchanted-item shimmer.", Kind.RANGE, null, null,
                0f, 1f, 0.05f, 2, "0.75");
        add(out, "glint_speed", "glint_speed", effects, "Enchantment glint speed",
                "How fast the enchanted-item shimmer travels.", Kind.RANGE, null, null,
                0f, 1f, 0.05f, 2, "0.5");

        // --- Camera & view ---------------------------------------------------------------------
        String camera = "Camera & view";
        add(out, "field_of_view", "gfx_field_of_view", camera, "Field of view",
                "The camera's field of view in degrees.", Kind.RANGE, null, null,
                30f, 110f, 5f, 0, "60");
        add(out, "fov_toggle", "gfx_field_of_view_toggle", camera, "Field of view toggle",
                "Whether the in-game field-of-view slider is offered.", Kind.TOGGLE, null,
                null, 0f, 0f, 0f, 0, "1");
        add(out, "view_bobbing", "gfx_viewbobbing", camera, "View bobbing",
                "The walking head-bob; off is steadier and often easier to aim with.",
                Kind.TOGGLE, null, null, 0f, 0f, 0f, 0, "1");
        add(out, "damage_bobbing", "gfx_damagebobbing", camera, "Damage tilt",
                "How far the camera tilts when the player takes a hit.",
                Kind.RANGE, null, null, 0f, 1f, 0.05f, 2, "1");
        add(out, "camera_shake", "camera_shake", camera, "Camera shake",
                "Screen shake from explosions and other events.", Kind.TOGGLE, null,
                null, 0f, 0f, 0f, 0, "1");

        // --- Upscaling -------------------------------------------------------------------------
        String upscaling = "Upscaling";
        add(out, "upscaling", "gfx_upscaling", upscaling, "Upscaling",
                "Renders at a lower resolution and scales the result up to the screen.",
                Kind.TOGGLE, null, null, 0f, 0f, 0f, 0, "1");
        add(out, "upscaling_mode", "upscaling_mode", upscaling, "Upscaling mode",
                "Temporal anti-aliasing upscaling, or the cheaper bilinear stretch.",
                Kind.CHOICE, new String[]{"0", "1"},
                new String[]{"TAA upscaling", "Bilinear"}, 0f, 0f, 0f, 0, "1");
        add(out, "upscaling_percentage", "upscaling_percentage", upscaling,
                "Upscaling target",
                "The percentage of native resolution the game renders at before upscaling.",
                Kind.RANGE, null, null, 0f, 100f, 5f, 0, "0");

        // --- Interface -------------------------------------------------------------------------
        String ui = "Interface";
        add(out, "hide_hud", "gfx_hidehud", ui, "Hide HUD",
                "Hides the hotbar, health and everything else the HUD draws.",
                Kind.TOGGLE, null, null, 0f, 0f, 0f, 0, "0");
        add(out, "hide_hand", "gfx_hidehand", ui, "Hide hand",
                "Hides the first-person hand and held item.", Kind.TOGGLE, null, null,
                0f, 0f, 0f, 0, "0");
        add(out, "hide_paperdoll", "gfx_hidepaperdoll", ui, "Hide paper doll",
                "Hides the third-person character preview in the corner.",
                Kind.TOGGLE, null, null, 0f, 0f, 0f, 0, "1");
        add(out, "player_names", "gfx_ingame_player_names", ui, "Player names",
                "Shows other players' names above their heads.", Kind.TOGGLE, null,
                null, 0f, 0f, 0f, 0, "1");
        add(out, "interface_opacity", "gfx_interface_opacity", ui, "Interface opacity",
                "Overall transparency of the interface.", Kind.RANGE, null, null,
                0f, 1f, 0.05f, 2, "1");
        add(out, "hud_opacity", "gfx_hud_background_opacity", ui, "HUD background opacity",
                "How solid the HUD's backing panel is.", Kind.RANGE, null, null,
                0f, 1f, 0.05f, 2, "0.6");
        add(out, "chat_opacity", "gfx_chat_background_opacity", ui, "Chat background opacity",
                "How solid the chat panel is.", Kind.RANGE, null, null,
                0f, 1f, 0.05f, 2, "0.7");
        add(out, "actionbar_opacity", "gfx_actionbar_background_opacity", ui,
                "Action bar background opacity",
                "How solid the bar that shows item names is.", Kind.RANGE, null, null,
                0f, 1f, 0.05f, 2, "0.6");
        add(out, "autosave_icon", "gfx_showautosaveicon", ui, "Autosave icon",
                "Shows the icon that appears while the world is saving.",
                Kind.TOGGLE, null, null, 0f, 0f, 0f, 0, "0");
        add(out, "tooltips", "gfx_tooltips", ui, "Hover tooltips",
                "The small description box shown when hovering an item.",
                Kind.TOGGLE, null, null, 0f, 0f, 0f, 0, "0");
        add(out, "box_selection", "gfx_classic_box_selection", ui, "Classic block outline",
                "The old-style block selection box instead of the modern outline.",
                Kind.TOGGLE, null, null, 0f, 0f, 0f, 0, "1");
        add(out, "screen_animations", "screen_animations", ui, "Menu animations",
                "Transitions and animations in the game's own menus.", Kind.TOGGLE, null,
                null, 0f, 0f, 0f, 0, "1");
        add(out, "panorama_speed", "panorama_scroll_speed", ui, "Menu panorama speed",
                "How fast the main-menu background pans.", Kind.RANGE, null, null,
                0f, 1f, 0.05f, 2, "1");
        add(out, "resizable_ui", "gfx_resizableui", ui, "Resizable interface",
                "Whether the interface scales with the screen.", Kind.TOGGLE, null, null,
                0f, 0f, 0f, 0, "1");
        add(out, "hotbar_scale", "gfx_hotbarScale", ui, "Hotbar scale",
                "Size of the hotbar.", Kind.RANGE, null, null, 0f, 1f, 0.05f, 2, "1");

        return Collections.unmodifiableList(out);
    }

    private static void add(List<Feature> out, String id, String key, String group, String name,
                            String description, Kind kind, String[] values, String[] labels,
                            float min, float max, float step, int decimals, String defaultValue) {
        add(out, id, key, group, name, description, kind, values, labels, min, max, step, decimals,
                defaultValue, null);
    }

    private static void add(List<Feature> out, String id, String key, String group, String name,
                            String description, Kind kind, String[] values, String[] labels,
                            float min, float max, float step, int decimals, String defaultValue,
                            boolean[] deferredOptions) {
        // A toggle is always the off/on pair, so a call site cannot forget it and leave a switch
        // that writes nothing.
        String[] resolvedValues = values;
        if (kind == Kind.TOGGLE && resolvedValues == null) {
            resolvedValues = new String[]{"0", "1"};
        }
        out.add(new Feature(id, key, group, name, description, kind, resolvedValues, labels, min,
                max, step, decimals, defaultValue, deferredOptions));
    }
}
