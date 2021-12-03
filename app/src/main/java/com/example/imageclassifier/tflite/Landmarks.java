package com.example.imageclassifier.tflite;

import static org.tensorflow.lite.support.image.ops.ResizeOp.ResizeMethod.NEAREST_NEIGHBOR;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.util.Size;

import androidx.annotation.NonNull;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.support.model.Model;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class Landmarks {
    private static final String MODEL_NAME = "lmks.tflite";

    Context context;
    Model model;
    int modelInputWidth, modelInputHeight, modelInputChannel;
    TensorImage inputImage;
    TensorBuffer outputBuffer;

    public int left = 0;
    public int top = 0;
    public float ratio = 0.F;

    private boolean isInitialized = false;

    Interpreter interpreter = null;
    int modelOutputClasses;

    public void init() throws IOException {
        interpreter = getInterpreter();
        initModelShape();

        isInitialized = true;
    }

    private Interpreter getInterpreter() throws IOException {
        ByteBuffer model = loadModelFile(MODEL_NAME);
        model.order(ByteOrder.nativeOrder());
        return new Interpreter(model);
    }

    private ByteBuffer loadModelFile(String modelName) throws IOException {
        //org.tensorflow.lite.support.common.FileUtil에 구현되어있음
        //        org.tensorflow.lite.support.common.FileUtil.loadMappedFile(context, modelName);
        AssetManager am = context.getAssets();
        AssetFileDescriptor afd = am.openFd(modelName);
        FileInputStream fis = new FileInputStream(afd.getFileDescriptor());
        FileChannel fc = fis.getChannel();
        long startOffset = afd.getStartOffset();
        long declaredLength = afd.getDeclaredLength();

        return fc.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void initModelShape() {
        Tensor inputTensor = interpreter.getInputTensor(0);
        int[] inputShape = inputTensor.shape();
        modelInputChannel = inputShape[0];
        modelInputWidth = inputShape[1];
        modelInputHeight = inputShape[2];

        inputImage = new TensorImage(inputTensor.dataType());

        Tensor outputTensor = interpreter.getOutputTensor(0);
        int[] outputShape = outputTensor.shape();
        modelOutputClasses = outputShape[1];
    }

    private Bitmap resizeBitmap(Bitmap bitmap) {
        return Bitmap.createScaledBitmap(bitmap, modelInputWidth, modelInputHeight, false);
    }

    private ByteBuffer convertBitmapToGrayByteBuffer(Bitmap bitmap) {
        ByteBuffer byteByffer = ByteBuffer.allocateDirect(bitmap.getByteCount());
        byteByffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int pixel : pixels) {
            int r = pixel >> 16 & 0xFF;
            int g = pixel >> 8 & 0xFF;
            int b = pixel & 0xFF;

            float avgPixelValue = (r + g + b) / 3.0f;
            float normalizedPixelValue = avgPixelValue / 255.0f;

            byteByffer.putFloat(normalizedPixelValue);
        }

        return byteByffer;
    }






    public Landmarks(@NonNull Context context) {
        this.context = context;
    }

//    public void init() throws IOException {
//        model = createNNAPIModel();
//        initModelShape();
//
//
//        isInitialized = true;
//    }

    private Model createModel() throws IOException {
        return Model.createModel(context, MODEL_NAME);
    }

    private Model createMultiThreadModel(int nThreads) throws IOException {
        Model.Options.Builder optionsBuilder = new Model.Options.Builder();
        optionsBuilder.setNumThreads(nThreads);
        return Model.createModel(context, MODEL_NAME, optionsBuilder.build());
    }

    private Model createGPUModel() throws IOException {
        Model.Options.Builder optionsBuilder = new Model.Options.Builder();
        CompatibilityList compatList = new CompatibilityList();

        // if the device has a supported GPU, add the GPU delegate
        if(compatList.isDelegateSupportedOnThisDevice()) {
            optionsBuilder.setDevice(Model.Device.GPU);
        }
        return Model.createModel(context, MODEL_NAME, optionsBuilder.build());
    }

    private Model createNNAPIModel() throws IOException {
        Model.Options.Builder optionsBuilder = new Model.Options.Builder();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            optionsBuilder.setDevice(Model.Device.NNAPI);
        }

        return Model.createModel(context, MODEL_NAME, optionsBuilder.build());
    }










    public boolean isInitialized() {
        return isInitialized;
    }


//    private void initModelShape() {
//        Tensor inputTensor = model.getInputTensor(0);
//        int[] shape = inputTensor.shape();
//        modelInputChannel = shape[0];
//        modelInputWidth = shape[1];
//        modelInputHeight = shape[2];
//
//        inputImage = new TensorImage(inputTensor.dataType());
//
//        Tensor outputTensor = model.getOutputTensor(0);
//        outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(), outputTensor.dataType());
//    }



    public Size getModelInputSize() {
        if(!isInitialized)
            return new Size(0, 0);
        return new Size(modelInputWidth, modelInputHeight);
    }

    private Bitmap convertBitmapToARGB8888(Bitmap bitmap) {
        return bitmap.copy(Bitmap.Config.ARGB_8888,true);
    }

    private TensorImage loadImage(Bitmap bitmap, int sensorOrientation) {
        bitmap = add_border_img(bitmap);
        if(bitmap.getConfig() != Bitmap.Config.ARGB_8888) {
            inputImage.load(convertBitmapToARGB8888(bitmap));
        } else {
            inputImage.load(bitmap);
        }

        int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
        int numRotation = sensorOrientation / 90;

        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
                .add(new ResizeOp(modelInputWidth, modelInputHeight, NEAREST_NEIGHBOR))
                .add(new Rot90Op(numRotation))
                .add(new NormalizeOp(0.0f, 255.0f))
                .build();

        return imageProcessor.process(inputImage);
    }

    public Bitmap add_border_img(Bitmap im){
        float[] old_size = {im.getWidth(), im.getHeight()};
        ratio = 224.0F / (Math.max(old_size[0], old_size[1]));
        float new_size[] = {old_size[0]*ratio, old_size[1] * ratio };
        left = (int) ((224 - (int)new_size[0])/2);
        top = (int) ((224 - (int)new_size[1])/2);
        im = Bitmap.createScaledBitmap(im, (int)new_size[0], (int)new_size[1], false);
        Bitmap imWithBorder = Bitmap.createBitmap(im.getWidth() + left * 2, im.getHeight() + top * 2, im.getConfig());
        Canvas canvas = new Canvas(imWithBorder);
        canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(im, left, top, null);

        return imWithBorder;
    }

//    public Pair<String, Float> classify(Bitmap image, int sensorOrientation) {
//        inputImage = loadImage(image, sensorOrientation);
//
//        Object[] inputs = new Object[]{inputImage.getBuffer()};
//        Map<Integer, Object> outputs = new HashMap();
//        outputs.put(0, outputBuffer.getBuffer().rewind());
//
//        model.run(inputs, outputs);
//
//        Map<String, Float> output =
//                new TensorLabel(labels, outputBuffer).getMapWithFloatValue();
//
//        return argmax(output);
//    }

//    public Pair<String, Float> classify(Bitmap image) {
//        return classify(image, 0);
//    }

//    public Map<Integer, Object> classify(Bitmap image, int sensorOrientation) {
//        inputImage = loadImage(image, sensorOrientation);
//
//        Object[] inputs = new Object[]{inputImage.getBuffer()};
//        Map<Integer, Object> output_1 = new HashMap();
//        Log.d("test1", outputBuffer.getBuffer().rewind().getClass().getSimpleName());
//        output_1.put(0, outputBuffer.getBuffer().rewind());
//
//        model.run(inputs, output_1);
//
//        return output_1;
//    }

    public float[] classify(Bitmap image, int sensorOrientation) {
//        ByteBuffer buffer = convertBitmapToGrayByteBuffer(resizeBitmap(image));
        inputImage = loadImage(image, sensorOrientation);

        float[][] result = new float[1][modelOutputClasses];

        interpreter.run(inputImage.getBuffer(), result);

        return result[0];
    }




//        Map<Integer, Object> outputs = new HashMap();
//        outputs.put(0, outputBuffer.getBuffer().rewind());
//
//        model.run(inputs, outputs);
//
//        Map<String, Float> output =
//                new TensorLabel(labels, outputBuffer).getMapWithFloatValue();
//
//        return argmax(output);
//    }
//
//    public Pair<String, Float> classify(Bitmap image) {
//        return classify(image, 0);
//    }

//    private Pair<String, Float> argmax(Map<String, Float> map) {
//        String maxKey = "";
//        float maxVal = -1;
//
//        for(Map.Entry<String, Float> entry : map.entrySet()) {
//            float f = entry.getValue();
//            if(f > maxVal) {
//                maxKey = entry.getKey();
//                maxVal = f;
//            }
//        }
//
//        return new Pair<>(maxKey, maxVal);
//    }

    public void finish() {
        if(model != null) {
            model.close();
            isInitialized = false;
        }
    }
}
