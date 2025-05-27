package com.example.projectquestonjava.core.di;

import android.content.Context;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava3.RxDataStore;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.util.concurrent.Executor;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class DataStoreModule {

    private static final String PREFERENCES_NAME = "app_preferences";

    @Provides
    @Singleton
    public RxDataStore<Preferences> provideRxPreferencesDataStore(
            @ApplicationContext Context context,
            @IODispatcher Executor ioExecutor
    ) {
        return new RxPreferenceDataStoreBuilder(context, PREFERENCES_NAME)
                .setIoScheduler(Schedulers.from(ioExecutor))
                .build();
    }
}