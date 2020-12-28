package com.qadsdk.base.dev.util;

public class ResizeUtil {
    private static final String TAG = "ResizeUtil";

    public static int[] getProportionalScale(int containerWidth, int containerHeight, int targetWidth, int targetHeight) {
        if (containerWidth <= 0 || targetWidth <= 0) {
            return new int[]{containerWidth, containerHeight};
        }
        float targetRate = targetHeight * 1f / targetWidth;
        float containerRate = containerHeight * 1f / containerWidth;
        int pxWidth = containerWidth;
        int pxHeight = containerHeight;
        if (containerRate > targetRate) {
            pxHeight = (int) (pxWidth * targetRate);
        } else {
            pxWidth = (int) (pxHeight / targetRate);
        }
        return new int[]{pxWidth, pxHeight};
    }
}
