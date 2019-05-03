package com.example.objectdetector;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.util.Log;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DetectionFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DetectionFragment#newInstance} factory method to
 * create an instance of this fragment.
 */

public class DetectionFragment extends Fragment  {

    private static final String TAG = "DetectionFragment";
    private static final String HANDLE_THREAD_NAME = "Classifier";
    private static final int READ_REQUEST_CODE = 42;

    private Button button;
    private ImageView image;
    private String device = "nnapi";
    private SSDdetectorQuant classifer;
    private Bitmap bitmap;

    /** An additional thread for running tasks that shouldn't block the UI. */
    private HandlerThread backgroundThread;

    /** A {@link Handler} for running tasks in the background. */
    private Handler backgroundHandler;

    //private OnFragmentInteractionListener mListener;

    public static Fragment newInstance() {
        return new DetectionFragment();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "Initializing background thread.");
        backgroundThread = new HandlerThread(HANDLE_THREAD_NAME);
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_blank, container, false);
    }

    public void onViewCreated(final View view, Bundle savedInstanceState) {
        image = (ImageView) view.findViewById(R.id.imageView);
        button = (Button) view.findViewById(R.id.button);
        try {
            classifer = new SSDdetectorQuant(getActivity());
        }
        catch (Exception e){
            Log.e(TAG, "Failed to initialize model.");
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
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            final Uri uri;
            if (resultData != null) {
                uri = resultData.getData();
                sendToBackGround(uri);
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
        Bitmap bitmaptoshow = Bitmap.createBitmap(classifer.drawRects(bitmap));

        getActivity().runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        image.setImageBitmap(bitmaptoshow);
                    }
                });
    }

    protected void sendToBackGround(Uri uri){
        Log.d(TAG, "Converting to bitmap.");
            backgroundHandler.post(() -> {
                try {
                    bitmap = Utils.getBitmapFromUri(uri, getActivity(), classifer.getImageSizeX(), classifer.getImageSizeY());
                }
                catch (Exception e) {
                    Log.e(TAG, "Cannot convert to bitmap.");
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
