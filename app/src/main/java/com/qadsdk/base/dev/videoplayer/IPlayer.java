package com.qadsdk.base.dev.videoplayer;

public interface IPlayer {
    void start(String path, PlayerEventListener listener);

    void onResume();

    void onPause();

    void onRestart();

    void seekTo(int time);

    void releasePlayer();

    void setResizeAdapter(IResizeAdapter adapter);

    int getProgressTime();

    int getVideoTotalTime();
}
