package io.github.ramboxeu.chainmail.mappings;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.INameMappingService;
import net.minecraftforge.fml.loading.FMLLoader;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

public class TinyNamingService implements INameMappingService {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Marker DUMP = MarkerManager.getMarker("MAPPINGSDUMP");
    private static final Marker MAPPINGS = MarkerManager.getMarker("MAPPINGS");

    private String lastSrgClass;

    private final HashMap<String, String> tempTinyMap = new HashMap<>(1000);
    private final HashMap<String, String> classes = new HashMap<>(1000);
    private final HashMap<String, String> methods = new HashMap<>(1000);
    private final HashMap<String, String> fields = new HashMap<>(1000);

    public TinyNamingService() {
        LOGGER.info("Constructed naming service");

        URL tinyPath = getClass().getClassLoader().getResource("1.16.3.tiny");
        URL srgPath = getClass().getClassLoader().getResource("1.16.3.tsrg");

        if (tinyPath != null) {
            loadTiny(tinyPath, new TinyMappingAcceptor() {
                @Override
                protected void acceptClass(String obfuscatedName, String tinyName) {
                    tempTinyMap.put(obfuscatedName, tinyName);
                }

                @Override
                protected void acceptMethod(String obfuscatedName, String tinyName, String owner, String descriptor) {
                    tempTinyMap.put(fullMethodName(owner, obfuscatedName, descriptor), tinyName);
                }

                @Override
                protected void acceptField(String obfuscatedName, String tinyName, String owner, String descriptor) {
                    tempTinyMap.put(fullFieldName(owner, obfuscatedName), tinyName);
                }
            });
        } else {
            LOGGER.error("Tiny mapping file not found!");
        }

        if (srgPath != null) {
            loadSrg(srgPath, new SrgMappingAcceptor() {

                @Override
                protected void acceptClass(String obfuscatedName, String srgName) {
                    if (tempTinyMap.containsKey(obfuscatedName)) {
                        classes.put(tempTinyMap.get(obfuscatedName), srgName);
                    } else {
                        LOGGER.warn(MAPPINGS, "{} wasn't found in the Tiny map", obfuscatedName);
                    }
                }

                @Override
                protected void acceptMethod(String obfuscatedName, String srgName, String owner, String descriptor) {
                    String name = fullMethodName(owner, obfuscatedName, descriptor);

                    if (tempTinyMap.containsKey(name)) {
                        methods.put(tempTinyMap.get(name), srgName);
                    } else {
                        LOGGER.warn(MAPPINGS, "{} wasn't found in the Tiny map", name);
                    }
                }

                @Override
                protected void acceptField(String obfuscatedName, String srgName, String owner) {
                    String name = fullFieldName(owner, obfuscatedName);

                    if (tempTinyMap.containsKey(name)) {
                        fields.put(tempTinyMap.get(name), srgName);
                    } else {
                        LOGGER.warn(MAPPINGS, "{} wasn't found in the Tiny map", name);
                    }
                }
            });
        } else {
            LOGGER.error("Srg mapping file not found!");
        }

        dumpMappings();
    }

    private void loadTiny(URL path, TinyMappingAcceptor acceptor) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(path.openStream()))) {
            // Works only for Tiny V1
            LOGGER.debug("Loading Tiny mappings!");
            reader.lines().skip(1)
                    .map(line -> line.split("\t"))
                    .filter(line -> line.length >= 2)
                    .forEach(line -> processTinyLine(line, acceptor));
        } catch (IOException e) {
            LOGGER.error("Error reading Tiny mappings ", e);
        }
    }

    private void processTinyLine(String[] line, TinyMappingAcceptor acceptor) {
        String type = line[0];

        switch (type) {
            case "CLASS":
                acceptor.acceptClass(line[1], line[2]);
                break;
            case "FIELD":
                acceptor.acceptField(line[3], line[4], line[1], line[2]);
                break;
            case "METHOD":
                acceptor.acceptMethod(line[3], line[4], line[1], line[2]);
                break;
        }
    }

    private void loadSrg(URL path, SrgMappingAcceptor acceptor) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(path.openStream()))) {
            reader.lines().forEach(line -> processSrgLine(line, acceptor));
        } catch (IOException e) {
            LOGGER.error("Error reading Srg mappings ", e);
        }
    }

    private void processSrgLine(String s, SrgMappingAcceptor acceptor) {
        if (s.charAt(0) == '\t') {
            String[] line = s.replace("\t", "").split(" ");

            if (line.length == 2) {
                acceptor.acceptField(line[0], line[1], lastSrgClass);
            } else {
                String obfuscatedName = line[0];

                if (!isJavaMethod(obfuscatedName)) {
                    acceptor.acceptMethod(obfuscatedName, line[2], lastSrgClass, line[1]);
                }
            }
        } else {
            String[] line = s.split(" ");
            acceptor.acceptClass(line[0], line[1]);
            lastSrgClass = line[0];
        }
    }

    @Override
    public String mappingName() {
        return "tinytosrg";
    }

    @Override
    public String mappingVersion() {
        return "1";
    }

    @Override
    public Map.Entry<String, String> understanding() {
        return Pair.of("tiny", "mcp");
    }

    @Override
    public BiFunction<Domain, String, String> namingFunction() {
        return this::convertMapping;
    }

    private String convertMapping(Domain domain, String source) {
        LOGGER.debug("Got name '{}', domain {}", source, domain);

        switch (domain) {
            case CLASS:
                return mapClass(source);
            case FIELD:
                return mapField(source);
            case METHOD:
                return mapMethod(source);
        }

        return source;
    }

    private String mapClass(String source) {
        String name = classes.getOrDefault(source, source);
        return FMLLoader.isProduction() ? name : map(Domain.CLASS, name);
    }

    private String mapField(String source) {
        String name = fields.getOrDefault(source, source);
        return FMLLoader.isProduction() ? name : map(Domain.FIELD, name);
    }

    private String mapMethod(String source) {
        String name = methods.getOrDefault(source, source);
        return FMLLoader.isProduction() ? name : map(Domain.METHOD, name);
    }

    private String map(Domain domain, String name) {
        return Optional.ofNullable(Launcher.INSTANCE)
                .map(Launcher::environment)
                .flatMap(env -> env.findNameMapping("srg"))
                .map(mapper -> mapper.apply(domain, name))
                .orElse(name);
    }

    private void dumpMappings() {
        classes.forEach((key, value) -> LOGGER.debug(DUMP, "Mapping : Class : {} -> {}", key, value));
        methods.forEach((key, value) -> LOGGER.debug(DUMP, "Mapping : Method : {} -> {}", key, value));
        fields.forEach((key, value) -> LOGGER.debug(DUMP, "Mapping : Field : {} -> {}", key, value));
    }

    private static boolean isJavaMethod(String name) {
        return name.equals("equals") || name.equals("toString") || name.equals("hashCode");
    }

    private static String fullMethodName(String owner, String name, String descriptor) {
        return owner + "." + name + descriptor;
    }

    private static String fullFieldName(String owner, String name) {
        return owner + "." + name;
    }

    private static abstract class TinyMappingAcceptor {
        protected abstract void acceptClass(String obfuscatedName, String tinyName);
        protected abstract void acceptMethod(String obfuscatedName, String tinyName, String owner, String descriptor);
        protected abstract void acceptField(String obfuscatedName, String tinyName, String owner, String descriptor);
    }

    private static abstract class SrgMappingAcceptor {
        protected abstract void acceptClass(String obfuscatedName, String srgName);
        protected abstract void acceptMethod(String obfuscatedName, String srgName, String owner, String descriptor);
        protected abstract void acceptField(String obfuscatedName, String srgName, String owner);
    }
}
