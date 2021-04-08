package io.github.ramboxeu.chainmail.modjson;

import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.forgespi.language.IConfigurable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Forge friendly fabric.mod.json wrapper
 */
public class ConfigWrapper implements IConfigurable {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Map<String, Object> configMap;
    private final String modId;

    private ConfigWrapper(Map<String, Object> configMap, String modId) {
        this.configMap = configMap;
        this.modId = modId;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getConfigElement(String... key) {
        return Optional.ofNullable((T) get(Arrays.asList(key)));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<? extends IConfigurable> getConfigList(String... key) {
        Object value = get(Arrays.asList(key));

        if (value instanceof Collection) {
            return (List<? extends IConfigurable>) value;
        } else {
            throw new IllegalStateException("Expected a collection at " + Arrays.toString(key) + " in " + modId + " configuration!");
        }
    }

    private Object get(List<String> path) {
        int lastElement = path.size() - 1;
        Map<?, ?> map = getMap(path.subList(0, lastElement));

        if (map != null) {
            return map.get(path.get(lastElement));
        }

        return null;
    }

    private Map<?, ?> getMap(List<String> path) {
        Map<?, ?> nested = configMap;

        for (String key : path) {
            Object value = nested.get(key);

            if (!(value instanceof Map)) {
                return null;
            }

            nested = (Map<?, ?>) value;
        }

        return nested;
    }

    public static class Builder {
        private final Map<String, Object> config = new HashMap<>();
        private final Map<String, Object> properties = new HashMap<>();
        private final FabricModJson json;
        private final String modId;

        private Builder(FabricModJson json) {
            this.json = json;
            this.modId = json.getModId();
        }


        public static ConfigWrapper fromJson(FabricModJson json) {
            return new Builder(json).build();
        }

        private ConfigWrapper build() {
            putRoot();
            putMods();
            putEntrypoints();
            putProperties();
            putDependencies();

            return new ConfigWrapper(config, modId);
        }

        private void putDependencies() {
            List<ConfigWrapper> dependencies = new ArrayList<>();
            boolean loaderVersionSet = false;

            for (FabricModJson.Dependency dep : json.getDependencies()) {
                if (dep.getId().equals("fabricloader")) {
                    loaderVersionSet = true;
                    config.put("loaderVersion", dep.getVersion().toString());
                    continue;
                }

                HashMap<String, Object> map = new HashMap<>();
                if (dep.getId().equals("minecraft")) {
                    map.put("modId", "minecraft");
                    map.put("versionRange", "[1.16.3]");
                    map.put("mandatory", true);
                    continue;
                }

                map.put("modId", dep.getId());
                map.put("versionRange", dep.getVersion().toString());
                map.put("mandatory", dep.isMandatory());
                dependencies.add(new ConfigWrapper(map, modId));
            }

            if (!loaderVersionSet) {
                config.put("loaderVersion", ModInfo.UNBOUNDED);
            }

            Map<String, List<ConfigWrapper>> dependencyMap = Collections.singletonMap(modId, dependencies);
            config.put("dependencies", dependencyMap);
        }

        private void putProperties() {
            config.put("modproperties", Collections.singletonMap(modId, properties));
        }

        private void putEntrypoints() {
            List<String> clientEntryPoints = mapEntryPoints(FabricModJson.EntrypointEnv.CLIENT);
            List<String> serverEntryPoints = mapEntryPoints(FabricModJson.EntrypointEnv.SERVER);
            List<String> commonEntryPoints = mapEntryPoints(FabricModJson.EntrypointEnv.MAIN);

            putProperty("clientEntrypoints", clientEntryPoints);
            putProperty("serverEntrypoints", serverEntryPoints);
            putProperty("commonEntrypoints", commonEntryPoints);
        }

        private List<String> mapEntryPoints(FabricModJson.EntrypointEnv env) {
            List<FabricModJson.Entrypoint> entrypoints = json.getEntrypoints().get(env);

            if (entrypoints != null)
                return entrypoints.stream().map(FabricModJson.Entrypoint::getEntrypoint).collect(Collectors.toList());

            return Collections.emptyList();
        }

        private void putMods() {
            Map<String, Object> map = new HashMap<>();

            map.put("modId", json.getModId());
            map.put("version", json.getVersion().toString());
            map.put("displayName", json.getName());

            config.put("mods", Collections.singletonList(new ConfigWrapper(map, "")));
        }

        private void putRoot() {
            config.put("modLoader", "fabric");
            config.put("license", json.getLicense());

            if (!json.getIssues().isEmpty()) {
                config.put("issueTrackerURL", json.getIssues());
            }
        }

        private void putProperty(String key, Object value) {
            properties.put(key, value);
        }
    }
}
