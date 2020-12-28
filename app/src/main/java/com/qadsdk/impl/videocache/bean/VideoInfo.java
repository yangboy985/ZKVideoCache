package com.qadsdk.impl.videocache.bean;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class VideoInfo {
    public static final int VIDEO_STATUS_DEFAULT = 0; // 没下载完，且没有相应的任务支持
    public static final int VIDEO_STATUS_COMPLETE = 1;
    public static final int VIDEO_STATUS_DOWNLOADING = 2;

    public static final String FIELD_URL = "url";
    public static final String FIELD_MD5 = "md5";
    public static final String FIELD_CONTENT_TYPE = "content_type";
    public static final String FIELD_LENGTH = "length";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_LAST_USE_TIME = "lastUseTime";

    private String url;
    private String md5;
    private String contentType;
    private long length;
    private int status = VIDEO_STATUS_DEFAULT;
    private long lastUseTime;

    private boolean isSaved = false;
    private long offset;
    private String tempFileName;
    private String cacheFileName;
    private boolean isDeleted = false;

    public VideoInfo() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getLastUseTime() {
        return lastUseTime;
    }

    public void setLastUseTime(long lastUseTime) {
        this.lastUseTime = lastUseTime;
    }

    public boolean isSaved() {
        return isSaved;
    }

    public void setSaved(boolean saved) {
        isSaved = saved;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public String getTempFileName() {
        return tempFileName;
    }

    public void setTempFileName(String tempFileName) {
        this.tempFileName = tempFileName;
    }

    public String getCacheFileName() {
        return cacheFileName;
    }

    public void setCacheFileName(String cacheFileName) {
        this.cacheFileName = cacheFileName;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }


    public boolean isComplete() {
        return status == VIDEO_STATUS_COMPLETE;
    }

    public boolean isDownloading() {
        return status == VIDEO_STATUS_DOWNLOADING;
    }

    public void setComplete() {
        status = VIDEO_STATUS_COMPLETE;
    }

    public void setDownloading() {
        status = VIDEO_STATUS_DOWNLOADING;
    }

    public VideoInfo readFromJson(JSONObject json) {
        url = json.optString(FIELD_URL);
        md5 = json.optString(FIELD_MD5);
        contentType = json.optString(FIELD_CONTENT_TYPE);
        length = json.optLong(FIELD_LENGTH);
        status = json.optInt(FIELD_STATUS);
        lastUseTime = json.optLong(FIELD_LAST_USE_TIME);
        return this;
    }

    public JSONObject writeToJson() {
        JSONObject json = null;
        try {
            json = new JSONObject();
            json.put(FIELD_URL, url);
            json.put(FIELD_MD5, md5);
            json.put(FIELD_CONTENT_TYPE, contentType);
            json.put(FIELD_LENGTH, length);
            json.put(FIELD_STATUS, status);
            json.put(FIELD_LAST_USE_TIME, lastUseTime);
        } catch (JSONException ignore) {
            return null;
        }
        return json;
    }

    public void updateByVideoInfo(VideoInfo info) {
        if (info == null || !url.equals(info.getUrl())) {
            return;
        }
        length = info.getLength();
        status = info.getStatus();
        lastUseTime = info.lastUseTime;
        // 其它字段不需要更新，这个是为了更新SP文件中的数据进行的更新
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VideoInfo info = (VideoInfo) o;
        return url.equals(info.url);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{url});
    }

    @Override
    public String toString() {
        return "VideoInfo{" +
                "url='" + url + '\'' +
                ", md5='" + md5 + '\'' +
                ", contentType='" + contentType + '\'' +
                ", length=" + length +
                ", status=" + status +
                ", lastUseTime=" + lastUseTime +
                '}';
    }
}
