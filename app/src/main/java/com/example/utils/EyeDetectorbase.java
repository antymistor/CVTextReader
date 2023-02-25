package com.example.utils;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.util.Size;
import androidx.annotation.Nullable;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by aizhiqiang on 2023/2/25
 *
 * @author aizhiqiang@bytedance.com
 */
public class EyeDetectorbase {
    static public class EyeStatus{
        public Bitmap inputFrame;
        public android.graphics.PointF pos;
        public boolean leftEyeIsOpen = false;
        public boolean rightEyeIsOpen = false;
        public int facecount = 0;
    }
    static public class EyeDetectorStatus{
        public Size inputframesize;
        public int maxfacecnt = 1;
    }

    EyeDetector.EyeDetectorStatus detstatus;
    EyeDetector.EyeStatus result;

    Camera mCamera;
    SurfaceTexture mTexture;
    public byte[] mPreviewData;
    public EyeDetectorbase(EyeDetector.EyeDetectorStatus sta) {
        detstatus = sta;
        result = new EyeDetector.EyeStatus();
        result.pos = new PointF();
        mCamera = Camera.open(1);
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPictureSize(detstatus.inputframesize.getHeight(), detstatus.inputframesize.getWidth());
        parameters.setPreviewSize(detstatus.inputframesize.getHeight(), detstatus.inputframesize.getWidth());
        mCamera.setParameters(parameters);
        mTexture = new SurfaceTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        try {
            mTexture = new SurfaceTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
            mCamera.setPreviewTexture(mTexture);
            mPreviewData = new byte[mCamera.getParameters().getPreviewSize().width *
                    mCamera.getParameters().getPreviewSize().height * 3 / 2];
            detstatus.inputframesize = new Size(mCamera.getParameters().getPreviewSize().width, mCamera.getParameters().getPreviewSize().height);
            mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    mCamera.addCallbackBuffer(mPreviewData);
                }
            });
            mCamera.addCallbackBuffer(mPreviewData);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public EyeDetector.EyeStatus getEyeStatus(){
        return  result;
    }

    public enum eyeEvent{
        doubleFlip,
        onlyLeftFlip,
        onlyRightFlip
    }
    public interface IEyeStatusListener {
        void onEvent(EyeDetector2.eyeEvent event);
    }
    private IEyeStatusListener statuslistener;
    private static class eyeRecord{
        boolean last_left_eye_open  = true;
        boolean last_right_eye_open = true;
        long last_eye_status_changed_time = 0;
    }
    eyeRecord eyerecord;
    private void handlelistener(@Nullable EyeDetector2.eyeEvent event){
        long lasttime = eyerecord.last_eye_status_changed_time;
        eyerecord.last_eye_status_changed_time = System.currentTimeMillis();
        if(event != null){
            statuslistener.onEvent(event);
        }
        if((System.currentTimeMillis() - lasttime) < 1000){
            statuslistener.onEvent(EyeDetector2.eyeEvent.doubleFlip);
        }
    }
    public void setListener(IEyeStatusListener listener){
        statuslistener = listener;
        eyerecord = new eyeRecord();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if(!eyerecord.last_left_eye_open &&
                        eyerecord.last_right_eye_open &&
                        result.leftEyeIsOpen &&
                        result.rightEyeIsOpen){
                    handlelistener(EyeDetector2.eyeEvent.onlyLeftFlip);
                }else if( eyerecord.last_left_eye_open &&
                        !eyerecord.last_right_eye_open &&
                        result.leftEyeIsOpen &&
                        result.rightEyeIsOpen){
                    handlelistener(EyeDetector2.eyeEvent.onlyRightFlip);
                }else if(! eyerecord.last_left_eye_open &&
                        !eyerecord.last_right_eye_open &&
                        result.leftEyeIsOpen &&
                        result.rightEyeIsOpen){
                    handlelistener(null);
                }
                eyerecord.last_left_eye_open  = result.leftEyeIsOpen;
                eyerecord.last_right_eye_open = result.rightEyeIsOpen;
            }
        }, 100, 100);
    }
}
