package com.devexperts.timetest;

/*
 * #%L
 * transformer
 * %%
 * Copyright (C) 2015 - 2017 Devexperts, LLC
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

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import static com.devexperts.timetest.TransformationUtils.*;
import static org.objectweb.asm.Opcodes.*;

class TestEnterPointsAdder extends MethodVisitor {
    private static final Type THROWABLE_TYPE = Type.getType(Throwable.class);

    private final GeneratorAdapter mv;

    private final Label tryLabel = new Label();
    private final Label catchLabel = new Label();

    TestEnterPointsAdder(GeneratorAdapter mv) {
        super(ASM_API, mv);
        this.mv = mv;
    }

    @Override
    public void visitCode() {
        super.visitCode();
        mv.visitLabel(tryLabel);
        mv.invokeStatic(METHODS_TYPE, ENTER_TRANSFORMED_METHOD);
    }

    @Override
    public void visitInsn(int opcode) {
        switch (opcode) {
        case ARETURN:
        case DRETURN:
        case FRETURN:
        case IRETURN:
        case LRETURN:
        case RETURN:
            mv.invokeStatic(METHODS_TYPE, LEAVE_TRANSFORMED_METHOD);
            mv.visitInsn(opcode);
            break;
        default:
            mv.visitInsn(opcode);
        }
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        mv.visitLabel(catchLabel);
        int throwableLocal = mv.newLocal(THROWABLE_TYPE);
        mv.storeLocal(throwableLocal);
        mv.invokeStatic(METHODS_TYPE, LEAVE_TRANSFORMED_METHOD);
        mv.loadLocal(throwableLocal);
        mv.throwException();
        mv.visitTryCatchBlock(tryLabel, catchLabel, catchLabel, null);
        mv.visitMaxs(maxStack, maxLocals);
    }
}
