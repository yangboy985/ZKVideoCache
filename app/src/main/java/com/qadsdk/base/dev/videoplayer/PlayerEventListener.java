package com.qadsdk.base.dev.videoplayer;

public interface PlayerEventListener {
    void onStateChange(ELState state, int currentProgressTime, int videoTotalTime);

    void onProgress(int currentSecond, int totalSecond);

    void onPlayStatus(boolean isBlocked);

    void onBlockTimeout();

    void onCacheProgress(int percentage);

    void uploadLog(int code, String msg);
}
