package com.qadsdk.impl.videocache.database;

import android.content.Context;

import com.qadsdk.impl.videocache.bean.VideoInfo;
import com.qadsdk.impl.videocache.database.filter.DataFinder;
import com.qadsdk.impl.videocache.utils.FileUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class VideoInfoStorage extends DataStorage<VideoInfo> {
    private static final String KEY_DATA_TYPE = "storage_video_info";

    public VideoInfoStorage(Context context) {
        super(context);
    }

    @Override
    public List<VideoInfo> query(DataFinder<VideoInfo> filter, Comparator<VideoInfo> oderBy) {
        List<VideoInfo> list = getData(filter);
        if (oderBy != null) {
            Collections.sort(list, oderBy);
        }
        return list;
    }

    @Override
    public boolean update(VideoInfo data) {
        if (data == null) {
            return false;
        }
        List<VideoInfo> list = getData(null);
        VideoInfo target = null;
        for (VideoInfo info : list) {
            if (info.getUrl().equals(data.getUrl())) {
                target = info;
                break;
            }
        }
        if (target == null) {
            return false;
        }
        target.updateByVideoInfo(data);
        saveData(list);
        return true;
    }

    @Override
    public boolean insert(VideoInfo data) {
        if (data == null) {
            return false;
        }
        try {
            JSONArray jsonArray = getJsonArrayData();
            JSONObject json = data.writeToJson();
            if (json == null) {
                return false;
            }
            jsonArray.put(json);
            writeData(KEY_DATA_TYPE, jsonArray.toString());
            return true;
        } catch (JSONException ignore) {
            // 存储发生异常，清理一切缓存
            writeData(KEY_DATA_TYPE, "[]");
            FileUtil.clearRootDir();
        }
        return false;
    }

    @Override
    public boolean insertOrUpdate(VideoInfo data) {
        if (data == null) {
            return false;
        }
        List<VideoInfo> list = getData(null);
        VideoInfo target = null;
        for (VideoInfo info : list) {
            if (info.getUrl().equals(data.getUrl())) {
                target = info;
                break;
            }
        }
        if (target == null) {
            list.add(data);
        } else {
            target.updateByVideoInfo(data);
        }
        saveData(list);
        return true;
    }

    @Override
    public boolean delete(VideoInfo data) {
        if (data == null) {
            return false;
        }
        List<VideoInfo> list = getData(null);
        VideoInfo target = null;
        for (VideoInfo info : list) {
            if (info.getUrl().equals(data.getUrl())) {
                target = info;
                break;
            }
        }
        list.remove(target);
        saveData(list);
        return true;
    }

    private JSONArray getJsonArrayData() throws JSONException {
        return new JSONArray(readData(KEY_DATA_TYPE, "[]"));
    }

    private List<VideoInfo> getData(DataFinder<VideoInfo> finder) {
        List<VideoInfo> infoList = new ArrayList<>();
        try {
            JSONArray jsonArray = getJsonArrayData();
            int num = jsonArray.length();
            VideoInfo info;
            for (int i = 0; i < num; i++) {
                info = new VideoInfo().readFromJson(jsonArray.getJSONObject(i));
                if (finder == null || finder.isTarget(info)) {
                    infoList.add(info);
                }
            }
        } catch (JSONException ignore) {
            // 存储发生异常，清理一切缓存
            writeData(KEY_DATA_TYPE, "[]");
            FileUtil.clearRootDir();
            infoList.clear();
        }
        return infoList;
    }

    private void saveData(List<VideoInfo> list) {
        JSONArray jsonArray = new JSONArray();
        JSONObject json;
        for (VideoInfo info : list) {
            json = info.writeToJson();
            if (json != null) {
                jsonArray.put(json);
            }
        }
        writeData(KEY_DATA_TYPE, jsonArray.toString());
    }
}
