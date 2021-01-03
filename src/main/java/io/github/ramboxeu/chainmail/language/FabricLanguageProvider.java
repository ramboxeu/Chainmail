package io.github.ramboxeu.chainmail.language;

import net.minecraftforge.forgespi.language.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

        @SuppressWarnings("unchecked")
        @Override
        public <T> T loadMod(IModInfo info, ClassLoader modClassLoader, ModFileScanData modFileScanResults) {
            try {
                Class<?> fabricContainer = Class.forName("io.github.ramboxeu.chainmail.language.FabricModContainer", true, Thread.currentThread().getContextClassLoader());
                LOGGER.debug("Loading FabricModContainer from classloader {} - got {}", Thread.currentThread().getContextClassLoader(), fabricContainer.getClassLoader());
                Constructor<?> constructor = fabricContainer.getConstructor(IModInfo.class);
                return (T)constructor.newInstance(info);
            } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                LOGGER.fatal("FabricModContainer was not found! ", e);
                throw new RuntimeException(e);
            }
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
