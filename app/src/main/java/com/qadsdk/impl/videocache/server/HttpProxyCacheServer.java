package com.qadsdk.impl.videocache.server;

import android.content.Context;
import android.text.TextUtils;

import com.qadsdk.base.dev.util.Logger;
import com.qadsdk.impl.videocache.VideoCacheListener;
import com.qadsdk.impl.videocache.bean.VideoInfo;
import com.qadsdk.impl.videocache.config.CacheConfig;
import com.qadsdk.impl.videocache.database.StorageHandler;
import com.qadsdk.impl.videocache.database.VideoInfoHelper;
import com.qadsdk.impl.videocache.exceptions.VCLogCode;
import com.qadsdk.impl.videocache.storage.FileCacheHelper;
import com.qadsdk.impl.videocache.utils.FileUtil;
import com.qadsdk.impl.videocache.utils.VideoCacheUtil;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class HttpProxyCacheServer {
    private static final String TAG = "HttpProxyCacheServer";

    private static final long WAIT_THREAD_START_TIME_OUT = 3000;

    private ServerSocket serverSocket;
    private SocketManager socketManager;
    private Thread waitRequestsThread;

    private VideoCacheListener mListener;

    public HttpProxyCacheServer() {
    }

    public void startServer(VideoCacheListener listener) {
        mListener = listener;
        try {
            InetAddress inetAddress = InetAddress.getByName(CacheConfig.PROXY_HOST);
            serverSocket = new ServerSocket(0, 5, inetAddress);
            CacheConfig.port = serverSocket.getLocalPort();
        } catch (IOException e) {
            Logger.e(TAG, "Error init local proxy server, err = " + e.getMessage());
            if (mListener != null) {
                mListener.uploadLog(null, VCLogCode.VC_SERVER_INIT_ERROR, "server socket init failure");
                mListener.serverStartEnd(false);
            }
            return;
        }
        socketManager = new SocketManager(mListener);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        waitRequestsThread = new Thread(new WaitRequestsRunnable(countDownLatch));
        waitRequestsThread.start();
        try {
            countDownLatch.await(WAIT_THREAD_START_TIME_OUT, TimeUnit.MILLISECONDS);
            if (mListener != null) {
                if (isServerAlive()) {
                    mListener.serverStartEnd(true);
                    return;
                } else {
                    mListener.uploadLog(null, VCLogCode.VC_SERVER_INIT_ERROR, "ping failure after server init");
                    mListener.serverStartEnd(false);
                }
            }
        } catch (InterruptedException e) {
            Logger.e(TAG, "Error countDownLatch await, err = " + e.getMessage());
            if (mListener != null) {
                mListener.uploadLog(null, VCLogCode.VC_SERVER_INIT_ERROR, "count down latch interrupt");
                mListener.serverStartEnd(false);
            }
        }
        stopServer();
    }

    public void stopServer() {
        if (socketManager != null) {
            socketManager.releaseManager();
        }
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignore) {
            }
        }
        if (waitRequestsThread != null && waitRequestsThread.isAlive()) {
            waitRequestsThread.interrupt();
        }
    }

    public static String getLocalUrl(Context context, String url) {
        if (context == null || TextUtils.isEmpty(url)) {
            Logger.e(TAG, "params is abnormal");
            return url;
        }
        StorageHandler.getInstance().initStorage(context);
        VideoInfo info = StorageHandler.getInstance().findByUrl(url);
        if (VideoInfoHelper.checkVideoInfo(info) && !FileCacheHelper.correctVideoInfo(info, false)) {
            try {
                File file = FileUtil.getCacheRootFileByName(info.getCacheFileName());
                if (file != null && file.exists()) {
                    return file.getCanonicalPath();
                }
            } catch (Exception ignore) {
            }
        }
        return appendToProxyUrl(url);
    }

    private static String appendToProxyUrl(String url) {
        return String.format(Locale.US, "http://%s:%d/%s", CacheConfig.PROXY_HOST, CacheConfig.port, VideoCacheUtil.encode(url));
    }

    public boolean isServerAlive() {
        return new Pinger().ping();
    }

    public static boolean isNeedStartServer(Context context, String url) {
        if (context == null || TextUtils.isEmpty(url)) {
            Logger.e(TAG, "params is abnormal");
            return false;
        }
        StorageHandler.getInstance().initStorage(context);
        VideoInfo info = StorageHandler.getInstance().findByUrl(url);
        if (VideoInfoHelper.checkVideoInfo(info) && !FileCacheHelper.correctVideoInfo(info, false)) {
            try {
                File file = FileUtil.getCacheRootFileByName(info.getCacheFileName());
                if (file != null && file.exists()) {
                    return false;
                }
            } catch (Exception ignore) {
            }
        }
        return true;
    }

    private final class WaitRequestsRunnable implements Runnable {

        private final CountDownLatch countDownLatch;

        public WaitRequestsRunnable(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            countDownLatch.countDown();
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Socket socket = serverSocket.accept();
                    Logger.i(TAG, "Accept new socket " + socket);
                    socketManager.serverAccept(socket);
                }
            } catch (Exception e) {
                if (mListener != null) {
                    mListener.uploadLog(null, VCLogCode.VC_SERVER_ACCEPT_ERROR, e.getMessage());
                }
                Logger.e(TAG, "Error during waiting connection, err = " + e.getMessage());
            }
        }
    }
}
