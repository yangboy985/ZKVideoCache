package com.qadsdk.impl.videocache.storage;

import com.qadsdk.impl.videocache.config.CacheConfig;
import com.qadsdk.impl.videocache.exceptions.CacheSpaceNotEnoughException;
import com.qadsdk.impl.videocache.utils.FileUtil;

import java.io.File;
import java.util.List;

public class SpaceCtrl {
    private long freeSpace = 0;

    public SpaceCtrl() {
        getFreeSpace();
    }

    private void getFreeSpace() {
        long totalUsedSpace = 0;
        File rootDir = new File(CacheConfig.cacheRoot);
        List<File> caches = FileUtil.findFiles(rootDir);
        for (File file : caches) {
            totalUsedSpace += file.length();
        }
        freeSpace = CacheConfig.maxVideoCacheSize - totalUsedSpace;
    }

    public synchronized void notifyWrite(int length) {
        freeSpace -= length;
    }

    public synchronized void notifyRelease(long length) {
        freeSpace += length;
    }

    public synchronized void releaseSpaceIfNeed(int length) {
        if (freeSpace < length) {
            long needSpace = length - freeSpace;
            long deletedSize = FileCacheHelper.releaseSpace(needSpace);
            freeSpace += deletedSize;
            if (deletedSize < needSpace) {
                // 最大缓存大小过小才会出现这种情况
                throw new CacheSpaceNotEnoughException("space not enough");
            }
        }
    }
}
