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


import com.devexperts.timetest.TestTimeProvider;
import com.devexperts.util.UnsafeHolder;
import org.junit.After;
import org.junit.Test;

import java.util.concurrent.locks.LockSupport;

import static org.junit.Assert.*;

/**
 * Tests for {@link com.devexperts.timetest.TestTimeProvider}
 */
public class TestTimeProviderTest {

    @After
    public void tearDown() throws Exception {
        TestTimeProvider.reset();
    }

    @Test
    public void testIncreaseAndSetTime() {
        TestTimeProvider.start(100);
        assertEquals(100, System.currentTimeMillis());
        TestTimeProvider.increaseTime(50);
        assertEquals(150, System.currentTimeMillis());
    }

    @Test(timeout = 1000)
    public void testWaitOnWakesUpByNotify() throws InterruptedException {
        TestTimeProvider.start();
        final Object monitor = new Object();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (monitor) {
                    monitor.notifyAll();
                }
            }
        });
        thread.start();
        synchronized (monitor) {
            monitor.wait(Long.MAX_VALUE);
        }
    }

    @Test(timeout = 1000)
    public void testWaitOnWakesUpByTimeout() throws InterruptedException {
        TestTimeProvider.start();
        final boolean[] failed = {false};
        final Object monitor = new Object();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (monitor) {
                    try {
                        monitor.wait(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                        failed[0] = true;
                    }
                }
            }
        });
        thread.start();
        TestTimeProvider.waitUntilThreadsAreFrozen(500);
        TestTimeProvider.increaseTime(100);
        TestTimeProvider.waitUntilThreadsAreFrozen(100);
        assertFalse(failed[0]);
    }

    @Test(timeout = 1000)
    public void testWaitOnWorksWithZeroArgument() throws InterruptedException {
        TestTimeProvider.start();
        final boolean[] failed = {false};
        final Object monitor = new Object();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (monitor) {
                    try {
                        monitor.wait();
                        failed[0] = true;
                    } catch (InterruptedException e) {
                        // ignored, done
                    }
                }
            }
        });
        thread.start();
        TestTimeProvider.waitUntilThreadsAreFrozen(500);
        TestTimeProvider.increaseTime(1);
        TestTimeProvider.waitUntilThreadsAreFrozen(500);
        thread.interrupt();
        TestTimeProvider.waitUntilThreadsAreFrozen(500);
        assertFalse(failed[0]);
    }

    @Test(timeout = 1000)
    public void testSleep() throws InterruptedException {
        TestTimeProvider.start();
        final boolean[] failed = {false};
        final long sleepTime = 100500;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(sleepTime);
                } catch (Exception e) {
                    e.printStackTrace();
                    failed[0] = true;
                }
            }
        }, "TestThread");
        thread.start();
        TestTimeProvider.waitUntilThreadsAreFrozen(500);
        TestTimeProvider.increaseTime(sleepTime);
        TestTimeProvider.waitUntilThreadsAreFrozen(500);
        assertFalse(failed[0]);
    }

    @Test(timeout = 1000)
    public void testSleepWithZeroArgument() throws InterruptedException {
        TestTimeProvider.start();
        Thread.sleep(0);
    }

    @Test(timeout = 2000)
    public void testWaitUntilThreadsFreeze() {
        TestTimeProvider.start();
        final boolean[] shouldBeTrue = {false};
        Thread thread = new Thread(new Runnable() {
            @SuppressWarnings("UnusedDeclaration")
            @Override
            public void run() {
                // do some work
                int sum = 0;
                for (int i = 0; i < 100_000_000; i++)
                    sum += i;

                shouldBeTrue[0] = true;
            }
        });
        thread.start();
        TestTimeProvider.waitUntilThreadsAreFrozen(1000);
        assertTrue(shouldBeTrue[0]);
    }

    @Test(timeout = 2000, expected = AssertionError.class)
    public void testWaitUntilThreadsFreezeThrowsAssertionError() {
        TestTimeProvider.start();
        Thread thread = new Thread(new Runnable() {
            @SuppressWarnings("UnusedDeclaration")
            @Override
            public void run() {
                // do some work
                int i = 0;
                while (true) {
                    i++;
                }
            }
        });
        thread.start();
        TestTimeProvider.waitUntilThreadsAreFrozen(10);
    }

    @Test(timeout = 1000)
    public void interruptionShouldNotBeIgnored() throws InterruptedException {
        TestTimeProvider.start();
        final boolean[] isInterrupted = {false};
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException e) {
                    isInterrupted[0] = true;
                }
            }
        }, "TestThread");
        thread.start();
        thread.interrupt();
        thread.join();
        assertTrue(isInterrupted[0]);
    }

    @Test(timeout = 2000)
    public void testWithSeveralThreads() throws InterruptedException {
        TestTimeProvider.start();
        final boolean[] failed = {false};
        final boolean[] shouldBeTrue = {false};
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    // ignored, done
                }
            }
        });
        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                    shouldBeTrue[0] = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    failed[0] = false;
                }
            }
        });
        t1.start();
        t2.start();
        TestTimeProvider.waitUntilThreadsAreFrozen(500);
        TestTimeProvider.increaseTime(100);
        TestTimeProvider.waitUntilThreadsAreFrozen(500);
        assertTrue(shouldBeTrue[0]);
        t1.interrupt();
        assertFalse(failed[0]);
    }

    @Test(timeout = 1000)
    public void testUnsafePark() throws InterruptedException {
        TestTimeProvider.start();
        final Object owner = new Object();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                UnsafeHolder.UNSAFE.park(false, Long.MAX_VALUE);
                synchronized (owner) {
                    owner.notifyAll();
                }
            }
        });
        thread.start();
        TestTimeProvider.waitUntilThreadsAreFrozen(100);
        synchronized (owner) {
            UnsafeHolder.UNSAFE.unpark(thread);
            owner.wait(Long.MAX_VALUE);
        }
    }

    @Test(timeout = 1000)
    public void testLockSupport() throws InterruptedException {
        TestTimeProvider.start();
        final Object owner = new Object();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                LockSupport.park();
                synchronized (owner) {
                    owner.notifyAll();
                }
            }
        });
        thread.start();
        TestTimeProvider.waitUntilThreadsAreFrozen(100);
        synchronized (owner) {
            LockSupport.unpark(thread);
            owner.wait(Long.MAX_VALUE);
        }
    }
}
