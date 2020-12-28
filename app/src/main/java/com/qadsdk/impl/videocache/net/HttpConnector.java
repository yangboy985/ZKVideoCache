package com.qadsdk.impl.videocache.net;

import com.qadsdk.base.dev.util.Logger;
import com.qadsdk.impl.videocache.utils.VideoCacheUtil;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;

public class HttpConnector {
    private static final String TAG = "HttpConnector";
    private static final int MAX_REDIRECTS = 5;
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 15000;

    public static HttpURLConnection openConnection(String url, long offset) throws IOException {
        HttpURLConnection connection;
        boolean redirected;
        int redirectCount = 0;
        do {
            Logger.d(TAG, "Open connection " + (offset > 0 ? " with offset " + offset : "") + " to " + url);
            connection = (HttpURLConnection) new URL(url).openConnection();
            if (offset > 0) {
                connection.setRequestProperty("Range", "bytes=" + offset + "-");
            }
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.connect();
            int code = connection.getResponseCode();
            redirected = code == HTTP_MOVED_PERM || code == HTTP_MOVED_TEMP || code == HTTP_SEE_OTHER;
            if (redirected) {
                url = connection.getHeaderField("Location");
                redirectCount++;
                VideoCacheUtil.closeUrlConnection(connection);
            }
            if (redirectCount > MAX_REDIRECTS) {
                throw new IOException("Too many redirects: " + redirectCount);
            }
        } while (redirected);
        return connection;
    }
}
