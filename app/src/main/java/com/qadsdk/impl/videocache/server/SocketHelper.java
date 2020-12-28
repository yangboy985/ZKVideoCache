package com.qadsdk.impl.videocache.server;

import com.qadsdk.base.dev.util.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class SocketHelper {
    private static final String TAG = "SocketHelper";

    public static boolean respIfIsPinger(LocalRequestInfo request, Socket socket) throws IOException {
        if (request == null || !isSocketWorkable(socket)) {
            throw new IllegalArgumentException("params are abnormal");
        }
        boolean isPinger = Pinger.isPingRequest(request.realUrl);
        if (isPinger) {
            Pinger.responseToPing(socket);
        }
        return isPinger;
    }

    public static LocalRequestInfo getRequestInfo(Socket socket) throws IOException {
        InputStream is = socket.getInputStream();
        LocalRequestInfo request = LocalRequestInfo.read(is);
        Logger.i(TAG, "getRequestInfo: " + request);
        return request;
    }

    public static void releaseSocket(Socket socket) {
        closeSocketInput(socket);
        closeSocketOutput(socket);
        closeSocket(socket);
    }

    private static void closeSocketInput(Socket socket) {
        try {
            if (!socket.isInputShutdown()) {
                socket.shutdownInput();
            }
        } catch (IOException e) {
            Logger.e(TAG, "release socket inputstream failure, " + e.getMessage());
        }
    }

    private static void closeSocketOutput(Socket socket) {
        try {
            if (!socket.isOutputShutdown()) {
                socket.shutdownOutput();
            }
        } catch (IOException e) {
            Logger.e(TAG, "release socket outputstream failure, " + e.getMessage());
        }
    }

    public static void closeSocket(Socket socket) {
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            Logger.e(TAG, "Error closing socket, " + e.getMessage());
        }
    }

    public static boolean isSocketWorkable(Socket socket) {
        if (socket == null) {
            Logger.i(TAG, "socket is null");
            return false;
        }
        if (socket.isClosed()) {
            Logger.i(TAG, "socket was closed");
            return false;
        }
        return true;
    }
}
