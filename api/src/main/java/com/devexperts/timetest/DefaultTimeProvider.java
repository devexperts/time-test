package com.devexperts.timetest;

/*
 * #%L
 * api
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

import com.devexperts.util.UnsafeHolder;

public class DefaultTimeProvider extends TimeProvider {
    @Override
    public long timeMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public long nanoTime() {
        return System.nanoTime();
    }

    @Override
    public void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    @Override
    public void sleep(long millis, int nanos) throws InterruptedException {
        Thread.sleep(millis, nanos);
    }

    @SuppressWarnings({"WaitWhileNotSynced", "WaitNotInLoop"})
    @Override
    public void waitOn(Object monitor, long millis) throws InterruptedException {
        monitor.wait(millis);
    }

    @SuppressWarnings({"WaitWhileNotSynced", "WaitNotInLoop"})
    @Override
    public void waitOn(Object monitor, long millis, int nanos) throws InterruptedException {
        monitor.wait(millis, nanos);
    }

    @Override
    public void notifyAll(Object monitor) {
        monitor.notifyAll();
    }

    @Override
    public void notify(Object monitor) {
        monitor.notify();
    }

    @Override
    public void park(boolean isAbsolute, long time) {
        UnsafeHolder.UNSAFE.park(isAbsolute, time);
    }

    @Override
    public void unpark(Object thread) {
        UnsafeHolder.UNSAFE.unpark(thread);
    }
}
