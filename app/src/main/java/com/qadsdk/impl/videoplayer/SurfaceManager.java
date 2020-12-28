package com.qadsdk.impl.videoplayer;

import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import com.qadsdk.base.dev.util.Logger;
import com.qadsdk.base.dev.util.ResizeUtil;
import com.qadsdk.base.dev.videoplayer.IResizeAdapter;

public class SurfaceManager implements TextureView.SurfaceTextureListener {
    private static final String TAG = "SurfaceManager";

    private TextureView mTextureView;
    private VideoPlayer mPlayer;
    private Surface mSurface;
    private SurfaceEventListener mListener;
    private IResizeAdapter mAdapter = new ParentResizeAdapter();

    private int[] mVideoSize = new int[2];
    private boolean isSurfacePrepared = false;
    private boolean isSetToPlayer = false;

    SurfaceManager(TextureView textureView, VideoPlayer player) {
        mTextureView = textureView;
        mPlayer = player;
        init();
    }

    private void init() {
        mTextureView.setSurfaceTextureListener(this);
        mTextureView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                updateSurfaceSize();
            }
        });
        if (mTextureView.getSurfaceTexture() != null) {
            Logger.i(TAG, "getSurfaceTexture not null, prepared");
            mSurface = new Surface(mTextureView.getSurfaceTexture());
            isSurfacePrepared = true;
            if (mPlayer != null) {
                isSetToPlayer = mPlayer.setSurface(mSurface);
            }
            if (mListener != null) {
                mListener.onSurfacePrepared();
            }
        }
    }

    void setSurfaceEventListener(SurfaceEventListener listener) {
        this.mListener = listener;
        if (isSurfacePrepared) {
            if (!isSetToPlayer && mPlayer != null) {
                isSetToPlayer = mPlayer.setSurface(mSurface);
            }
            if (mListener != null) {
                mListener.onSurfacePrepared();
            }
        }
    }

    public boolean isSurfacePrepared() {
        return isSurfacePrepared;
    }

    public void setResizeAdapter(IResizeAdapter adapter) {
        this.mAdapter = adapter;
    }

    void uploadVideoSize(final int width, final int height) {
        if (width == 0 || height == 0) {
            return;
        }
        if (width == mVideoSize[0] || height == mVideoSize[1]) {
            return;
        }
        mVideoSize[0] = width;
        mVideoSize[1] = height;
        mTextureView.post(new Runnable() {
            @Override
            public void run() {
                updateSurfaceSize();
            }
        });
    }

    private void updateSurfaceSize() {
        if (mAdapter == null) {
            return;
        }
        int[] res = mAdapter.getRealSize(mTextureView, mVideoSize[0], mVideoSize[1]);
        if (res == null) {
            return;
        }
        Logger.i(TAG, "updateSurfaceSize, [width]: " + res[0] + ", [height]: " + res[1]);
        ViewGroup.LayoutParams lp = mTextureView.getLayoutParams();
        if (lp != null) {
            lp.width = res[0];
            lp.height = res[1];
            mTextureView.setLayoutParams(lp);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Logger.i(TAG, "onSurfaceTextureAvailable");
        if (surface == null) {
            return;
        }
        if (mSurface != null && isSetToPlayer) {
            mSurface.release();
        }
        mSurface = new Surface(surface);
        updateSurfaceSize();
        isSurfacePrepared = true;
        if (mPlayer != null && !isSetToPlayer) {
            isSetToPlayer = mPlayer.setSurface(mSurface);
        }
        if (mListener != null) {
            mListener.onSurfacePrepared();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        updateSurfaceSize();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Logger.i(TAG, "onSurfaceTextureDestroyed");
        isSurfacePrepared = false;
        if (mPlayer != null) {
            mPlayer.setSurface(null);
            isSetToPlayer = false;
        }
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    public void releaseSurfaceManager() {
        isSurfacePrepared = false;
        mPlayer.setSurface(null);
        isSetToPlayer = false;
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        mPlayer = null;
        mTextureView = null;
    }

    public interface SurfaceEventListener {
        void onSurfacePrepared();
    }

    public static class ParentResizeAdapter implements IResizeAdapter {
        @Override
        public int[] getRealSize(TextureView textureView, int videoWidth, int videoHeight) {
            // 这种不适用parent里面还有其它子view可以挤压TextureView空间的情况
            if (videoWidth == 0 || videoHeight == 0) {
                return null;
            }
            ViewGroup parent = (ViewGroup) textureView.getParent();
            if (parent == null) {
                return null;
            }
            int width = parent.getWidth();
            int height = parent.getHeight();
            if (width == 0 || height == 0) {
                return null;
            }
            return ResizeUtil.getProportionalScale(width, height, videoWidth, videoHeight);
        }
    }
}
