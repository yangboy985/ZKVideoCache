package com.qadsdk.impl.videocache.database;

import android.content.Context;
import android.content.SharedPreferences;

import com.qadsdk.impl.videocache.config.CacheConfig;
import com.qadsdk.impl.videocache.database.filter.DataFinder;

import java.util.Comparator;
import java.util.List;

public abstract class DataStorage<T> {
    private Context mContext;

    public DataStorage(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context is null");
        }
        mContext = context.getApplicationContext();
    }

    protected void writeData(String key, String data) {
        getSharedPreferences().edit().putString(key, data).apply();
    }

    protected String readData(String key, String defaultValue) {
        return getSharedPreferences().getString(key, defaultValue);
    }

    protected SharedPreferences getSharedPreferences() {
        return mContext.getSharedPreferences(CacheConfig.SP_VIDEO_CACHE_CONFIG, Context.MODE_PRIVATE);
    }

    public abstract List<T> query(DataFinder<T> filter, Comparator<T> oderBy);

    public abstract boolean update(T data);

    public abstract boolean insert(T data);

    public abstract boolean insertOrUpdate(T data);

    public abstract boolean delete(T data);
}
