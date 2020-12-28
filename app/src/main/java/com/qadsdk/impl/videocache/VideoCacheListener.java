package com.qadsdk.impl.videocache;

public interface VideoCacheListener {
    void serverStartEnd(boolean isSuccess);

    void cacheProgress(String url, int percentage);

    void uploadLog(String url, int code, String msg);
}
