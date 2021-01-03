package io.github.ramboxeu.chainmail.remapper;

import cpw.mods.modlauncher.api.*;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class RemapperTrasfromerService implements ITransformationService {
    private static final Logger LOGGER = LogManager.getLogger();
    private final List<ITransformer> transformers = new ArrayList<>();

    @Override
    public String name() {
        return "chainmail remapper";
    }

    @Override
    public void initialize(IEnvironment environment) {
        LOGGER.debug(environment);
    }

    @Override
    public void beginScanning(IEnvironment environment) {
        LOGGER.debug("Scanning!");
        LamdbaExceptionUtils.uncheck(() -> Files.list(FMLPaths.MODSDIR.get()))
                .filter(path -> path.toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                .map(path -> LamdbaExceptionUtils.uncheck(p -> FileSystems.newFileSystem(p, Thread.currentThread().getContextClassLoader()), path))
                .forEach(fileSystem -> {
                    fileSystem.getRootDirectories().forEach(path -> {
                        try {
                            Stream<Path> classFilePaths = Files.find(path, Integer.MAX_VALUE, (p, a) -> p.getNameCount() > 0 && p.getFileName().toString().endsWith(".class"));
                            List<Type> classes = new ArrayList<>();
                            classFilePaths.forEach(classFilePath -> {
                                try {
                                    InputStream classFile = Files.newInputStream(classFilePath);
                                    ClassReader reader = new ClassReader(classFile);
//                                    LOGGER.debug("Found class: {}", reader.getClassName());
                                    classes.add(Type.getObjectType(reader.getClassName()));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                            transformers.add(new RemapperTransformer(classes));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                });

    }

    @Override
    public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException {
        LOGGER.debug("On load!");
    }


    @Override
    public List<ITransformer> transformers() {
        return transformers;
    }
}
