package com.example.objectdetector;

import android.app.Activity;

import java.io.IOException;

public class InceptionV3Float extends ImageClassifier {

    //Image related. Native to inception
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128.0f;

    private float[][] labelProbArray = null;

    InceptionV3Float(Activity activity) throws IOException {
        super(activity);
        labelProbArray = new float[1][getNumLabels()];
    }

    @Override
    protected void addPixelValue(int pixelValue) {
        imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
        imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
        imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
    }

    @Override
    protected String getModelPath() {
        return "inception_v3.tflite";
    }
    @Override
    protected String getLabelPath() {
        return "labels.txt";
    }
    @Override
    protected int getImageSizeX() {
        return 299;
    }
    @Override
    protected int getImageSizeY() {
        return 299;
    }
    @Override
    protected int getNumBytesPerChannel() {
        return 4;
    }
    @Override
    protected float getProbability(int labelIndex) {
        return labelProbArray[0][labelIndex];
    }
    @Override
    protected void setProbability(int labelIndex, Number value) {
        labelProbArray[0][labelIndex] = value.floatValue();
    }
    @Override
    protected void runInference() {
        tflite.run(imgData, labelProbArray);
    }

}
