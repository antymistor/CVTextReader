package com.example.utils;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.media.FaceDetector;
import java.nio.ByteBuffer;

/**
 * Created by aizhiqiang on 2023/2/22
 *
 * @author aizhiqiang@bytedance.com
 */
public class EyeDetector extends EyeDetectorbase {
    FaceDetector facedect;
    FaceDetector.Face[] facelist;
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
        super(sta);
        facedect = new FaceDetector(detstatus.inputframesize.getWidth(),
                detstatus.inputframesize.getHeight(), detstatus.maxfacecnt);
        facelist = new FaceDetector.Face[detstatus.maxfacecnt];
    }

    @Override
    public EyeStatus getEyeStatus(){
        result.inputFrame = rawByteArray2RGBABitmap2(mPreviewData, detstatus.inputframesize.getWidth(), detstatus.inputframesize.getHeight());
        result.pos.set(new PointF());
        result.facecount = facedect.findFaces(result.inputFrame, facelist);
        if(facelist[0] != null && facelist[0].confidence() >0.5 && result.facecount > 0){
            facelist[0].getMidPoint(result.pos);
        }
        return  result;
    }
}
