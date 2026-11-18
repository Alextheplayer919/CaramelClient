package org.chimeramc.client.universe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds the JSON spec array {@code caramel_universe_load_mods} takes, from the installed
 * {@code .universe} packages the user has enabled.
 *
 * <p>The Rust loader's {@code load_mods} takes one JSON array of
 * {@code {"id", "lib_path", "dependencies", "permissions", "trusted"}} objects and owns every
 * decision after that (load order, cycle refusal, per-mod permission reduction). This class is the
 * only place that shape is produced on the Java side, so a field rename is a one-line change rather
 * than a silent "the loader saw no mods".
 *
 * <p>Pure and Android-free: it takes plain values and returns a string, so the marshalling is
 * pinned by a JVM test rather than only exercised on a device.
 */
public final class UniverseModSpecs {

    /** One installed package's spec, as the loader needs it. */
    public static final class Spec {
        public final String id;
        public final String libPath;
        public final List<String> dependencies;
        public final List<String> permissions;
        public final boolean trusted;

        public Spec(String id, String libPath, List<String> dependencies,
                    List<String> permissions, boolean trusted) {
            this.id = id == null ? "" : id;
            this.libPath = libPath == null ? "" : libPath;
            this.dependencies = dependencies == null
                    ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(dependencies));
            this.permissions = permissions == null
                    ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(permissions));
            this.trusted = trusted;
        }
    }

    private UniverseModSpecs() {
    }

    /**
     * Renders the specs as the loader's JSON array.
     *
     * <p>A spec with a blank id is dropped: the loader keys everything on the id, so an id-less
     * entry could not be planned, ordered or unloaded. Gson is used rather than hand-built JSON so
     * a path or a permission containing a quote or backslash is escaped correctly — a hand-built
     * string was how a Windows-style path could have corrupted the whole array.
     */
    public static String toJson(List<Spec> specs) {
        JsonArray array = new JsonArray();
        if (specs != null) {
            for (Spec spec : specs) {
                if (spec == null || spec.id.trim().isEmpty()) {
                    continue;
                }
                JsonObject object = new JsonObject();
                object.addProperty("id", spec.id);
                object.addProperty("lib_path", spec.libPath);
                object.add("dependencies", stringArray(spec.dependencies));
                object.add("permissions", stringArray(spec.permissions));
                object.addProperty("trusted", spec.trusted);
                array.add(object);
            }
        }
        return array.toString();
    }

    private static JsonArray stringArray(List<String> values) {
        JsonArray array = new JsonArray();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.trim().isEmpty()) {
                    array.add(value);
                }
            }
        }
        return array;
    }
}
