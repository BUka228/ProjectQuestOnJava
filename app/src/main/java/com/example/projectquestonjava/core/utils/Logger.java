package com.example.projectquestonjava.core.utils;

public interface Logger {
    void info(String message);
    void warn(String message);
    void debug(String message);
    void error(String message, Throwable throwable);

    // Перегруженные методы с тегом
    void info(String tag, String message);
    void warn(String tag, String message);
    void debug(String tag, String message);
    void error(String tag, String message, Throwable throwable);
    void error(String tag, String message);

}