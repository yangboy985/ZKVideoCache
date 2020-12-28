package com.qadsdk.impl.videocache.storage;

import com.qadsdk.base.dev.util.Logger;
import com.qadsdk.impl.videocache.bean.VideoInfo;
import com.qadsdk.impl.videocache.database.StorageHandler;
import com.qadsdk.impl.videocache.exceptions.CacheErrException;
import com.qadsdk.impl.videocache.utils.FileUtil;
import com.qadsdk.impl.videocache.utils.VideoCacheUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class VideoAccessor {
    private static final String TAG = "VideoAccessor";

    public static final int CACHE_NOT_ENOUGH = -2;
    public static final int WRITE_INTERRUPT = -3;

    private SpaceCtrl mCtrl;
    private VideoInfo mInfo;
    private RandomAccessFile dataFile;
    private File tmp;
    private File cache;
    private boolean isWriteErred = false;
    private boolean isWriteInterrupt = false;
    private boolean isOnlyForUsing = false;

    public VideoAccessor(SpaceCtrl ctrl, VideoInfo info) throws FileNotFoundException {
        mCtrl = ctrl;
        mInfo = info;
        cache = FileUtil.getCacheRootFileByName(mInfo.getCacheFileName());
        if (!mInfo.isComplete()) {
            Logger.i(TAG, "video need cache");
            tmp = FileUtil.getCacheRootFileByName(mInfo.getTempFileName());
            dataFile = new RandomAccessFile(tmp, "rw");
        } else {
            Logger.i(TAG, "video is cached");
            dataFile = new RandomAccessFile(cache, "r");
        }
    }

    public synchronized void setOnlyForUsing(boolean onlyForUsing) {
        isOnlyForUsing = onlyForUsing;
    }

    public synchronized void write(byte[] data, int length) throws CacheErrException {
        try {
            if (dataFile.length() != mInfo.getOffset()) {
                throw new CacheErrException("download occur error, offset is not equals with length of file");
            }
            if (!mInfo.isDownloading()) {
                mInfo.setDownloading();
                if (!isOnlyForUsing) {
                    StorageHandler.getInstance().insertOrUpdate(mInfo);
                }
            }
            if (!isOnlyForUsing) {
                mCtrl.releaseSpaceIfNeed(length);
            }
            dataFile.seek(mInfo.getOffset());
            dataFile.write(data, 0, length);
            mInfo.setOffset(mInfo.getOffset() + length);
        } catch (IOException e) {
            throw new CacheErrException(e);
        }
    }

    public synchronized int read(byte[] buffer, long offset, int length) throws IOException {
        if (isWriteErred) {
            return WRITE_INTERRUPT;
        }
        if (offset == mInfo.getLength() && mInfo.getLength() > 0) {
            return -1;
        }
        long realNeedLength = offset + length;
        if (realNeedLength > mInfo.getLength()) {
            realNeedLength = mInfo.getLength();
        }
        if (dataFile.length() < realNeedLength) {
            if (isWriteInterrupt) {
                return WRITE_INTERRUPT;
            } else {
                return CACHE_NOT_ENOUGH;
            }
        }
        dataFile.seek(offset);
        return dataFile.read(buffer, 0, length);
    }

    public synchronized long availableLength() {
        try {
            return dataFile.length();
        } catch (Exception ignore) {
        }
        return 0;
    }

    public synchronized void completeCache() {
        if (isOnlyForUsing) {
            return;
        }
        if (tmp.renameTo(cache)) {
            RandomAccessFile oldAccess = dataFile;
            boolean isErr = false;
            try {
                dataFile = new RandomAccessFile(cache, "r");
            } catch (FileNotFoundException e) {
                Logger.i(TAG, "tmp rename cache success, but find cache failure, " + e.getMessage());
                isErr = true;
            }
            if (!isErr) {
                VideoCacheUtil.closeIO(oldAccess);
            }
        }
    }

    public synchronized void setErr() {
        isWriteErred = true;
        releaseCache();
    }

    public synchronized void setWriteInterrupt() {
        isWriteInterrupt = true;
    }

    public synchronized void releaseCache() {
        // 不进行VideoInfo的数据更新，应该在这之前做好才对
        VideoCacheUtil.closeIO(dataFile);
        if (isOnlyForUsing) {
            tmp.delete();
        }
        dataFile = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VideoAccessor fileCache = (VideoAccessor) o;
        return mInfo.equals(fileCache.mInfo);
    }

    @Override
    public int hashCode() {
        return mInfo.hashCode();
    }
}
