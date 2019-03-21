package com.ustadmobile.core.util;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TestAsyncServiceManager {

    private ScheduledExecutorService scheduledExecutor;

    private AtomicLong lastStartTime = new AtomicLong();

    private AtomicLong lastStopTime = new AtomicLong();

    private AsyncServiceManager setupAsyncServiceManagerMock(int startDelay, int stopDelay) {
        AsyncServiceManager asyncServiceManager = spy(AsyncServiceManager.class);
        doAnswer(invocation -> {
            new Thread(() -> {
                lastStartTime.set(System.currentTimeMillis());
                try { Thread.sleep(startDelay); }
                catch(InterruptedException e) {}
                asyncServiceManager.notifyStateChanged(AsyncServiceManager.STATE_STARTED);
            }).start();
            return null;
        }).when(asyncServiceManager).start();

        doAnswer(invocation -> {
            new Thread(() -> {
                lastStopTime.set(System.currentTimeMillis());
                try { Thread.sleep(stopDelay); }
                catch(InterruptedException e) {}
                asyncServiceManager.notifyStateChanged(AsyncServiceManager.STATE_STOPPED);
            }).start();
            return null;
        }).when(asyncServiceManager).stop();

        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

        asyncServiceManager.setDelayedExecutor((runnable, delay) -> {
            scheduledExecutor.schedule(runnable, delay, TimeUnit.MILLISECONDS);
        });

        return asyncServiceManager;
    }

    @After
    public void tearDown() {
        if(scheduledExecutor != null)
            scheduledExecutor.shutdown();
    }

    @Test
    public void givenServiceStopped_whenSetEnabledTrueCalled_shouldCallStart() {
        AsyncServiceManager serviceManager = setupAsyncServiceManagerMock(1000, 1000);
        serviceManager.setEnabled(true);
        verify(serviceManager, timeout(2000*1000)).start();
        verify(serviceManager, times(1)).start();
    }

    @Test
    public void givenServiceStopped_whenSetEnabledCalledTwice_shouldCallStartOnce() {
        AsyncServiceManager serviceManager = setupAsyncServiceManagerMock(1000, 1000);
        serviceManager.setEnabled(true);
        serviceManager.setEnabled(true);
        verify(serviceManager, timeout(5000)).start();
        verify(serviceManager, times(1)).start();
    }

    @Test
    public void givenServiceStarting_whenStopCalled_shouldStopAfterStarted() throws InterruptedException{
        AsyncServiceManager serviceManager = setupAsyncServiceManagerMock(0, 1000);
        serviceManager.setEnabled(true);
        Thread.sleep(1000);
        serviceManager.setEnabled(false);
        verify(serviceManager, timeout(5000)).stop();
    }

    @Test
    public void givenServiceStarting_whenStopCalled_shouldCallStopAfterStarted() throws InterruptedException{
        AsyncServiceManager serviceManager = setupAsyncServiceManagerMock(1000, 1000);
        serviceManager.setEnabled(true);
        Thread.sleep(100);
        serviceManager.setEnabled(false);

        verify(serviceManager, timeout(10000)).stop();
        verify(serviceManager, timeout(10000)).start();
        Assert.assertTrue("Stopped after started",
                lastStopTime.get() > lastStartTime.get());
    }


}
