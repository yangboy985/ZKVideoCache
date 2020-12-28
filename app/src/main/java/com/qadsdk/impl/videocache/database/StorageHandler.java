package com.qadsdk.impl.videocache.database;

import android.annotation.SuppressLint;
import android.content.Context;

import com.qadsdk.base.dev.util.Logger;
import com.qadsdk.impl.videocache.bean.VideoInfo;
import com.qadsdk.impl.videocache.database.filter.DownloadingFinder;
import com.qadsdk.impl.videocache.database.filter.UrlFinder;
import com.qadsdk.impl.videocache.storage.FileCacheHelper;

import java.util.List;

public class StorageHandler {
    private static final String TAG = "StorageHandler";

    private DataStorage<VideoInfo> storage;

    private StorageHandler() {
    }

    public synchronized void initStorage(Context context) {
        if (storage != null) {
            return;
        }
        if (context == null) {
            throw new IllegalArgumentException("context is null");
        }
        storage = new VideoInfoStorage(context);
    }

    public synchronized List<VideoInfo> queryAll(boolean isExcludeDownloading, Consumer consumer) {
        checkStorage();
        DownloadingFinder finder = null;
        if (isExcludeDownloading) {
            finder = new DownloadingFinder(false);
        }
        List<VideoInfo> results = storage.query(finder, null);
        if (consumer != null) {
            for (VideoInfo info : results) {
                if (!consumer.accept(info)) {
                    break;
                }
            }
        }
        return results;
    }

    public synchronized List<VideoInfo> queryAllDownloading(Consumer consumer) {
        checkStorage();
        List<VideoInfo> results = storage.query(new DownloadingFinder(true), null);
        if (consumer != null) {
            for (VideoInfo info : results) {
                if (!consumer.accept(info)) {
                    break;
                }
            }
        }
        return results;
    }

    public synchronized VideoInfo findByUrl(String url) {
        Logger.i(TAG, "[findInfoByUrl]: url = " + url);
        checkStorage();
        UrlFinder filter = UrlFinder.getFilter(url);
        if (filter == null) {
            return null;
        }
        List<VideoInfo> list = storage.query(filter, null);
        if (list.size() == 1) {
            return list.get(0);
        }
        if (list.size() > 1) {
            Logger.i(TAG, "Uniqueness of url was destroyed, url = " + url);
        }
        for (VideoInfo info : list) {
            FileCacheHelper.releaseCacheVideo(info);
        }
        return null;
    }

    public synchronized boolean insert(VideoInfo info) {
        if (!VideoInfoHelper.checkVideoInfo(info)) {
            return false;
        }
        checkStorage();
        boolean res = storage.insert(info);
        Logger.i(TAG, "insert: res = " + res);
        return res;
    }

    public synchronized void update(VideoInfo info) {
        if (!VideoInfoHelper.checkVideoInfo(info) || !info.isSaved()) {
            return;
        }
        checkStorage();
        boolean res = storage.update(info);
        Logger.i(TAG, "update: res = " + res);
    }

    public synchronized boolean insertOrUpdate(VideoInfo info) {
        if (!VideoInfoHelper.checkVideoInfo(info)) {
            return false;
        }
        checkStorage();
        boolean res = storage.insertOrUpdate(info);
        if (res) {
            info.setSaved(true);
        }
        Logger.i(TAG, "insertOrUpdate: res = " + res);
        return res;
    }

    public synchronized boolean delete(VideoInfo info) {
        checkStorage();
        boolean res = storage.delete(info);
        Logger.i(TAG, "delete: res = " + res);
        if (res) {
            info.setSaved(false);
        }
        return res;
    }

    private void checkStorage() {
        if (storage == null) {
            throw new IllegalStateException("storage has't init");
        }
    }

    public void releaseStorage() {
        storage = null;
    }

    public static StorageHandler getInstance() {
        return StorageHandlerImpl.INSTANCE;
    }

    private static final class StorageHandlerImpl {
        @SuppressLint("StaticFieldLeak")
        private static final StorageHandler INSTANCE = new StorageHandler();
    }

    public interface Consumer {
        /**
         * 遍历用
         *
         * @param info 视频信息
         * @return true：继续遍历；false：中断遍历
         */
        boolean accept(VideoInfo info);
    }
}
