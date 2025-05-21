package com.example.projectquestonjava.core.di;

import android.app.NotificationManager;
import android.content.Context;

import androidx.core.content.PermissionChecker;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import kotlinx.coroutines.CoroutineDispatcher; // Оставляем, если для ApplicationScope нужен CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers; // Для Dispatchers.Default и Dispatchers.Main

import java.util.concurrent.Executor;
import java.util.concurrent.Executors; // Для создания ExecutorService

import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    // Оставляем CoroutineDispatchers для ApplicationScope, если он используется с корутинами
    // Если ApplicationScope тоже будет на Java Executors, то это нужно изменить
    @Provides
    @Singleton
    @DefaultDispatcher
    public CoroutineDispatcher provideDefaultDispatcher() {
        return Dispatchers.getDefault();
    }

    @Provides
    @Singleton
    @MainDispatcher
    public CoroutineDispatcher provideMainDispatcher() {
        return Dispatchers.getMain();
    }

    // Предоставляем Executor для IO операций
    @Provides
    @Singleton
    @IODispatcher
    public Executor provideIOExecutor() {
        // Можно использовать фиксированный пул или кешированный
        // Для IO операций обычно подходит кешированный или пул с размером, зависящим от кол-ва ядер
        return Executors.newFixedThreadPool(Math.max(2, Math.min(Runtime.getRuntime().availableProcessors() - 1, 4)));
        // Или, если Kotlin все еще доступен в DI:
        // return Dispatchers.getIO().asExecutor();
    }


    @Provides
    @Singleton
    @ApplicationScope
    public CoroutineScope provideApplicationScope(
            @DefaultDispatcher CoroutineDispatcher defaultDispatcher
    ) {
        return new CoroutineScope(SupervisorJob.create().plus(defaultDispatcher));
    }

    @Provides
    public NotificationManager provideNotificationManager(@ApplicationContext Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Provides
    @Singleton
    public PermissionChecker provideAndroidPermissionChecker(@ApplicationContext Context context) {
        return new AndroidPermissionChecker(context);
    }

    @Provides
    @Singleton
    public SnackbarManager provideSnackbarManager() {
        return new SnackbarManager();
    }
}