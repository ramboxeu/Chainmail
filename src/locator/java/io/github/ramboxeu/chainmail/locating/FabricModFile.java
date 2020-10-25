package io.github.ramboxeu.chainmail.locating;

import io.github.ramboxeu.chainmail.modjson.FabricModJson;
import io.github.ramboxeu.chainmail.modjson.SimpleConfigWrapper;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.moddiscovery.CoreModFile;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.forgespi.language.IConfigurable;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.language.IModLanguageProvider;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.forgespi.locating.IModLocator;
import net.minecraftforge.forgespi.locating.ModFileFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @implNote Although plain {@link net.minecraftforge.forgespi.locating.IModFile} exists, this has to extend
 * {@link ModFile} or else it would crash due to ClassCastException
 */
public class FabricModFile extends ModFile {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Path path;
    private final IModLocator locator;
    private IModLanguageProvider loader;
    private final ModFileFactory.ModFileInfoParser parser;
    private IModFileInfo modFileInfo;

    public FabricModFile(Path path, IModLocator locator, ModFileFactory.ModFileInfoParser parser) {
        super(path, locator, parser);
        this.path = path;
        this.locator = locator;
        this.parser = parser;
    }

    @Override
    public boolean identifyMods() {
        modFileInfo = parser.build(this);
        if (modFileInfo == null) {
            return false;
        }

        LOGGER.debug("Loading Fabric mod: {}", getFilePath());
        return true;
    }

    @Override
    public Optional<Path> getAccessTransformer() {
        return Optional.empty();
    }

    @Override
    public List<CoreModFile> getCoreMods() {
        return Collections.emptyList();
    }

    @Override
    public IModFileInfo getModFileInfo() {
        return modFileInfo;
    }

    @Override
    public Path getFilePath() {
        return path;
    }

    @Override
    public Path findResource(String className) {
        return locator.findPath(this, className);
    }

    @Override
    public List<IModInfo> getModInfos() {
        return modFileInfo.getMods();
    }

    @Override
    public Type getType() {
        return Type.MOD;
    }

    @Override
    public void identifyLanguage() {
        // FIXME: 10/23/2020 34 is not a proper loader version (but Forge somehow finds it)
        try {
            this.loader = FMLLoader.getLanguageLoadingProvider().findLanguage(this, "fabric", VersionRange.createFromVersionSpec("[34,)"));
        } catch (InvalidVersionSpecificationException ignored) {}
    }

    @Override
    public IModLanguageProvider getLoader() {
        return loader;
    }

    @Override
    public IModLocator getLocator() {
        return locator;
    }

    @Override
    public String toString() {
        return "Fabric Mod File: " + getFilePath();
    }

    public static FabricModFile create(Path file, IModLocator locator) {
        return new FabricModFile(file, locator, FabricModFile::createForgeModFileInfo);
    }

    private static IModFileInfo createForgeModFileInfo(IModFile modFile) {
        LOGGER.debug("Potential Fabric mod: {}", modFile.getFilePath());
        Path modJson = modFile.findResource("fabric.mod.json");

        if (!Files.exists(modJson)) {
            // Prevent spamming waring for every (possible) Forge mod found
            if (!Files.exists(modFile.getLocator().findPath(modFile, "META-INF", "mods.toml"))) {
                LOGGER.warn("Mod file {} is missing fabric.mod.json", modFile);
                return null;
            }
        }

        try {
            FabricModJson parsedModJson = FabricModJson.parseJson(modJson);

            if (parsedModJson != null) {
                LOGGER.debug("Found {}@{}", parsedModJson.getModId(), parsedModJson.getVersion());

                try {
                    // ModFileInfo constructor is package-private, so it has to be instantiated through reflection
                    Constructor<ModFileInfo> constructor = ModFileInfo.class.getDeclaredConstructor(ModFile.class, IConfigurable.class);
                    constructor.setAccessible(true);

                    return constructor.newInstance((ModFile) modFile, SimpleConfigWrapper.wrapFabricModJson(parsedModJson));
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException e) {
                    LOGGER.debug("Refection based Forge ModFileInfo creator failed: {}", () -> e);
                } catch (InvocationTargetException e) {
                    LOGGER.debug("Refection based Forge ModFileInfo creator failed: {}", e::getTargetException);
                }
            }
        } catch (IOException e) {
            LOGGER.fatal("Error occurred while trying to parse {} fabric.mod.json: {}", modFile, e);
        }

        return null;
    }
}
