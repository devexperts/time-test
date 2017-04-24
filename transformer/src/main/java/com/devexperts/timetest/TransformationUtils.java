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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import static org.objectweb.asm.Type.*;

class TransformationUtils {
    static final int ASM_API = Opcodes.ASM5;
    static final Type METHODS_TYPE = Type.getType("com/devexperts/timetest/Methods");
    static final Type OBJECT_TYPE = Type.getType(Object.class);

    static final Method TIME_MILLIS = new Method("timeMillis", LONG_TYPE, new Type[]{});
    static final Method NANO_TIME = new Method("nanoTime", LONG_TYPE, new Type[]{});

    static final Method SLEEP_1 = new Method("sleep", VOID_TYPE, new Type[]{LONG_TYPE});
    static final Method SLEEP_2 = new Method("sleep", VOID_TYPE, new Type[]{LONG_TYPE, INT_TYPE});

    static final Method WAIT_ON_0 = new Method("waitOn", VOID_TYPE, new Type[]{OBJECT_TYPE});
    static final Method WAIT_ON_1 = new Method("waitOn", VOID_TYPE, new Type[]{OBJECT_TYPE, LONG_TYPE});
    static final Method WAIT_ON_2 = new Method("waitOn", VOID_TYPE, new Type[]{OBJECT_TYPE, LONG_TYPE, INT_TYPE});

    static final Method NOTIFY = new Method("notify", VOID_TYPE, new Type[]{OBJECT_TYPE});
    static final Method NOTIFY_ALL = new Method("notifyAll", VOID_TYPE, new Type[]{OBJECT_TYPE});

    static final Method PARK = new Method("park", VOID_TYPE, new Type[]{BOOLEAN_TYPE, LONG_TYPE});
    static final Method UNPARK = new Method("unpark", VOID_TYPE, new Type[]{OBJECT_TYPE});

    static final Method ENTER_TRANSFORMED_METHOD = new Method("enterTestingCodeMethod", VOID_TYPE, new Type[]{});
    static final Method LEAVE_TRANSFORMED_METHOD = new Method("leaveTestingCodeMethod", VOID_TYPE, new Type[]{});
}