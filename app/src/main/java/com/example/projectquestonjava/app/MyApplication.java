package com.example.projectquestonjava.app;

import android.app.Application;
import androidx.annotation.NonNull; // Для NonNull в FutureCallback
import com.example.projectquestonjava.core.data.initializers.TestDataInitializer;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.utils.Logger; // Наш интерфейс
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors; // Для directExecutor
import java.util.concurrent.Executor;
import javax.inject.Inject;
import dagger.hilt.android.HiltAndroidApp;
// import org.slf4j.LoggerFactory; // Если используется SLF4J напрямую

@HiltAndroidApp
public class MyApplication extends Application {

    @Inject
    TestDataInitializer testDataInitializer;

    @Inject
    @IODispatcher // Убедись, что аннотация правильная
    Executor ioExecutor; // Нужен для выполнения ListenableFuture

    @Inject
    Logger logger; // Наш логгер

    @Override
    public void onCreate() {
        super.onCreate();

        // Инициализация нашего логгера (если он требует этого, например, Logback)
        // org.slf4j.LoggerFactory.getLogger(MyApplication.class).info("MyApplication onCreate - Logback possibly initialized here.");
        logger.info("MyApplication", "onCreate - Application starting.");


        // --- ПЕРЕМЕЩАЕМ ИНИЦИАЛИЗАЦИЮ ТЕСТОВЫХ ДАННЫХ СЮДА ---
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
                // Здесь Snackbar не показать, так как нет Activity. Только логирование.
            }
        }, MoreExecutors.directExecutor()); // Коллбэк можно выполнить в том же потоке, если он простой
    }
}