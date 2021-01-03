package io.github.ramboxeu.chainmail.remapper;

import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.TraceClassVisitor;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RemapperTransformer implements ITransformer<ClassNode> {
    private static final Logger LOGGER = LogManager.getLogger();

    private final List<Type> classes;

    public RemapperTransformer(List<Type> classes) {
        this.classes = classes;
    }

    @Override
    public ClassNode transform(ClassNode input, ITransformerVotingContext context) {
        LOGGER.debug("Transforming {}", input.name);

        ClassNode remappedNode = new ClassNode();
        input.accept(new ClassRemapper(remappedNode, new TinyRemapper()));

        return remappedNode;
    }

    @Override
    public TransformerVoteResult castVote(ITransformerVotingContext context) {
        return TransformerVoteResult.YES;
    }

    @Override
    public Set<Target> targets() {
        return classes.stream().map(clazz -> ITransformer.Target.targetPreClass(clazz.getClassName())).collect(Collectors.toSet());
    }
}
