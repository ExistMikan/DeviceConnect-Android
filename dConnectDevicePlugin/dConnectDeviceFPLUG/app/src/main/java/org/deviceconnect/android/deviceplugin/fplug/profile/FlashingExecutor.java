/*
 FlashingExecutor
 Copyright (c) 2016 NTT DOCOMO,INC.
 Released under the MIT license
 http://opensource.org/licenses/mit-license.php
 */
package org.deviceconnect.android.deviceplugin.fplug.profile;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Support Class for Light Profile flashing parameter.
 *
 * @author NTT DOCOMO, INC.
 */
public class FlashingExecutor {

    public interface LightControllable {
        void changeLight(boolean isOn, CompleteListener listener);
    }

    public interface CompleteListener {
        void onComplete();
    }

    private LightControllable mListener;
    private ScheduledExecutorService mFlashingService = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture mLatestScheduledFuture;
    private Queue<Long> mFlashingQueue = new ConcurrentLinkedQueue<Long>();
    private boolean mIsOn = true;
    private int mLastIdentifier = 0;

    public FlashingExecutor() {
    }

    public synchronized void setLightControllable(LightControllable controllable) {
        mListener = controllable;
        Object o = null;
    }

    public synchronized void start(long[] flashing) {
        cancelSchedule();
        updateQueue(flashing);
        setOn(true);
        schedule(new Runnable() {
            @Override
            public void run() {
                controlLight(this);
            }
        }, getNextFlashingInterval());
    }

    private synchronized void controlLight(final Runnable runnable) {
        mLastIdentifier++;
        final int identifier = mLastIdentifier;
        LightControllable listener = getLightControllable();
        if (listener == null) {
            next(runnable, identifier);
            return;
        }
        listener.changeLight(isOn(), new CompleteListener() {
            @Override
            public void onComplete() {
                next(runnable, identifier);
            }
        });
    }

    private synchronized void next(Runnable runnable, int identifier) {
        //Return if other execution has been begin.
        if (mLastIdentifier != identifier) {
            return;
        }
        toggleOnOff();
        Long interval = getNextFlashingInterval();
        if (interval != null) {
            schedule(runnable, interval);
        } else {
            onFinish();
        }
    }

    private synchronized void onFinish() {
        mListener = null;
        mLatestScheduledFuture = null;
    }

    private synchronized LightControllable getLightControllable() {
        return mListener;
    }

    private synchronized void cancelSchedule() {
        if (mLatestScheduledFuture != null && !mLatestScheduledFuture.isCancelled()) {
            mLatestScheduledFuture.cancel(false);
        }
    }

    private synchronized void schedule(Runnable runnable, long interval) {
        mLatestScheduledFuture = mFlashingService.schedule(runnable, interval, TimeUnit.MILLISECONDS);
    }

    private synchronized void updateQueue(long[] flashing) {
        mFlashingQueue.clear();
        for (long value : flashing) {
            mFlashingQueue.add(value);
        }
    }

    private synchronized Long getNextFlashingInterval() {
        return mFlashingQueue.poll();
    }

    private synchronized void setOn(boolean isOn) {
        mIsOn = isOn;
    }

    private synchronized boolean isOn() {
        return mIsOn;
    }

    private synchronized void toggleOnOff() {
        mIsOn = !mIsOn;
    }

}
