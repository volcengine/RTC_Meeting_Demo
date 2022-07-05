package com.volcengine.vertcdemo.bean;

import android.view.SurfaceView;

import com.ss.bytertc.engine.VideoCanvas;

public class VideoCanvasWrapper {
    public VideoCanvas videoCanvas;
    public boolean showVideo;

    public VideoCanvasWrapper(VideoCanvas videoCanvas, boolean showVideo) {
        this.videoCanvas = videoCanvas;
        this.showVideo = showVideo;
    }

    public SurfaceView getSurfaceView() {
        if (videoCanvas == null) {
            return null;
        }
        return (SurfaceView) videoCanvas.renderView;
    }
}
