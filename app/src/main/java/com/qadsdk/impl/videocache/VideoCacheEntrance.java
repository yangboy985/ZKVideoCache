package com.qadsdk.impl.videocache;

import android.content.Context;

import com.qadsdk.impl.videocache.config.CacheConfig;
import com.qadsdk.impl.videocache.database.StorageHandler;
import com.qadsdk.impl.videocache.server.HttpProxyCacheServer;
import com.qadsdk.impl.videocache.storage.FileCacheHelper;
import com.qadsdk.impl.videocache.utils.ThreadPoolUtil;

/**
 * 线程环境过于复杂，还要支持多进程，只能简化能力了
 * 仅支持播放时边播边缓存；
 * 不支持多下载；
 * 不支持提前缓存；
 * 不支持多进程多个VideoCacheEntrance同时操作，一个进程同一时间仅允许一个有效的VideoCacheEntrance存在
 */
public class VideoCacheEntrance {
    private HttpProxyCacheServer mServer;
    public static Context appContext;

    public VideoCacheEntrance(Context context) {
        if (context == null) {
            throw new NullPointerException("context is null");
        }
        appContext = context.getApplicationContext();
        CacheConfig.cacheRoot = appContext.getDir(CacheConfig.CACHE_DIR_NAME, Context.MODE_PRIVATE).toString();
        StorageHandler.getInstance().initStorage(appContext);
        // 程序里面的线程都是自然结束（stop都是条件结束），因此程序应付不了突然暴毙的情况，在这里矫正保证缓存的准确性
        FileCacheHelper.clearDownloadingBeforeServerStart();
        // 清理垃圾文件，缓存信息纠错
        FileCacheHelper.releaseUselessCache(appContext);
    }

    public static String getLocalUrl(Context context, String url) {
        return HttpProxyCacheServer.getLocalUrl(context, url);
    }

    public static boolean isNeedStartServer(Context context, String url) {
        return HttpProxyCacheServer.isNeedStartServer(context, url);
    }

    public void asyncStartLocalServer(final VideoCacheListener listener) {
        ThreadPoolUtil.getInstance().submit(new Runnable() {
            @Override
            public void run() {
                if (mServer != null && mServer.isServerAlive()) {
                    listener.serverStartEnd(true);
                    return;
                }
                mServer = new HttpProxyCacheServer();
                mServer.startServer(listener);
            }
        });
    }

    public void releaseServer() {
        ThreadPoolUtil.getInstance().submit(new Runnable() {
            @Override
            public void run() {
                if (mServer != null) {
                    mServer.stopServer();
                }
                ThreadPoolUtil.getInstance().shutdown();
            }
        });
    }
}
