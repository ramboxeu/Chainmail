package io.github.ramboxeu.chainmail.locating;

import io.github.ramboxeu.chainmail.language.FabricModFileInfo;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.forgespi.locating.IModLocator;
import net.minecraftforge.forgespi.locating.ModFileFactory;

import java.nio.file.Path;

/**
 * @implNote Although plain {@link net.minecraftforge.forgespi.locating.IModFile} exists, this has to extend
 * {@link ModFile} or else it would crash due to ClassCastException
 */
public class FabricModFile extends ModFile {
    public FabricModFile(Path file, IModLocator locator, ModFileFactory.ModFileInfoParser parser) {
        super(file, locator, parser);
    }

    @Override
    public String toString() {
        return "Fabric Mod File: " + getFilePath();
    }

    public static FabricModFile create(Path file, IModLocator locator) {
        return new FabricModFile(file, locator, FabricModFileInfo::parseJson);
    }
}
