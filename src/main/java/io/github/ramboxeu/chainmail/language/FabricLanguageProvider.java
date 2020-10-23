package io.github.ramboxeu.chainmail.language;

import net.minecraftforge.forgespi.language.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Generic language provider. Its job is to delegate the work to specialized loaders according to config
 */
public class FabricLanguageProvider implements IModLanguageProvider {
    private static final Logger LOGGER = LogManager.getLogger();

    private static class FabricLanguageLoader implements IModLanguageLoader {
        private final String modId;

        public FabricLanguageLoader(String modId) {
            this.modId = modId;
        }

        public String getModId() {
            return modId;
        }

        @Override
        public <T> T loadMod(IModInfo info, ClassLoader modClassLoader, ModFileScanData modFileScanResults) {
            return null;
        }
    }

    @Override
    public String name() {
        return "fabric";
    }

    @Override
    public Consumer<ModFileScanData> getFileVisitor() {
        return scanData -> {
            final Map<String, FabricLanguageLoader> loaderMap = scanData.getIModInfoData().stream()
                    .flatMap(modFileInfo -> modFileInfo.getMods().stream())
                    .map(modInfo -> new FabricLanguageLoader(modInfo.getModId()))
                    .collect(Collectors.toMap(FabricLanguageLoader::getModId, Function.identity(), (a, b) -> a));

            scanData.addLanguageLoader(loaderMap);
        };
    }

    @Override
    public <R extends ILifecycleEvent<R>> void consumeLifecycleEvent(Supplier<R> eventSupplier) {
        LOGGER.debug("Lifecycle event: {}", eventSupplier.get());
    }
}
