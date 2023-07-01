package com.example.utils;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import androidx.annotation.Nullable;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by antymistor on 2023/2/25
 *
 * @author azq2018@zju.edu.cn
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
    private Timer detectorTimer;
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
    public void destroy(){
        if(mCamera != null){
            mCamera.stopPreview();
            mCamera.release();
        }
        if(mTexture != null){
            mTexture.release();
        }
        if(detectorTimer != null){
            detectorTimer.cancel();
            detectorTimer = null;
        }
    }

    public EyeDetector.EyeStatus getEyeStatus(){
        return  result;
    }

    public enum eyeEvent{
        LeftEyeClose,
        RightEyeClose,
        EyeAllOpen,
        EyeAllClose
    }
    public interface IEyeStatusListener {
        void onEvent(EyeDetector2.eyeEvent event);
    }
    private IEyeStatusListener statuslistener;
    private static class eyeRecord{
        LinkedList<Pair<Boolean, Boolean>> EyeQueue = new LinkedList<Pair<Boolean, Boolean>>();
        long last_eye_status_changed_time = 0;
        int maxQueuesize = 6;
    }
    eyeRecord eyerecord;
    public void setListener(IEyeStatusListener listener){
        statuslistener = listener;
        eyerecord = new eyeRecord();
        detectorTimer = new Timer();
        detectorTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                eyerecord.EyeQueue.add(new Pair<>(result.leftEyeIsOpen, result.rightEyeIsOpen));
                while(eyerecord.maxQueuesize < eyerecord.EyeQueue.size()){
                    eyerecord.EyeQueue.poll();
                }
                if(eyerecord.EyeQueue.size() == eyerecord.maxQueuesize){
                    if(eyerecord.EyeQueue.get(4).first && eyerecord.EyeQueue.get(4).second &&
                       eyerecord.EyeQueue.get(5).first && eyerecord.EyeQueue.get(5).second){
                       statuslistener.onEvent(eyeEvent.EyeAllOpen);
                    }else if(!eyerecord.EyeQueue.get(3).first && eyerecord.EyeQueue.get(3).second &&
                       !eyerecord.EyeQueue.get(4).first && eyerecord.EyeQueue.get(4).second &&
                       !eyerecord.EyeQueue.get(5).first && eyerecord.EyeQueue.get(5).second) {
                       statuslistener.onEvent(eyeEvent.LeftEyeClose);
                    }else if(eyerecord.EyeQueue.get(3).first  && !eyerecord.EyeQueue.get(3).second &&
                             eyerecord.EyeQueue.get(4).first  && !eyerecord.EyeQueue.get(4).second &&
                             eyerecord.EyeQueue.get(5).first  && !eyerecord.EyeQueue.get(5).second){
                        statuslistener.onEvent(eyeEvent.RightEyeClose);
                    }else if(!eyerecord.EyeQueue.get(0).first  && !eyerecord.EyeQueue.get(0).second &&
                             !eyerecord.EyeQueue.get(1).first  && !eyerecord.EyeQueue.get(1).second &&
                             !eyerecord.EyeQueue.get(2).first  && !eyerecord.EyeQueue.get(2).second &&
                             !eyerecord.EyeQueue.get(3).first  && !eyerecord.EyeQueue.get(3).second &&
                             !eyerecord.EyeQueue.get(4).first  && !eyerecord.EyeQueue.get(4).second &&
                             !eyerecord.EyeQueue.get(5).first  && !eyerecord.EyeQueue.get(5).second){
                        statuslistener.onEvent(eyeEvent.EyeAllClose);
                    }
                }
            }
        }, 100, 50);
    }
}
