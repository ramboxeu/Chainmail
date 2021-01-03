package io.github.ramboxeu.chainmail.container;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.*;

public class ModInstanceWrapperBuilder {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final InstanceLoader LOADER = new InstanceLoader();

    private static final Type WRAPPER_TYPE = Type.getType(IModInstanceWrapper.class);

    private final String className;
    private final Map<String, List<String>> entrypoints;
    private final List<Entrypoint> classes;

    public ModInstanceWrapperBuilder(Map<String, List<String>> entrypoints, String modId) {
        this.entrypoints = entrypoints;
        this.className = modId + "_Instance";
        this.classes = getClasses();
    }

    @SuppressWarnings("unchecked")
    public Class<? extends IModInstanceWrapper> assembleClass() {
        ClassWriter wrapperWriter = new ClassWriter(0);
        wrapperWriter.visit(V1_8, ACC_PUBLIC | ACC_SUPER, className, null, "java/lang/Object", new String[]{ WRAPPER_TYPE.getInternalName() });

        defineFields(wrapperWriter);

        MethodVisitor ctorVisitor = wrapperWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        defineConstructor(ctorVisitor);

        MethodVisitor runInitVisitor = wrapperWriter.visitMethod(ACC_PUBLIC, "runInitialization", "()V", null, null);
        defineInitialization(runInitVisitor);

        MethodVisitor runClientInitVisitor = wrapperWriter.visitMethod(ACC_PUBLIC, "runClientInitialization", "()V", null, null);
        defineClientInitialization(runClientInitVisitor);

        MethodVisitor runServerInitVisitor = wrapperWriter.visitMethod(ACC_PUBLIC, "runServerInitialization", "()V", null, null);
        defineServerInitialization(runServerInitVisitor);

        wrapperWriter.visitEnd();

        return (Class<? extends IModInstanceWrapper>) LOADER.define(className, wrapperWriter.toByteArray());
    }

    private void defineFields(ClassVisitor visitor) {
        for (Entrypoint entrypoint : classes) {
            visitor.visitField(ACC_PRIVATE | ACC_FINAL, entrypoint.fieldName, entrypoint.type.getDescriptor(), null, null);
        }
    }

    private void defineConstructor(MethodVisitor visitor) {
        visitor.visitCode();
        visitor.visitVarInsn(ALOAD, 0);
        visitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

        for (Entrypoint entrypoint : classes) {
            visitor.visitVarInsn(ALOAD, 0);
            visitor.visitTypeInsn(NEW, entrypoint.type.getInternalName());
            visitor.visitInsn(DUP);
            visitor.visitMethodInsn(INVOKESPECIAL, entrypoint.type.getInternalName(), "<init>", "()V", false);
            visitor.visitFieldInsn(PUTFIELD, className, entrypoint.fieldName, entrypoint.type.getDescriptor());
        }

        visitor.visitInsn(RETURN);
        visitor.visitMaxs(3, 1);
        visitor.visitEnd();
    }

    private void defineInitialization(MethodVisitor visitor) {
        visitor.visitCode();
        visitor.visitVarInsn(ALOAD, 0);

        for (Entrypoint entrypoint : classes) {
            if (entrypoint.isCommon()) {
                visitor.visitFieldInsn(GETFIELD, className, entrypoint.fieldName, entrypoint.type.getDescriptor());
                visitor.visitMethodInsn(INVOKEINTERFACE, "net/fabricmc/api/ModInitializer", "onInitialize", "()V", true);
            }
        }

        visitor.visitInsn(RETURN);
        visitor.visitMaxs(2, 1);
        visitor.visitEnd();
    }

    private void defineClientInitialization(MethodVisitor visitor) {
        visitor.visitCode();
        visitor.visitVarInsn(ALOAD, 0);

        for (Entrypoint entrypoint : classes) {
            if (entrypoint.isClient()) {
                visitor.visitFieldInsn(GETFIELD, className, entrypoint.fieldName, entrypoint.type.getDescriptor());
                visitor.visitMethodInsn(INVOKEINTERFACE, "net/fabricmc/api/ClientModInitializer", "onInitializeClient", "()V", true);
            }
        }

        visitor.visitInsn(RETURN);
        visitor.visitMaxs(2, 1);
        visitor.visitEnd();
    }

    private void defineServerInitialization(MethodVisitor visitor) {
        visitor.visitCode();
        visitor.visitVarInsn(ALOAD, 0);

        for (Entrypoint entrypoint : classes) {
            if (entrypoint.isServer()) {
                visitor.visitFieldInsn(GETFIELD, className, entrypoint.fieldName, entrypoint.type.getDescriptor());
                visitor.visitMethodInsn(INVOKEINTERFACE, "net/fabricmc/api/DedicatedServerModInitializer", "onInitializeServer", "()V", true);
            }
        }

        visitor.visitInsn(RETURN);
        visitor.visitMaxs(2, 1);
        visitor.visitEnd();
    }

    private List<Entrypoint> getClasses() {
        return entrypoints.entrySet().stream()
                .flatMap(entry -> entry.getValue()
                        .stream()
                        .distinct()
                        .map(e -> new Entrypoint(e, entry.getKey())
                        )
                )
                .collect(Collectors.toList());
    }

    private static String getFieldName(String name) {
        String[] parts = name.split("\\.");
        return parts[parts.length - 1] + "Instance";
    }

    private static class InstanceLoader extends ClassLoader {
        private InstanceLoader() {
            super(null);
        }

        @Override
        protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            return Class.forName(name, resolve, Thread.currentThread().getContextClassLoader());
        }

        Class<?> define(String name, byte[] data)
        {
            return defineClass(name, data, 0, data.length);
        }
    }

    private static class Entrypoint {
        private final Type type;
        private final String fieldName;
        private final String env;

        public Entrypoint(String type, String env) {
            this.type = Type.getType("L" + type.replace('.', '/') + ";");
            this.fieldName = env + getFieldName(type);
            this.env = env;
        }

        public boolean isClient() {
            return env.equals("client");
        }

        public boolean isServer() {
            return env.equals("server");
        }

        public boolean isCommon() {
            return env.equals("main");
        }
    }
}
