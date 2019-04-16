package com.example.objectdetector;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SSDdetectorQuant extends ImageDetector {

    private float[][] labelProbArray = null;

    private static final int NUM_DETECTIONS = 10;
    private float[][][] outputLocations;
    private float[][] outputClasses;
    private float[][] outputScores;
    private float[] numDetections;
    Map<Integer, Object> outputMap = new HashMap<>();
    private Recognition[] recognitions = null;

    SSDdetectorQuant(Activity activity) throws IOException {
        super(activity);
        outputLocations = new float[1][NUM_DETECTIONS][4];
        outputClasses = new float[1][NUM_DETECTIONS];
        outputScores = new float[1][NUM_DETECTIONS];
        numDetections = new float[1];
        outputMap.put(0, outputLocations);
        outputMap.put(1, outputClasses);
        outputMap.put(2, outputScores);
        outputMap.put(3, numDetections);
        labelProbArray = new float[1][getNumLabels()];
    }

    @Override
    protected void addPixelValue(int pixelValue) {
        imgData.put((byte) ((pixelValue >> 16) & 0xFF));
        imgData.put((byte) ((pixelValue >> 8) & 0xFF));
        imgData.put((byte) (pixelValue & 0xFF));
    }


    @Override
    protected String getModelPath() {
        return "detect.tflite";
    }
    @Override
    protected String getLabelPath() {
        return "labelmap.txt";
    }
    @Override
    protected int getImageSizeX() {
        return 300;
    }
    @Override
    protected int getImageSizeY() {
        return 300;
    }
    @Override
    protected int getNumBytesPerChannel() {
        return 1;
    }

    @Override
    protected void runInference() {
        Object[] inputArray = {imgData};
        tflite.runForMultipleInputsOutputs(inputArray, outputMap);
    }

    @Override
    protected void recognize(){
        float n = ((float []) outputMap.get(3) )[0];
        List<String> labelsList = getlabelList();
        recognitions = new Recognition[(int) n];
        for (int i =0; i<n;++i){
            RectF rectF = new RectF( getImageSizeX()*((float [][][]) outputMap.get(0)) [0][i][1], getImageSizeY()*((float [][][]) outputMap.get(0) )[0][i][0], getImageSizeX()*((float [][][]) outputMap.get(0) )[0][i][3], getImageSizeY()*((float [][][]) outputMap.get(0) )[0][i][2]);
            recognitions[i] = new Recognition(""+ ((float [][]) outputMap.get(1) )[0][i], labelsList.get( (int) ((float [][]) outputMap.get(1) )[0][i] +1), ((float [][]) outputMap.get(2) )[0][i], rectF );
        }
    }

    public Recognition[] getRecognitions() {
        return recognitions;
    }

    @Override
    protected Bitmap drawRects(Bitmap bitmap){
        Bitmap copybitmap = Bitmap.createBitmap(bitmap);
        Canvas canvasToShow = new Canvas(copybitmap);
        canvasToShow.drawBitmap(bitmap, 0, 0, null);
        final Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f);

        final Paint paint2 = new Paint();
        paint2.setColor(Color.RED);
        paint2.setStyle(Paint.Style.FILL);
        paint2.setStrokeWidth(1.0f);

        DecimalFormat df = new DecimalFormat("0.##");

        for (Recognition recognition: recognitions){
            if (recognition.getConfidence()>0.5) {
                canvasToShow.drawRect(recognition.getLocation(), paint);
                String toShow = recognition.getTitle() +" "+ df.format( 100*recognition.getConfidence()) +"%";
                canvasToShow.drawText(toShow, recognition.getLocation().left, recognition.getLocation().top-4, paint2);
            }
        }
        return copybitmap;
    }
}
