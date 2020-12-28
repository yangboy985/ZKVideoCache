package com.qadsdk.impl.videocache.storage;

import android.content.Context;
import android.text.TextUtils;

import com.qadsdk.base.dev.util.Logger;
import com.qadsdk.impl.videocache.bean.VideoInfo;
import com.qadsdk.impl.videocache.config.CacheConfig;
import com.qadsdk.impl.videocache.database.StorageHandler;
import com.qadsdk.impl.videocache.utils.FileUtil;
import com.qadsdk.impl.videocache.utils.VideoCacheUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileCacheHelper {
    private static final String TAG = "FileCacheHelper";

    public static long releaseSpace(final long needSpace) {
        final long[] res = new long[1];
        StorageHandler.getInstance().queryAll(true, new StorageHandler.Consumer() {
            private long deletedSize = 0;

            @Override
            public boolean accept(VideoInfo info) {
                deletedSize += FileCacheHelper.releaseCacheVideo(info);
                res[0] = deletedSize;
                return deletedSize < needSpace;
            }
        });
        return res[0];
    }

    public static void clearDownloadingBeforeServerStart() {
        StorageHandler.getInstance().queryAllDownloading(new StorageHandler.Consumer() {
            @Override
            public boolean accept(VideoInfo info) {
                releaseCacheVideo(info);
                return true;
            }
        });
    }

    public static void releaseUselessCache(Context context) {
        if (context == null || isClearedUselessCache(context)) {
            // 一天清理一次就行了，不用过于频繁
            return;
        }
        final List<String> clearedFileNames = new ArrayList<>();
        StorageHandler.getInstance().queryAll(false, new StorageHandler.Consumer() {
            @Override
            public boolean accept(VideoInfo info) {
                FileCacheHelper.getCacheFileName(info);
                FileCacheHelper.checkCacheVideo(info);
                if (!info.isDeleted()) {
                    clearedFileNames.add(info.isComplete() ? info
                            .getCacheFileName() : info.getTempFileName());
                }
                return true;
            }
        });
        // 清理纯垃圾文件（只是一个保险措施）
        File files = new File(CacheConfig.cacheRoot);
        List<File> caches = FileUtil.findFiles(files);
        for (File f : caches) {
            if (!clearedFileNames.contains(f.getName())) {
                FileUtil.deleteFile(f);
            }
        }
    }

    private static boolean isClearedUselessCache(Context context) {
        long lastClearTime = context.getSharedPreferences(CacheConfig.SP_VIDEO_CACHE_CONFIG, Context.MODE_PRIVATE).getLong("lastClearTime", 0);
        return VideoCacheUtil.isTheSameDay(lastClearTime, System.currentTimeMillis());
    }

    public static boolean isCanCacheVideo(long length) {
        return length < CacheConfig.maxVideoCacheSize && length > 0;
    }

    public static boolean isCacheComplete(VideoInfo info) {
        FileCacheHelper.getCacheFileName(info);
        File cache = FileUtil.getCacheRootFileByName(info.getCacheFileName());
        return cache != null && cache.exists() && cache.length() == info.getLength();
    }

    /**
     * 检查和矫正videoinfo的信息，并创建相应的tmp或cache文件
     *
     * @param info 视频缓存数据
     * @return true：需要下载；false：已完成下载
     */
    public static boolean correctVideoInfo(VideoInfo info, boolean ifCreateTmp) {
        if (info.isDownloading()) {
            return true;
        }
        FileCacheHelper.getCacheFileName(info);
        File dir = new File(CacheConfig.cacheRoot);
        File videoFile = new File(dir, info.getCacheFileName());
        File tmpFile = new File(dir, info.getTempFileName());
        boolean[] res = checkRootDir(dir, tmpFile, info, ifCreateTmp);
        if (res[0]) {
            res = checkCompleteFile(videoFile, tmpFile, info, ifCreateTmp);
        }
        if (res[0]) {
            res = checkTmpFile(videoFile, tmpFile, info, ifCreateTmp);
        }
        StorageHandler.getInstance().update(info);
        return res[1];
    }

    private static boolean[] checkTmpFile(File videoFile, File tmpFile, VideoInfo info, boolean ifCreateTmp) {
        boolean isNeedDownload = true;
        if (tmpFile.exists()) {
            long tmpLength = tmpFile.length();
            if (tmpLength == info.getLength()) {
                tmpFile.renameTo(videoFile);
                info.setComplete();
                isNeedDownload = false;
            } else if (tmpLength > info.getLength()) {
                tmpFile.delete();
                if (ifCreateTmp) {
                    try {
                        tmpFile.createNewFile();
                    } catch (IOException e) {
                        Logger.e(TAG, "create tmpFile failure, err is " + e.getMessage());
                    }
                }
                info.setOffset(0);
            } else {
                info.setOffset(tmpLength);
            }
        } else {
            try {
                tmpFile.createNewFile();
            } catch (IOException e) {
                Logger.e(TAG, "create tmpFile failure, err is " + e.getMessage());
            }
        }
        return new boolean[]{false, isNeedDownload};
    }

    private static boolean[] checkCompleteFile(File videoFile, File tmpFile, VideoInfo info, boolean ifCreateTmp) {
        boolean isNeedNextCheck = true;
        boolean isNeedDownload = true;
        if (info.isComplete()) {
            if (videoFile.exists() && videoFile.length() == info.getLength()) {
                isNeedDownload = false;
            } else {
                info.setOffset(0);
                info.setStatus(VideoInfo.VIDEO_STATUS_DEFAULT);
                if (videoFile.exists()) {
                    videoFile.delete();
                }
                if (tmpFile.exists()) {
                    tmpFile.delete();
                }
                if (ifCreateTmp) {
                    try {
                        tmpFile.createNewFile();
                    } catch (IOException e) {
                        Logger.e(TAG, "create tmpFile failure, err is " + e.getMessage());
                    }
                }
            }
            isNeedNextCheck = false;
        } else {
            if (videoFile.exists()) {
                videoFile.delete();
            }
        }
        return new boolean[]{isNeedNextCheck, isNeedDownload};
    }

    private static boolean[] checkRootDir(File dir, File tmpFile, VideoInfo info, boolean ifCreateTmp) {
        boolean isNeedNextCheck = true;
        if (!dir.exists()) {
            dir.mkdirs();
            if (ifCreateTmp) {
                try {
                    tmpFile.createNewFile();
                } catch (IOException e) {
                    Logger.e(TAG, "create tmpFile failure, err is " + e.getMessage());
                }
            }
            info.setOffset(0);
            info.setStatus(VideoInfo.VIDEO_STATUS_DEFAULT);
            isNeedNextCheck = false;
        }
        return new boolean[]{isNeedNextCheck, true};
    }

    public static void checkCacheVideo(VideoInfo info) {
        if (info.getLastUseTime() + CacheConfig.MAX_NOT_OPT_CACHE_TIME < System.currentTimeMillis()) {
            FileCacheHelper.releaseCacheVideo(info);
            return;
        }
        Logger.i(TAG, "video cache complete: " + info.isComplete());
        if (info.isComplete()) {
            File cache = FileUtil.getCacheRootFileByName(info.getCacheFileName());
            if (!cache.exists() || cache.length() != info.getLength()) {
                Logger.i(TAG, "cache file is not exist");
                FileCacheHelper.releaseCacheVideo(info);
            } else {
                FileUtil.deleteFile(FileUtil.getCacheRootFileByName(info.getTempFileName()));
            }
        } else {
            File tmp = FileUtil.getCacheRootFileByName(info.getTempFileName());
            if (!tmp.exists()) {
                Logger.i(TAG, "tmp file is not exist");
                FileCacheHelper.releaseCacheVideo(info);
            } else {
                info.setOffset(tmp.length());
                FileUtil.deleteFile(FileUtil.getCacheRootFileByName(info.getCacheFileName()));
            }
        }
    }

    public static long releaseCacheVideo(VideoInfo info) {
        Logger.i(TAG, "releaseCacheVideo, url = " + info.getUrl());
        if (StorageHandler.getInstance().delete(info)) {
            info.setDeleted(true);
            return FileCacheHelper.clearCacheFile(info);
        }
        Logger.i(TAG, "delete database data failure");
        return 0;
    }

    private static long clearCacheFile(VideoInfo info) {
        long deleteSize = 0;
        FileCacheHelper.getCacheFileName(info);
        File cache = FileUtil.getCacheRootFileByName(info.getCacheFileName());
        File tmp = FileUtil.getCacheRootFileByName(info.getTempFileName());
        long cacheSize = 0;
        if (cache != null && cache.exists()) {
            cacheSize = cache.length();
        }
        Logger.i(TAG, "cacheSize: " + cacheSize);
        long tmpSize = 0;
        if (tmp != null && tmp.exists()) {
            tmpSize = tmp.length();
        }
        Logger.i(TAG, "tmpSize: " + tmpSize);
        if (FileUtil.deleteFile(cache)) {
            deleteSize += cacheSize;
        }
        if (FileUtil.deleteFile(tmp)) {
            deleteSize += tmpSize;
        }
        return deleteSize;
    }

    public static void getCacheFileName(VideoInfo info) {
        info.setTempFileName(info.getMd5() + CacheConfig.CACHE_FILE_TMP_SUFFIX);
        String extension = VideoCacheUtil.getExtension(info.getUrl());
        if (TextUtils.isEmpty(extension)) {
            info.setCacheFileName(info.getMd5());
        } else {
            info.setCacheFileName(info.getMd5() + "." + extension);
        }
    }
}
