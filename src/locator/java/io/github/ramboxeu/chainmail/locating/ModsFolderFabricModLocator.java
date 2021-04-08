package io.github.ramboxeu.chainmail.locating;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.common.jimfs.PathType;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import io.github.ramboxeu.chainmail.utils.PathUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.moddiscovery.AbstractJarFileLocator;
import net.minecraftforge.forgespi.locating.IModFile;
import org.antlr.v4.runtime.ANTLRErrorStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * Locates mods in "mods" folder
 */
public class ModsFolderFabricModLocator extends AbstractJarFileLocator {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String JAR_SUFFIX = ".jar";

    private final Path modsFolder;
    private final FileSystem embeddedJarStorage = Jimfs.newFileSystem("FabricNestedJarStorage",
            Configuration.builder(PathType.unix())
                    .setRoots("/")
                    .setWorkingDirectory("/")
                    .setAttributeViews("basic")
                    .build()
    );

    public ModsFolderFabricModLocator() {
        LOGGER.info("ModsFolder locator constructed");
        modsFolder = FMLPaths.MODSDIR.get();
    }

    @Override
    public List<IModFile> scanMods() {
        LOGGER.info("Scanning mods folder {} for Fabric mods", modsFolder);

        List<IModFile> modFiles = new ArrayList<>();

        LamdbaExceptionUtils.uncheck(() -> Files.list(modsFolder))
                .sorted(Comparator.comparing(PathUtils::sanitizePath))
                .filter(path -> PathUtils.sanitizePath(path).endsWith(JAR_SUFFIX))
                .forEach(path -> loadJar(path, modFiles));

        return modFiles;
    }

    private void loadJar(Path path, List<IModFile> modFiles) {
        FabricModFile modFile = FabricModFile.create(path, this);
        FileSystem fs = createFileSystem(modFile);

        modJars.put(modFile, fs);
        modFiles.add(modFile);

        findEmbeddedJars(fs, modFiles);
    }

    private void findEmbeddedJars(FileSystem fs, List<IModFile> modFiles) { // use IModFile to get the "fabric.mod.json"
        Path modJsonPath = fs.getPath("", "fabric.mod.json");

        if (Files.exists(modJsonPath)) {
            List<String> embeddedJars = getEmbeddedJarPaths(modJsonPath);

            if (embeddedJars != null) {
                LOGGER.debug("Found embedded jars: {}", embeddedJars);

                for (String path : embeddedJars) {
                    Path extractedJarPath = extractEmbeddedJar(fs.getPath("", path));

                    if (extractedJarPath != null)
                        loadJar(extractedJarPath, modFiles);
                }
            }
        }
    }

    private Path extractEmbeddedJar(Path embeddedPath) {
        try {
            Path dest = modsFolder.resolve(embeddedPath.getFileName().toString());

            if (Files.notExists(dest))
                return Files.copy(embeddedPath, dest);

            return dest;
        } catch (IOException e) {
            LOGGER.error("Failed to copy '{}' embedded jar", embeddedPath);
        }

        return null;
    }

    private static List<String> getEmbeddedJarPaths(Path path) {
        try (JsonReader reader = new JsonReader(Files.newBufferedReader(path))) {
            JsonElement rootElem = new JsonParser().parse(reader);

            if (!rootElem.isJsonObject()) {
                LOGGER.error("Root is not an object, couldn't read embedded jars");
            } else {
                JsonObject root = rootElem.getAsJsonObject();

                if (root.has("jars")) {
                    JsonElement jarsElem = root.get("jars");

                    if (jarsElem.isJsonArray()) {
                        List<String> nestedJars = new ArrayList<>();

                        for (JsonElement jarEntryElem : jarsElem.getAsJsonArray()) {
                            if (jarEntryElem.isJsonObject()) {
                                JsonObject jarEntryObj = jarEntryElem.getAsJsonObject();

                                if (jarEntryObj.has("file")) {
                                    JsonElement fileElem = jarEntryObj.get("file");

                                    if (fileElem.isJsonPrimitive()) {
                                        nestedJars.add(fileElem.getAsString());
                                    }
                                }
                            }
                        }

                        return nestedJars;
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error while reading embedded jars", e);
        }

        return null;
    }

    @Override
    public Optional<Manifest> findManifest(Path file) {
        try (JarInputStream jarStream = new JarInputStream(Files.newInputStream(file), true)) {
            return Optional.ofNullable(jarStream.getManifest());
        } catch (IOException ignored) {}

        return Optional.empty();
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
