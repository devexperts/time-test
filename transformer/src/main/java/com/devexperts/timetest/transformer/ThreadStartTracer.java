package com.devexperts.timetest.transformer;

/*
 * #%L
 * transformer
 * %%
 * Copyright (C) 2015 - 2018 Devexperts, LLC
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

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.GeneratorAdapter;

import static com.devexperts.timetest.transformer.TransformationUtils.ASM_API;
import static com.devexperts.timetest.transformer.TransformationUtils.METHODS_TYPE;
import static com.devexperts.timetest.transformer.TransformationUtils.START_THREAD_METHOD;

public class ThreadStartTracer extends MethodVisitor {
    private final GeneratorAdapter mv;

    ThreadStartTracer(GeneratorAdapter mv) {
        super(ASM_API, mv);
        this.mv = mv;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (owner.equals("java/lang/Thread") && name.equals("start0")) {
            mv.dup();
            mv.invokeStatic(METHODS_TYPE, START_THREAD_METHOD);
        }
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }
}
