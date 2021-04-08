package io.github.ramboxeu.chainmail.language;

import io.github.ramboxeu.chainmail.container.IModInstanceWrapper;
import io.github.ramboxeu.chainmail.container.ModInstanceWrapperBuilder;
import io.github.ramboxeu.chainmail.modjson.FabricModJson;
import io.github.ramboxeu.chainmail.utils.Utils;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.event.lifecycle.IModBusEvent;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

public class FabricModContainer extends ModContainer {
    private static final Logger LOGGER = LogManager.getLogger();

    private final IModInstanceWrapper instance;

    public FabricModContainer(IModInfo info) {
        super(info);

        List<ModInstanceWrapperBuilder.Entrypoint> entrypoints = Utils.getEntrypoints(info);
//        if (!entrypoints.isEmpty()) {
            try {
                // This should maybe get moved to construct event
                instance = new ModInstanceWrapperBuilder(entrypoints, modId).assembleClass().newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                LOGGER.error("Error while constructing {} wrapper", modId, e);
                throw new RuntimeException(e);
            }
//        } else {
//            throw new IllegalStateException("Empty entrypoints array");
//        }

        LOGGER.debug("Constructed container for: {}", info.getModId());

        this.contextExtension = () -> null;
    }

    @Override
    public boolean matches(Object mod) {
        return instance.equals(mod);
    }

    @Override
    public Object getMod() {
        return instance;
    }

    @Override
    protected <T extends Event & IModBusEvent> void acceptEvent(T e) {
        LOGGER.debug("Received event: {}", e);

        Class<? extends Event> eventClass = e.getClass();

        if (eventClass == FMLCommonSetupEvent.class) {
            instance.runInitialization();
        }

        if (eventClass == FMLClientSetupEvent.class) {
            instance.runClientInitialization();
        }

        if (eventClass == FMLDedicatedServerSetupEvent.class) {
            instance.runServerInitialization();
        }
    }
}
