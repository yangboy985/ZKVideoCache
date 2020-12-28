package com.qadsdk.impl.videocache.net;

import android.text.TextUtils;

import com.qadsdk.base.dev.util.Logger;
import com.qadsdk.impl.videocache.VideoCacheEntrance;
import com.qadsdk.impl.videocache.bean.VideoInfo;
import com.qadsdk.impl.videocache.utils.NetUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

public class SourceHelper {
    private static final String TAG = "SourceHelper";
    private static final int MAX_RETRY_TIMES = 3;

    private VideoInfo mInfo;
    private NetSourceRequester mRequester;
    private boolean isConnected = false;

    public SourceHelper(VideoInfo info) {
        mInfo = info;
        mRequester = new NetSourceRequester(info);
    }

    public int read(byte[] bytes, int off, int len) throws IOException {
        return mRequester.read(bytes, off, len);
    }

    public synchronized boolean tryConnect() {
        tryConnect(-1);
        return isConnected;
    }

    public synchronized boolean tryConnect(long offset) {
        if (!NetUtil.isNetworkConnected(VideoCacheEntrance.appContext)) {
            Logger.e(TAG, "network is't connect");
            return false;
        }
        int times = 0;
        do {
            times++;
            isConnected = mRequester.tryConnect(offset == -1 ? mInfo.getOffset() : offset);
        } while (!isConnected && times <= MAX_RETRY_TIMES);
        return isConnected;
    }

    public boolean respClient(OutputStream os, long offset) throws IOException {
        String respMsg = "HTTP/1.1 500 INTERNAL SERVER ERROR\n\n";
        boolean isValidResp = false;
        if (!mInfo.isComplete() && !isConnected) {
            tryConnect();
        }
        if (isConnected || mInfo.isComplete()) {
            respMsg = createRespMsg(offset);
            if (TextUtils.isEmpty(respMsg)) {
                respMsg = "HTTP/1.1 400 BAD REQUEST\n\n";
            } else {
                isValidResp = true;
            }
        } else {
            String httpErrMsg = mRequester.getHttpErrorResp();
            if (httpErrMsg != null) {
                respMsg = httpErrMsg;
            }
        }
        os.write(respMsg.getBytes("UTF-8"));
        Logger.i(TAG, "[resp msg]: " + respMsg);
        return isValidResp;
    }

    private String createRespMsg(long offset) {
        if (mInfo.getLength() > 0 && offset >= mInfo.getLength()) {
            return null;
        }
        boolean isPartial = offset > 0 && mInfo.getLength() > 0;
        String mime = mInfo.getContentType();
        long contentLength = mInfo.getLength() > 0 ? mInfo.getLength() - offset : 0;
        long sourceLength = mInfo.getLength();
        StringBuilder respMsgBuilder = new StringBuilder()
                .append(isPartial ? "HTTP/1.1 206 PARTIAL CONTENT\n" : "HTTP/1.1 200 OK\n")
                .append("Accept-Ranges: bytes\n");
        // 数据一定要准确，不准确就不要传
        if (contentLength > 0 && sourceLength > 0) {
            respMsgBuilder.append(String.format(Locale.getDefault(), "Content-Length: %d\n", contentLength));
            if (isPartial) {
                respMsgBuilder.append(String.format(Locale.getDefault(), "Content-Range: bytes %d-%d/%d\n", offset, sourceLength - 1, sourceLength));
            }
        }
        respMsgBuilder.append(!TextUtils.isEmpty(mime) ? String.format(Locale.getDefault(), "Content-Type: %s\n", mime) : "")
                .append("\n");
        return respMsgBuilder.toString();
    }

    public void releaseHelper() {
        isConnected = false;
        mRequester.releaseConnection();
    }
}
