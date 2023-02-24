package com.example.utils;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.FaceDetector;
import android.opengl.GLES11Ext;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by aizhiqiang on 2023/2/22
 *
 * @author aizhiqiang@bytedance.com
 */
public class EyeDetector {
    static public class EyeStatus{
        public Bitmap inputFrame;
        public android.graphics.PointF pos;
    }
    static public class EyeDetectorStatus{
       public Size inputframesize;
       public int maxfacecnt = 1;
    }
    FaceDetector facedect;
    FaceDetector.Face[] facelist;

    EyeDetectorStatus detstatus;
    EyeStatus result;

    Camera mCamera;
    SurfaceTexture mTexture;
    private byte[] mPreviewData;
    public Bitmap rawByteArray2RGBABitmap2(byte[] data, int width, int height) {
        int frameSize = width * height;
        byte[] rgba = new byte[width * height * 2];
        for (int i = 0; i < height; i++)
            for (int j = 0; j < width; j++) {
                int y = (0xff & ((int) data[i * width + j]));
                int u = (0xff & ((int) data[frameSize + (i >> 1) * width + (j & ~1)]));
                int v = (0xff & ((int) data[frameSize + (i >> 1) * width + (j & ~1) + 1]));
                y = Math.max(y, 16);
                int r = Math.round(0.1455f * (y - 16) + 0.399f * (v - 128));
                int g = Math.round(0.291f  * (y - 16) - 0.20325f * (v - 128) - 0.09775f * (u - 128));
                int b = Math.round(0.1455f * (y - 16) + 0.25225f * (u - 128));
                r = r < 0 ? 0 : Math.min(r, 31);
                g = g < 0 ? 0 : Math.min(g, 63);
                b = b < 0 ? 0 : Math.min(b, 31);
                rgba[(i * width + j) * 2 + 1] = (byte) (((b << 3) + (g >> 3)) & 0xff);
                rgba[(i * width + j) * 2] = (byte) (((g << 5) + r) & 0xff);
            }
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565 );
        bmp.copyPixelsFromBuffer(ByteBuffer.wrap(rgba));
        return bmp;
    }
    public EyeDetector(EyeDetectorStatus sta) {
        detstatus = sta;
        result = new EyeStatus();
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
            facedect = new FaceDetector(detstatus.inputframesize.getWidth(),
                    detstatus.inputframesize.getHeight(), detstatus.maxfacecnt);
            facelist = new FaceDetector.Face[detstatus.maxfacecnt];
            mCamera.setDisplayOrientation(90);
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

    public EyeStatus getEyeStatus(){
        result.inputFrame = rawByteArray2RGBABitmap2(mPreviewData, detstatus.inputframesize.getWidth(), detstatus.inputframesize.getHeight());
        result.pos.set(new PointF());
        int cnt = facedect.findFaces(result.inputFrame, facelist);
        if(facelist[0] != null && facelist[0].confidence() >0.5 && cnt > 0){
            facelist[0].getMidPoint(result.pos);
        }
        return  result;
    }
}
