package io.github.ramboxeu.chainmail.remapper;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.INameMappingService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.commons.Remapper;

import java.util.Optional;
import java.util.function.BiFunction;

public class TinyRemapper extends Remapper {
    private static final Logger LOGGER = LogManager.getLogger();

    public TinyRemapper() {

    }

    @Override
    public String mapMethodName(String owner, String name, String descriptor) {
        if (isClassWhitelisted(owner)) {
            String mappedName = mapName(INameMappingService.Domain.METHOD, name);
            LOGGER.debug("Remapping : Method : {}{} of {}", name, descriptor, owner);
            return mappedName;
        }

        return name;
    }

    @Override // TODO: 1/2/2021 Whitelist
    public String mapInvokeDynamicMethodName(String name, String descriptor) {
        LOGGER.debug("Remapping : Method : dynamic {}{}", name, descriptor);
        return name;
    }

    @Override
    public String mapFieldName(String owner, String name, String descriptor) {
        if (isClassWhitelisted(name)) {
            String mappedName = mapName(INameMappingService.Domain.FIELD, name);
            LOGGER.debug("Remapping : Field : {} {} of {}", descriptor, name, owner);
            return mappedName;
        }

        return name;
    }

    @Override
    public String mapPackageName(String name) {
        LOGGER.debug("Remapping : Package : {}", name);
        return name;
    }

    @Override
    public String mapModuleName(String name) {
        LOGGER.debug("Remapping : Module : {}", name);
        return name;
    }

    @Override
    public String map(String internalName) {
        if (isClassWhitelisted(internalName)) {
            String mappedName = mapName(INameMappingService.Domain.CLASS, internalName);
            LOGGER.debug("Remapping : {} found {}", internalName, mappedName);
            return mappedName;
        }

        return internalName;
    }

    private static String mapName(INameMappingService.Domain domain, String name) {
        return Optional.ofNullable(Launcher.INSTANCE)
                .map(Launcher::environment)
                .flatMap(env -> env.findNameMapping("tiny"))
                .map(mapper -> mapper.apply(domain, name))
                .orElse(name);
    }

    // TODO: Improve mojang package matching
    private boolean isClassWhitelisted(String name) {
        return name.startsWith("net/minecraft") || (name.startsWith("com/mojang"));
    }
}
