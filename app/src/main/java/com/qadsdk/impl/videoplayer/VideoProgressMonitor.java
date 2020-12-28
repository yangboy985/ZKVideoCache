package com.qadsdk.impl.videoplayer;

import android.os.Handler;
import android.os.Looper;

import com.qadsdk.base.dev.util.Logger;
import com.qadsdk.base.dev.videoplayer.PlayerEventListener;

public class VideoProgressMonitor {
    private static final String TAG = "VideoProgressMonitor";

    private static final long POLLING_TIME_INTERVAL = 500;
    private static final long MIN_POLLING_TIME_INTERVAL = 100;
    private static final long BLOCK_TIME_OUT = 15000; // 15s

    private Handler mHandler;
    private VideoPlayer mVideoPlayer;
    private long mCurrentPosition = 0;
    private long mDuration = 0;
    private int mSecondDuration = 0; // 秒级视屏时长
    private boolean isStarted = false;
    private volatile boolean isPaused = false;
    private boolean isVideoBlocked = false;
    private volatile boolean isVideoComplete = false;

    private volatile boolean isBlockingTimerStarted = false;
    private volatile boolean isStartPlayerEnd = true;

    private PlayerEventListener mEventListener = null;

    public VideoProgressMonitor(VideoPlayer player, PlayerEventListener listener) {
        if (player == null || listener == null) {
            Logger.i(TAG, "player or mediaPlayer or listener is null");
            return;
        }
        mVideoPlayer = player;
        mEventListener = listener;
        // 激励视频场景不需要极高的精度，因此不另开线程
        if (Looper.myLooper() != null) {
            mHandler = new Handler();
        } else {
            mHandler = new Handler(Looper.getMainLooper());
        }
    }

    public synchronized void startPlayerSuccess() {
        isStartPlayerEnd = true;
        if (isStarted) {
            resume();
        } else {
            startCountDown();
        }
    }

    public synchronized void startPlayerStart() {
        isStartPlayerEnd = false;
        if (mHandler != null) {
            blockTimerStart(mCurrentPosition);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isStartPlayerEnd && !isPaused) {
                        if (mEventListener != null) {
                            mEventListener.onPlayStatus(true);
                        }
                    }
                }
            }, 500);
        }
    }

    private synchronized void blockTimerStart(final long blockPosition) {
        if (isBlockingTimerStarted) {
            return;
        }
        isBlockingTimerStarted = true;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                isBlockingTimerStarted = false;
                if (mCurrentPosition == blockPosition && !isPaused) {
                    if (mEventListener != null) {
                        mEventListener.onBlockTimeout();
                    }
                }
            }
        }, BLOCK_TIME_OUT);
    }

    private void startCountDown() {
        if (mHandler == null) {
            return;
        }
        Logger.i(TAG, "startCountDown");
        isStarted = true;
        mCurrentPosition = 0;
        mSecondDuration = getTotalSecond();
        if (mSecondDuration <= 1) {
            Logger.i(TAG, "duration of video is too short");
            return;
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                startPolling();
            }
        });
    }

    private long getNeedDelayMillis() {
        // 主要是为了矫正误差，这个误差不会很大
        long currentPosition = 0;
        try {
            currentPosition = mVideoPlayer.getProgressTime();
        } catch (Exception ignore) {
        }
        long delayMillis = POLLING_TIME_INTERVAL - currentPosition % POLLING_TIME_INTERVAL;
        if (delayMillis < MIN_POLLING_TIME_INTERVAL) {
            // 误差不可能超过400ms，因此很可能是视屏卡顿了
            delayMillis = MIN_POLLING_TIME_INTERVAL;
        }
        return delayMillis;
    }

    private synchronized void startPolling() {
        if (isProgressChanged()) {
            mEventListener.onProgress((int) mCurrentPosition / 1000, mSecondDuration);
        }
        // 视频最后一秒可能是不完整的
        if (!isPaused) {
            if ((mSecondDuration - mCurrentPosition / 1000) >= 1 && !isVideoComplete) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!isPaused) {
                            startPolling();
                        }
                    }
                }, getNeedDelayMillis());
            } else {
                endMonitor();
            }
        } else {
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    private void endMonitor() {
        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mVideoPlayer != null) {
                    mVideoPlayer.monitorEnd();
                }
            }
        }, 1100);
    }

    private boolean isProgressChanged() {
        if (isVideoComplete) {
            mCurrentPosition = mDuration;
            return false;
        }
        long oldPosition = mCurrentPosition;
        try {
            mCurrentPosition = mVideoPlayer.getProgressTime();
        } catch (Exception ignore) {
        }
        if (mCurrentPosition > 0) {
            // 不处理视屏开始播放
            boolean videoStatus = isVideoBlocked;
            isVideoBlocked = oldPosition == mCurrentPosition && mCurrentPosition != mDuration;
            if (videoStatus != isVideoBlocked) {
                if (isVideoBlocked) {
                    blockTimerStart(mCurrentPosition);
                } else {
                    // 因为blockTimerStart参数的原因，每个blockTimer只会在一个进度上起作用
                    isBlockingTimerStarted = false;
                }
                mEventListener.onPlayStatus(isVideoBlocked);
            }
        } else {
            mCurrentPosition = isVideoComplete ? mDuration : oldPosition;
        }

        // 进度为0s或进度在秒级变了
        return oldPosition / 1000 != mCurrentPosition / 1000 || mCurrentPosition == 0;
    }

    public void setVideoComplete() {
        Logger.i(TAG, "setVideoComplete");
        isVideoComplete = true;
    }

    private void resume() {
        Logger.i(TAG, "resume");
        if (mHandler != null) {
            isPaused = false;
            startPolling();
        }
    }

    public void pause() {
        if (mHandler != null) {
            isPaused = true;
            isBlockingTimerStarted = false;
            isStartPlayerEnd = true;
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    public synchronized void destroy() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        isStarted = false;
        isBlockingTimerStarted = false;
        isPaused = false;
        isStartPlayerEnd = true;
        isVideoComplete = false;
        isVideoBlocked = false;
        mVideoPlayer = null;
        mHandler = null;
        mEventListener = null;
    }

    public int getDuration() {
        return mSecondDuration;
    }

    private int getTotalSecond() {
        try {
            mDuration = mVideoPlayer.getVideoTotalTime();
        } catch (Exception ignore) {
        }
        int totalCount = (int) (mDuration / 1000);
        if (mDuration % 1000 > 0) {
            totalCount++;
        }
        return totalCount;
    }
}
