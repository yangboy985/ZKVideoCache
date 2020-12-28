package com.qadsdk.impl.videocache.tasks;

import com.qadsdk.base.dev.util.Logger;
import com.qadsdk.impl.videocache.VideoCacheListener;
import com.qadsdk.impl.videocache.bean.VideoInfo;
import com.qadsdk.impl.videocache.config.CacheConfig;
import com.qadsdk.impl.videocache.net.SourceHelper;
import com.qadsdk.impl.videocache.server.LocalRequestInfo;
import com.qadsdk.impl.videocache.server.SocketHelper;
import com.qadsdk.impl.videocache.server.UrlResponder;
import com.qadsdk.impl.videocache.storage.VideoAccessor;
import com.qadsdk.impl.videocache.storage.VideoDataProvider;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;

public class VideoPlayTask extends ResponseTask {
    private static final String TAG = "VideoPlayTask";

    private static final long MAX_WAIT_SIZE = 1024 * 1024L; // 广告都是小视频，2M参数基本上不太可能

    private UrlResponder mUrlResponder;
    private OutputStream mReqOs = null;
    private VideoInfo mInfo;
    private VideoDataProvider mProvider;
    private VideoAccessor mAccessor;

    private final byte[] readBytes = new byte[CacheConfig.DEFAULT_BUFFER_SIZE];
    private long readOffset;

    private VideoCacheListener mListener;

    public VideoPlayTask(UrlResponder responder, Socket socket, LocalRequestInfo request, VideoCacheListener listener) throws IOException {
        super(socket, request);
        if (responder != null) {
            mUrlResponder = responder;
            mProvider = mUrlResponder.getDataProvider();
            mInfo = mProvider.getVideoInfo();
        }
        mListener = listener;
    }

    @Override
    protected void playVideo() throws IOException, InterruptedException, IllegalAccessException {
        readOffset = mRequest.offset;
        checkSocket();
        mReqOs = mSocket.getOutputStream();
        if (!mUrlResponder.waitProviderReady(1000)) {
            mReqOs.write("HTTP/1.1 500 INTERNAL SERVER ERROR\n\n".getBytes("UTF-8"));
            return;
        }
        // 响应请求，建立长连接
        checkSocket();
        if (!mProvider.respClient(mReqOs, readOffset)) {
            return;
        }
        mAccessor = mProvider.getVideoAccessor();
        if (mAccessor == null) {
            throw new IllegalAccessException("get video accessor failure");
        }

        if (isClientReqVideoParams()) {
            getVideoParamsByNet();
        } else {
            playByCache();
        }
    }

    private boolean isClientReqVideoParams() {
        // 可能是在请求包含在视频里面的一些视频解析参数
        long waitSize = readOffset - mAccessor.availableLength();
        if (waitSize <= 0) {
            return false;
        }
        // 广告视频没有快进操作，也就是说，mediaPlayer的请求不应该会超过当前缓存大小，如果超过
        // 那mediaPlayer可能是在请求视频解析参数
        return waitSize > MAX_WAIT_SIZE;
    }

    @Override
    protected void execFinally() {
        mUrlResponder = null;
        mProvider = null;
        mAccessor = null;
    }

    private void playByCache() throws IOException {
        Logger.i(TAG, "playByCache");
        int length;
        while (true) {
            if (!isCarryOn()) {
                Logger.i(TAG, "playByCache: INTERRUPTING");
                break;
            }
            length = mAccessor.read(readBytes, readOffset, CacheConfig.DEFAULT_BUFFER_SIZE);
            checkSocket();
            if (length > 0) {
                mReqOs.write(readBytes, 0, length);
                readOffset += length;
                // 保证不会读取到-1，简化逻辑
                if (readOffset == mInfo.getLength()) {
                    Logger.i(TAG, "all cache are read");
                    mReqOs.flush();
                    break;
                }
            } else if (length != VideoAccessor.CACHE_NOT_ENOUGH) {
                break;
            }
        }
    }

    private void getVideoParamsByNet() throws IOException {
        Logger.i(TAG, "getVideoParamsByNet");
        SourceHelper helper = new SourceHelper(mInfo);
        if (!helper.tryConnect(readOffset)) {
            throw new ConnectException("connect net failure");
        }
        int readLength;
        boolean isChangeToCache = false;
        while (true) {
            if (!isCarryOn()) {
                Logger.i(TAG, "playByNet: INTERRUPTING");
                break;
            }
            readLength = helper.read(readBytes, 0, CacheConfig.DEFAULT_BUFFER_SIZE);
            Logger.i(TAG, "[readLength]: " + readLength);
            if (readLength == -1) {
                mReqOs.flush();
                break;
            }
            if (mInfo.getOffset() > readOffset) {
                isChangeToCache = true;
                break;
            }
            checkSocket();
            mReqOs.write(readBytes, 0, readLength);
            readOffset += readLength;
        }
        if (isChangeToCache) {
            playByCache();
        }
    }

    private void checkSocket() throws SocketException {
        if (mSocket.isClosed() || mUrlResponder.mRequestNoCounter.get() != mRequest.requestNo) {
            Logger.i(TAG, "current top no. = " + mUrlResponder.mRequestNoCounter.get());
            throw new SocketException("client is closed or drop the socket, [req no.]: " + mRequest.requestNo);
        }
    }

    @Override
    public void stop() {
        // 写操作可能阻塞线程
        SocketHelper.releaseSocket(mSocket);
        super.stop();
    }

    @Override
    public void doStop() {
    }
}
