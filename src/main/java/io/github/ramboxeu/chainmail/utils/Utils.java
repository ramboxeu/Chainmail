package io.github.ramboxeu.chainmail.utils;

import net.minecraftforge.forgespi.language.IModInfo;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Utils {

    @SuppressWarnings("unchecked")
    public static Map<String, List<String>> getEntrypoints(IModInfo info) {
        return (Map<String, List<String>>) info.getModProperties().getOrDefault("entrypoints", Collections.emptyMap());
    }
}
