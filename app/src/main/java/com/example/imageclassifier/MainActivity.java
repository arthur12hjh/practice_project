package com.example.imageclassifier;

import static java.lang.Math.abs;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.app.Fragment;

import com.example.imageclassifier.camera.CameraFragment;
import com.example.imageclassifier.tflite.FindFace;
import com.example.imageclassifier.tflite.Landmarks;
import com.example.imageclassifier.utils.YuvToRgbConverter;

import java.io.IOException;

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
    private Bitmap face_img2 = null;

    private HandlerThread handlerThread;
    private Handler handler;

    private boolean isProcessingFrame = false;

    int displayWidth;
    int displayHeight;

    private FrameLayout getsize;
    private ImageView testbitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        textView = findViewById(R.id.textView);

        getsize = findViewById(R.id.getsize);
        testbitmap = findViewById(R.id.testbitmap);

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

                Matrix rotateMatrix3 = new Matrix();
                rotateMatrix3.postRotate(180); //-360~360
                Bitmap rgb2 = Bitmap.createBitmap(rgbFrameBitmap, 0, 0,
                        rgbFrameBitmap.getWidth(), rgbFrameBitmap.getHeight(), rotateMatrix3, false);



                final float[] pred_bb = ff.classify(rgb2, sensorOrientation);

                float[] reverse_bb = {224.0F-pred_bb[0], 224.0F-pred_bb[1], 224.0F-pred_bb[2], 224.0F-pred_bb[3]};



                float rr = (float)displayWidth /  (224.0F-(ff.top*2));
                float prev_ratio = (float)previewHeight / (float)previewWidth;

                Log.d("prevratio", " " + ff.left + "," + ff.top);

                final int[] ori_bb = {(int)((pred_bb[0]-ff.top)*rr), (int)((pred_bb[1]-ff.left)*rr),
                        (int)((pred_bb[2]-ff.top)*rr), (int)((pred_bb[3]-ff.left)*rr)};


//                final int[] ori_bb = {(int)((pred_bb[0]-(float)ff.left)  *rr), (int)((pred_bb[1]-(float)ff.top)*rr),
//                                        (int)((pred_bb[2]-(float)ff.left)*rr), (int)((pred_bb[3]-(float)ff.left)*rr)};

                Log.d("good0", "nice!");
                Log.d("good0", " size is " + getsize.getWidth());
                Log.d("good0", String.valueOf( " " + pred_bb[0] + ",  " +  pred_bb[1]));
                Log.d("good0", String.valueOf( " " + pred_bb[2] + ",  " +  pred_bb[3]));

                Log.d("good1", "nice!");
                Log.d("good1", " size is " + getsize.getWidth());
                Log.d("good1", String.valueOf( " " + ori_bb[0] + ",  " +  ori_bb[1]));
                Log.d("good1", String.valueOf( " " + ori_bb[2] + ",  " +  ori_bb[3]));

                Log.d("good2", "nice!");
                Log.d("good2", " size is " + getsize.getWidth());
                Log.d("good2", String.valueOf( " " + (displayWidth - ori_bb[0]) + ",  " +  (getsize.getHeight() - ori_bb[1])));
                Log.d("good2", String.valueOf( " " + (displayWidth - ori_bb[2]) + ",  " +  (getsize.getHeight() - ori_bb[3])));


                final float[] center = {(ori_bb[0]+ori_bb[2])/2, (ori_bb[1]+ori_bb[3])/2};
                final int face_size = Math.max(Math.abs(ori_bb[2] - ori_bb[0]), Math.abs(ori_bb[3] - ori_bb[1]));
                final int[] new_bb = {(int)(center[0]-(float)(face_size)*0.6F), (int)(center[1]-(float)(face_size)*0.6F),
                                        (int)(center[0]+(float)(face_size)*0.6F), (int)(center[1]+(float)(face_size)*0.6F)};
                for(int i=0; i<4; i++){
                    if(new_bb[i] > 99999)   new_bb[i] = 99999;
                    if(new_bb[i] < 0)   new_bb[i] = 0;
                }

                Log.d("good3", "bigface");
                Log.d("good3", String.valueOf( " " + new_bb[0] + ",  " +  new_bb[1]));
                Log.d("good3", String.valueOf( " " + new_bb[2] + ",  " +  new_bb[3]));



                Matrix rotateMatrix = new Matrix();
                rotateMatrix.postRotate(90); //-360~360
                Bitmap sideInversionImg = Bitmap.createBitmap(rgbFrameBitmap, 0, 0,
                        rgbFrameBitmap.getWidth(), rgbFrameBitmap.getHeight(), rotateMatrix, false);

//                Bitmap side_2 = ff.add_border_img(sideInversionImg);
                Log.d("top", ""+ff.top+", left is" + ff.left);


                float rate = (float)getsize.getWidth() / (float)sideInversionImg.getWidth();

                Log.d("kk", ""+sideInversionImg.getWidth());

                if(((int)((float)new_bb[2]/rate) <= sideInversionImg.getWidth()) && (int)((float)new_bb[3]/rate) <= sideInversionImg.getHeight())
                    face_img = cropBitmap(sideInversionImg, (int)((float)new_bb[0]/rate), (int)((float)new_bb[1]/rate),
                            (int)((float)new_bb[2]/rate), (int)((float)new_bb[3]/rate));

//                float rate = (float)sideInversionImg.getWidth() / (float) getsize.getWidth();
//                Log.d("good3", ""+ sideInversionImg.getWidth());
//
//
//                if((int) (new_bb[2]*rate) <= sideInversionImg.getWidth())
//                    face_img = cropBitmap(sideInversionImg, (int) (new_bb[0]*rate), (int) (new_bb[1]*rate), (int) (new_bb[2]*rate), (int) (new_bb[3]*rate));
////
                Matrix rotateMatrix2 = new Matrix();
                rotateMatrix2.postRotate(90); //-360~360
                face_img2 = Bitmap.createBitmap(face_img, 0, 0,
                        face_img.getWidth(), face_img.getHeight(), rotateMatrix2, false);


                final float[] pred_lmks = lm.classify(face_img2, sensorOrientation);
//                float[] reverse_lmks = new float[18];
//                for(int i=0; i<18; i++){
//                    reverse_lmks[i] = 224.0F - pred_lmks[i];
//                }


                final int[][] ori_lmks = new int[9][2];
                int k = 0;



                for(int i=0; i<9; i++){
                    ori_lmks[i][0] = (int)(((pred_lmks[k++]-lm.top)/(lm.ratio)) * rate) + new_bb[0];
                    ori_lmks[i][1] = (int)(((pred_lmks[k++]-lm.left)/(lm.ratio)) * rate) + new_bb[1];
                }
////
//                Log.d("good4", "bigface");
//                Log.d("good7", String.valueOf( "face " + face_img.getWidth() + ",  " +  face_img.getHeight()));
//                Log.d("good7", String.valueOf( "lm.ratio " + lm.ratio ));
//                Log.d("good4", String.valueOf( "new_bb[0] " + new_bb[0] + ",  " +  new_bb[1]));
//                Log.d("good4", String.valueOf( " " + pred_lmks[0] + ",  " +  pred_lmks[1]));
//                Log.d("good4", String.valueOf( " " + pred_lmks[2] + ",  " +  pred_lmks[3]));
//                Log.d("good4", String.valueOf( " " + pred_lmks[4] + ",  " +  pred_lmks[5]));
//                Log.d("good4", String.valueOf( " " + pred_lmks[6] + ",  " +  pred_lmks[7]));
//                Log.d("good4", String.valueOf( " " + pred_lmks[8] + ",  " +  pred_lmks[9]));
//                Log.d("good4", String.valueOf( " " + pred_lmks[10] + ",  " +  pred_lmks[11]));
//                Log.d("good4", String.valueOf( " " + pred_lmks[12] + ",  " +  pred_lmks[13]));
//                Log.d("good4", String.valueOf( " " + pred_lmks[14] + ",  " +  pred_lmks[15]));
//                Log.d("good4", String.valueOf( " " + pred_lmks[16] + ",  " +  pred_lmks[17]));
//
//

                final long elapsedTime = SystemClock.uptimeMillis() - startTime;



//                for (int i = 0; i < output.length; i++) {
//                    System.out.println(output[i]);
//                }

                runOnUiThread(() -> {
//                    String logStr = elapsedTime + " ms";
//                    logText.setText(logStr);

                            Bitmap bitmap = Bitmap.createBitmap(getsize.getWidth(),getsize.getHeight(), Bitmap.Config.ARGB_8888);
                            Canvas canvas = new Canvas(bitmap);
                            canvas.drawColor(Color.TRANSPARENT);

                            ImageView filter = findViewById(R.id.filter);
                            filter.setImageBitmap(bitmap);

                            Paint paint = new Paint();

                            paint.setColor(Color.RED);
                            paint.setStrokeWidth(7f);

//                            Bitmap test = ff.add_border_img(rgbFrameBitmap);






                            Bitmap ttt = ff.add_border_img(face_img);

                            testbitmap.setImageBitmap(ttt);


//                            canvas.drawPoint((displayWidth - ori_bb[0]), (getsize.getHeight() - ori_bb[1]), paint);
//                            canvas.drawPoint((displayWidth - ori_bb[2]), (getsize.getHeight() - ori_bb[3]), paint);
//                            canvas.drawPoint(ori_bb[0], ori_bb[1], paint);
//                            canvas.drawPoint(ori_bb[2], ori_bb[3], paint);

//                            canvas.drawPoint(reverse_lmks[0], reverse_lmks[1], paint);
//                            canvas.drawPoint(reverse_lmks[2], reverse_lmks[3], paint);
//////
////                            paint.setStrokeWidth(30f);
////
//                            canvas.drawPoint(new_bb[0], new_bb[1], paint);
//                            canvas.drawPoint(new_bb[2], new_bb[3], paint);


                            paint.setStrokeWidth(5f);
                            canvas.drawPoint(ori_lmks[0][0], ori_lmks[0][1], paint);
                            canvas.drawPoint(ori_lmks[1][0], ori_lmks[1][1], paint);
                            canvas.drawPoint(ori_lmks[2][0], ori_lmks[2][1], paint);
                            canvas.drawPoint(ori_lmks[3][0], ori_lmks[3][1], paint);
                            canvas.drawPoint(ori_lmks[4][0], ori_lmks[4][1], paint);
                            canvas.drawPoint(ori_lmks[5][0], ori_lmks[5][1], paint);
                            canvas.drawPoint(ori_lmks[6][0], ori_lmks[6][1], paint);
                            canvas.drawPoint(ori_lmks[7][0], ori_lmks[7][1], paint);
                            canvas.drawPoint(ori_lmks[8][0], ori_lmks[8][1], paint);

//                            Log.d("good4", String.valueOf( "ori_lmks[0][0] " + ori_lmks[0][0] + ",  " +  ori_lmks[0][1]));
//                            Log.d("good4", String.valueOf( "new_bb[0] " + new_bb[0] + ",  " +  new_bb[1]));
//                            paint.setStrokeWidth(10f);
//                            canvas.drawPoint(ori_lmks[0][0], ori_lmks[0][1], paint);
//                            canvas.drawPoint(ori_lmks[1][0], ori_lmks[1][1], paint);


//                            canvas.drawPoint(new_bb[0] + ori_lmks[0][0], new_bb[1] + ori_lmks[0][1], paint);
//                            canvas.drawPoint(new_bb[2] + ori_lmks[1][0], new_bb[3] + ori_lmks[1][1], paint);





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