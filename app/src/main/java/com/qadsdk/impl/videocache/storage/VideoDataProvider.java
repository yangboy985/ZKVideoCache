package com.qadsdk.impl.videocache.storage;

import android.os.SystemClock;

import com.qadsdk.base.dev.util.Logger;
import com.qadsdk.impl.videocache.VideoCacheListener;
import com.qadsdk.impl.videocache.bean.VideoInfo;
import com.qadsdk.impl.videocache.config.CacheConfig;
import com.qadsdk.impl.videocache.database.StorageHandler;
import com.qadsdk.impl.videocache.database.VideoInfoHelper;
import com.qadsdk.impl.videocache.exceptions.CacheErrException;
import com.qadsdk.impl.videocache.exceptions.CacheSpaceNotEnoughException;
import com.qadsdk.impl.videocache.exceptions.VCLogCode;
import com.qadsdk.impl.videocache.net.SourceHelper;
import com.qadsdk.impl.videocache.tasks.CustomStopRunnable;
import com.qadsdk.impl.videocache.utils.VideoCacheUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class VideoDataProvider extends CustomStopRunnable {
    private static final String TAG = "VideoDataProvider";

    private static final long WAIT_FILE_CACHE_TIMEOUT = 10000L; // 10s

    private VideoInfo mInfo;
    private SourceHelper mSourceHelper;
    private VideoCacheListener mListener;
    private VideoAccessor mAccessor;
    private SpaceCtrl mCtrl;
    private boolean isOnlyCacheForUsing = false;
    private boolean isDownloadComplete = false;
    private boolean isRespPrepared = false;

    private ArrayBlockingQueue<VideoAccessor> accessorContainer = new ArrayBlockingQueue<>(1);

    private int mCachePercentage;
    private final byte[] readBytes = new byte[CacheConfig.DEFAULT_BUFFER_SIZE];
    private long downloadStartTime = 0;

    public VideoDataProvider(String url, SpaceCtrl ctrl, VideoCacheListener listener) {
        getVideoInfo(url);
        mCtrl = ctrl;
        mListener = listener;
        mSourceHelper = new SourceHelper(mInfo);
    }

    private void getVideoInfo(String url) {
        String decodeUrl = VideoCacheUtil.getDecodeUrl(url);
        VideoInfo info = StorageHandler.getInstance().findByUrl(decodeUrl);
        if (info == null) {
            info = VideoInfoHelper.createInfoByUrl(url);
        }
        if (info == null) {
            throw new IllegalArgumentException("url is abnormal, " + url);
        }
        FileCacheHelper.getCacheFileName(info);
        FileCacheHelper.correctVideoInfo(info, true);
        mInfo = info;
        mInfo.setLastUseTime(System.currentTimeMillis());
    }

    private void updateVideoInfo() {
        mInfo.setStatus(VideoInfo.VIDEO_STATUS_DEFAULT);
        mInfo.setLastUseTime(System.currentTimeMillis());
        mInfo.setSaved(false);
        mInfo.setOffset(0);
        mInfo.setDeleted(false);
    }

    public synchronized VideoAccessor getVideoAccessor() throws InterruptedException {
        if (mAccessor != null && accessorContainer.isEmpty()) {
            return mAccessor;
        } else {
            return accessorContainer.poll(WAIT_FILE_CACHE_TIMEOUT, TimeUnit.MILLISECONDS);
        }
    }

    public synchronized boolean respClient(OutputStream os, long offset) throws IOException {
        return mSourceHelper.respClient(os, offset);
    }

    @Override
    public void execute() {
        boolean isSuccess = providerPrepare();
        if (!isSuccess) {
            isRespPrepared = false;
            Logger.e(TAG, "provider prepare failure");
            mSourceHelper.releaseHelper();
            return;
        }
        if (mInfo.isComplete()) {
            isRespPrepared = true;
            Logger.i(TAG, "video is cached");
            return;
        }
        isRespPrepared = true;
        download();
    }

    private void download() {
        Logger.i(TAG, "download");
        try {
            int readLength = 0;
            boolean isReadNetSourceFailure = false;
            if (mListener != null) {
                mListener.uploadLog(mInfo.getUrl(), VCLogCode.VC_CACHE_START, "[offset]: " + mInfo.getOffset());
            }
            if (downloadStartTime == 0) {
                downloadStartTime = SystemClock.uptimeMillis();
            }
            while (true) {
                if (!isCarryOn()) {
                    Logger.i(TAG, "download interrupt");
                    break;
                }
                try {
                    readLength = mSourceHelper.read(readBytes, 0, CacheConfig.DEFAULT_BUFFER_SIZE);
                } catch (Exception e) {
                    Logger.e(TAG, "read net source failure, " + e.getMessage());
                    isReadNetSourceFailure = true;
                }
                if (isReadNetSourceFailure && isCarryOn() && mSourceHelper.tryConnect()) {
                    isReadNetSourceFailure = false;
                    continue;
                } else if (isReadNetSourceFailure) {
                    throw new Exception("read net source failure");
                }
                if (readLength == -1) {
                    if (mInfo.getOffset() != mInfo.getLength() && mInfo.getLength() > 0) {
                        Logger.e(TAG, "download end, offset = " + mInfo.getOffset() + ", length = " + mInfo.getLength());
                        writeErr();
                    } else {
                        Logger.i(TAG, "download complete");
                        mAccessor.completeCache();
                        isDownloadComplete = true;
                        if (mListener != null) {
                            mListener.uploadLog(mInfo.getUrl(), VCLogCode.VC_CACHE_COMPLETE, "[video length]: "
                                    + mInfo.getLength() + ", [space]: " + (SystemClock.uptimeMillis() - downloadStartTime));
                        }
                    }
                    break;
                }
                mAccessor.write(readBytes, readLength);
                if (!isOnlyCacheForUsing) {
                    mCtrl.notifyWrite(readLength);
                }
                tryNotifyProgress();
            }
        } catch (CacheErrException | CacheSpaceNotEnoughException e) {
            Logger.e(TAG, "download failure, " + e.getMessage());
            writeErr();
        } catch (Exception e) {
            Logger.e(TAG, "download Exception, " + e.getMessage());
            mAccessor.setWriteInterrupt();
        } finally {
            downloadEnd();
            if (!mInfo.isComplete()) {
                isRespPrepared = false;
            }
        }
    }

    private void writeErr() {
        mAccessor.setErr();
        if (!isOnlyCacheForUsing) {
            mCtrl.notifyRelease(FileCacheHelper.releaseCacheVideo(mInfo));
        }
    }

    private void downloadEnd() {
        mSourceHelper.releaseHelper();
        if (mInfo.isDeleted()) {
            synchronized (this) {
                updateVideoInfo();
            }
            return;
        }
        boolean isCanCache;
        Logger.i(TAG, "downloadEnd: isDownloadComplete: " + isDownloadComplete);
        if (isDownloadComplete) {
            if (mInfo.getLength() == 0) {
                isCanCache = FileCacheHelper.isCanCacheVideo(mInfo.getLength());
                if (isCanCache) {
                    mInfo.setLength(mInfo.getOffset());
                    isOnlyCacheForUsing = false;
                    mAccessor.setOnlyForUsing(false);
                    mAccessor.completeCache();
                    mCtrl.notifyRelease(mInfo.getLength());
                }
            }
            isCanCache = !isOnlyCacheForUsing;
            mInfo.setComplete();
        } else {
            mInfo.setStatus(VideoInfo.VIDEO_STATUS_DEFAULT);
            isCanCache = !isOnlyCacheForUsing;
        }
        if (isCanCache) {
            Logger.i(TAG, "downloadEnd: update info");
            StorageHandler.getInstance().insertOrUpdate(mInfo);
        }
    }

    @Override
    public void doStop() {
    }

    @Override
    public void resetRunnable() {
        super.resetRunnable();
        if (!isComplete()) {
            isRespPrepared = false;
            accessorContainer.clear();
            mAccessor = null;
        }
    }

    private boolean providerPrepare() {
        Logger.i(TAG, "providerPrepare");
        if (!isCarryOn()) {
            Logger.i(TAG, "is interrupt before prepare");
            return false;
        }
        boolean isErr = false;
        try {
            mAccessor = new VideoAccessor(mCtrl, mInfo);
        } catch (FileNotFoundException e) {
            Logger.i(TAG, "get VideoAccessor failure, " + e.getMessage());
            isErr = true;
        }
        if (mInfo.isComplete()) {
            Logger.i(TAG, "video is cached");
            if (isErr) {
                mCtrl.notifyRelease(FileCacheHelper.releaseCacheVideo(mInfo));
                if (mInfo.isDeleted()) {
                    Logger.e(TAG, "err delete success");
                    synchronized (this) {
                        updateVideoInfo();
                    }
                }
                return false;
            } else {
                return true;
            }
        }
        if (isErr) {
            return false;
        }
        if (!mSourceHelper.tryConnect()) {
            Logger.i(TAG, "tryConnect failure");
            return false;
        }
        Logger.i(TAG, "video length: " + mInfo.getLength());
        isOnlyCacheForUsing = !FileCacheHelper.isCanCacheVideo(mInfo.getLength());
        mAccessor.setOnlyForUsing(isOnlyCacheForUsing);
        if (!isOnlyCacheForUsing) {
            Logger.i(TAG, "this video can cache, " + mInfo.getUrl());
            mInfo.setStatus(VideoInfo.VIDEO_STATUS_DEFAULT);
            if (!StorageHandler.getInstance().insertOrUpdate(mInfo)) {
                // 更新或插入任务到数据库失败
                Logger.e(TAG, "insertOrUpdate failure");
                return false;
            }
        }
        Logger.i(TAG, "task prepared");
        accessorContainer.add(mAccessor);
        return true;
    }

    private void tryNotifyProgress() {
        if (mInfo.getLength() <= 0) {
            return;
        }
        int percentage = Long.valueOf(mInfo.getOffset() * 100 / mInfo.getLength()).intValue();
        if (mListener != null && mCachePercentage != percentage) {
            mCachePercentage = percentage;
            mListener.cacheProgress(mInfo.getUrl(), mCachePercentage);
        }
    }

    public boolean isComplete() {
        if (mInfo == null || !isRespPrepared) {
            return false;
        }
        return mInfo.isComplete();
    }

    public VideoInfo getVideoInfo() {
        return mInfo;
    }

    public synchronized void releaseProvider() {
        if (mAccessor != null) {
            mAccessor.releaseCache();
        }
    }
}
