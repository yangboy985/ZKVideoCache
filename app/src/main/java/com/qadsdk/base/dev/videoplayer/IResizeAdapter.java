package com.qadsdk.base.dev.videoplayer;

import android.view.TextureView;

public interface IResizeAdapter {
    int[] getRealSize(TextureView textureView, int videoWidth, int videoHeight);
}
