package com.qadsdk.impl.videoplayer;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Build;
import android.text.TextUtils;
import android.view.TextureView;

import com.qadsdk.base.dev.util.Logger;
import com.qadsdk.base.dev.videoplayer.ELState;
import com.qadsdk.base.dev.videoplayer.IResizeAdapter;
import com.qadsdk.base.dev.videoplayer.PlayerEventListener;

import java.io.IOException;

public class VideoPlayer extends BaseVideoPlayer implements SurfaceManager.SurfaceEventListener, VideoCacheHelper.VideoCacheEnvironmentListener {
    private static final String TAG = "VideoPlayer";

    private Context mContext;
    private SurfaceManager mSurfaceManager;
    private VideoProgressMonitor mMonitor;
    private VideoCacheHelper mCacheHelper;
    private PlayerEventListener mPlayerEventListener;

    private String mPath;
    private boolean isStarted = false;
    private boolean isPaused = false;
    private int mPauseProgress = 0;

    public VideoPlayer(TextureView textureView) {
        if (textureView == null || textureView.getContext() == null) {
            throw new IllegalArgumentException("textureView is abnormal");
        }
        mContext = textureView.getContext().getApplicationContext();
        mSurfaceManager = new SurfaceManager(textureView, this);
        createCacheHelper();
    }

    @Override
    public void start(String path, PlayerEventListener listener) {
        if (TextUtils.isEmpty(path)) {
            Logger.e(TAG, "start failure, path = " + path);
            return;
        }
        this.mPlayerEventListener = listener;
        reset();
        mPath = path;
        createMediaPlayer();
        mSurfaceManager.setSurfaceEventListener(this);
    }

    @Override
    public void onSurfacePrepared() {
        Logger.i(TAG, "onSurfacePrepared, curState: " + mCurState);
        if ((!ELState.EL_PAUSE.equals(mCurState) && !ELState.EL_INVALID.equals(mCurState)) || isPaused) {
            return;
        }
        mCacheHelper.environmentPrepare(mPath);
    }

    @Override
    protected void onPlayerPrepared() {
        if (mMediaPlayer != null && mSurfaceManager.isSurfacePrepared() && !isPaused) {
            Logger.i(TAG, "start play video" + (isStarted ? ", pause progress: " + mPauseProgress : ""));
            if (isStarted) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        mMediaPlayer.seekTo(mPauseProgress, MediaPlayer.SEEK_CLOSEST);
                    } else {
                        mMediaPlayer.seekTo(mPauseProgress);
                    }
                } catch (IllegalStateException ignore) {
                    ignore.printStackTrace();
                    mPauseProgress = 0;
                }
            }
            mMediaPlayer.start();
            mMediaPlayer.setVolume(1, 1);
            successMonitorPlayerStart();
            stateChanged(isStarted ? ELState.EL_RESUME : ELState.EL_START);
        }
    }

    @Override
    public void onResume() {
        if (mCurState != ELState.EL_PAUSE && !isPaused) {
            return;
        }
        Logger.i(TAG, "onResume");
        isPaused = false;
        mMediaPlayer.reset();
        mSurfaceManager.setSurfaceEventListener(this);
    }

    @Override
    public void onPause() {
        if (mCurState == ELState.EL_COMPLETE) {
            return;
        }
        Logger.i(TAG, "onPause");
        if (mMediaPlayer != null) {
            isPaused = true;
            mSurfaceManager.setSurfaceEventListener(null);
            if (mMediaPlayer.isPlaying()) {
                Logger.i(TAG, "pause play video");
                mMediaPlayer.pause();
                mPauseProgress = getProgressTime();
                stateChanged(ELState.EL_PAUSE);
                if (mMonitor != null) {
                    mMonitor.pause();
                }
            } else if (mCurState == ELState.EL_INVALID || mCurState == ELState.EL_PREPARE) {
                mCurState = ELState.EL_PAUSE;
            }
        }
    }

    @Override
    public void onRestart() {
        reset();
        mSurfaceManager.setSurfaceEventListener(this);
    }

    @Override
    public void seekTo(int time) {
        if (mMediaPlayer == null || !mMediaPlayer.isPlaying()) {
            return;
        }
        if (mMonitor == null || mMonitor.getDuration() == 0) {
            return;
        }
        if (time > mMonitor.getDuration()) {
            time = mMonitor.getDuration();
        }
        time *= 1000;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mMediaPlayer.seekTo(time, MediaPlayer.SEEK_CLOSEST);
            } else {
                mMediaPlayer.seekTo(time);
            }
        } catch (IllegalStateException ignore) {
        }
    }

    @Override
    public void releasePlayer() {
        reset();
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (mSurfaceManager != null) {
            mSurfaceManager.releaseSurfaceManager();
            mSurfaceManager = null;
        }
        if (mCacheHelper != null) {
            mCacheHelper.releaseHelper();
            mCacheHelper = null;
        }
    }

    @Override
    protected synchronized void notifyStateChange(ELState state) {
        Logger.i(TAG, "notifyStateChange " + state);
        if (ELState.EL_START.equals(state)) {
            isStarted = true;
        }
        getVideoTotalTime();
        int currentProgressTime = mVideoTotalTime;
        if (ELState.EL_ERROR.equals(state)) {
            if (mMonitor != null) {
                // 异常情况下监视器可能一直运行下去
                mMonitor.destroy();
            }
        } else if (!ELState.EL_COMPLETE.equals(state)) {
            currentProgressTime = getProgressTime();
        } else if (mMonitor != null && mMonitor.getDuration() > 0) {
            mMonitor.setVideoComplete();
            if (mPlayerEventListener != null) {
                mPlayerEventListener.onProgress(mMonitor.getDuration(), mMonitor.getDuration());
            }
        }
        if (mPlayerEventListener != null) {
            mPlayerEventListener.onStateChange(state, currentProgressTime, mVideoTotalTime);
        }
    }

    private void setMonitorPlayerStart() {
        if (mMonitor != null) {
            Logger.i(TAG, "setMonitorPlayerStart");
            mMonitor.startPlayerStart();
        }
    }

    private void successMonitorPlayerStart() {
        if (mMonitor != null) {
            Logger.i(TAG, "endMonitorPlayerStart");
            mMonitor.startPlayerSuccess();
        }
    }

    @Override
    protected void videoSizeChanged(int width, int height) {
        mSurfaceManager.uploadVideoSize(width, height);
    }

    @Override
    public void reset() {
        super.reset();
        isStarted = false;
        isPaused = false;
        mPauseProgress = 0;
        mSurfaceManager.setSurfaceEventListener(null);
        if (mMonitor != null) {
            mMonitor.destroy();
        }
        mMonitor = new VideoProgressMonitor(this, mPlayerEventListener);
    }

    public void monitorEnd() {
        if (!ELState.EL_COMPLETE.equals(mCurState) && mMediaPlayer != null) {
            // 监视器end，播放器没有释放，可能出现了异常，没有回调播放完成
            stateChanged(ELState.EL_COMPLETE);
        }
    }

    private void createCacheHelper() {
        if (mCacheHelper == null) {
            mCacheHelper = new VideoCacheHelper(mContext, this);
        }
        mCacheHelper.setVideoCacheListener(this);
    }

    @Override
    public void onPrepared(final String uri, boolean isEnvironmentPrepared) {
        Logger.i(TAG, "start proxy server " + ((isEnvironmentPrepared ? "success" : "failure") + ", url = " + uri));
        try {
            Logger.i(TAG, "prepareAsync: " + uri);
            setMonitorPlayerStart();
            mMediaPlayer.setDataSource(uri);
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            Logger.e(TAG, "prepareAsync occur err, " + e.getMessage());
        }
    }

    @Override
    public void onProgressChanged(int percentage) {
        if (percentage == 100) {
            Logger.i(TAG, "url: " + mPath + ", cache complete");
        }
        if (mPlayerEventListener != null) {
            mPlayerEventListener.onCacheProgress(percentage);
        }
    }

    @Override
    public void uploadLog(int code, String msg) {
        if (mPlayerEventListener != null) {
            mPlayerEventListener.uploadLog(code, msg);
        }
    }

    @Override
    public void setResizeAdapter(IResizeAdapter adapter) {
        mSurfaceManager.setResizeAdapter(adapter);
    }
}
