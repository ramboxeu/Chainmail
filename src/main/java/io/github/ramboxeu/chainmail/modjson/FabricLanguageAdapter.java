package io.github.ramboxeu.chainmail.modjson;

/**
 * Fabric specific language adapter type
 */
public enum FabricLanguageAdapter {
    DEFAULT("default", "fabric default"), // Only DEFAULT is supported for now
    KOTLIN("kotlin", ""),
    SCALA("scala", ""),
    GROOVY("groovy", "");

    private final String fabricName;
    private final String forgeName;

    FabricLanguageAdapter(String fabricName, String forgeName) {
        this.fabricName = fabricName;
        this.forgeName = forgeName;
    }

    public String getName() {
        return forgeName;
    }

    public static FabricLanguageAdapter fromFabricName(String name) {
        switch (name) {
            case "default":
                return DEFAULT;
            case "kotlin":
                return KOTLIN;
            case "scala":
                return SCALA;
            case "groovy":
                return GROOVY;
            default:
                throw new IllegalArgumentException(name + " is not valid language adapter name");
        }
    }
}
