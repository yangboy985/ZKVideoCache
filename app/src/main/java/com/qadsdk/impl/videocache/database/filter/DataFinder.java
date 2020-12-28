package com.qadsdk.impl.videocache.database.filter;

public interface DataFinder<T> {
    boolean isTarget(T data);
}
