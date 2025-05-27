package com.example.projectquestonjava.core.di;

import com.example.projectquestonjava.core.utils.DateTimeUtils;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class UtilModule {

    @Provides
    @Singleton
    public DateTimeUtils provideDateTimeUtils() {
        return new DateTimeUtils();
    }
}