package io.github.ramboxeu.chainmail.utils;

import io.github.ramboxeu.chainmail.container.ModInstanceWrapperBuilder;
import io.github.ramboxeu.chainmail.container.ModInstanceWrapperBuilder.Entrypoint;
import io.github.ramboxeu.chainmail.modjson.FabricModJson;
import net.minecraftforge.forgespi.language.IModInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.github.ramboxeu.chainmail.container.ModInstanceWrapperBuilder.*;

public class Utils {

    @SuppressWarnings("unchecked")
    public static List<Entrypoint> getEntrypoints(IModInfo info) {
        List<Entrypoint> entrypoints = new ArrayList<>();

        List<String> client = (List<String>) info.getModProperties().getOrDefault("clientEntrypoints", Collections.emptyList());

        if (!client.isEmpty()) {
            entrypoints.addAll(client.stream()
                    .map(s -> new Entrypoint(s, Environment.CLIENT))
                    .collect(Collectors.toList()));
        }

        List<String> server = (List<String>) info.getModProperties().getOrDefault("serverEntrypoints", Collections.emptyList());

        if (!server.isEmpty()) {
            entrypoints.addAll(server.stream()
                    .map(s -> new Entrypoint(s, Environment.SERVER))
                    .collect(Collectors.toList()));
        }

        List<String> common = (List<String>) info.getModProperties().getOrDefault("commonEntrypoints", Collections.emptyList());

        if (!common.isEmpty()) {
            entrypoints.addAll(common.stream()
                    .map(s -> new Entrypoint(s, Environment.COMMON))
                    .collect(Collectors.toList()));
        }

        return entrypoints;
    }
}
