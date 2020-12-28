package com.qadsdk.impl.videocache.tasks;

import android.os.SystemClock;

import com.qadsdk.base.dev.util.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public abstract class CustomStopRunnable implements Runnable {
    private static final String TAG = "CustomStopRunnable";

    private static final long INTERRUPT_TIME_OUT = 3000L;

    protected static final int INIT = 0;
    protected static final int RUNNING = 1;
    protected static final int STOPPING = 2;
    protected static final int END = 5;
    protected static final int INTERRUPT = 6;

    private ReentrantLock mLock = new ReentrantLock();
    private Condition mCondition;

    protected int mStatus = INIT;

    private Listener mListener;

    public CustomStopRunnable() {
    }

    @Override
    public final void run() {
        if (mListener != null) {
            mListener.runStart(this);
        }
        mLock.lock();
        if (mStatus == INIT) {
            mStatus = RUNNING;
            mLock.unlock();
            execute();
            mLock.lock();
            Logger.i(TAG, "execute end, get lock");
            if (mStatus == RUNNING) {
                mStatus = END;
            } else {
                mStatus = INTERRUPT;
            }
            // 可能在不能调用isCarryOn的情况下执行了中断，这种情况下一定会走到任务完成，但是中断等待已经开始，因此可能需要唤醒
            if (mCondition != null) {
                Logger.i(TAG, "[run]: mCondition is not null");
                mCondition.signal();
                mCondition = null;
            }
        } else {
            mStatus = INTERRUPT;
        }
        mLock.unlock();
        if (mListener != null) {
            mListener.runEnd(this);
        }
    }

    protected boolean isCarryOn() {
        boolean res = true;
        if (mCondition != null && mStatus != RUNNING) {
            if (mStatus == STOPPING) {
                res = false;
                doStop();
            }
        }
        return res;
    }

    public void stop() {
        Logger.i(TAG, "call stop");
        mLock.lock();
        if (mStatus == RUNNING) {
            mCondition = mLock.newCondition();
            mStatus = STOPPING;
            try {
                long startTime = SystemClock.uptimeMillis();
                mCondition.await(INTERRUPT_TIME_OUT, TimeUnit.MILLISECONDS);
                Logger.i(TAG, "INTERRUPT_TIME_OUT: " + (SystemClock.uptimeMillis() - startTime));
            } catch (InterruptedException e) {
                Logger.e(TAG, "await failure, err is " + e.getMessage());
            } finally {
                mLock.unlock();
                mCondition = null;
            }
        } else if (mStatus == INIT) {
            mStatus = STOPPING;
            mLock.unlock();
        } else {
            mLock.unlock();
        }
    }

    public boolean isRunning() {
        return mStatus == RUNNING;
    }

    public boolean isNotStart() {
        return mStatus == INIT;
    }

    public void resetRunnable() {
        mStatus = INIT;
        mCondition = null;
    }

    public abstract void execute();

    public abstract void doStop();

    public void setListener(Listener mListener) {
        this.mListener = mListener;
    }

    public interface Listener {
        void runStart(CustomStopRunnable runnable);

        void runEnd(CustomStopRunnable runnable);
    }
}
