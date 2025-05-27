package com.example.projectquestonjava.core.di; // Убедись, что пакет правильный

import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface ScheduledExecutor {
}