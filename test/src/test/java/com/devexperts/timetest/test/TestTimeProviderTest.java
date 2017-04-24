package com.devexperts.timetest.test;

/*
 * #%L
 * test
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


import com.devexperts.timetest.Repeat;
import com.devexperts.timetest.RepeatRule;
import com.devexperts.timetest.TestTimeProvider;
import com.devexperts.util.UnsafeHolder;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import static org.junit.Assert.*;

/**
 * Tests for {@link com.devexperts.timetest.TestTimeProvider}
 */
public class TestTimeProviderTest {

    @Rule
    public RepeatRule repeatRule = new RepeatRule();

    @After
    public void tearDown() throws Exception {
        TestTimeProvider.reset();
    }

    @Test
    @Repeat(100)
    public void testIncreaseAndSetTime() {
        TestTimeProvider.start(100);
        assertEquals(100, System.currentTimeMillis());
        TestTimeProvider.increaseTime(50);
        assertEquals(150, System.currentTimeMillis());
    }

    @Test
    @Repeat(100)
    public void testWaitOnWakesUpByNotify() throws InterruptedException {
        TestTimeProvider.start();
        Phaser phaser = new Phaser(2);
        Object monitor = new Object();
        Thread thread = new Thread(() -> {
            phaser.arriveAndAwaitAdvance();
            synchronized (monitor) {
                monitor.notifyAll();
            }
        }, "TestThread");
        thread.start();
        synchronized (monitor) {
            phaser.arriveAndAwaitAdvance();
            monitor.wait(Long.MAX_VALUE);
        }
    }

    @Test
    @Repeat(100)
    public void testWaitOnWakesUpByTimeout() throws InterruptedException {
        TestTimeProvider.start();
        AtomicBoolean failed = new AtomicBoolean();
        Phaser phaser = new Phaser(2);
        Object monitor = new Object();
        Thread thread = new Thread(() -> {
            synchronized (monitor) {
                try {
                    phaser.arriveAndAwaitAdvance();
                    monitor.wait(100);
                } catch (Exception e) {
                    e.printStackTrace();
                    failed.set(true);
                }
            }
        }, "TestThread");
        thread.start();
        phaser.arriveAndAwaitAdvance();
        TestTimeProvider.waitUntilThreadsAreFrozen(500);
        TestTimeProvider.increaseTime(100);
        TestTimeProvider.waitUntilThreadsAreFrozen(500);
        assertFalse(failed.get());
    }

    @Test
    @Repeat(100)
    public void testWaitOnWorksWithZeroArgument() throws InterruptedException {
        TestTimeProvider.start();
        AtomicBoolean failed = new AtomicBoolean();
        Object monitor = new Object();
        Thread thread = new Thread(() -> {
            synchronized (monitor) {
                try {
                    monitor.wait();
                } catch (InterruptedException e) {
                    failed.set(true);
                }
            }
        }, "TestThread");
        thread.start();
        while (thread.getState() == Thread.State.NEW) ;
        TestTimeProvider.waitUntilThreadsAreFrozen(500);
        TestTimeProvider.increaseTime(1);
        TestTimeProvider.waitUntilThreadsAreFrozen(500);
        thread.interrupt();
        while (thread.getState() == Thread.State.TIMED_WAITING) ;
        TestTimeProvider.waitUntilThreadsAreFrozen(500);
        assertTrue(failed.get());
    }

    @Test
    @Repeat(100)
    public void testSleep() throws InterruptedException {
        TestTimeProvider.start();
        AtomicBoolean failed = new AtomicBoolean();
        long sleepTime = 100500;
        Thread thread = null;
        try {
            thread = new Thread(() -> {
                try {
                    Thread.sleep(sleepTime);
                } catch (Exception e) {
                    e.printStackTrace();
                    failed.set(true);
                }
            }, "TestThread");
            thread.start();
            TestTimeProvider.waitUntilThreadsAreFrozen(1000);
            TestTimeProvider.increaseTime(sleepTime);
            TestTimeProvider.waitUntilThreadsAreFrozen(1000);
            assertFalse(failed.get());
        } finally {
            thread.stop();
        }
    }

    @Test
    @Repeat(100)
    public void testSleepWithZeroArgument() throws InterruptedException {
        TestTimeProvider.start();
        Thread.sleep(0);
    }

    @Test
    @Repeat(100)
    public void testWaitUntilThreadsAreFrozen() throws InterruptedException {
        TestTimeProvider.start();
        AtomicBoolean done = new AtomicBoolean();
        int[] sum = new int[1];
        Thread thread = new Thread(() -> {
            // do some work
            for (int i = 0; i < 50_000_000; i++)
                sum[0] += i;
            done.set(true);
        });
        thread.start();
        TestTimeProvider.waitUntilThreadsAreFrozen(1000);
        assertTrue(done.get());
    }

    @Test(expected = AssertionError.class)
    @Repeat(100)
    public void testWaitUntilThreadsFreezeThrowsAssertionError() throws InterruptedException {
        TestTimeProvider.start();
        Thread thread = null;
        boolean[] done = new boolean[1];
        try {
            thread = new Thread(() -> {
                // do some work
                int i = 0;
                while (!done[0])
                    i++;
            }, "TestThread");
            thread.start();
            TestTimeProvider.waitUntilThreadsAreFrozen(10);
        } finally {
            done[0] = true;
        }
    }

    @Test
    @Repeat(100)
    public void interruptionShouldNotBeIgnored() throws InterruptedException {
        TestTimeProvider.start();
        AtomicBoolean isInterrupted = new AtomicBoolean();
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                isInterrupted.set(true);
            }
        }, "TestThread");
        thread.start();
        thread.interrupt();
        thread.join();
        assertTrue(isInterrupted.get());
    }

    @Test
    @Repeat(100)
    public void testWithSeveralThreads() throws InterruptedException {
        TestTimeProvider.start();
        AtomicBoolean failed = new AtomicBoolean();
        AtomicBoolean sleep1 = new AtomicBoolean(true);
        AtomicBoolean sleep2 = new AtomicBoolean(true);
        Thread t1 = new Thread(() -> {
            try {
                Thread.sleep(200);
                sleep1.set(false);
            } catch (InterruptedException e) {
                // ignored, done
            }
        }, "TestThread_1");
        Thread t2 = new Thread(() -> {
            try {
                Thread.sleep(100);
                sleep2.set(false);
            } catch (Exception e) {
                e.printStackTrace();
                failed.set(true);
            }
        }, "TestThread_2");
        t1.start();
        t2.start();
        while (t1.getState() == Thread.State.NEW) ;
        while (t2.getState() == Thread.State.NEW) ;
        TestTimeProvider.waitUntilThreadsAreFrozen(500);
        TestTimeProvider.increaseTime(100);
        TestTimeProvider.waitUntilThreadsAreFrozen(500);
        t1.interrupt();
        t1.join();
        t2.join();
        assertTrue(sleep1.get());
        assertFalse(failed.get());
        assertFalse(sleep2.get());
    }

    @Test(timeout = 5000)
    @Repeat(100)
    public void testUnsafePark() throws InterruptedException {
        TestTimeProvider.start();
        final Object owner = new Object();
        Thread thread = new Thread(() -> {
            UnsafeHolder.UNSAFE.park(false, Long.MAX_VALUE);
            synchronized (owner) {
                owner.notifyAll();
            }
        }, "TestThread");
        thread.start();
        TestTimeProvider.waitUntilThreadsAreFrozen(200);
        synchronized (owner) {
            UnsafeHolder.UNSAFE.unpark(thread);
            owner.wait(Long.MAX_VALUE);
        }
    }

    @Test(timeout = 10_000)
    @Repeat(100)
    public void testLockSupport() throws InterruptedException {
        TestTimeProvider.start();
        long millis = 10000000;
        Thread thread = new Thread(() -> {
            LockSupport.parkNanos(millis * 1_000_000);
        }, "TestThread");
        thread.start();
        while (thread.getState() == Thread.State.NEW) ;
        TestTimeProvider.waitUntilThreadsAreFrozen(1000);
        TestTimeProvider.increaseTime(millis);
        thread.join();
    }

    @Test(timeout = 5000)
    @Repeat(100)
    public void testUnparkBeforePark() throws InterruptedException {
        TestTimeProvider.start();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Thread thread = new Thread(() -> {
            UnsafeHolder.UNSAFE.park(false, Long.MAX_VALUE);
            countDownLatch.countDown();
        }, "TestThread");
        UnsafeHolder.UNSAFE.unpark(thread);
        thread.start();
        countDownLatch.await();
    }
}
