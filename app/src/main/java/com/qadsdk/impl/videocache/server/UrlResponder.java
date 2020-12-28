package com.qadsdk.impl.videocache.server;

import android.os.SystemClock;
import android.text.TextUtils;

import com.qadsdk.base.dev.util.Logger;
import com.qadsdk.impl.videocache.VideoCacheListener;
import com.qadsdk.impl.videocache.storage.SpaceCtrl;
import com.qadsdk.impl.videocache.storage.VideoDataProvider;
import com.qadsdk.impl.videocache.tasks.VideoPlayTask;
import com.qadsdk.impl.videocache.utils.VideoCacheUtil;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicLong;

public class UrlResponder {
    private static final String TAG = "UrlResponder";

    private String mUrl;
    private Thread mProviderThread;
    private VideoDataProvider mProvider;
    private VideoCacheListener mListener;

    public final AtomicLong mRequestNoCounter = new AtomicLong(0);

    UrlResponder(SpaceCtrl ctrl, String url, VideoCacheListener listener) {
        if (TextUtils.isEmpty(url) || !url.startsWith("http")) {
            throw new IllegalArgumentException("url is abnormal");
        }
        mUrl = url;
        mListener = listener;
        mProvider = new VideoDataProvider(mUrl, ctrl, mListener);
    }

    public VideoPlayTask getTask(Socket socket, LocalRequestInfo request) throws IOException {
        if (!mUrl.equals(request.realUrl)) {
            throw new IllegalArgumentException("url of request is not equal with mUrl");
        }
        request.setRequestNo(mRequestNoCounter.incrementAndGet());
        return new VideoPlayTask(this, socket, request, mListener);
    }

    public VideoDataProvider getDataProvider() {
        return mProvider;
    }

    public synchronized boolean waitProviderReady(long timeout) {
        if (mProviderThread == null || (!mProvider.isComplete() && !mProviderThread.isAlive() && !mProvider.isNotStart())) {
            mProvider.resetRunnable();
            mProviderThread = new Thread(mProvider, "video_cache-" + VideoCacheUtil.computeMD5(mUrl));
            mProviderThread.start();
            long startTime = SystemClock.uptimeMillis();
            boolean isReady = false;
            while (true) {
                if (mProvider.isRunning() || mProviderThread.isAlive()) {
                    Logger.i(TAG, "provider readied");
                    isReady = true;
                    break;
                }
                if (SystemClock.uptimeMillis() - startTime >= timeout) {
                    break;
                }
            }
            return isReady;
        }
        return true;
    }

    public void releaseResponder() {
        if (mProviderThread != null && mProviderThread.isAlive()) {
            mProvider.stop();
        }
        mProvider.releaseProvider();
    }
}
