package com.devexperts.timetest;

/*
 * #%L
 * time-test
 * %%
 * Copyright (C) 2015 Devexperts, LLC
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


import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class ChangeTimeMethodsAspect {

    @Around("call(long System.currentTimeMillis())")
    public long changeSystemCurrentTimeMillis(ProceedingJoinPoint point) throws Throwable {
        return TimeProvider.getTimeProvider().timeMillis();
    }

    @Around("call(long System.nanoTime())")
    public long changeSystemNanoTime(ProceedingJoinPoint point) throws Throwable {
        return TimeProvider.getTimeProvider().nanoTime();
    }

    @Around("call(void Thread.sleep(long))")
    public void changeThreadSleepForMillis(ProceedingJoinPoint point) throws Throwable {
        TimeProvider.getTimeProvider().sleep((Long) point.getArgs()[0]);
    }

    @Around("call(* Thread.sleep(long, int))")
    public void changeThreadSleepForMillisAndNanos(ProceedingJoinPoint point) throws Throwable {
        TimeProvider.getTimeProvider().sleep((Long) point.getArgs()[0], (Integer) point.getArgs()[1]);
    }

    @Around("call(void Object.wait(long))")
    public void changeObjectWaitForMillis(ProceedingJoinPoint point) throws Throwable {
        TimeProvider.getTimeProvider().waitOn(point.getTarget(), (Long) point.getArgs()[0]);
    }

    @Around("call(* Object.wait(long, int))")
    public void changeObjectWaitForMillisAndNanos(ProceedingJoinPoint point) throws Throwable {
        TimeProvider.getTimeProvider().waitOn(point.getTarget(), (Long) point.getArgs()[0], (Integer) point.getArgs()[1]);
    }

    @Around("call(* sun.misc.Unsafe.park(boolean, long))")
    public void changeUnsafePark(ProceedingJoinPoint point) throws Throwable {
        final boolean isAbsolute = (Boolean) point.getArgs()[0];
        final long time = (Long) point.getArgs()[1];
        TimeProvider.getTimeProvider().park(isAbsolute, time);
    }

    @Around("call(* sun.misc.Unsafe.unpark(Object))")
    public void changeUnsafeUnpark(ProceedingJoinPoint point) throws Throwable {
        Object thread = point.getArgs()[0];
        TimeProvider.getTimeProvider().unpark(thread);
    }

}