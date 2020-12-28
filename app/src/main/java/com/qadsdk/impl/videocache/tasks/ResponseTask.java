package com.qadsdk.impl.videocache.tasks;

import com.qadsdk.base.dev.util.Logger;
import com.qadsdk.impl.videocache.server.LocalRequestInfo;
import com.qadsdk.impl.videocache.server.SocketHelper;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public abstract class ResponseTask extends CustomStopRunnable {
    private static final String TAG = "ResponseTask";

    protected Socket mSocket;
    protected LocalRequestInfo mRequest;

    public ResponseTask(Socket socket, LocalRequestInfo request) throws IOException {
        if (!SocketHelper.isSocketWorkable(socket)) {
            throw new IOException("socket is't workable");
        }
        this.mSocket = socket;
        this.mRequest = request;
    }

    @Override
    public final void execute() {
        handleRequest();
    }

    private void handleRequest() {
        try {
            if (!SocketHelper.respIfIsPinger(mRequest, mSocket)) {
                playVideo();
            }
        } catch (SocketException e) {
            Logger.e(TAG, "handleRequest: SocketException, " + e.getMessage());
        } catch (IOException e) {
            Logger.e(TAG, "handleRequest: " + e.getMessage());
        } catch (Exception e) {
            Logger.e(TAG, "handleRequest: Exception, " + e.getMessage());
        } finally {
            execFinally();
            // 这里不使用releaseSocket，input和output报错会对mediaPlayer造成影响
            SocketHelper.closeSocket(mSocket);
        }
    }

    public String getUrl() {
        return mRequest.realUrl;
    }

    protected abstract void playVideo() throws IOException, InterruptedException, IllegalAccessException;

    protected abstract void execFinally();
}
