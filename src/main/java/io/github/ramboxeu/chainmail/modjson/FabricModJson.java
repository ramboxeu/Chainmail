package io.github.ramboxeu.chainmail.modjson;

import org.apache.maven.artifact.versioning.ArtifactVersion;

import java.util.Map;

public class FabricModJson {
    private final String modId;
    private final ArtifactVersion version;
    private final Map<EntrypointEnv, Entrypoint> entrypoints;

    public FabricModJson(String modId, ArtifactVersion version) {
        this.modId = modId;
        this.version = version;
        entrypoints = null;
    }

    public String getModId() {
        return modId;
    }

    public ArtifactVersion getVersion() {
        return version;
    }

    public static class Entrypoint {
        private final EntrypointLangAdapter adapter;
        private final String entrypoint;

        public Entrypoint(EntrypointLangAdapter adapter, String entrypoint) {
            this.adapter = adapter;
            this.entrypoint = entrypoint;
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
}
