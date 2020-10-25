package io.github.ramboxeu.chainmail.modjson;

import net.minecraftforge.forgespi.language.IConfigurable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class SimpleConfigWrapper implements IConfigurable {
    private final HashMap<String, Object> config;

    private SimpleConfigWrapper(HashMap<String, Object> config) {
        this.config = config;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getConfigElement(String... key) {
        return Optional.ofNullable((T)config.get(String.join(".", key)));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<? extends IConfigurable> getConfigList(String... key) {
        return (List<? extends IConfigurable>) config.get(String.join(".", key));
    }

    public static SimpleConfigWrapper wrapFabricModJson(FabricModJson modJson) {
        HashMap<String, Object> config = new HashMap<>();
        HashMap<String, Object> modConfig = new HashMap<>();

        // Config is for now created by hand
        modConfig.put("modId", modJson.getModId());
        modConfig.put("version", modJson.getVersion().toString());
        modConfig.put("displayName", modJson.getName());

        config.put("modLoader", "fabric");
        config.put("loaderVersion", "0.0.1"); // TODO: 10/23/2020 Match with Implementation-Version
        config.put("license", modJson.getLicense());
        config.put("mods", Collections.singletonList(new SimpleConfigWrapper(modConfig)));
        config.put("dependencies." + modJson.getModId(), Collections.emptyList());
        config.put("modproperties." + modJson.getModId(), Collections.singletonMap("entrypoints", modJson.getEntrypoints()));

        return new SimpleConfigWrapper(config);
    }
}
