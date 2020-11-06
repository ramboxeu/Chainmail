package io.github.ramboxeu.chainmail.mod;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("chainmail")
public class Chainmail {
    private static final Logger LOGGER = LogManager.getLogger();

    public Chainmail() {
        LOGGER.debug("Constructed dummy mod");
    }
}
