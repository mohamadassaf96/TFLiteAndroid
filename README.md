# TFLiteAndroid

## Description

Android App that performs live object detection, classification and object detection in offline mode using TensorFlowLite.

## Build App

* Clone this repository.
* Download InceptionV3 https://storage.googleapis.com/download.tensorflow.org/models/tflite/model_zoo/upload_20180427/inception_v3_2018_04_27.tgz, Copy inception_v3.tflite and labels.txt to root/app/src/main/assets.
* Connect your phone and run.

## Notes

* In offline classification/detection, you will see "use CPU" and "use GPU" buttons. The app will continuously send inference requests and display their duration. This was done for testing purposes to observe speed up when using GPU on float models.
* If you want to add custom TensorFlowLite models:
  * Add .tflite and .txt files to root/app/src/main/assets
  * Create a new class that adequately inherits from "ImageClassifier" or "ImageDetector".
  * Override model specific methods (Check InceptionV3Float.java and SSDdetectorQuat.java for your reference).
