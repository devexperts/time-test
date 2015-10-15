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


import com.devexperts.util.UnsafeHolder;

/**
 * Provides time-based methods. Should be thread-safe.
 */
public abstract class TimeProvider {

    // ========== Time-based methods ==========

    /**
     * Default time provider that uses standard system methods.
     */
    private static final TimeProvider DEFAULT = new TimeProvider() {
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
        public void park(boolean isAbsolute, long time) {
            UnsafeHolder.UNSAFE.park(isAbsolute, time);
        }

        @Override
        public void unpark(Object thread) {
            UnsafeHolder.UNSAFE.unpark(thread);
        }
    };
    private static TimeProvider timeProvider = DEFAULT;

    /**
     * Resets time provider to {@link #DEFAULT default}. Should be used for test purpose only.
     */
    static void resetTimeProvider() {
        setTimeProvider(DEFAULT);
    }

    /**
     * Returns current time provider.
     *
     * @return current time provider.
     */
    public static TimeProvider getTimeProvider() {
        return timeProvider;
    }

    /**
     * Sets new time provider. Should be used for test purpose only.
     *
     * @param timeProvider time provider.
     */
    static void setTimeProvider(TimeProvider timeProvider) {
        TimeProvider.timeProvider = timeProvider;
    }

    /**
     * @see System#currentTimeMillis()
     */
    public abstract long timeMillis();

    /**
     * @see System#nanoTime()
     */
    public abstract long nanoTime();

    /**
     * @see Thread#sleep(long)
     */
    public abstract void sleep(long millis) throws InterruptedException;

    // ========== Utility methods ==========

    /**
     * @see Thread#sleep(long, int)
     */
    public abstract void sleep(long millis, int nanos) throws InterruptedException;

    /**
     * @see Object#wait(long)
     */
    public abstract void waitOn(Object monitor, long millis) throws InterruptedException;

    /**
     * @see Object#wait(long, int)
     */
    public abstract void waitOn(Object monitor, long millis, int nanos) throws InterruptedException;

    /**
     * @see sun.misc.Unsafe#park(boolean, long)
     */
    public abstract void park(boolean isAbsolute, long time);

    /**
     * @see sun.misc.Unsafe#unpark(Object)
     */
    public abstract void unpark(Object thread);
}
