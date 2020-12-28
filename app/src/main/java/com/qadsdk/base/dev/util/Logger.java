package com.qadsdk.base.dev.util;

import android.os.Environment;
import android.util.Log;

import java.io.File;

public class Logger {
    public static boolean sIsDebug = true;
    private static final String TAG = "TQad";
    public static void d(String tag, String msg) {
        if(!sIsDebug) {
            return;
        }
        if (msg != null) {
            if (msg.length() < 2048) {
                Log.i(TAG, "" + tag +": " + msg);
            } else {
                int len = msg.length();
                int cnt = len / 2048 + 1;

                for (int i = 0; i < cnt; i++) {
                    int start = i * 2048;
                    int end = start + 2048;
                    if (end > len) {
                        end = len;
                    }
                    Log.i(TAG, "" + tag +": " + msg.substring(start, end));
                }
            }
        }
    }
    public static void i(String tag, String msg) {
        if(!sIsDebug) {
            return;
        }
        if (msg != null) {
            if (msg.length() < 2048) {
                Log.i(TAG, "" + tag +": " + msg);
            } else {
                int len = msg.length();
                int cnt = len / 2048 + 1;

                for (int i = 0; i < cnt; i++) {
                    int start = i * 2048;
                    int end = start + 2048;
                    if (end > len) {
                        end = len;
                    }
                    Log.i(TAG, "" + tag +": " + msg.substring(start, end));
                }
            }
        }
    }
    public static  void w(String tag, String msg) {
        if (!sIsDebug) {
            return;
        }
        Log.w(TAG, "" + tag +": " + msg);
    }
    public static  void e(String tag, String msg) {
        if (!sIsDebug) {
            return;
        }
        Log.e(TAG, "" + tag +": " + msg);
    }

    public static boolean checkDebug() {
        try {
            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/qlogex.show";
            File file = new File(path);
            return file.exists();
        } catch (Throwable ignore) {
        }
        return false;
    }
}
