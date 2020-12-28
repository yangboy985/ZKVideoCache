package com.zk.videocache;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.qadsdk.impl.videocache.bean.VideoInfo;
import com.qadsdk.impl.videocache.database.StorageHandler;
import com.qadsdk.impl.videocache.storage.FileCacheHelper;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    Spinner spinner;
    private TextView out;

    ArrayAdapter<String> adapter;

    List<String> array = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initData();
        init();
    }

    private void initData() {
        array.add(Video.ORANGE_1.name());
        array.add(Video.ORANGE_2.name());
        array.add(Video.ORANGE_3.name());
        array.add(Video.ORANGE_4.name());
        array.add(Video.ORANGE_5.name());
        array.add(Video.ORANGE_6.name());
    }

    private void init() {
        spinner = findViewById(R.id.video_list);
        out = findViewById(R.id.out);
        adapter = new ArrayAdapter<>(this, R.layout.item_spinner, array);
        spinner.setAdapter(adapter);
        findViewById(R.id.query_all).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                out.setText("");
                StorageHandler.getInstance().initStorage(MainActivity.this);
                StorageHandler.getInstance().queryAll(false, new StorageHandler.Consumer() {
                    @Override
                    public boolean accept(VideoInfo info) {
                        out.setText(out.getText() + info.toString() + "\n");
                        return true;
                    }
                });
            }
        });
        findViewById(R.id.clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                out.setText("");
                StorageHandler.getInstance().initStorage(MainActivity.this);
                StorageHandler.getInstance().queryAll(false, new StorageHandler.Consumer() {
                    @Override
                    public boolean accept(VideoInfo info) {
                        out.setText(out.getText() + info.toString() + "\n");
                        FileCacheHelper.releaseCacheVideo(info);
                        return true;
                    }
                });
            }
        });
        findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSharedPreferences("video_test", Context.MODE_PRIVATE).edit().putString("url", Video.getUrlByIndex(spinner.getSelectedItemPosition())).apply();
                startActivity(new Intent(MainActivity.this, VideoActivity.class));
            }
        });
    }
}