package com.qadsdk.impl.videocache.database.filter;

import com.qadsdk.impl.videocache.bean.VideoInfo;

public class DownloadingFinder implements DataFinder<VideoInfo> {
    private boolean isPositiveCondition;

    public DownloadingFinder(boolean isPositiveCondition) {
        this.isPositiveCondition = isPositiveCondition;
    }

    @Override
    public boolean isTarget(VideoInfo data) {
        if (data == null) {
            // 空值不会被输出
            return false;
        }
        return isPositiveCondition == data.isDownloading();
    }
}
