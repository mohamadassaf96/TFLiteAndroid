package com.example.objectdetector;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileDescriptor;
import java.io.IOException;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link BlankFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link BlankFragment#newInstance} factory method to
 * create an instance of this fragment.
 */

public class BlankFragment extends Fragment  {


    private static final String TAG = "objectdetector";
    private static final String HANDLE_THREAD_NAME = "Classifier";
    private static final int READ_REQUEST_CODE = 42;

    private TextView textView;
    private Button button;
    private ImageView image;
    private CheckBox useGPU;
    private CheckBox useCPU;
    private String device = "nnapi";
    private SSDdetectorQuant classifer;
    private Bitmap bitmap;

    /** An additional thread for running tasks that shouldn't block the UI. */
    private HandlerThread backgroundThread;

    /** A {@link Handler} for running tasks in the background. */
    private Handler backgroundHandler;

    //private OnFragmentInteractionListener mListener;

    public static Fragment newInstance() {
        return new BlankFragment();
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
        return inflater.inflate(R.layout.fragment_blank, container, false);
    }

    public void onViewCreated(final View view, Bundle savedInstanceState) {
        textView = (TextView) view.findViewById(R.id.textView);
        image = (ImageView) view.findViewById(R.id.imageView);
        button = (Button) view.findViewById(R.id.button);
        useGPU = (CheckBox) view.findViewById(R.id.checkBox);
        useCPU = (CheckBox) view.findViewById(R.id.checkBox2);
        textView.setVisibility(View.INVISIBLE);
        try {
            classifer = new SSDdetectorQuant(getActivity());
        }
        catch (Exception e){
            textView.setText(e.toString());
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                        textView.setVisibility(View.VISIBLE);
                        textView.setText(textToShow);
                        image.setImageBitmap(bitmaptoshow);
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
        try {
            backgroundHandler.post(() -> {
                try {
                    bitmap = getBitmapFromUri(uri);
                }
                catch (Exception e) {
                    System.out.println(e.toString());
                }
            });
        }
        catch (Exception e){
            System.out.println(e.toString());
        }
    }

    protected Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                getActivity().getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        image = getResizedBitmap(image, classifer.getImageSizeX(),classifer.getImageSizeY());
        parcelFileDescriptor.close();
        return image;
    }

    protected static Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }


//    // TODO: Rename method, update argument and hook method into UI event
//    public void onButtonPressed(Uri uri) {
//        if (mListener != null) {
//            mListener.onFragmentInteraction(uri);
//        }
//    }

//    @Override
//    public void onAttach(Context context) {
//        super.onAttach(context);
//        if (context instanceof OnFragmentInteractionListener) {
//            mListener = (OnFragmentInteractionListener) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
//    }

//    @Override
//    public void onDetach() {
//        super.onDetach();
//        mListener = null;
//    }



    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
//    public interface OnFragmentInteractionListener {
//        // TODO: Update argument type and name
//        void onFragmentInteraction(Uri uri);
//    }
}
