package com.qadsdk.impl.videoplayer;

import android.content.Context;

import com.qadsdk.base.dev.util.Logger;
import com.qadsdk.impl.videocache.VideoCacheEntrance;
import com.qadsdk.impl.videocache.VideoCacheListener;

public class VideoCacheHelper implements VideoCacheListener {
    private static final String TAG = "VideoCacheHelper";

    private Context mAppContext;
    private VideoCacheEntrance mEntrance;
    private VideoCacheEnvironmentListener mListener;
    private String mUrl;
    private String mUrlForPlayer;

    VideoCacheHelper(Context context, VideoPlayer player) {
        mAppContext = context.getApplicationContext();
        mEntrance = new VideoCacheEntrance(mAppContext);
    }

    synchronized void environmentPrepare(String url) {
        Logger.i(TAG, "environmentPrepare: url = " + url);
        if (!url.startsWith("http")) {
            if (mListener != null) {
                mListener.onPrepared(url, false);
            }
            return;
        }
        mUrl = url;
        if (VideoCacheEntrance.isNeedStartServer(mAppContext, url)) {
            Logger.i(TAG, "isNeedStartServer true");
            if (mEntrance != null) {
                mEntrance.asyncStartLocalServer(this);
            }
        } else {
            mUrlForPlayer = VideoCacheEntrance.getLocalUrl(mAppContext, mUrl);
            Logger.i(TAG, "isNeedStartServer false, url = " + mUrlForPlayer);
            if (mListener != null) {
                boolean isCached = !mUrl.equals(mUrlForPlayer);
                mListener.onPrepared(isCached ? mUrlForPlayer : mUrl, isCached);
            }
        }
    }

    synchronized void setVideoCacheListener(VideoCacheEnvironmentListener listener) {
        this.mListener = listener;
    }

    @Override
    public synchronized void serverStartEnd(boolean isSuccess) {
        Logger.i(TAG, "serverStartEnd " + isSuccess);
        if (isSuccess) {
            mUrlForPlayer = VideoCacheEntrance.getLocalUrl(mAppContext, mUrl);
        }
        if (mListener != null) {
            mListener.onPrepared(isSuccess ? mUrlForPlayer : mUrl, isSuccess);
        }
    }

    @Override
    public synchronized void cacheProgress(String url, int percentage) {
        if (mUrl.equals(url)) {
            if (mListener != null) {
                mListener.onProgressChanged(percentage);
            }
        }
    }

    @Override
    public void uploadLog(String url, int code, String msg) {
        if (mUrl.equals(url) || url == null) {
            if (mListener != null) {
                mListener.uploadLog(code, msg);
            }
        }
    }

    synchronized void releaseHelper() {
        mListener = null;
        if (mEntrance != null) {
            mEntrance.releaseServer();
        }
    }

    public interface VideoCacheEnvironmentListener {
        void onPrepared(String uri, boolean isEnvironmentPrepared);

        void onProgressChanged(int percentage);

        void uploadLog(int code, String msg);
    }
}
