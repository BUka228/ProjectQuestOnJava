package com.example.projectquestonjava.app;

import android.app.Application;
import androidx.annotation.NonNull;
import com.example.projectquestonjava.core.data.initializers.TestDataInitializer;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.utils.Logger;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class MyApplication extends Application {

    @Inject
    TestDataInitializer testDataInitializer;

    @Inject
    @IODispatcher
    Executor ioExecutor;

    @Inject
    Logger logger;
    @Override
    public void onCreate() {
        super.onCreate();

        // Инициализация нашего логгера
        logger.info("MyApplication", "onCreate - Application starting.");

        logger.info("MyApplication", "onCreate: Initializing test data from Application class...");
        ListenableFuture<Void> initFuture = testDataInitializer.initializeTestDataIfEmpty();
        Futures.addCallback(initFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                logger.info("MyApplication", "onCreate: Test data initialization successful or data already exists (from Application).");
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error("MyApplication", "onCreate: Error initializing test data (from Application)", t);
            }
        }, MoreExecutors.directExecutor());
    }
}