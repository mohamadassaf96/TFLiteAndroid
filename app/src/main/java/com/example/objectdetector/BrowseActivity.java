package com.example.objectdetector;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class BrowseActivity extends MainActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse);
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.container, BlankFragment.newInstance())
                .commit();
    }
}
