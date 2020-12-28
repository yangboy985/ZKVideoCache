package com.qadsdk.impl.videocache.database.filter;

import android.text.TextUtils;

import com.qadsdk.base.dev.util.Logger;
import com.qadsdk.impl.videocache.bean.VideoInfo;

public class UrlFinder implements DataFinder<VideoInfo> {
    private static final String TAG = "UrlFilter";

    private String mUrl;

    private UrlFinder(String url) {
        this.mUrl = url;
    }

    public static UrlFinder getFilter(String url) {
        if (TextUtils.isEmpty(url) || !url.startsWith("http")) {
            Logger.i(TAG, "get filter failure");
            return null;
        }
        return new UrlFinder(url);
    }

    @Override
    public boolean isTarget(VideoInfo data) {
        if (data == null) {
            return false;
        }
        return mUrl.equals(data.getUrl());
    }
}
