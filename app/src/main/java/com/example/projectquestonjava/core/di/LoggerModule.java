package com.example.projectquestonjava.core.di;

import com.example.projectquestonjava.core.utils.Logger;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import org.slf4j.LoggerFactory;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class LoggerModule {

    @Provides
    @Singleton
    public Logger provideLogger() {

        return new Logger() {
            // Для методов без тега будем использовать логгер этого анонимного класса
            private final org.slf4j.Logger defaultLogger = LoggerFactory.getLogger(LoggerModule.class);

            @Override public void info(String message) { defaultLogger.info(message); }
            @Override public void warn(String message) { defaultLogger.warn(message); }
            @Override public void debug(String message) { defaultLogger.debug(message); }
            @Override public void error(String message, Throwable throwable) { defaultLogger.error(message, throwable); }
            @Override public void error(String tag, String message) { LoggerFactory.getLogger(tag).error(message); } // Используем tag как имя логгера

            @Override public void info(String tag, String message) { LoggerFactory.getLogger(tag).info(message); }
            @Override public void warn(String tag, String message) { LoggerFactory.getLogger(tag).warn(message); }
            @Override public void debug(String tag, String message) { LoggerFactory.getLogger(tag).debug(message); }
            @Override public void error(String tag, String message, Throwable throwable) { LoggerFactory.getLogger(tag).error(message, throwable); }
        };
    }
}