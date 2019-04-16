package com.example.objectdetector;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.os.SystemClock;

import org.tensorflow.lite.Delegate;
import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

public abstract class ImageDetector {

    //app related, fixed
    private static final String TAG = "objectdetector";

    //image related, fixed
    private static final int DIM_BATCH_SIZE = 1;
    private static final int DIM_PIXEL_SIZE = 3;
    protected ByteBuffer imgData = null;
    private int[] intValues = new int[getImageSizeX() * getImageSizeY()];

    //model related
    protected Interpreter tflite;
    private MappedByteBuffer tfliteModel;
    Delegate gpuDelegate;
    private Interpreter.Options tfliteOptions = new Interpreter.Options();
    private List<String> labelList;


    ImageDetector(Activity activity) throws IOException {
        tfliteModel = loadModelFile(activity);
        tflite = new Interpreter(tfliteModel, tfliteOptions);
        labelList = loadLabelList(activity);
        imgData = ByteBuffer.allocateDirect(DIM_BATCH_SIZE * getImageSizeX() * getImageSizeY() *DIM_PIXEL_SIZE * getNumBytesPerChannel());
        imgData.order(ByteOrder.nativeOrder());
    }

    public void useGpu() {
        tfliteOptions = new Interpreter.Options();
        try {
            gpuDelegate = Class.forName("org.tensorflow.lite.experimental.GpuDelegate")
                    .asSubclass(Delegate.class)
                    .getDeclaredConstructor()
                    .newInstance();
        }
        catch (Exception e){
            return;
        }
        tfliteOptions.addDelegate(gpuDelegate);
        recreateInterpreter();
    }

    public void useCPU(){
        tfliteOptions = new Interpreter.Options();
        tfliteOptions.setUseNNAPI(false);
        recreateInterpreter();
    }

    public void useNNAPI(){
        tfliteOptions = new Interpreter.Options();
        tfliteOptions.setUseNNAPI(true);
        recreateInterpreter();
    }

    private void recreateInterpreter() {
        if (tflite != null) {
            tflite.close();
            tflite = new Interpreter(tfliteModel, tfliteOptions);
        }
    }

    String classifyFrame(Bitmap bitmap) {
        if (tflite == null){
            return "Uninitialized Classifier";
        }
        convertBitmapToByteBuffer(bitmap);
        long startTime = SystemClock.uptimeMillis();
        runInference();
        long endTime = SystemClock.uptimeMillis();
        recognize();
        String textToShow = Long.toString(endTime - startTime) + "ms";
        return textToShow;
    }

    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(getModelPath());
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private  List<String> loadLabelList(Activity activity) throws IOException {
        List <String> labelList = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(activity.getAssets().open(getLabelPath())));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    private void convertBitmapToByteBuffer(Bitmap bitmap){
        if (imgData == null){
            return;
        }
        imgData.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i=0; i<getImageSizeX();++i){
            for (int j = 0; j < getImageSizeY(); ++j){
                final int val = intValues[pixel++];
                addPixelValue(val);
            }
        }
    }

    protected int getNumLabels() {
        return labelList.size();
    }

    protected List<String> getlabelList() {
        return labelList;
    }

    //abstract methods
    protected abstract int getImageSizeX();
    protected abstract int getImageSizeY();
    protected abstract void addPixelValue(int pixelValue);
    protected abstract int getNumBytesPerChannel();
    protected abstract void runInference();
    protected abstract String getModelPath();
    protected abstract String getLabelPath();
    protected abstract Bitmap drawRects(Bitmap bitmap);
    protected  abstract void recognize();

}
