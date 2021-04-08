package io.github.ramboxeu.chainmail.modjson;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.ramboxeu.chainmail.modjson.FabricModJson.Entrypoint;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.locating.IModFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

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
        List<Dependency> dependencies = getDependencies(root);
        List<String> nestedJars = getNestedJars(root);

        return new FabricModJson(modId, license, name, "", version, entrypoints, dependencies, nestedJars);
    }

    private static List<String> getNestedJars(JsonObject root) {
        if (root.has("jars")) {
            JsonElement jarsElem = root.get("jars");

            if (jarsElem.isJsonArray()) {
                List<String> nestedJars = new ArrayList<>();

                for (JsonElement jarEntryElem : jarsElem.getAsJsonArray()) {
                    if (jarEntryElem.isJsonObject()) {
                        JsonObject jarEntryObj = jarEntryElem.getAsJsonObject();

                        if (jarEntryObj.has("file")) {
                            JsonElement fileElem = jarEntryObj.get("file");

                            if (fileElem.isJsonPrimitive()) {
                                nestedJars.add(fileElem.getAsString());
                            }
                        }
                    }
                }

                return nestedJars;
            }
        }

        return Collections.emptyList();
    }

    private static List<Dependency> getDependencies(JsonObject root) {
        List<Dependency> dependencies = Collections.emptyList();

        if (root.has("depends")) {
            JsonElement dependenciesElem = root.get("depends");

            if (dependenciesElem.isJsonObject()) {
                dependencies = getDependencies(dependenciesElem.getAsJsonObject().entrySet(), true);
            }
        }

        if (root.has("recommends")) {
            JsonElement dependenciesElem = root.get("recommends");

            if (dependenciesElem.isJsonObject()) {
                List<Dependency> deps = getDependencies(dependenciesElem.getAsJsonObject().entrySet(), false);

                if (dependencies.isEmpty()) {
                    dependencies = deps;
                } else {
                    dependencies.addAll(deps);
                }
            }
        }

        return dependencies;
    }

    private static List<Dependency> getDependencies(Set<Map.Entry<String, JsonElement>> element, boolean mandatory) {
        List<Dependency> dependencies = new ArrayList<>();

        for (Map.Entry<String, JsonElement> dependency : element) {
            JsonElement dependencyElem = dependency.getValue();

            if (dependencyElem.isJsonPrimitive()) {
                VersionRange version = parseVersionRange(dependencyElem.getAsString());

                if (version != null) {
                    dependencies.add(new Dependency(dependency.getKey(), version, mandatory));
                }
            }
        }

        return dependencies;
    }

    // Rudimentary NPM semver parser
    private static VersionRange parseVersionRange(String range) {
        try {
            char a = range.charAt(0);

            if (range.length() > 2) {
                char b = range.charAt(1);

                if (a == '>' && b == '=') {
                    return VersionRange.createFromVersionSpec("[" + range.substring(2) + ",)");
                } else if (a == '<' && b == '=') {
                    return VersionRange.createFromVersionSpec("(," + range.substring(2) + "]");
                } else {
                    return VersionRange.createFromVersionSpec(range.substring(1));
                }
            } else {
                if (a == '>') {
                    return VersionRange.createFromVersionSpec("(" + range.substring(1) + ",)");
                } else if (a == '<') {
                    return VersionRange.createFromVersionSpec("(," + range.substring(1) + ")");
                } else if (a == '*') {
                    return VersionRange.createFromVersionSpec("(0,)");
                } else {
                    return VersionRange.createFromVersionSpec(range.substring(1));
                }
            }

        } catch (InvalidVersionSpecificationException e) {
            return ModInfo.UNBOUNDED;
        }
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

        return Collections.emptyMap();
    }

    private static List<Entrypoint> getEntrypoint(JsonElement element, EntrypointEnv env) {
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
