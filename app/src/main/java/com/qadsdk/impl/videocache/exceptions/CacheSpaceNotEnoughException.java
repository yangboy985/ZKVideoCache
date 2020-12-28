package com.qadsdk.impl.videocache.exceptions;

public class CacheSpaceNotEnoughException extends RuntimeException {
    public CacheSpaceNotEnoughException(Throwable cause) {
        super(cause);
    }

    public CacheSpaceNotEnoughException(String message) {
        super(message);
    }
}
