package com.qadsdk.impl.videocache.database;

import android.text.TextUtils;

import com.qadsdk.base.dev.util.Logger;
import com.qadsdk.impl.videocache.bean.VideoInfo;
import com.qadsdk.impl.videocache.storage.FileCacheHelper;
import com.qadsdk.impl.videocache.utils.VideoCacheUtil;

public class VideoInfoHelper {
    private static final String TAG = "VideoInfoHelper";

    public static boolean checkVideoInfo(VideoInfo info) {
        if (info == null) {
            Logger.e(TAG, "info is null");
            return false;
        }
        Logger.i(TAG, "[checkVideoInfo]: " + info.toString());
        boolean isValid = !TextUtils.isEmpty(info.getUrl());
        isValid &= !TextUtils.isEmpty(info.getMd5());
        return isValid;
    }

    public static VideoInfo createInfoByUrl(String url) {
        if (TextUtils.isEmpty(url) || !url.startsWith("http")) {
            return null;
        }
        VideoInfo info = new VideoInfo();
        info.setSaved(false);
        info.setUrl(VideoCacheUtil.getDecodeUrl(url));
        info.setMd5(VideoCacheUtil.computeMD5(info.getUrl()));
        FileCacheHelper.getCacheFileName(info);
        return info;
    }
}
