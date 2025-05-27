package com.example.projectquestonjava.core.di;

import com.example.projectquestonjava.core.utils.Logger;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import timber.log.Timber;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class LoggerModule {

    @Provides
    @Singleton
    public Logger provideLogger() {
        // Timber должен быть инициализирован в Application классе
        return new Logger() {
            @Override public void info(String message) { Timber.i(message); }
            @Override public void warn(String message) { Timber.w(message); }
            @Override public void debug(String message) { Timber.d(message); }
            @Override public void error(String message, Throwable throwable) { Timber.e(throwable, message); }
            @Override public void info(String tag, String message) { Timber.tag(tag).i(message); }
            @Override public void warn(String tag, String message) { Timber.tag(tag).w(message); }
            @Override public void debug(String tag, String message) { Timber.tag(tag).d(message); }
            @Override public void error(String tag, String message, Throwable throwable) { Timber.tag(tag).e(throwable, message); }
            @Override public void error(String tag, String message) { Timber.tag(tag).e(message); } // Перегрузка
        };
    }
}