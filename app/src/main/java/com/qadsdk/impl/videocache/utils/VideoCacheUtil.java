package com.qadsdk.impl.videocache.utils;

import android.text.TextUtils;

import java.io.Closeable;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Just simple utils.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class VideoCacheUtil {
    private static final String TAG = "ProxyCacheUtils";

    private static final int MAX_EXTENSION_LENGTH = 4;

    public static String getExtension(String url) {
        if (TextUtils.isEmpty(url) || !url.startsWith("http") || !url.contains("/")) {
            return null;
        }
        int dotIndex = url.lastIndexOf('.');
        int slashIndex = url.lastIndexOf('/');
        return dotIndex != -1 && dotIndex > slashIndex && dotIndex + 2 + MAX_EXTENSION_LENGTH > url.length() ?
                url.substring(dotIndex + 1) : "";
    }

    public static String encode(String url) {
        try {
            return URLEncoder.encode(url, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Error encoding url", e);
        }
    }

    public static String decode(String url) {
        try {
            return URLDecoder.decode(url, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Error decoding url", e);
        }
    }

    public static String computeMD5(String string) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] digestBytes = messageDigest.digest(string.getBytes());
            return bytesToHexString(digestBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String getDecodeUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }
        if (url.contains("/")) {
            return url;
        } else {
            return VideoCacheUtil.decode(url);
        }
    }

    public static void closeIO(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignore) {
        }
    }

    public static void closeUrlConnection(HttpURLConnection connection) {
        if (connection != null) {
            try {
                connection.getOutputStream().close();
            } catch (Exception ignore) {
            }
            try {
                connection.getInputStream().close();
            } catch (Exception ignore) {
            }
            try {
                connection.disconnect();
            } catch (Exception ignore) {
            }
        }
    }

    public static boolean isTheSameDay(long time1, long time2) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy:MM:dd", Locale.getDefault());
        return format.format(new Date(time1)).equals(format.format(new Date(time2)));
    }
}
