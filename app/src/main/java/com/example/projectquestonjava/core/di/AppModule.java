package com.example.projectquestonjava.core.di;

import android.app.NotificationManager;
import android.content.Context;
import androidx.core.content.ContextCompat; // Для getMainExecutor
import com.example.projectquestonjava.core.managers.SnackbarManager;
import com.example.projectquestonjava.core.utils.AndroidPermissionChecker;
import com.example.projectquestonjava.core.utils.PermissionChecker;

import coil.ImageLoader;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    @Provides
    @Singleton
    @IODispatcher
    public Executor provideIOExecutor() {
        return Executors.newCachedThreadPool();
    }

    @Provides
    @Singleton
    @DefaultExecutor
    public Executor provideDefaultExecutor() {
        return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    @Provides
    @Singleton
    @ScheduledExecutor
    public ScheduledExecutorService provideScheduledExecutorService() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    @Provides
    @Singleton
    @MainExecutor
    public Executor provideMainExecutor(@ApplicationContext Context context) {
        return ContextCompat.getMainExecutor(context);
    }


    @Provides
    @Singleton
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

    @Provides
    @Singleton
    public ImageLoader provideImageLoader(@ApplicationContext Context context) {
        return new ImageLoader.Builder(context)
                .crossfade(true)
                .build();
    }
}