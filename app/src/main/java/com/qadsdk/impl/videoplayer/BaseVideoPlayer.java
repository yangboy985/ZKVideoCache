package com.qadsdk.impl.videoplayer;

import android.media.MediaPlayer;
import android.view.Surface;

import com.qadsdk.base.dev.util.Logger;
import com.qadsdk.base.dev.videoplayer.ELState;
import com.qadsdk.base.dev.videoplayer.IPlayer;

public abstract class BaseVideoPlayer implements IPlayer {
    private static final String TAG = "BaseVideoPlayer";

    protected MediaPlayer mMediaPlayer;

    protected ELState mCurState;
    protected boolean isPrepared = false;
    protected int mVideoTotalTime;

    protected void createMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            return;
        }
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Logger.i(TAG, "onPrepared");
                isPrepared = true;
                stateChanged(ELState.EL_PREPARE);
            }
        });
        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Logger.i(TAG, "setOnErrorListener.onError " + what + " " + extra);
                if (what == 1 && extra == -2147483648) {
                    if (ELState.EL_INVALID.equals(mCurState)) {
                        // 视频资源本身可能就有问题
                        stateChanged(ELState.EL_START_FAILURE);
                    }
                    return true;
                } else if (what == -38 && extra == 0) {
                    return true;
                } else {
                    stateChanged(ELState.EL_ERROR);
                    return false;
                }
            }
        });
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Logger.i(TAG, "onCompletion");
                if (!ELState.EL_ERROR.equals(mCurState)) {
                    stateChanged(ELState.EL_COMPLETE);
                }
            }
        });
        mMediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                Logger.i(TAG, "onVideoSizeChanged, [width]: " + width + ", [height]: " + height);
                videoSizeChanged(width, height);
            }
        });
        mMediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                    stateChanged(ELState.EL_RENDERING_START);
                }
                return false;
            }
        });
    }

    @Override
    public int getProgressTime() {
        int current = 0;
        if (mMediaPlayer != null) {
            try {
                current = mMediaPlayer.getCurrentPosition();
            } catch (Exception ignore) {
            }
        } else if (ELState.EL_COMPLETE.equals(mCurState)) {
            current = mVideoTotalTime;
        }
        return Math.max(current, 0);
    }

    @Override
    public int getVideoTotalTime() {
        if (mVideoTotalTime > 0) {
            return mVideoTotalTime;
        }
        if (mMediaPlayer == null || !mMediaPlayer.isPlaying()) {
            return mVideoTotalTime;
        }
        int totalTime = 0;
        if (mMediaPlayer != null) {
            totalTime = mMediaPlayer.getDuration();
        }
        mVideoTotalTime = Math.max(totalTime, 0);
        return mVideoTotalTime;
    }

    boolean setSurface(Surface surface) {
        if (mMediaPlayer != null) {
            mMediaPlayer.setSurface(surface);
            return true;
        }
        return false;
    }

    public void reset() {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
        }
        isPrepared = false;
        mCurState = ELState.EL_INVALID;
    }

    protected void stateChanged(ELState state) {
        Logger.i(TAG, "stateChanged, state: " + state);
        if (mCurState.equals(state)) {
            return;
        }
        if (ELState.EL_PREPARE.equals(state)) {
            if (ELState.EL_PAUSE.equals(mCurState)) {
                onPlayerPrepared();
            } else {
                mCurState = state;
                notifyStateChange(mCurState);
                onPlayerPrepared();
            }
            return;
        }
        mCurState = state;
        notifyStateChange(mCurState);
    }

    protected abstract void onPlayerPrepared();

    protected abstract void notifyStateChange(ELState state);

    protected abstract void videoSizeChanged(int width, int height);
}
