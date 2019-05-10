package com.example.objectdetector;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ClassificationFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ClassificationFragment#newInstance} factory method to
 * create an instance of this fragment.
 */

public class ClassificationFragment extends Fragment  {


    private static final String TAG = "ClassificationFragment";
    private static final String HANDLE_THREAD_NAME = "Classifier";
    private static final int READ_REQUEST_CODE = 42;

    private TextView textView;
    private Button button;
    private ImageView image;
    private CheckBox useGPU;
    private CheckBox useCPU;
    private String device = "nnapi";
    private ImageClassifier classifer;
    private Bitmap bitmap;

    /** An additional thread for running tasks that shouldn't block the UI. */
    private HandlerThread backgroundThread;

    /** A {@link Handler} for running tasks in the background. */
    private Handler backgroundHandler;

    //private OnFragmentInteractionListener mListener;

    public static Fragment newInstance() {
        return new ClassificationFragment();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        backgroundThread = new HandlerThread(HANDLE_THREAD_NAME);
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_classification, container, false);
    }

    public void onViewCreated(final View view, Bundle savedInstanceState) {
        textView = (TextView) view.findViewById(R.id.textView);
        image = (ImageView) view.findViewById(R.id.imageView);
        button = (Button) view.findViewById(R.id.button);
        useGPU = (CheckBox) view.findViewById(R.id.useCPU);
        useCPU = (CheckBox) view.findViewById(R.id.useGPU);
        textView.setVisibility(View.INVISIBLE);
        try {
            classifer = new InceptionV3Float(getActivity());
        }
        catch (Exception e){
            Log.e(TAG, "Cannot initialize classifier.");
            e.printStackTrace();
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Sending browse request.");
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(intent, READ_REQUEST_CODE);
            }
        });

        useGPU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (useGPU.isChecked()) {
                    if (useCPU.isChecked()){
                        textView.setText("Error, turning off CPU");
                        useCPU.setChecked(false);
                    }
                    device = "gpu";
                    sendToBackGround();
                }
                else {
                    device = "nnapi";
                    sendToBackGround();
                }
            }
        });

        useCPU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (useCPU.isChecked()) {
                    if (useGPU.isChecked()){
                        textView.setText("Error, turning off GPU");
                        useGPU.setChecked(false);
                    }
                    device = "cpu";
                    sendToBackGround();
                }
                else {
                    device = "nnapi";
                    sendToBackGround();
                }
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            final Uri uri;
            if (resultData != null) {
                Log.d(TAG, "Converting to bitmap.");
                uri = resultData.getData();
                sendToBackGround(uri);
                Log.d(TAG, "Running the model.");
                backgroundHandler.post(Classify);
            }
        }
    }

    private Runnable Classify =
            new Runnable() {
                @Override
                public void run() {
                    ClassifyFrame();
                    backgroundHandler.post(Classify);
                }
            };

    private void ClassifyFrame(){
        final String textToShow = classifer.classifyFrame(bitmap);
        getActivity().runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                            textView.setVisibility(View.VISIBLE);
                            textView.setText(textToShow);
                            image.setImageBitmap(bitmap);
                    }
                });
    }

    protected void sendToBackGround(){
        switch (device){
            case "cpu":
                backgroundHandler.post(()->{classifer.useCPU();});
                break;
            case "gpu":
                backgroundHandler.post(()->{classifer.useGpu();});
                break;
            case "nnapi":
                backgroundHandler.post(()->{classifer.useNNAPI();});
                break;
        }
    }

    protected void sendToBackGround(Uri uri){
        backgroundHandler.post(() -> {
                try {
                    bitmap = Utils.getBitmapFromUri(uri, getActivity(), classifer.getImageSizeX(), classifer.getImageSizeY());
                }
                catch (Exception e) {
                    Log.e(TAG, "Cannot get bitmap.");
                    e.printStackTrace();
                }
            });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "quitting ...");
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }
}