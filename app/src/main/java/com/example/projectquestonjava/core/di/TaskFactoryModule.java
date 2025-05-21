package com.example.projectquestonjava.core.di;

import com.example.projectquestonjava.core.data.factories.TaskFactoryImpl;
import com.example.projectquestonjava.core.domain.factories.TaskFactory;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class TaskFactoryModule {
    @Provides
    @Singleton
    public static TaskFactory provideTaskFactory(DateTimeUtils dateTimeUtils) { 
        return new TaskFactoryImpl(dateTimeUtils);
    }
}