package com.example.projectquestonjava.app;

import android.app.Application;

import com.github.mikephil.charting.BuildConfig;

import dagger.hilt.android.HiltAndroidApp;
import timber.log.Timber;


@HiltAndroidApp
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant();
            Timber.d("Timber is initialized for DEBUG build.");
        } else {
            Timber.d("Timber is initialized for RELEASE build (potentially with a release tree).");
        }
    }
}