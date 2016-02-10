package com.devexperts.timetest;

/*
 * #%L
 * transformer
 * %%
 * Copyright (C) 2015 - 2016 Devexperts, LLC
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.objectweb.asm.Opcodes.*;

public class ChangeTimeMethodsTransformer implements ClassFileTransformer {

    private static final int ASM_API = Opcodes.ASM5;
    private final List<Pattern> includes;
    private final List<Pattern> excludes;

    public ChangeTimeMethodsTransformer(Configuration configuration) {
        includes = new ArrayList<>(configuration.include().length);
        for (String s : configuration.include())
            includes.add(GlobUtil.compile(s.replaceAll("\\.", "/")));
        excludes = new ArrayList<>(configuration.exclude().length);
        for (String s : configuration.exclude())
            excludes.add(GlobUtil.compile(s.replaceAll("\\.", "/")));
    }

    @Override
    public byte[] transform(ClassLoader loader, final String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
        if ((className.startsWith("com/devexperts/timetest/") && !className.startsWith("com/devexperts/timetest/test/")) ||
                className.startsWith("sun/") ||
                className.startsWith("java/") ||
                className.startsWith("org/apache/maven/"))
            return null;

        boolean process = false;
        for (Pattern p : includes) {
            if (p.matcher(className).matches()) {
                process = true;
                break;
            }
        }
        for (Pattern p : excludes) {
            if (p.matcher(className).matches()) {
                process = false;
                break;
            }
        }
        if (!process)
            return null;

        ClassReader cr = new ClassReader(classfileBuffer);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassVisitor transformer = new ClassVisitor(ASM_API, cw) {
            @Override
            public MethodVisitor visitMethod(int access, final String mname, String mdesc, String signature, String[] exceptions) {
                return new MethodVisitor(ASM_API, super.visitMethod(access, mname, mdesc, signature, exceptions)) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        if (processSystemTime(opcode, owner, name, desc, itf) ||
                                processSleep(opcode, owner, name, desc, itf) ||
                                processWait(opcode, owner, name, desc, itf) ||
                                processUnsafe(opcode, owner, name, desc, itf))
                            return;
                        super.visitMethodInsn(opcode, owner, name, desc, itf);
                    }

                    private boolean processUnsafe(int opcode, String owner, String name, String desc, boolean itf) {
                        if (owner.equals("sun/misc/Unsafe")) {
                            if (name.equals("park")) {
                                mv.visitMethodInsn(INVOKESTATIC, "com/devexperts/timetest/Methods", "park", "(ZJ)V", false);
                                mv.visitInsn(POP);
                                return true;
                            } else if (name.equals("unpark")) {
                                mv.visitMethodInsn(INVOKESTATIC, "com/devexperts/timetest/Methods", "unpark", "(Ljava/lang/Object;)V", false);
                                mv.visitInsn(POP);
                                return true;
                            }
                        }
                        return false;
                    }

                    private boolean processWait(int opcode, String owner, String name, String desc, boolean itf) {
                        if (opcode == INVOKEVIRTUAL && name.equals("wait")) {
                            switch (desc) {
                                case "()V":
                                    mv.visitMethodInsn(INVOKESTATIC, "com/devexperts/timetest/Methods", "waitOn", "(Ljava/lang/Object;)V", false);
                                    return true;
                                case "(J)V":
                                    mv.visitMethodInsn(INVOKESTATIC, "com/devexperts/timetest/Methods", "waitOn", "(Ljava/lang/Object;J)V", false);
                                    return true;
                                case "(JI)V":
                                    mv.visitMethodInsn(INVOKESTATIC, "com/devexperts/timetest/Methods", "waitOn", "(Ljava/lang/Object;JI)V", false);
                                    return true;
                            }
                        }
                        return false;
                    }

                    private boolean processSleep(int opcode, String owner, String name, String desc, boolean itf) {
                        if (!owner.equals("java/lang/Thread"))
                            return false;
                        if (name.equals("sleep")) {
                            if (desc.equals("(J)V")) {
                                mv.visitMethodInsn(INVOKESTATIC, "com/devexperts/timetest/Methods", "sleep", "(J)V", false);
                                return true;
                            } else if (desc.equals("(JI)V")) {
                                mv.visitMethodInsn(INVOKESTATIC, "com/devexperts/timetest/Methods", "sleep", "(JI)V", false);
                                return true;
                            }
                        }
                        return false;
                    }

                    private boolean processSystemTime(int opcode, String owner, String name, String desc, boolean itf) {
                        if (!owner.equals("java/lang/System"))
                            return false;
                        if (name.equals("currentTimeMillis")) {
                            mv.visitMethodInsn(INVOKESTATIC, "com/devexperts/timetest/Methods", "timeMillis", "()J", false);
                            return true;
                        } else if (name.equals("nanoTime")) {
                            mv.visitMethodInsn(INVOKESTATIC, "com/devexperts/timetest/Methods", "nanoTime", "()J", false);
                            return true;
                        }
                        return false;
                    }
                };
            }
        };
        cr.accept(transformer, 0);
        return cw.toByteArray();
    }
}