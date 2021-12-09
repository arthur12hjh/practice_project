package com.example.imageclassifier;

import static java.lang.Math.abs;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.app.Fragment;

import com.example.imageclassifier.camera.CameraFragment;
import com.example.imageclassifier.tflite.FindFace;
import com.example.imageclassifier.tflite.Landmarks;
import com.example.imageclassifier.utils.YuvToRgbConverter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private static final String WRITE_EXTERNAL_STORAGE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    private static final int PERMISSION_REQUEST_CODE = 1;

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

    private int filterNum = 0;
    final int GET_GALLERY_IMAGE = 200;
    private ImageView filter;

    Bitmap rgbFrameBitmap2;

    ImageButton filterbt;
    ImageButton Shutter;
    ImageButton album;
    ImageButton bitSunglass;
    ImageButton Sunglass1;
    ImageButton rudolph;
    ImageButton backFilter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        filter = findViewById(R.id.filter);

        getsize = findViewById(R.id.getsize);



        Display display = getWindowManager().getDefaultDisplay();  // in Activity
        Point size = new Point();
        display.getSize(size); // or getSize(size)
        displayWidth = size.x;
        displayHeight = size.y;

        try {
            ff = new FindFace(this);
            ff.init();
            lm = new Landmarks(this);
            lm.init();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        if(checkSelfPermission(CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED
        && checkSelfPermission(WRITE_EXTERNAL_STORAGE_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            setFragment();
        } else {
            requestPermissions(new String[]{CAMERA_PERMISSION, WRITE_EXTERNAL_STORAGE_PERMISSION}, PERMISSION_REQUEST_CODE);
        }

        filterbt = (ImageButton) findViewById(R.id.filterbt);
        Shutter = (ImageButton) findViewById(R.id.Shutter);
        album = (ImageButton) findViewById(R.id.testbitmap);
        bitSunglass = (ImageButton) findViewById(R.id.bitSunglass);
        Sunglass1 = (ImageButton) findViewById(R.id.Sunglass1);
        rudolph = (ImageButton) findViewById(R.id.rudolph);
        backFilter = (ImageButton) findViewById(R.id.circleX);

        album.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(intent,GET_GALLERY_IMAGE);
            }
        });
//


        filterbt.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // 1. 촬영, 앨범, 필터 버튼 지우기
                filterbt.setVisibility(View.GONE);
                Shutter.setVisibility(View.GONE);
                album.setVisibility(View.GONE);

                // 2. 필터 탭 열기
                bitSunglass.setVisibility(View.VISIBLE);
                Sunglass1.setVisibility(View.VISIBLE);
                backFilter.setVisibility(View.VISIBLE);
                rudolph.setVisibility(View.VISIBLE);

            }
        });

        backFilter.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // 1. 필터 종류 선택하여 전역변수로 넘기기
                filterNum = 0;

                // 2. 필터 탭 종료 후 촬영, 앨범, 필터 버튼 재생성
                bitSunglass.setVisibility(View.GONE);
                Sunglass1.setVisibility(View.GONE);
                backFilter.setVisibility(View.GONE);
                rudolph.setVisibility(View.GONE);
                filterbt.setVisibility(View.VISIBLE);
                Shutter.setVisibility(View.VISIBLE);
                album.setVisibility(View.VISIBLE);

            }
        });

        bitSunglass.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // 1. 필터 종류 선택하여 전역변수로 넘기기
                filterNum = 1;

                // 2. 필터 탭 종료 후 촬영, 앨범, 필터 버튼 재생성
                bitSunglass.setVisibility(View.GONE);
                Sunglass1.setVisibility(View.GONE);
                backFilter.setVisibility(View.GONE);
                rudolph.setVisibility(View.GONE);
                filterbt.setVisibility(View.VISIBLE);
                Shutter.setVisibility(View.VISIBLE);
                album.setVisibility(View.VISIBLE);

            }
        });

        Sunglass1.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // 1. 필터 종류 선택하여 전역변수로 넘기기
                filterNum = 2;

                // 2. 필터 탭 종료 후 촬영, 앨범, 필터 버튼 재생성
                bitSunglass.setVisibility(View.GONE);
                Sunglass1.setVisibility(View.GONE);
                backFilter.setVisibility(View.GONE);
                rudolph.setVisibility(View.GONE);
                filterbt.setVisibility(View.VISIBLE);
                Shutter.setVisibility(View.VISIBLE);
                album.setVisibility(View.VISIBLE);

            }
        });

        rudolph.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // 1. 필터 종류 선택하여 전역변수로 넘기기
                filterNum = 3;

                // 2. 필터 탭 종료 후 촬영, 앨범, 필터 버튼 재생성
                bitSunglass.setVisibility(View.GONE);
                Sunglass1.setVisibility(View.GONE);
                backFilter.setVisibility(View.GONE);
                rudolph.setVisibility(View.GONE);
                filterbt.setVisibility(View.VISIBLE);
                Shutter.setVisibility(View.VISIBLE);
                album.setVisibility(View.VISIBLE);

            }
        });

        Shutter.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                Matrix rotateMatrix3 = new Matrix();
                rotateMatrix3.postRotate(90); //-360~360
                rgbFrameBitmap2 = Bitmap.createBitmap(rgbFrameBitmap, 0, 0,
                        rgbFrameBitmap.getWidth(), rgbFrameBitmap.getHeight(), rotateMatrix3, false);

                BitmapDrawable drawable = (BitmapDrawable) filter.getDrawable();
                Bitmap bitmap = drawable.getBitmap();

                int resizeWidth = getsize.getWidth();

                double aspectRatio = (double) rgbFrameBitmap2.getHeight() / (double) rgbFrameBitmap2.getWidth();
                int targetHeight = (int) (resizeWidth * aspectRatio);
                Bitmap result = Bitmap.createScaledBitmap(rgbFrameBitmap2, resizeWidth, targetHeight, false);

                Bitmap resultOverlayBmp = Bitmap.createBitmap(result.getWidth()
                        , result.getHeight()
                        , result.getConfig());

                Paint alphaPaint = new Paint();
                alphaPaint.setAlpha(255);


                //캔버스를 통해 비트맵을 겹치기한다.
                Canvas canvas = new Canvas(resultOverlayBmp);
                canvas.drawBitmap(result, new Matrix(), null);
                canvas.drawBitmap(bitmap, new Matrix(), alphaPaint);

                saveFile(resultOverlayBmp);
                Toast.makeText(getApplicationContext(), "image saved", Toast.LENGTH_SHORT).show();
            }
        });


    }

    private void saveFile(Bitmap bmp) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "image_1024.JPG");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/*");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        ContentResolver contentResolver = getContentResolver();
        Uri item = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try {
            ParcelFileDescriptor pdf = contentResolver.openFileDescriptor(item, "w", null);

            if (pdf == null) {
                Log.d("asdf", "null");
            } else {
                InputStream inputStream = getImageInputStram(bmp);
                byte[] strToByte = getBytes(inputStream);
                FileOutputStream fos = new FileOutputStream(pdf.getFileDescriptor());
                fos.write(strToByte);
                fos.close();
                inputStream.close();
                pdf.close();
                contentResolver.update(item, values, null, null);


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear();
                    values.put(MediaStore.Images.Media.IS_PENDING, 0);
                    contentResolver.update(item, values, null, null);
                }

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private InputStream getImageInputStram( Bitmap bmp) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, bytes);
        byte[] bitmapData = bytes.toByteArray();
        ByteArrayInputStream bs = new ByteArrayInputStream(bitmapData);
        return bs;
    }

    public byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
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

    @RequiresApi(api = Build.VERSION_CODES.R)
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

    @RequiresApi(api = Build.VERSION_CODES.R)
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


        if (image == null) {
            isProcessingFrame = false;
            return;
        }


        YuvToRgbConverter.yuvToRgb(this, image, rgbFrameBitmap);

        runInBackground(() -> {
            if (ff != null && ff.isInitialized()) {

                final float displayRatio = ((float)Math.min(displayHeight, displayHeight)) / 224.0F;

                final long startTime = SystemClock.uptimeMillis();

                Matrix rotateMatrix3 = new Matrix();
                rotateMatrix3.postRotate(180); //-360~360
                Bitmap rgb2 = Bitmap.createBitmap(rgbFrameBitmap, 0, 0,
                        rgbFrameBitmap.getWidth(), rgbFrameBitmap.getHeight(), rotateMatrix3, false);



                final float[] pred_bb = ff.classify(rgb2, sensorOrientation);

                float rr = (float)displayWidth /  (224.0F-(ff.top*2));


                final int[] ori_bb = {(int)((pred_bb[0]-ff.top)*rr), (int)((pred_bb[1]-ff.left)*rr),
                        (int)((pred_bb[2]-ff.top)*rr), (int)((pred_bb[3]-ff.left)*rr)};


                final float[] center = {(ori_bb[0]+ori_bb[2])/2, (ori_bb[1]+ori_bb[3])/2};
                final int face_size = Math.max(Math.abs(ori_bb[2] - ori_bb[0]), Math.abs(ori_bb[3] - ori_bb[1]));
                final int[] new_bb = {(int)(center[0]-(float)(face_size)*0.6F), (int)(center[1]-(float)(face_size)*0.6F),
                        (int)(center[0]+(float)(face_size)*0.6F), (int)(center[1]+(float)(face_size)*0.6F)};
                for(int i=0; i<4; i++){
                    if(new_bb[i] > 99999)   new_bb[i] = 99999;
                    if(new_bb[i] < 0)   new_bb[i] = 0;
                }

                Matrix rotateMatrix = new Matrix();
                rotateMatrix.postRotate(90); //-360~360
                Bitmap sideInversionImg = Bitmap.createBitmap(rgbFrameBitmap, 0, 0,
                        rgbFrameBitmap.getWidth(), rgbFrameBitmap.getHeight(), rotateMatrix, false);


                float rate = (float)getsize.getWidth() / (float)sideInversionImg.getWidth();

                if(((int)((float)new_bb[2]/rate) <= sideInversionImg.getWidth()) && (int)((float)new_bb[3]/rate) <= sideInversionImg.getHeight())
                    face_img = cropBitmap(sideInversionImg, (int)((float)new_bb[0]/rate), (int)((float)new_bb[1]/rate),
                            (int)((float)new_bb[2]/rate), (int)((float)new_bb[3]/rate));

                Matrix rotateMatrix2 = new Matrix();
                rotateMatrix2.postRotate(90); //-360~360
                face_img2 = Bitmap.createBitmap(face_img, 0, 0,
                        face_img.getWidth(), face_img.getHeight(), rotateMatrix2, false);


                final float[] pred_lmks = lm.classify(face_img2, sensorOrientation);

                final int[][] ori_lmks = new int[9][2];
                int k = 0;



                for(int i=0; i<9; i++){
                    ori_lmks[i][0] = (int)(((pred_lmks[k++]-lm.top)/(lm.ratio)) * rate) + new_bb[0];
                    ori_lmks[i][1] = (int)(((pred_lmks[k++]-lm.left)/(lm.ratio)) * rate) + new_bb[1];
                }

                runOnUiThread(() -> {

                            Bitmap bitmap = Bitmap.createBitmap(getsize.getWidth(),getsize.getHeight(), Bitmap.Config.ARGB_8888);
                            Canvas canvas = new Canvas(bitmap);
                            canvas.drawColor(Color.TRANSPARENT);

                            filter.setImageBitmap(bitmap);

                            Paint paint = new Paint();

                            paint.setColor(Color.RED);
                            paint.setStrokeWidth(7f);


                            Bitmap picture;
                            Matrix matrix;

                            int xd, yd;
                            yd = (int) Math.pow((ori_lmks[1][1]-ori_lmks[0][1]),2);
                            xd = (int) Math.pow((ori_lmks[1][0]-ori_lmks[0][0]),2);
                            double disWidth = Math.sqrt((double)yd+(double)xd);
                            double height = disWidth * 1.5;
                            double width = disWidth*3;

                            int startx, starty;

                            double radian = Math.atan2(((float)ori_lmks[1][1] - ori_lmks[0][1]),((float)ori_lmks[1][0] - ori_lmks[0][0]));
                            double degree = radian*180 / Math.PI;
                            double one = height / 2;
                            double two = (width - disWidth) / 2;

                            Bitmap rotatedpicture;

                            switch (filterNum) {
                                case 1:
                                    picture = BitmapFactory.decodeResource(getResources(),R.drawable.pngegg);
                                    matrix = new Matrix();

                                    matrix.postRotate((float) degree);

                                    picture = Bitmap.createScaledBitmap(picture,(int)width,(int)height,true);

                                    if(degree>=0) {
                                        startx = (int) ((double)ori_lmks[0][0] - one * Math.sin(radian) - two * Math.cos(radian));
                                        starty = (int) ((double)ori_lmks[0][1] - one * Math.cos(radian) - two * Math.sin(radian));
                                    } else {
                                        startx = (int) ((float)ori_lmks[0][0] + (height / 2) * Math.sin(radian) - ((width - disWidth) / 2) * Math.cos(radian));
                                        starty = (int) ((float)ori_lmks[0][1] - (height / 2) * Math.cos(radian) + ((width - disWidth)) * Math.sin(radian));
                                    }
                                    rotatedpicture = Bitmap.createBitmap(picture, 0, 0, picture.getWidth(), picture.getHeight(), matrix, true);
                                    canvas.drawBitmap(rotatedpicture,startx,starty,null);

                                    picture.recycle();
                                    rotatedpicture.recycle();
                                    break;
                                case 2:
                                    picture = BitmapFactory.decodeResource(getResources(),R.drawable.sunglass);
                                    matrix = new Matrix();

                                    matrix.postRotate((float) degree);

                                    picture = Bitmap.createScaledBitmap(picture,(int)width,(int)height,true);

                                    if(degree>=0) {
                                        startx = (int) ((double)ori_lmks[0][0] - one * Math.sin(radian) - two * Math.cos(radian));
                                        starty = (int) ((double)ori_lmks[0][1] - one * Math.cos(radian) - two * Math.sin(radian));
                                    } else {
                                        startx = (int) ((float)ori_lmks[0][0] + (height / 2) * Math.sin(radian) - ((width - disWidth) / 2) * Math.cos(radian));
                                        starty = (int) ((float)ori_lmks[0][1] - (height / 2) * Math.cos(radian) + ((width - disWidth)) * Math.sin(radian));
                                    }
                                    rotatedpicture = Bitmap.createBitmap(picture, 0, 0, picture.getWidth(), picture.getHeight(), matrix, true);
                                    canvas.drawBitmap(rotatedpicture,startx,starty,null);

                                    picture.recycle();
                                    rotatedpicture.recycle();
                                    break;
                                case 3:
                                    picture = BitmapFactory.decodeResource(getResources(),R.drawable.rudolph_nose);

                                    disWidth = Math.abs(ori_lmks[1][0]-ori_lmks[0][0])*2;

                                    picture = Bitmap.createScaledBitmap(picture,(int)disWidth/2,(int)disWidth/2,true);

                                    canvas.drawBitmap(picture,ori_lmks[2][0] - (int)disWidth/4,ori_lmks[2][1] - (int)disWidth/4,null);


                                    picture.recycle();
                                    break;
                            }
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



    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }


}