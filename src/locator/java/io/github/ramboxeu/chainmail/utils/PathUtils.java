package io.github.ramboxeu.chainmail.utils;

import java.nio.file.Path;
import java.util.Locale;

public class PathUtils {
    public static String sanitizePath(Path path) {
        return path.toString().toLowerCase(Locale.ROOT);
    }
}
