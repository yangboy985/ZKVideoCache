package com.qadsdk.impl.videocache.net;

import android.text.TextUtils;

import com.qadsdk.base.dev.util.Logger;
import com.qadsdk.impl.videocache.bean.VideoInfo;
import com.qadsdk.impl.videocache.utils.VideoCacheUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_PARTIAL;

public class NetSourceRequester {
    private static final String TAG = "NetVideoRequester";

    private VideoInfo mInfo;
    private HttpURLConnection mConnection;
    private InputStream mInputStream;
    private long startOffset;
    private int respCode;
    private String respMsg;
    private boolean isRespHttpErrorCode = false;

    public NetSourceRequester(VideoInfo info) {
        mInfo = info;
    }

    public synchronized int read(byte[] bytes, int off, int len) throws IOException {
        try {
            if (!isConnected()) {
                throw new ConnectException("not yet connect");
            }
            if (mInputStream == null) {
                mInputStream = mConnection.getInputStream();
            }
            return mInputStream.read(bytes, off, len);
        } catch (IOException e) {
            releaseConnection();
            throw new IOException(e);
        }
    }

    public synchronized boolean tryConnect(long offset) {
        if (offset == startOffset && mConnection != null) {
            return true;
        }
        releaseConnection();
        startOffset = offset;
        boolean isConnectSuccess = false;
        try {
            mConnection = HttpConnector.openConnection(mInfo.getUrl(), startOffset);
            respCode = mConnection.getResponseCode();
            Logger.i(TAG, "[respCode]: " + respCode);
            isConnectSuccess = isConnected();
            if (isConnectSuccess) {
                fillVideoInfo();
            } else {
                isRespHttpErrorCode = true;
                respMsg = mConnection.getResponseMessage();
            }
            Logger.i(TAG, "[isConnectSuccess]: " + isConnectSuccess);
        } catch (IOException e) {
            Logger.e(TAG, "openConnection failure, err is " + e.getMessage());
        } finally {
            if (!isConnectSuccess) {
                releaseConnection();
            }
        }
        return isConnectSuccess;
    }

    String getHttpErrorResp() {
        if (isRespHttpErrorCode && !TextUtils.isEmpty(respMsg)) {
            return "HTTP/1.1 " + respCode + " " + respMsg + "\n\n";
        }
        return null;
    }

    private void fillVideoInfo() {
        readVideoLength();
        mInfo.setContentType(mConnection.getContentType());
    }

    private void readVideoLength() {
        long contentLength = getContentLength();
        if (contentLength <= 0) {
            Logger.e(TAG, "read content length failure");
        } else if (respCode == HTTP_OK) {
            mInfo.setLength(contentLength);
        } else {
            mInfo.setLength(contentLength + startOffset);
        }
        Logger.i(TAG, "readVideoLength: " + mInfo.getLength());
    }

    private long getContentLength() {
        int res = mConnection.getContentLength();
        if (res > 0) {
            return res;
        }
        String contentLength = mConnection.getHeaderField("Content-Length");
        if (TextUtils.isEmpty(contentLength)) {
            return -1;
        }
        return Long.parseLong(contentLength);
    }

    private boolean isConnected() {
        if (mConnection != null) {
            return respCode == HTTP_OK || respCode == HTTP_PARTIAL;
        }
        return false;
    }

    public synchronized void releaseConnection() {
        Logger.i(TAG, "releaseConnection");
        VideoCacheUtil.closeUrlConnection(mConnection);
        mConnection = null;
        VideoCacheUtil.closeIO(mInputStream);
        mInputStream = null;
    }
}
