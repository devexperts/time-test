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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import java.util.*;

/**
 * Time provider for testing. You can manipulate with time to your notice.
 * Use {@link #start()} method to start using this provider and {@link #reset()} to reset time provider to default.
 * <p>
 * See tests for example.
 */
public class TestTimeProvider extends TimeProvider {

    private static final Logging LOG = Logging.getLogging(TestTimeProvider.class);
    private static final TestTimeProvider INSTANCE = new TestTimeProvider();
    // For checking that TestTimeProvider isn't started already.
    @GuardedBy("TestTimeProvider")
    private static boolean started;
    @GuardedBy("TestTimeProvider")
    private static Exception stacktraceOnStart;

    @GuardedBy("this")
    private final PriorityQueue<ThreadInfo> threadQueue = new PriorityQueue<>();
    @GuardedBy("this")
    private final Set<Thread> waitingThreads = new HashSet<>();
    @GuardedBy("this")
    private final Map<Object, Object> parkThreadOwners = new HashMap<>();
    @GuardedBy("this")
    private Set<Thread> threadsOnStart = new HashSet<>();
    @GuardedBy("this")
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
    public static void waitUntilThreadsAreFrozen(long timeout) {
        INSTANCE.waitUntilThreadsAreFrozen0(timeout);
    }

    // ========== TimeProvider implementation ==========

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
    public void sleep(long millis, int nanos) throws InterruptedException {
        millis = checkTimeArgumentsAndGetMillis(millis, nanos);
        if (millis == 0) // does not need to sleep.
            return;
        long endTime;
        synchronized (this) {
            endTime = currentTime + millis;
            if (endTime < currentTime) // overflowed.
                endTime = Long.MAX_VALUE;
        }
        Object owner = new Object();
        synchronized (owner) {
            while (endTime > currentTime) {
                waitOn(owner, millis);
            }
        }
    }

    @Override
    @GuardedBy("monitor")
    public void waitOn(Object monitor, long millis) throws InterruptedException {
        waitOn(monitor, millis, 0);
    }

    @Override
    @GuardedBy("monitor")
    public void waitOn(Object monitor, long millis, int nanos) throws InterruptedException {
        millis = checkTimeArgumentsAndGetMillis(millis, nanos);
        waitOn0(monitor, false, millis);
    }

    @Override
    public void park(boolean isAbsolute, long time) {
        long millis = time / 1_000_000;
        int nanos = (int) (time % 1_000_000);
        millis = checkTimeArgumentsAndGetMillis(millis, nanos);
        Object owner = new Object();
        synchronized (this) {
            parkThreadOwners.put(Thread.currentThread(), owner);
        }
        try {
            synchronized (owner) {
                waitOn0(owner, isAbsolute, millis);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void unpark(Object thread) {
        Object owner;
        synchronized (this) {
            owner = parkThreadOwners.remove(thread);
        }
        if (owner != null) {
            synchronized (owner) {
                owner.notifyAll();
            }
        }
    }

    // ========== Internal methods ==========

    private synchronized void start0(long startTime) {
        threadsOnStart = Thread.getAllStackTraces().keySet();
        waitingThreads.clear();
        parkThreadOwners.clear();
        setTime(startTime);
    }

    private synchronized void setTime0(long millis) {
        currentTime = millis;
        invalidate();
    }

    private synchronized void increaseTime0(long millis) {
        currentTime += millis;
        invalidate();
    }

    @GuardedBy("monitor")
    private void waitOn0(Object monitor, boolean isAbsolute, long millis) throws InterruptedException {
        // Just wait if time equals to 0.
        if (!isAbsolute && millis == 0) {
            monitor.wait();
            return;
        }
        // Count resume time.
        long resumeTime = isAbsolute ? millis : currentTime + millis;
        if (!isAbsolute && resumeTime < currentTime) // overflowed.
            resumeTime = Long.MAX_VALUE;
        // Check that resume time is greater than current.
        if (resumeTime <= currentTime)
            return;
        // Create thread info and add it to priority queue.
        ThreadInfo threadInfo = new ThreadInfo(monitor, resumeTime);
        synchronized (this) {
            waitingThreads.add(Thread.currentThread());
            threadQueue.add(threadInfo);
            // Notify that thread changes state to WAITING.
            notifyAll();
        }
        try {
            // Wait. If thread should be waked up then monitor.notifyAll() will be executed in #invalidate().
            monitor.wait();
        } finally {
            synchronized (this) {
                threadInfo.resumed = true;
                waitingThreads.remove(Thread.currentThread());
                // Notify that new thread changes state to resumed.
                // For #waitUntilThreadsAreFrozen() and #invalidate().
                notifyAll();
            }
        }
    }

    private long checkTimeArgumentsAndGetMillis(long millis, int nanos) {
        if (millis < 0)
            throw new IllegalArgumentException("timeout value is negative");
        if (nanos < 0 || nanos > 999_999)
            throw new IllegalArgumentException("nanosecond timeout value out of range");
        if (nanos >= 500_000 || (nanos != 0 && millis == 0))
            millis++;
        return millis;
    }

    // Should be called from methods which change current time.
    @GuardedBy("this")
    private void invalidate() {
        while (!threadQueue.isEmpty() && threadQueue.peek().resumeTime <= currentTime) {
            ThreadInfo threadInfo = threadQueue.poll();
            if (!threadInfo.resumed) {
                synchronized (threadInfo.owner) {
                    threadInfo.owner.notifyAll();
                }
                while (!threadInfo.resumed) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        LOG.warn("Interrupted invalidate");
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    private synchronized void waitUntilThreadsAreFrozen0(long timeout) {
        Set<List<StackTraceElement>> threadsInWaitingStateStackTraces = new HashSet<>();
        // Store end time.
        final long endTime = System.currentTimeMillis() + timeout;
        // This map contains non-frozen threads with stack traces.
        Map<Thread, StackTraceElement[]> badThreads = new HashMap<>();
        // Exception for stack trace logging.
        Exception logException = new Exception();
        while (true) {
            // Store current time.
            long time = System.currentTimeMillis();
            // Fill bad threads.
            badThreads.clear();
            for (Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()) {
                if (threadsOnStart.contains(e.getKey()) || waitingThreads.contains(e.getKey()))
                    continue;
                Thread.State threadState = e.getKey().getState();
                if (threadState == Thread.State.TERMINATED
                        || threadState == Thread.State.WAITING
                        || threadState == Thread.State.NEW)
                    continue;
                if (threadState == Thread.State.TIMED_WAITING) {
                    if (threadsInWaitingStateStackTraces.add(Arrays.asList(e.getValue()))) {
                        logException.setStackTrace(e.getValue());
                        LOG.warn("Thread in TIMED_WAITING state, see stack trace.", logException);
                    }
                    continue;
                }
                badThreads.put(e.getKey(), e.getValue());
            }
            // Break if all threads are frozen or time is up.
            if (badThreads.isEmpty() || time >= endTime)
                break;
            // Wait for changes.
            try {
                wait(endTime - time);
            } catch (InterruptedException e) {
                LOG.warn("Interrupted waitUntilThreadsAreFrozen");
                Thread.currentThread().interrupt();
                break;
            }
        }
        // Finish if all threads are frozen.
        if (badThreads.isEmpty())
            return;
        // Log stack traces for non-frozen threads and throw AssertionError.
        LOG.error("Waiting until threads are frozen failed by timeout. See stack traces for non-frozen threads:");
        for (Map.Entry<Thread, StackTraceElement[]> e : badThreads.entrySet()) {
            logException.setStackTrace(e.getValue());
            LOG.error("Stacktrace for " + e.getKey(), logException);
        }
        throw new AssertionError("Waiting until threads are frozen failed by timeout.");
    }

    private static class ThreadInfo implements Comparable<ThreadInfo> {
        private final Object owner;
        private final long resumeTime;

        @GuardedBy("TestTimeProvider.this")
        private boolean resumed = false;

        private ThreadInfo(Object owner, long resumeTime) {
            this.owner = owner;
            this.resumeTime = resumeTime;
        }

        @Override
        public int compareTo(@Nonnull ThreadInfo other) {
            return Long.compare(resumeTime, other.resumeTime);
        }
    }
}
