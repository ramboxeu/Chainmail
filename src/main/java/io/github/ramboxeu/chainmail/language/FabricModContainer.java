package io.github.ramboxeu.chainmail.language;

import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FabricModContainer extends ModContainer {
    private static final Logger LOGGER = LogManager.getLogger();

    public FabricModContainer(IModInfo info) {
        super(info);
        LOGGER.debug("Constructed container for: {}", info.getModId());
    }

    @Override
    public boolean matches(Object mod) {
        return false;
    }

    @Override
    public Object getMod() {
        return null;
    }
}
