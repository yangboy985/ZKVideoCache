package com.qadsdk.impl.videocache.server;

import com.qadsdk.base.dev.util.Logger;
import com.qadsdk.impl.videocache.VideoCacheListener;
import com.qadsdk.impl.videocache.exceptions.VCLogCode;
import com.qadsdk.impl.videocache.storage.SpaceCtrl;
import com.qadsdk.impl.videocache.tasks.CustomStopRunnable;
import com.qadsdk.impl.videocache.tasks.VideoPlayTask;
import com.qadsdk.impl.videocache.utils.ThreadPoolUtil;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SocketManager implements CustomStopRunnable.Listener {
    private static final String TAG = "SocketManager";

    private ExecutorService socketProcessor = null;
    private LinkedBlockingQueue<Runnable> mQueue = new LinkedBlockingQueue<Runnable>(200);

    private List<CustomStopRunnable> mRunningList = new ArrayList<>();
    private final Map<String, UrlResponder> responderMap = new HashMap<>();

    private VideoCacheListener mListener;
    private SpaceCtrl mSpaceCtrl;

    SocketManager(VideoCacheListener listener) {
        mListener = listener;
        mSpaceCtrl = new SpaceCtrl();
        // 线程池核心线程有点多，但是真正长时间对接播放器的只有一个（广告目前没一个界面几个视频的情况，估计以后也不会有）
        // 过剩的核心线程数据是为了扩大socket的处理量，防止暴增的socket让连接出现异常，影响播放
        socketProcessor = new ThreadPoolExecutor(20, 20, 0L,
                TimeUnit.MILLISECONDS, mQueue, new ThreadPoolExecutor.DiscardPolicy());
    }

    public void serverAccept(final Socket socket) {
        // 这个方法处于单线程中
        final LocalRequestInfo request;
        try {
            request = SocketHelper.getRequestInfo(socket);
        } catch (IOException | IllegalArgumentException e) {
            if (mListener != null) {
                mListener.uploadLog(null, VCLogCode.VC_GET_REQ_INFO_ERROR, e.getMessage());
            }
            Logger.e(TAG, "parse request info failure, " + e.getMessage());
            return;
        }
        ThreadPoolUtil.getInstance().submit(new Runnable() {
            @Override
            public void run() {
                submitTask(socket, request);
            }
        });
    }

    private void submitTask(Socket socket, LocalRequestInfo request) {
        boolean isErr = false;
        try {
            VideoPlayTask task = null;
            if (Pinger.isPingRequest(request.realUrl)) {
                task = new VideoPlayTask(null, socket, request, mListener);
            } else {
                UrlResponder responder = null;
                synchronized (responderMap) {
                    responder = responderMap.get(request.realUrl);
                    if (responder == null) {
                        responder = new UrlResponder(mSpaceCtrl, request.realUrl, mListener);
                        responderMap.put(request.realUrl, responder);
                    }
                }
                task = responder.getTask(socket, request);
                task.setListener(this);
            }
            socketProcessor.submit(task);
            Logger.i(TAG, "submitTask success, req no = " + request.requestNo);
        } catch (Exception e) {
            if (mListener != null) {
                mListener.uploadLog(request.realUrl, VCLogCode.VC_SUBMIT_CACHE_TASK_ERROR, e.getMessage());
            }
            Logger.e(TAG, "submitTask failure, " + e.getMessage());
            isErr = true;
        } finally {
            if (isErr) {
                SocketHelper.releaseSocket(socket);
            }
        }
    }

    synchronized void releaseManager() {
        mQueue.clear();
        for (CustomStopRunnable r : mRunningList) {
            r.stop();
        }
        mRunningList.clear();
        if (socketProcessor != null && !socketProcessor.isShutdown()) {
            socketProcessor.shutdown();
        }
        synchronized (responderMap) {
            Collection<UrlResponder> responders = responderMap.values();
            for (UrlResponder responder : responders) {
                responder.releaseResponder();
            }
        }
    }

    @Override
    public synchronized void runStart(CustomStopRunnable runnable) {
        mRunningList.add(runnable);
    }

    @Override
    public synchronized void runEnd(CustomStopRunnable runnable) {
        mRunningList.remove(runnable);
    }
}
