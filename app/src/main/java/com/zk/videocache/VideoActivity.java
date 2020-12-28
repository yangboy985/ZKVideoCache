package com.zk.videocache;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.TextureView;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.qadsdk.base.dev.util.Logger;
import com.qadsdk.base.dev.videoplayer.ELState;
import com.qadsdk.base.dev.videoplayer.PlayerEventListener;
import com.qadsdk.impl.videoplayer.VideoPlayer;

import java.util.Locale;

public class VideoActivity extends Activity implements PlayerEventListener {
    private static final String TAG = "VideoActivity";
    private TextureView textureView;
    private TextView stop, total, seek, percentage;
    private EditText seekTime;

    private VideoPlayer player;
    private String mUrl = null;
    int status = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        textureView = findViewById(R.id.tv_video);
        stop = findViewById(R.id.stop);
        total = findViewById(R.id.total_time);
        seek = findViewById(R.id.seek);
        percentage = findViewById(R.id.percentage);
        seekTime = findViewById(R.id.seek_time);

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player == null) {
                    return;
                }
                if (status == 0) {
                    status = 1;
                    stop.setBackgroundColor(Color.parseColor("#ff0000"));
                    player.onPause();

                } else if (status == 1) {
                    status = 0;
                    stop.setBackgroundColor(Color.parseColor("#00ff00"));
                    player.onResume();
                } else {
                    status = 0;
                    stop.setBackgroundColor(Color.parseColor("#00ff00"));
                    player.start(mUrl, VideoActivity.this);
                }
            }
        });

        seek.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String str = seekTime.getText().toString().trim();
                if (!TextUtils.isEmpty(str)) {
                    if (player != null) {
                        player.seekTo(Integer.parseInt(str));
                    }
                }
            }
        });

        mUrl = getSharedPreferences("video_test", Context.MODE_PRIVATE).getString("url", null);
        Logger.i(TAG, "[mUrl]: " + mUrl);
        startVideo();
    }

    @Override
    protected void onDestroy() {
        if (player != null) {
            player.releasePlayer();
        }
        super.onDestroy();
    }

    private void startVideo() {
        player = new VideoPlayer(textureView);
        player.start(mUrl, this);
    }

    @Override
    public void onStateChange(ELState state, int currentProgressTime, int videoTotalTime) {
        if (state.equals(ELState.EL_COMPLETE)) {
            status = 2;
            stop.setBackgroundColor(Color.parseColor("#B69DCB"));
        }
    }

    @Override
    public void onProgress(int currentSecond, int totalSecond) {
        total.setText(String.format(Locale.getDefault(), "%d/%d", currentSecond, totalSecond));
    }

    @Override
    public void onPlayStatus(boolean isBlocked) {
        Logger.i(TAG, "[onPlayStatus]: " + isBlocked);
    }

    @Override
    public void onBlockTimeout() {
        Logger.i(TAG, "onBlockTimeout");
    }

    @Override
    public void onCacheProgress(final int percentage) {
        Logger.i(TAG, "[onCacheProgress]: " + percentage);
        this.percentage.post(new Runnable() {
            @Override
            public void run() {
                VideoActivity.this.percentage.setText(String.format(Locale.getDefault(), "%d%%", percentage));
            }
        });
    }

    @Override
    public void uploadLog(int code, String msg) {
        Logger.i(TAG, "[code]: " + code + ", [msg]: " + msg);
    }
}