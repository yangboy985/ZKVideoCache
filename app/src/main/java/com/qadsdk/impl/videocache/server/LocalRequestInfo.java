package com.qadsdk.impl.videocache.server;

import android.text.TextUtils;

import com.qadsdk.base.dev.util.Logger;
import com.qadsdk.impl.videocache.utils.VideoCacheUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalRequestInfo {
    private static final String TAG = "LocalRequestInfo";

    private static final Pattern RANGE_HEADER_PATTERN = Pattern.compile("[Rr]ange: ?bytes=(\\d+)-");
    private static final Pattern REAL_REQUEST_INFO_PATTERN = Pattern.compile("GET /(.*) HTTP");

    public String realUrl;
    public long offset;
    public long requestNo;

    public LocalRequestInfo(String request) {
        this.offset = findRangeOffset(request);
        this.realUrl = VideoCacheUtil.decode(findRealQuest(request));
    }

    public void setRequestNo(long requestNo) {
        this.requestNo = requestNo;
    }

    public static LocalRequestInfo read(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        StringBuilder requestStr = new StringBuilder();
        String line;
        while (!TextUtils.isEmpty(line = reader.readLine())) {
            requestStr.append(line).append('\n');
        }
        return new LocalRequestInfo(requestStr.toString());
    }

    private long findRangeOffset(String request) {
        Matcher matcher = RANGE_HEADER_PATTERN.matcher(request);
        if (matcher.find()) {
            String range = matcher.group(1);
            return Long.parseLong(range);
        }
        return 0;
    }

    private String findRealQuest(String request) {
        Logger.i(TAG, "[request]: " + request);
        Matcher matcher = REAL_REQUEST_INFO_PATTERN.matcher(request);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("find request info failure");
    }

    @Override
    public String toString() {
        return "LocalRequestInfo{" +
                "uri='" + realUrl + '\'' +
                ", rangeOffset=" + offset +
                '}';
    }
}
