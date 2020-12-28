package com.qadsdk.impl.videocache.config;

public class CacheConfig {
    public static final String PROXY_HOST = "127.0.0.1";

    public static final String SP_VIDEO_CACHE_CONFIG = "sp_video_cache_config";

    public static final int DEFAULT_BUFFER_SIZE = 8 * 1024;
    public static final long MAX_NOT_OPT_CACHE_TIME = 30 * 24 * 60 * 60 * 1000L; // 视频缓存若有30天没有使用，则清理掉
    private static final long DEFAULT_MAX_SIZE = 256 * 1024 * 1024L;

    public static final String CACHE_DIR_NAME = "zk_video_cache";

    public static final String CACHE_FILE_TMP_SUFFIX = "_tmp";

    public static int port;

    public static String cacheRoot;

    public static long maxVideoCacheSize = DEFAULT_MAX_SIZE;
}
