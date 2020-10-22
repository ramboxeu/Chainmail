package io.github.ramboxeu.chainmail.language;

import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.locating.IModFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.artifact.versioning.VersionRange;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FabricModFileInfo implements IModFileInfo {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public List<IModInfo> getMods() {
        return Collections.emptyList();
    }

    @Override
    public String getModLoader() {
        return "";
    }

    @Override
    public VersionRange getModLoaderVersion() {
        return VersionRange.createFromVersion("");
    }

    @Override
    public boolean showAsResourcePack() {
        return false;
    }

    @Override
    public Map<String, Object> getFileProperties() {
        return Collections.emptyMap();
    }

    @Override
    public String getLicense() {
        return "none";
    }

    public static IModFileInfo parseJson(IModFile modFile) {
        LOGGER.debug("Potential Fabric mod: {}", modFile.getFilePath());
        Path modJson = modFile.findResource("fabric.mod.json");

        if (!Files.exists(modJson)) {
            // Don't spam waring for every (possible) Forge mod found
            if (!Files.exists(modFile.getLocator().findPath(modFile, "META-INF", "mods.toml"))) {
                LOGGER.warn("Mod file {} is missing fabric.mod.json and it is not considered a valid mod", modFile);
                return null;
            }
        }

        return new FabricModFileInfo();
    }
}
