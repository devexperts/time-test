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

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.GeneratorAdapter;

import static com.devexperts.timetest.TransformationUtils.*;
import static org.objectweb.asm.Opcodes.*;

class ChangeTimeMethodsMethodTransformer extends MethodVisitor {
    private final GeneratorAdapter mv;

    ChangeTimeMethodsMethodTransformer(GeneratorAdapter mv) {
        super(ASM_API, mv);
        this.mv = mv;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (processSystemTime(opcode, owner, name, desc, itf) ||
            processSleep(opcode, owner, name, desc, itf) ||
            processWait(opcode, owner, name, desc, itf) ||
            processNotify(opcode, owner, name, desc, itf) ||
            processUnsafe(opcode, owner, name, desc, itf))
            return;
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    private boolean processUnsafe(int opcode, String owner, String name, String desc, boolean itf) {
        if (owner.equals("sun/misc/Unsafe")) {
            if (name.equals("park")) {
                mv.invokeStatic(METHODS_TYPE, PARK);
                mv.pop();
                return true;
            } else if (name.equals("unpark")) {
                mv.invokeStatic(METHODS_TYPE, UNPARK);
                mv.pop();
                return true;
            }
        }
        return false;
    }

    private boolean processWait(int opcode, String owner, String name, String desc, boolean itf) {
        if (opcode == INVOKEVIRTUAL && name.equals("wait")) {
            switch (desc) {
            case "()V":
                mv.invokeStatic(METHODS_TYPE, WAIT_ON_0);
                return true;
            case "(J)V":
                mv.invokeStatic(METHODS_TYPE, WAIT_ON_1);
                return true;
            case "(JI)V":
                mv.invokeStatic(METHODS_TYPE, WAIT_ON_2);
                return true;
            }
        }
        return false;
    }

    private boolean processNotify(int opcode, String owner, String name, String desc, boolean itf) {
        if (opcode == INVOKEVIRTUAL && name.equals("notify") && desc.equals("()V")) {
            mv.invokeStatic(METHODS_TYPE, NOTIFY);
            return true;
        } else if (opcode == INVOKEVIRTUAL && name.equals("notifyAll") && desc.equals("()V")) {
            mv.invokeStatic(METHODS_TYPE, NOTIFY_ALL);
            return true;
        }
        return false;
    }

    private boolean processSleep(int opcode, String owner, String name, String desc, boolean itf) {
        if (!owner.equals("java/lang/Thread"))
            return false;
        if (name.equals("sleep")) {
            if (desc.equals("(J)V")) {
                mv.invokeStatic(METHODS_TYPE, SLEEP_1);
                return true;
            } else if (desc.equals("(JI)V")) {
                mv.invokeStatic(METHODS_TYPE, SLEEP_2);
                return true;
            }
        }
        return false;
    }

    private boolean processSystemTime(int opcode, String owner, String name, String desc, boolean itf) {
        if (!owner.equals("java/lang/System"))
            return false;
        if (name.equals("currentTimeMillis") && desc.equals("()J")) {
            mv.invokeStatic(METHODS_TYPE, TIME_MILLIS);
            return true;
        } else if (name.equals("nanoTime") && desc.equals("()J")) {
            mv.invokeStatic(METHODS_TYPE, NANO_TIME);
            return true;
        }
        return false;
    }
}
