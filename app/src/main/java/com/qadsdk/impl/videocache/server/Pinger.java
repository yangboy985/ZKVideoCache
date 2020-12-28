package com.qadsdk.impl.videocache.server;

import com.qadsdk.base.dev.util.Logger;
import com.qadsdk.impl.videocache.config.CacheConfig;
import com.qadsdk.impl.videocache.net.HttpConnector;
import com.qadsdk.impl.videocache.utils.VideoCacheUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

class Pinger {
    private static final String TAG = "Pinger";
    private static final String PING_REQUEST = "ping";
    private static final String PING_RESPONSE = "ping ok";

    private static final int REPEAT_TIMES = 3;
    private static final int TIME_OUT = 70;

    private final ExecutorService pingExecutor = Executors.newSingleThreadExecutor();

    Pinger() {
    }

    boolean ping() {
        int timeout = TIME_OUT;
        int attempts = 0;
        while (attempts < REPEAT_TIMES) {
            try {
                Future<Boolean> pingFuture = pingExecutor.submit(new PingCallable());
                boolean pinged = pingFuture.get(timeout, MILLISECONDS);
                if (pinged) {
                    return true;
                }
            } catch (TimeoutException e) {
                Logger.w(TAG, "Error pinging server (attempt: " + attempts + ", timeout: " + timeout + "). ");
            } catch (InterruptedException | ExecutionException e) {
                Logger.w(TAG, "Error pinging server due to unexpected error, " + e.getMessage());
            }
            attempts++;
            timeout *= 2;
        }
        return false;
    }

    public static boolean isPingRequest(String request) {
        return PING_REQUEST.equals(request);
    }

    public static void responseToPing(Socket socket) throws IOException {
        OutputStream out = socket.getOutputStream();
        out.write("HTTP/1.1 200 OK\n\n".getBytes());
        out.write(PING_RESPONSE.getBytes());
    }

    private boolean pingServer() throws IOException {
        HttpURLConnection connection = null;
        try {
            String pingUrl = getPingUrl();
            connection = HttpConnector.openConnection(pingUrl, 0);
            InputStream is = connection.getInputStream();
            byte[] expectedResponse = PING_RESPONSE.getBytes();
            byte[] response = new byte[expectedResponse.length];
            is.read(response);
            return Arrays.equals(expectedResponse, response);
        } finally {
            VideoCacheUtil.closeUrlConnection(connection);
        }
    }

    private String getPingUrl() {
        return String.format(Locale.US, "http://%s:%d/%s", CacheConfig.PROXY_HOST, CacheConfig.port, PING_REQUEST);
    }

    private class PingCallable implements Callable<Boolean> {

        @Override
        public Boolean call() throws Exception {
            return pingServer();
        }
    }

}
