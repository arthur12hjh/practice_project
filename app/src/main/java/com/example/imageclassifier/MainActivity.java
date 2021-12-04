package com.example.imageclassifier;

import static java.lang.Math.abs;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.app.Fragment;

import com.example.imageclassifier.camera.CameraFragment;
import com.example.imageclassifier.camera.CustomView;
import com.example.imageclassifier.tflite.Classifier;
import com.example.imageclassifier.tflite.FindFace;
import com.example.imageclassifier.tflite.Landmarks;
import com.example.imageclassifier.utils.YuvToRgbConverter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "[IC]MainActivity";

    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private static final int PERMISSION_REQUEST_CODE = 1;

    private TextView textView;
    private TextView logText;

    private FindFace ff;
    private Landmarks lm;

    private int previewWidth = 0;
    private int previewHeight = 0;
    private int sensorOrientation = 0;

    private Bitmap rgbFrameBitmap = null;
    private Bitmap face_img = null;

    private HandlerThread handlerThread;
    private Handler handler;

    private boolean isProcessingFrame = false;

    int displayWidth;
    int displayHeight;

    private FrameLayout getsize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        textView = findViewById(R.id.textView);

        getsize = findViewById(R.id.getsize);

        Display display = getWindowManager().getDefaultDisplay();  // in Activity
        /* getActivity().getWindowManager().getDefaultDisplay() */ // in Fragment
        Point size = new Point();
        display.getSize(size); // or getSize(size)
        displayWidth = size.x;
        displayHeight = size.y;


//        logText = findViewById(R.id.logText);

//        Bitmap bitmap = Bitmap.createBitmap(800,800, Bitmap.Config.ARGB_8888);
//        Canvas canvas = new Canvas(bitmap);
//        canvas.drawColor(Color.WHITE);
//
//        ImageView filter = findViewById(R.id.filter);
//        filter.setImageBitmap(bitmap);
//
//        Paint paint = new Paint();
//
//        paint.setColor(Color.RED);
//        paint.setStrokeWidth(30f);
//        canvas.drawPoint(360, 640, paint);



        try {
            ff = new FindFace(this);
            ff.init();
            lm = new Landmarks(this);
            lm.init();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        if(checkSelfPermission(CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            setFragment();
        } else {
            requestPermissions(new String[]{CAMERA_PERMISSION}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    protected synchronized void onDestroy() {
        ff.finish();
        lm.finish();
        super.onDestroy();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();

        handlerThread = new HandlerThread("InferenceThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public synchronized void onPause() {
        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }

        super.onPause();
    }

    @Override
    public synchronized void onStart() {
        super.onStart();
    }

    @Override
    public synchronized void onStop() {
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == PERMISSION_REQUEST_CODE) {
            if(grantResults.length > 0 && allPermissionsGranted(grantResults)) {
                setFragment();
            } else {
                Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private boolean allPermissionsGranted(final int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

//    protected void setFragment() {
//        Size inputSize = cls.getModelInputSize();
//        String cameraId = chooseCamera();
//
//        if(inputSize.getWidth() > 0 && inputSize.getHeight() > 0 && !cameraId.isEmpty()) {
//            Fragment fragment = CameraFragment.newInstance(
//                    (size, rotation) -> {
//                        previewWidth = size.getWidth();
//                        previewHeight = size.getHeight();
//                        sensorOrientation = rotation - getScreenOrientation();
//                    },
//                    reader->processImage(reader),
//                    inputSize,
//                    cameraId);
//
//            Log.d(TAG, "inputSize : " + cls.getModelInputSize() +
//                    "sensorOrientation : " + sensorOrientation);
//            getFragmentManager().beginTransaction().replace(
//                    R.id.fragment, fragment).commit();
//        } else {
//            Toast.makeText(this, "Can't find camera", Toast.LENGTH_SHORT).show();
//        }
//    }

    protected void setFragment() {
        Size inputSize_2 = ff.getModelInputSize();
        String cameraId = chooseCamera();

        if(inputSize_2.getWidth() > 0 && inputSize_2.getHeight() > 0 && !cameraId.isEmpty()) {
            Fragment fragment = CameraFragment.newInstance(
                    (size, rotation) -> {
                        previewWidth = size.getWidth();
                        previewHeight = size.getHeight();
                        sensorOrientation = rotation - getScreenOrientation();
                    },
                    reader->processImage(reader),
                    inputSize_2,
                    cameraId);




            Log.d("kk", "inputSize : " + ff.getModelInputSize() +
                    "sensorOrientation : " + sensorOrientation);
            getFragmentManager().beginTransaction().replace(
                    R.id.fragment, fragment).commit();
        } else {
            Toast.makeText(this, "Can't find camera", Toast.LENGTH_SHORT).show();
        }
    }

    private String chooseCamera() {
        final CameraManager manager =
                (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try {
            for (final String cameraId : manager.getCameraIdList()) {
                final CameraCharacteristics characteristics =
                        manager.getCameraCharacteristics(cameraId);

                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            e.printStackTrace();
        }

        return "";
    }

    protected int getScreenOrientation() {
        switch (getDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }

    protected void processImage(ImageReader reader) {
        if (previewWidth == 0 || previewHeight == 0) {
            return;
        }

        if(rgbFrameBitmap == null) {
            rgbFrameBitmap = Bitmap.createBitmap(
                    previewWidth,
                    previewHeight,
                    Bitmap.Config.ARGB_8888);
        }

        if (isProcessingFrame) {
            return;
        }

        isProcessingFrame = true;

        final Image image = reader.acquireLatestImage();

        Log.d("ratio" , "imaggesize = " + image.getHeight() + ", " + image.getWidth());
        Log.d("dspsize", "display" + displayWidth + ", " + displayHeight);

        if (image == null) {
            isProcessingFrame = false;
            return;
        }


        Log.d("getsize", String.valueOf(image.getHeight()));

        YuvToRgbConverter.yuvToRgb(this, image, rgbFrameBitmap);


//        runInBackground(() -> {
//            if (cls != null && cls.isInitialized()) {
//                final long startTime = SystemClock.uptimeMillis();
//                final Pair<String, Float> output = cls.classify(rgbFrameBitmap, sensorOrientation);
//                final long elapsedTime = SystemClock.uptimeMillis() - startTime;
//
//                runOnUiThread(() -> {
//                    String logStr = elapsedTime + " ms";
//                    logText.setText(logStr);
//                    String resultStr = String.format(Locale.ENGLISH,
//                            "class : %s, prob : %.2f%%",
//                            output.first, output.second * 100);
//                    textView.setText(resultStr);
//                });
//            }
//            image.close();
//            isProcessingFrame = false;
//        });

        runInBackground(() -> {
            if (ff != null && ff.isInitialized()) {

                final float displayRatio = ((float)Math.min(displayHeight, displayHeight)) / 224.0F;

                final long startTime = SystemClock.uptimeMillis();
                final float[] pred_bb = ff.classify(rgbFrameBitmap, sensorOrientation);



                float rr = (float)displayWidth /  ( 224.0F - (ff.left * 2));
                float prev_ratio = (float)previewHeight / (float)previewWidth;

                Log.d("prevratio", " " + ff.left);

                final int[] ori_bb = {(int)((pred_bb[0]-(float)ff.left)  *rr), (int)((pred_bb[1]-(float)ff.top)*rr),
                                        (int)((pred_bb[2]-(float)ff.left)*rr), (int)((pred_bb[3]-(float)ff.left)*rr)};

                Log.d("good", "nice!");
                Log.d("good", " size is " + getsize.getWidth());
                Log.d("good", String.valueOf( " " + ori_bb[0] + ",  " +  ori_bb[1]));
                Log.d("good", String.valueOf( " " + ori_bb[2] + ",  " +  ori_bb[3]));

                final float[] center = {(ori_bb[0]+ori_bb[2])/2, (ori_bb[1]+ori_bb[3])/2};
                final int face_size = Math.max(Math.abs(ori_bb[2] - ori_bb[0]), Math.abs(ori_bb[3] - ori_bb[1]));
                final int[] new_bb = {(int)(center[0]-(float)(face_size)*0.6F), (int)(center[1]-(float)(face_size)*0.6F),
                                        (int)(center[0]+(float)(face_size)*0.6F), (int)(center[1]+(float)(face_size)*0.6F)};
                for(int i=0; i<4; i++){
                    if(new_bb[i] > 99999)   new_bb[i] = 99999;
                    if(new_bb[i] < 0)   new_bb[i] = 0;
                }






//                face_img = cropBitmap(rgbFrameBitmap, new_bb[0], new_bb[1], new_bb[2], new_bb[3]);

//                final float[] pred_lmks = lm.classify(face_img, sensorOrientation);
//                final int[][] ori_lmks = new int[9][2];
//                int k = 0;
//                for(int i=0; i<9; i++){
//                    ori_lmks[i][0] = (int)((pred_lmks[k++]-lm.left)/lm.ratio) + new_bb[0];
//                    ori_lmks[i][1] = (int)((pred_lmks[k++]-lm.top)/lm.ratio) + new_bb[1];
//                }

                final long elapsedTime = SystemClock.uptimeMillis() - startTime;



//                for (int i = 0; i < output.length; i++) {
//                    System.out.println(output[i]);
//                }

                runOnUiThread(() -> {
//                    String logStr = elapsedTime + " ms";
//                    logText.setText(logStr);

                            Bitmap bitmap = Bitmap.createBitmap(800,800, Bitmap.Config.ARGB_8888);
                            Canvas canvas = new Canvas(bitmap);
                            canvas.drawColor(Color.WHITE);

                            ImageView filter = findViewById(R.id.filter);
                            filter.setImageBitmap(bitmap);

                            Paint paint = new Paint();

                            paint.setColor(Color.RED);
                            paint.setStrokeWidth(30f);
//                            canvas.drawPoint((displayWidth - ori_bb[0]), (getsize.getHeight() - ori_bb[1]), paint);
//                            canvas.drawPoint((displayWidth - ori_bb[2]), (getsize.getHeight() - ori_bb[3]), paint);





//                    Log.d("test", (String) output.get(0));
//                    String resultStr = String.format(Locale.ENGLISH,
//                            "class : %f, prob : %f",
//                            output.get(0), output.get(1));
//                    textView.setText(resultStr);
                }

                );


            }
            image.close();
            isProcessingFrame = false;
        });

    }





    static public Bitmap cropBitmap(Bitmap original, int x1, int y1, int x2, int y2) {
        Bitmap result = Bitmap.createBitmap(original, x1, y1, Math.abs(x2-x1), Math.abs(y2-y1));
        if (result != original) {
            original.recycle();
        }
        return result;
    }


    public int[] find_left_top(Image image){
        float old_size[] = {image.getWidth(), image.getHeight()};
        float ratio = (float) (224.0 / ((image.getWidth() > image.getHeight()) ? image.getWidth() : image.getHeight()));
        float new_size[] = {old_size[0]*ratio, old_size[1]*ratio};
        int delta_w = 224 - (int)new_size[0];
        int delta_h = 224 - (int)new_size[1];
        int top = delta_h / 2;
        int left = delta_w / 2;
        int result[] = {left, top};
        return result;
    }


    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }
}