package io.github.ramboxeu.chainmail.locating;

import io.github.ramboxeu.chainmail.language.FabricModFileInfo;
import net.minecraftforge.fml.loading.moddiscovery.CoreModFile;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.language.IModLanguageProvider;
import net.minecraftforge.forgespi.locating.IModLocator;
import net.minecraftforge.forgespi.locating.ModFileFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    }

    @Override
    public IModLanguageProvider getLoader() {
        return null;
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
        return new FabricModFile(file, locator, FabricModFileInfo::parseJson);
    }
}
