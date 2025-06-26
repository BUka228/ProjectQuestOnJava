package com.example.projectquestonjava.core.di;

import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Квалификатор для Executor'а, предназначенного для общих фоновых вычислений (CPU-bound).
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultExecutor {
}