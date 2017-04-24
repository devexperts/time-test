package com.devexperts.timetest;

/*
 * #%L
 * transformer
 * %%
 * Copyright (C) 2015 - 2016 Devexperts, LLC
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import com.devexperts.jagent.CachingClassFileTransformer;
import com.devexperts.jagent.ClassInfo;
import com.devexperts.jagent.ClassInfoCache;
import com.devexperts.jagent.ClassInfoVisitor;
import com.devexperts.jagent.FrameClassWriter;
import com.devexperts.jagent.Log;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.commons.TryCatchBlockSorter;
import org.objectweb.asm.util.CheckClassAdapter;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.devexperts.timetest.TransformationUtils.ASM_API;

class TimeTestTransformer extends CachingClassFileTransformer {
    private final List<Pattern> includes;
    private final List<Pattern> excludes;
    private final List<Pattern> testClassesPatterns;
    private final ClassInfoCache ciCache;

    TimeTestTransformer(Configuration configuration, Log log, String agentVersion) {
        super(log, agentVersion);
        this.ciCache = new ClassInfoCache(log);
        includes = createPatterns(configuration.include());
        excludes = createPatterns(configuration.exclude());
        testClassesPatterns = createPatterns(configuration.testClasses());
    }

    private List<Pattern> createPatterns(String[] strPatterns) {
        return Arrays.stream(strPatterns)
            .map(s -> GlobUtil.compile(s.replaceAll("\\.", "/")))
            .collect(Collectors.toList());
    }

    @Override
    protected boolean processClass(String className, ClassLoader loader) {
        if ((className.startsWith("com/devexperts/timetest/") && !className.startsWith("com/devexperts/timetest/test/"))
                || className.startsWith("com/sun/")
                || (className.startsWith("sun/") && !className.startsWith("sun/swing/") && !className.startsWith("sun/awt/"))
                || className.startsWith("jdk/")
                || (className.startsWith("java/") && !className.startsWith("java/util/concurrent/")))
        {
            return false;
        }
        boolean process = false;
        if (includes.stream().anyMatch(p -> p.matcher(className).matches()))
            process = true;
        if (excludes.stream().anyMatch(p -> p.matcher(className).matches()))
            process = false;
        return process;
    }

    private boolean inTestingCode(String className) {
        return testClassesPatterns.stream().anyMatch(p -> p.matcher(className).matches());
    }

    @Override
    public byte[] transformImpl(ClassLoader loader, final String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
        ClassReader cr = new ClassReader(classfileBuffer);
        ClassInfoVisitor ciVisitor = new ClassInfoVisitor();
        cr.accept(ciVisitor, 0);
        ClassInfo cInfo = ciVisitor.buildClassInfo();
        ciCache.getOrInitClassInfoMap(loader).put(className, cInfo);
        ClassWriter cw = new FrameClassWriter(loader, ciCache, cInfo.getVersion());
        boolean testClass = inTestingCode(className);
        ClassVisitor cv = new ClassVisitor(ASM_API, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String mname, String mdesc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, mname, mdesc, signature, exceptions);
                mv = new JSRInlinerAdapter(mv, access, mname, mdesc, signature, exceptions);
                mv = new ChangeTimeMethodsMethodTransformer(new GeneratorAdapter(mv, access, mname, mdesc));
                if (testClass && !mname.equals("<init>") && !mname.equals("<cinit>")) {
                    mv = new TestEnterPointsAdder(new GeneratorAdapter(mv, access, mname, mdesc));
                    mv = new TryCatchBlockSorter(mv, access, mname, mdesc, signature, exceptions);
                }
                return mv;
            }
        };
        cv  = new CheckClassAdapter(cv); // TODO debug
        cr.accept(cv, ClassReader.EXPAND_FRAMES);
        return cw.toByteArray();
    }
}
