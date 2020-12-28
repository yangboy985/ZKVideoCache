package com.qadsdk.impl.videocache.utils;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolUtil {
    private ThreadPoolExecutor mExecutor;

    private ThreadPoolUtil() {
    }

    private void newPool() {
        if (mExecutor == null) {
            mExecutor = new ThreadPoolExecutor(3, 50, 0,
                    TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(200),
                    Executors.defaultThreadFactory(), new ThreadPoolExecutor.DiscardPolicy());
        }
    }

    public synchronized void shutdown() {
        if (mExecutor != null) {
            mExecutor.shutdown();
            mExecutor = null;
        }
    }

    public synchronized void submit(Runnable runnable) {
        newPool();
        mExecutor.submit(runnable);
    }

    public static ThreadPoolUtil getInstance() {
        return ThreadPoolUtilImpl.INSTANCE;
    }

    private static final class ThreadPoolUtilImpl {
        private static final ThreadPoolUtil INSTANCE = new ThreadPoolUtil();
    }
}
