package com.example.utils;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by antymistor on 2023/2/25
 *
 * @author azq2018@zju.edu.cn
 */
public class EyeDetector2 extends  EyeDetectorbase{
    FaceDetector detector;
    Timer handleTimer;
    public EyeDetector2(EyeDetector.EyeDetectorStatus sta) {
        super(sta);
        detector =  FaceDetection.getClient(new FaceDetectorOptions.Builder()
                                                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                                                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                                                .build());
        handleTimer = new Timer();
        handleTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                detector.process(InputImage.fromByteArray(
                        mPreviewData,
                        detstatus.inputframesize.getWidth(),
                        detstatus.inputframesize.getHeight(),
                        270,
                        InputImage.IMAGE_FORMAT_NV21)).addOnSuccessListener(
                            new OnSuccessListener<List<Face>>() {
                                @Override
                                public void onSuccess(List<Face> faces) {
                                    float rightEyeOpenProb = 0;
                                    float leftEyeOpenProb = 0;
                                    if(faces.size() > 0) {
                                        if (faces.get(0).getRightEyeOpenProbability() != null) {
                                             rightEyeOpenProb = faces.get(0).getRightEyeOpenProbability() == null ?
                                                    0 : faces.get(0).getRightEyeOpenProbability();
                                             leftEyeOpenProb = faces.get(0).getLeftEyeOpenProbability() ==  null ?
                                                    0 : faces.get(0).getLeftEyeOpenProbability();
                                        }
                                    }
                                    result.facecount      = faces.size();
                                    result.rightEyeIsOpen = rightEyeOpenProb > 0.5f;
                                    result.leftEyeIsOpen  = leftEyeOpenProb > 0.5f;
                                }
                        });
            }
        }, 50, 30);
    }

    @Override
    public void destroy() {
        super.destroy();
        if(handleTimer != null){
            handleTimer.cancel();
            handleTimer = null;
        }
        if(detector != null){
            detector.close();
            detector = null;
        }
    }
}
