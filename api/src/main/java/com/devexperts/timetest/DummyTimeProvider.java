package com.devexperts.timetest;

/*
 * #%L
 * time-test
 * %%
 * Copyright (C) 2015 Devexperts, LLC
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


/**
 * Dummy implementation of {@link TimeProvider}. Throws {@link UnsupportedOperationException}
 * on all method calls. It is recommended to use this {@code time provider} for tests as default.
 */
public class DummyTimeProvider extends TimeProvider {

    private static final DummyTimeProvider INSTANCE = new DummyTimeProvider();

    private DummyTimeProvider() {
    }

    /**
     * Starts using {@link DummyTimeProvider} instead of current.
     */
    public static void start() {
        TimeProvider.setTimeProvider(INSTANCE);
    }

    /**
     * Resets time provider to default.
     */
    public static void reset() {
        TimeProvider.resetTimeProvider();
    }

    @Override
    public long timeMillis() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long nanoTime() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sleep(long millis) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sleep(long millis, int nanos) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void waitOn(Object monitor, long millis) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void waitOn(Object monitor, long millis, int nanos) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void park(boolean isAbsolute, long time) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unpark(Object thread) {
        throw new UnsupportedOperationException();
    }
}
