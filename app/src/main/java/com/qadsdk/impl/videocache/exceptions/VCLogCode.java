package com.qadsdk.impl.videocache.exceptions;

public class VCLogCode {
    private static final int VC_CODE_BASE = 10000000;

    public static final int VC_SERVER_INIT_ERROR = VC_CODE_BASE + 1;

    public static final int VC_SERVER_ACCEPT_ERROR = VC_CODE_BASE + 2;

    public static final int VC_GET_REQ_INFO_ERROR = VC_CODE_BASE + 3;

    public static final int VC_SUBMIT_CACHE_TASK_ERROR = VC_CODE_BASE + 4;

    public static final int VC_CACHE_START = VC_CODE_BASE + 5;

    public static final int VC_CACHE_COMPLETE = VC_CODE_BASE + 6;
}
