package io.github.ramboxeu.chainmail.modjson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class FabricModJson {
    private static final Logger LOGGER = LogManager.getLogger();

    private final String modId;
    private final String license;
    private final String name;
    private final String issuesUrl;
    private final ArtifactVersion version;
    private final Map<EntrypointEnv, List<Entrypoint>> entrypoints;
    private final List<Dependency> dependencies;
    private final List<String> nestedJars;

    public FabricModJson(String modId, String license, String name, String issuesUrl, ArtifactVersion version, Map<EntrypointEnv, List<Entrypoint>> entrypoints, List<Dependency> dependencies, List<String> nestedJars) {
        this.modId = modId;
        this.license = license;
        this.name = name;
        this.issuesUrl = issuesUrl;
        this.version = version;
        this.entrypoints = entrypoints;
        this.dependencies = dependencies;
        this.nestedJars = nestedJars;
    }

    public String getModId() {
        return modId;
    }

    public String getLicense() {
        return license;
    }

    public String getName() {
        return name;
    }

    public ArtifactVersion getVersion() {
        return version;
    }

    public Map<EntrypointEnv, List<Entrypoint>> getEntrypoints() {
        return entrypoints;
    }

    public String getIssues() {
        return issuesUrl;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public List<String> getNestedJars() {
        return nestedJars;
    }

    public static class Entrypoint {
        private final EntrypointLangAdapter adapter;
        private final String entrypoint;

        public Entrypoint(EntrypointLangAdapter adapter, String entrypoint) {
            this.adapter = adapter;
            this.entrypoint = entrypoint;
        }

        public EntrypointLangAdapter getAdapter() {
            return adapter;
        }

        public String getEntrypoint() {
            return entrypoint;
        }
    }

    public enum EntrypointEnv {
        MAIN,
        CLIENT,
        SERVER
    }

    public enum EntrypointLangAdapter {
        DEFAULT("fabric default"), // Only DEFAULT is supported for now
        KOTLIN(""),
        SCALA(""),
        GROOVY("");

        private final String forgeName;

        EntrypointLangAdapter(String forgeName) {
            this.forgeName = forgeName;
        }

        public String getName() {
            return forgeName;
        }
    }

    public static class Dependency {
        private final String id;
        private final VersionRange version;
        private final boolean mandatory;

        public Dependency(String id, VersionRange version, boolean mandatory) {
            this.id = id;
            this.version = version;
            this.mandatory = mandatory;
        }

        public String getId() {
            return id;
        }

        public VersionRange getVersion() {
            return version;
        }

        public boolean isMandatory() {
            return mandatory;
        }
    }

    /**
     * Parses fabric.mod.json. Automatically determines schema version and chooses appropriate parser.
     *
     * @param modJson path to the fabric.mod.json
     * @return object containing parsed metadata
     * @throws IOException if an I/O exception occurs while reading file
     */
    public static FabricModJson parseJson(Path modJson) throws IOException {
        JsonElement rootElem = new JsonParser().parse(new JsonReader(Files.newBufferedReader(modJson)));

        if (!rootElem.isJsonObject()) {
            LOGGER.error("Mod json: {} is invalid, it is not a object", modJson);
            return null;
        }

        JsonObject root = rootElem.getAsJsonObject();

        int schemaVersion = 0;
        if (root.has("schemaVersion")) {
            JsonElement schemaElem = root.get("schemaVersion");
            if (schemaElem.isJsonPrimitive()) {
                try {
                    schemaVersion = schemaElem.getAsInt();
                } catch (NumberFormatException exception) {
                    LOGGER.error("Mod json: {} is invalid, error occurred while parsing schema version: {}", modJson, exception);
                    return null;
                }
            }
        }

        switch (schemaVersion) {
            case 0:
                LOGGER.info("Schema version 0 is not supported yet");
                break;
            case 1:
                return FabricModJsonV1Parser.parseJson(root, modJson.toString());
            default:
                LOGGER.error("Mod json: {} is invalid, {} is not valid schema version", modJson, schemaVersion);
                break;
        }

        return null;
    }
}
