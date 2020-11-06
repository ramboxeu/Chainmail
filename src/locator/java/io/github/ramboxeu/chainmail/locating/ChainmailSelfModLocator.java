package io.github.ramboxeu.chainmail.locating;

import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.forgespi.locating.IModLocator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.jar.Manifest;

/**
 * Purpose of this locator is locate Chainmail language provider from and embedded JAR. It's needed, because Forge
 * excludes JARs with locators in them from further locating process, during which lang providers are identified.
 * Because the aim of the project is being as simple to use as possible, using this hack users will only need one JAR
 * instead of two.
 */
public class ChainmailSelfModLocator implements IModLocator {
    private static final Logger LOGGER = LogManager.getLogger();

    public ChainmailSelfModLocator() {
        LOGGER.info("Chainmail self locator constructed");

        if (!FMLLoader.isProduction()) {
            LOGGER.info("Running in dev environment");
        } else {
            LOGGER.info("Running in production environment");
        }
    }

    @Override
    public List<IModFile> scanMods() {
        return Collections.emptyList();
    }

    @Override
    public String name() {
        return "Chainmail self locator";
    }

    @Override
    public Path findPath(IModFile modFile, String... path) {
        return null;
    }

    @Override
    public void scanFile(IModFile modFile, Consumer<Path> pathConsumer) {

    }

    @Override
    public Optional<Manifest> findManifest(Path file) {
        return Optional.empty();
    }

    @Override
    public void initArguments(Map<String, ?> arguments) {
        LOGGER.debug("Args: {}", arguments);
    }

    @Override
    public boolean isValid(IModFile modFile) {
        return false;
    }
}
