package com.example.objectdetector;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

public class BrowseActivity extends MainActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse);

        final String[] colors = {"Classification", "Detection"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Please choose a method");
        builder.setItems(colors, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if ("Detection".equals(colors[which])){
                    getFragmentManager()
                            .beginTransaction()
                            .replace(R.id.container, DetectionFragment.newInstance())
                            .commit();
                }
                else if ("Classification".equals(colors[which])){
                    getFragmentManager()
                            .beginTransaction()
                            .replace(R.id.container, ClassificationFragment.newInstance())
                            .commit();
                }
            }
        });
        builder.show();
    }
}
