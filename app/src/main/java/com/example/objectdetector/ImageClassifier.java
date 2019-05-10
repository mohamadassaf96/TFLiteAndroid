package com.example.objectdetector;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public abstract class ImageClassifier {

    //app related, fixed
    private static final String TAG = "ImageClassifier";

    //image related, fixed
    private static final int RESULTS_TO_SHOW = 1;
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
    private float[][] filterLabelProbArray = null;

    //results related
    private static final int FILTER_STAGES = 3;
    private static final float FILTER_FACTOR = 0.4f;

    private PriorityQueue<Map.Entry<String, Float>> sortedLabels = new PriorityQueue<>(RESULTS_TO_SHOW, new Comparator<Map.Entry<String, Float>>() {
        @Override
        public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
            return (o1.getValue()).compareTo(o2.getValue());
        }
    });

    ImageClassifier(Activity activity) throws IOException {
        Log.d(TAG, "Loading model and its components.");
        tfliteModel = loadModelFile(activity);
        tflite = new Interpreter(tfliteModel, tfliteOptions);
        labelList = loadLabelList(activity);
        imgData = ByteBuffer.allocateDirect(DIM_BATCH_SIZE * getImageSizeX() * getImageSizeY() *DIM_PIXEL_SIZE * getNumBytesPerChannel());
        imgData.order(ByteOrder.nativeOrder());
        filterLabelProbArray = new float[FILTER_STAGES][labelList.size()];
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
            Log.e(TAG, "Cannot start gpu delegate.");
            e.printStackTrace();
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
        Log.d(TAG, "Running inference.");
        convertBitmapToByteBuffer(bitmap);
        long startTime = SystemClock.uptimeMillis();
        runInference();
        long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "Applying filter.");
        applyFilter();
        Log.d(TAG, "sorting labels.");
        String textToShow = printTopKLabels();
        textToShow = Long.toString(endTime - startTime) + "ms" + textToShow;
        return textToShow;
    }

    private String printTopKLabels(){
        for (int i=0; i<labelList.size(); ++i){
            sortedLabels.add(new AbstractMap.SimpleEntry<>(labelList.get(i), getProbability(i)));
            if (sortedLabels.size() > RESULTS_TO_SHOW) {
                sortedLabels.poll();
            }
        }
        String textToShow = "";
        final int size = sortedLabels.size();
        for (int i =0; i<size; ++i){
            Map.Entry<String, Float> label = sortedLabels.poll();
            textToShow = String.format("\n%s: %4.2f", label.getKey(), label.getValue()) + textToShow;
        }
        return textToShow;
    }

    private void applyFilter(){
        int num_labels = getNumLabels();
        for (int j=0; j<num_labels; ++j){
            filterLabelProbArray[0][j] += FILTER_FACTOR*(getProbability(j) - filterLabelProbArray[0][j]);
        }
        for (int i=1; i<FILTER_STAGES; ++i){
            for (int j=0; j<num_labels; ++j){
                filterLabelProbArray[i][j] += FILTER_FACTOR*(filterLabelProbArray[i-1][j] - filterLabelProbArray[i][j]);
            }
        }
        for (int j=0; j<num_labels; ++j){
            setProbability(j, filterLabelProbArray[FILTER_STAGES - 1][j]);
        }
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

    protected int getNumLabels() {
        return labelList.size();
    }

    protected List<String> getlabelList() {
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

    //abstract methods
    protected abstract int getImageSizeX();
    protected abstract int getImageSizeY();
    protected abstract void addPixelValue(int pixelValue);
    protected abstract int getNumBytesPerChannel();
    protected abstract void runInference();
    protected abstract String getModelPath();
    protected abstract String getLabelPath();
    protected abstract float getProbability(int labelIndex);
    protected abstract void setProbability(int labelIndex, Number value);

}
