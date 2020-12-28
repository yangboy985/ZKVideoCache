package com.qadsdk.impl.videocache.exceptions;

public class CacheErrException extends RuntimeException {
    public CacheErrException(String message) {
        super(message);
    }

    public CacheErrException(Throwable cause) {
        super(cause);
    }
}
