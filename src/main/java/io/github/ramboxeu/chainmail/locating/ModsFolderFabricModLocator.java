package io.github.ramboxeu.chainmail.locating;

import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import io.github.ramboxeu.chainmail.utils.PathUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.moddiscovery.AbstractJarFileLocator;
import net.minecraftforge.forgespi.locating.IModFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Locates mods in "mods" folder
 */
public class ModsFolderFabricModLocator extends AbstractJarFileLocator {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String JAR_SUFFIX = ".jar";

    private final Path modsFolder;

    public ModsFolderFabricModLocator() {
        LOGGER.info("ModsFolder locator constructed");
        modsFolder = FMLPaths.MODSDIR.get();
    }

    @Override
    public List<IModFile> scanMods() {
        LOGGER.info("Scanning mods folder {} for Fabric mods", modsFolder);

        return LamdbaExceptionUtils.uncheck(() -> Files.list(modsFolder))
                .sorted(Comparator.comparing(PathUtils::sanitizePath))
                .filter(path -> PathUtils.sanitizePath(path).endsWith(JAR_SUFFIX))
                .map(path -> FabricModFile.create(path, this))
                .peek(fabricModFile -> modJars.compute(fabricModFile, (modFile, fileSystem) -> createFileSystem(modFile)))
                .collect(Collectors.toList());
    }

    @Override
    public String name() {
        return "fabric mods folder";
    }

    @Override
    public String toString() {
        return "{ " + name() + " locator at " + modsFolder + " }";
    }

    @Override
    public void initArguments(Map<String, ?> arguments) {

    }
}
