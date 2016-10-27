package com.devexperts.timetest;

/*
 * #%L
 * api
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

@SuppressWarnings("unused") // used in transformer
public class Methods {
    public static long timeMillis() {
        return TimeProvider.getTimeProvider().timeMillis();
    }

    public static long nanoTime() {
        return TimeProvider.getTimeProvider().nanoTime();
    }

    public static void sleep(long millis) throws InterruptedException {
        TimeProvider.getTimeProvider().sleep(millis);
    }

    public static void sleep(long millis, int nanos) throws InterruptedException {
        TimeProvider.getTimeProvider().sleep(millis, nanos);
    }

    public static void waitOn(Object monitor) throws InterruptedException {
        TimeProvider.getTimeProvider().waitOn(monitor, 0);
    }

    public static void waitOn(Object monitor, long millis) throws InterruptedException {
        TimeProvider.getTimeProvider().waitOn(monitor, millis);
    }

    public static void waitOn(Object monitor, long millis, int nanos) throws InterruptedException {
        TimeProvider.getTimeProvider().waitOn(monitor, millis, nanos);
    }

    public static void park(boolean isAbsolute, long time) {
        TimeProvider.getTimeProvider().park(isAbsolute, time);
    }

    public static void unpark(Object thread) {
        TimeProvider.getTimeProvider().unpark(thread);
    }
}
