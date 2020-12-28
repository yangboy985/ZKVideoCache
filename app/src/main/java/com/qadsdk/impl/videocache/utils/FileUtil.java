package com.qadsdk.impl.videocache.utils;

import android.text.TextUtils;

import com.qadsdk.impl.videocache.config.CacheConfig;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class FileUtil {
    public static List<File> findFiles(File directory) {
        List<File> result = new LinkedList<>();
        if (directory == null) {
            return result;
        }
        if (!directory.isDirectory()) {
            result.add(directory);
            return result;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return result;
        }
        for (File f : files) {
            result.addAll(findFiles(f));
        }
        return result;
    }

    public static void clearRootDir() {
        File root = new File(CacheConfig.cacheRoot);
        if (root.exists()) {
            List<File> files = findFiles(root);
            for (File f : files) {
                f.delete();
            }
        }
    }

    public static boolean deleteFile(File file) {
        if (file == null) {
            return false;
        }
        if (file.exists()) {
            return file.delete();
        }
        return true;
    }

    public static File getCacheRootFileByName(String name) {
        if (TextUtils.isEmpty(name)) {
            return null;
        }
        return new File(CacheConfig.cacheRoot + File.separator + name);
    }
}
