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


import com.devexperts.logging.Logging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Time provider for testing. You can manipulate with time to your notice.
 * Use {@link #start()} method to start using this provider and {@link #reset()} to reset time provider to default.
 * <p>
 * See tests for example.
 * <p>
 * <h3>Implementation details</h3>
 * Implementation of this class contains hard to understand logic with locks for synchronization.
 * This paragraph tries to describe this logic.
 * <p>
 * <p> Here are three base operations: <b>waitOn</b> (used for all waiting operations implementation),
 * <b>invalidate</b> (invoked when time is changed) and
 * <b>waitUntilThreadsAreFrozen</b> (used to wait a moment, when all threads are done or in WAITING state).
 */
public class TestTimeProvider extends TimeProvider {

    private static final Logging LOG = Logging.getLogging(TestTimeProvider.class);
    private static final TestTimeProvider INSTANCE = new TestTimeProvider();

    private static final long WAITING_TIMEOUT = 10; // ms

    // For checking that TestTimeProvider isn't started already.
    private static boolean started;
    private static Exception stacktraceOnStart;

    private final IdentityHashMap<Object, List<ThreadInfo>> waitingThreads = new IdentityHashMap<>();
    private final IdentityHashMap<Thread, ThreadInfo> threadInfos = new IdentityHashMap<>();
    private Set<Thread> setToBeWaited = Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));

    private Set<Thread> threadsOnStart = new HashSet<>();
    private volatile long currentTime = 0; // volatile for getters.

    private TestTimeProvider() {
    }

    // ========== Static methods for instance management ==========

    /**
     * Starts using {@link TestTimeProvider} instead of current.
     *
     * @param startTime start time.
     */
    public static synchronized void start(long startTime) {
        // Check that not started already.
        if (started) {
            LOG.error("TestTimeProvider is started already, last start stacktrace:", stacktraceOnStart);
            throw new IllegalStateException("TestTimeProvider is started already");
        }
        // Mark as started and keep current stacktrace.
        started = true;
        stacktraceOnStart = new Exception();
        // Start time provider.
        INSTANCE.start0(startTime);
        TimeProvider.setTimeProvider(INSTANCE);
    }

    /**
     * Starts using {@link TestTimeProvider} instead of current.
     * Sets {@link System#currentTimeMillis()} as start time.
     */
    public static void start() {
        start(System.currentTimeMillis());
    }

    /**
     * Resets time provider to default.
     */
    public synchronized static void reset() {
        INSTANCE.resetTime();
        TimeProvider.resetTimeProvider();
        started = false;
    }

    /**
     * Increases time on specified delta.
     *
     * @param millis delta time, measured in milliseconds,
     *               between the current time and midnight, January 1, 1970 UTC.
     */
    public static void increaseTime(long millis) {
        INSTANCE.increaseTime0(millis);
    }

    /**
     * Sets the specified time.
     *
     * @param millis time to be setted, measured in milliseconds,
     *               between the current time and midnight, January 1, 1970 UTC.
     */
    public static void setTime(long millis) {
        INSTANCE.setTime0(millis);
    }

    /**
     * Waits until all {@code Threads}, created after {@link #start()} invocation, are frozen.
     * At first waits for a short time during which all work threads have to wake up.
     * Throws AssertionError if any {@code Threads} are non-frozen after specified timeout.
     *
     * @param timeout timeout, measured in milliseconds, after which all {@code Threads}
     *                created after {@link #start()} invocation should be frozen.
     * @throws AssertionError if any {@code Threads} are non-frozen after specified timeout.
     */
    public static void waitUntilThreadsAreFrozen(long timeout) throws InterruptedException {
        INSTANCE.waitUntilThreadsAreFrozen0(timeout);
    }


    // ========== TimeProvider implementation ==========

    private synchronized void start0(long startTime) {
        threadsOnStart = Thread.getAllStackTraces().keySet();
        setTime0(startTime);
    }

    private synchronized void resetTime() {
        currentTime = 0;
    }

    private synchronized void increaseTime0(long millis) {
        setTime0(currentTime + millis);
    }

    private long checkTimeArgumentsAndGetMillis(long millis, int nanos) {
        if (millis < 0)
            throw new IllegalArgumentException("Timeout value is negative");
        if (nanos < 0 || nanos > 999_999)
            throw new IllegalArgumentException("Nanosecond timeout value out of range");
        if (nanos >= 500_000 || (nanos != 0 && millis == 0))
            millis++;
        return millis;
    }

    @Override
    public long timeMillis() {
        return currentTime;
    }

    @Override
    public long nanoTime() {
        return currentTime * 1_000_000;
    }

    @Override
    public void sleep(long millis) throws InterruptedException {
        sleep(millis, 0);
    }

    @Override
    public void waitOn(Object monitor, long millis) throws InterruptedException {
        waitOn(monitor, millis, 0);
    }

    @Override
    public void notify(Object monitor) {
        notifyAll(monitor);
    }

    private synchronized void setTime0(long millis) {
        synchronized (this) {
            if (millis < currentTime)
                throw new IllegalArgumentException(
                    "Time cannot be decreased, current=" + currentTime + ", new=" + millis);
            currentTime = millis;
            threadInfos.values().stream()
                .filter(ti -> currentTime >= ti.resumeTime)
                .forEach(ti -> ti.resumed = true);
            waitingThreads.values().forEach(tis -> tis.removeIf(ti -> ti.resumed));
            waitingThreads.entrySet().removeIf(e -> e.getValue().isEmpty());
        }
    }

    @Override
    public synchronized void notifyAll(Object monitor) {
        synchronized (this) {
            List<ThreadInfo> tis = waitingThreads.remove(monitor);
            if (tis == null)
                return;
            tis.forEach(ti -> ti.resumed = true);
        }
    }

    @Override
    public void sleep(long millis, int nanos) throws InterruptedException {
        millis = checkTimeArgumentsAndGetMillis(millis, nanos);
        if (millis == 0) // does not need to sleep.
            return;
        // Sleep can be simulated via wait() call without
        // any possibility of notify() call on the same monitor
        Object monitor = new Object();
        synchronized (monitor) {
            waitOn(monitor, millis);
        }
    }

    @Override
    public void waitOn(Object monitor, long millis, int nanos) throws InterruptedException {
        ThreadInfo ti;
        long resumeTime;
        setToBeWaited.add(Thread.currentThread());
        synchronized (this) {
            millis = checkTimeArgumentsAndGetMillis(millis, nanos);
            // Wait forever if millis == 0
            resumeTime = millis != 0 ? currentTime + millis : Long.MAX_VALUE;
            ti = new ThreadInfo(Thread.currentThread(), monitor, resumeTime);
            List<ThreadInfo> tis = waitingThreads.computeIfAbsent(monitor, m -> new ArrayList<>());
            tis.add(ti);
            threadInfos.put(ti.thread, ti);
            setToBeWaited.remove(Thread.currentThread());
        }
        // Wait with small timeout until
        // current time is equals or greater than resume time
        // or notify() is called on the monitor
        while (true) {
            monitor.wait(WAITING_TIMEOUT);
            if (Thread.currentThread().isInterrupted())
                throw new InterruptedException();
            if (ti.resumed) {
                synchronized (this) {
                    threadInfos.remove(ti.thread);
                }
                return;
            }
        }
    }

    private Set<Thread> unparkedThreads = Collections.newSetFromMap(new IdentityHashMap<>());

    @Override
    public void park(boolean isAbsolute, long time) {
        ThreadInfo ti;
        // This thread could wait for "this" lock,
        // so store it to set with potentially waiting
        setToBeWaited.add(Thread.currentThread());
        synchronized (this) {
            // This thread could wait
            setToBeWaited.remove(Thread.currentThread());
            // If current thread has been unparked already,
            // just remove it from "unparked threads" set and return
            if (unparkedThreads.remove(Thread.currentThread()))
                return;
            // Count time millis
            long millis = time / 1_000_000;
            int nanos = (int) (time % 1_000_000);
            millis = checkTimeArgumentsAndGetMillis(millis, nanos);
            long resumeTime = isAbsolute ? millis : currentTime + millis;
            if (currentTime >= resumeTime)
                return;
            // Store information about thread
            ti = new ThreadInfo(Thread.currentThread(), null, resumeTime);
            threadInfos.put(ti.thread, ti);
        }
        // Simulate park via waiting on unused monitor
        while (true) {
            try {
                Thread.sleep(WAITING_TIMEOUT);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (Thread.currentThread().isInterrupted())
                return;
            if (ti.resumed) {
                synchronized (this) {
                    threadInfos.remove(ti.thread);
                }
                return;
            }
        }
    }

    @Override
    public void unpark(Object thread) {
        synchronized (this) {
            ThreadInfo ti = threadInfos.get(thread);
            // If thread hasn't been parked already, mark it as unparked,
            // otherwise mark it as "resumed"
            if (ti == null)
                unparkedThreads.add((Thread) thread);
            else
                ti.resumed = true;
        }
    }

    private synchronized void waitUntilThreadsAreFrozen0(long timeout) throws InterruptedException {
        // Store end time
        long endTime = System.currentTimeMillis() + timeout;
        Exception logException = new Exception();
        while (true) {
            // Store current time.
            long time = System.currentTimeMillis();
            List<Map.Entry<Thread, StackTraceElement[]>> badThreads =
                Thread.getAllStackTraces().entrySet().stream().filter(e -> {
                    Thread t = e.getKey();
                    if (threadsOnStart.contains(t))
                        return false;
                    ThreadInfo ti = threadInfos.get(t);
                    if (ti != null)
                        return ti.resumed;
                    switch (t.getState()) {
                        case TERMINATED:
                        case WAITING:
                        case NEW:
                            return false;
                        case BLOCKED:
                            return setToBeWaited.contains(t);
                        default:
                            return true;
                    }
                }).collect(Collectors.toList());
            if (badThreads.isEmpty())
                return;
            if (time >= endTime) {
                LOG.error("Waiting until threads are frozen failed by timeout. See stack traces for non-frozen threads:");
                for (Map.Entry<Thread, StackTraceElement[]> e : badThreads) {
                    logException.setStackTrace(e.getValue());
                    LOG.error("Stacktrace for " + e.getKey(), logException);
                }
                throw new AssertionError("Waiting until threads are frozen failed by timeout.");
            }
            wait(WAITING_TIMEOUT);
        }
    }

    private static class ThreadInfo {
        volatile boolean resumed;
        final Thread thread;
        final Object monitor;
        final long resumeTime; // Long.MAX_VALUE if thread shouldn't be resumed by the time limit expiration

        private ThreadInfo(Thread thread, Object monitor, long resumeTime) {
            this.thread = thread;
            this.monitor = monitor;
            this.resumeTime = resumeTime;
        }
    }
}
