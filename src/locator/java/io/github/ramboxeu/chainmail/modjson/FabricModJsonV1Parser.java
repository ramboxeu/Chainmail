package io.github.ramboxeu.chainmail.modjson;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.ramboxeu.chainmail.modjson.FabricModJson.Entrypoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import static io.github.ramboxeu.chainmail.modjson.FabricModJson.*;

/**
 * Schema Version 1 fabric.mod.json parser
 */
public class FabricModJsonV1Parser {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Pattern MODID_PATTERN = Pattern.compile("^[a-z][a-z0-9-_]{1,63}$"); // Copied from Fabric wiki

    // Fails softly. If fabric.mod.json file is invalid the mod will not load, but the game won't crash
    public static FabricModJson parseJson(JsonObject root, String modJson) {
        String modId = getModId(root);
        if (modId == null) {
            LOGGER.error("Mod json: {} is invalid, no valid modid found", modJson);
            return null;
        }

        ArtifactVersion version = getVersion(root);
        if (version == null) {
            LOGGER.error("Mod json: {} is invalid, no valid version found", modJson);
            return null;
        }

        String name = getName(root, modId);
        String license = getLicense(root);
        Map<EntrypointEnv, List<Entrypoint>> entrypoints = getEntrypoints(root);

        return new FabricModJson(modId, license, name, version, entrypoints);
    }

    private static int getSchemaVersion(JsonObject root) {
        if (root.has("schemaVersion")) {
            JsonElement schemaElem = root.get("schemaVersion");

            if (schemaElem.isJsonPrimitive()) {
                try {
                    return schemaElem.getAsInt();
                } catch(NumberFormatException ignored) {}
            }
        }

        return -1;
    }

    private static String getModId(JsonObject root) {
        if (root.has("id")) {
            JsonElement modIdElem = root.get("id");

            if (modIdElem.isJsonPrimitive()) {
                String modId = modIdElem.getAsString();

                if (MODID_PATTERN.matcher(modId).matches()) {
                    return modId;
                }
            }
        }

        return null;
    }

    private static ArtifactVersion getVersion(JsonObject root) {
        if (root.has("version")) {
            JsonElement verElem = root.get("version");

            if (verElem.isJsonPrimitive()) {
                return new DefaultArtifactVersion(verElem.getAsString());
            }
        }

        return null;
    }

    private static String getName(JsonObject root, String other) {
        if (root.has("name")) {
            JsonElement nameElem = root.get("name");

            if (nameElem.isJsonPrimitive()) {
                return nameElem.getAsString();
            }
        }

        return other;
    }

    private static String getLicense(JsonObject root) {
        if (root.has("license")) {
            JsonElement licenseElem = root.get("license");

            if (licenseElem.isJsonPrimitive()) {
                return licenseElem.getAsString();
            }
        }

        return "none";
    }

    private static Map<EntrypointEnv, List<Entrypoint>> getEntrypoints(JsonObject root) {
        if (root.has("entrypoints")) {
            JsonElement entrypointsElem = root.get("entrypoints");

            if (entrypointsElem.isJsonObject()) {
                Map<EntrypointEnv, List<Entrypoint>> entrypoints = new EnumMap<>(EntrypointEnv.class);

                for (Map.Entry<String, JsonElement> entry : entrypointsElem.getAsJsonObject().entrySet()) {
                    switch (entry.getKey()) {
                        case "main": {
                            final List<Entrypoint> entrypoint = getEntrypoint(entry.getValue(), EntrypointEnv.MAIN);
                            if (entrypoint != null) entrypoints.putIfAbsent(EntrypointEnv.MAIN, entrypoint);
                            break;
                        }
                        case "server": {
                            final List<Entrypoint> entrypoint = getEntrypoint(entry.getValue(), EntrypointEnv.SERVER);
                            if (entrypoint != null) entrypoints.putIfAbsent(EntrypointEnv.SERVER, entrypoint);
                            break;
                        }
                        case "client": {
                            final List<Entrypoint> entrypoint = getEntrypoint(entry.getValue(), EntrypointEnv.CLIENT);
                            if (entrypoint != null) entrypoints.putIfAbsent(EntrypointEnv.CLIENT, entrypoint);
                            break;
                        }
                    }
                }

                return entrypoints;
            }
        }

        return null;
    }

    public static List<Entrypoint> getEntrypoint(JsonElement element, EntrypointEnv env) {
        if (element.isJsonArray()) {
            JsonArray entrypointsArr = element.getAsJsonArray();
            List<Entrypoint> entrypoints = new ArrayList<>(entrypointsArr.size());

            for (JsonElement entrypointElem : entrypointsArr) {
                if (entrypointElem.isJsonPrimitive()) {
                    entrypoints.add(new Entrypoint(EntrypointLangAdapter.DEFAULT, element.getAsString()));
                } else if (entrypointElem.isJsonObject()) {
                    JsonObject entrypointObj = entrypointElem.getAsJsonObject();

                    if (entrypointObj.has("value") && entrypointObj.has("adapter")) {
                        JsonElement valueElem = entrypointObj.get("value");
                        JsonElement adapterElem = entrypointObj.get("adapter");

                        if (valueElem.isJsonPrimitive() && adapterElem.isJsonPrimitive()) {
                            String value = valueElem.getAsString();

                            // Currently only default adapter is supported
                            if ("default".equals(adapterElem.getAsString())) {
                                entrypoints.add(new Entrypoint(EntrypointLangAdapter.DEFAULT, value));
                            }
                        }
                    }
                }
            }

            return entrypoints.size() > 0 ? entrypoints : null;
        }

        return null;
    }
}
