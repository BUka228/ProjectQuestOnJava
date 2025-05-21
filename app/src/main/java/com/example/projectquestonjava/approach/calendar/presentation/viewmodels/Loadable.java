package com.example.projectquestonjava.approach.calendar.presentation.viewmodels;

import androidx.annotation.Nullable;

public abstract class Loadable<T> {
    private Loadable() {} // Приватный конструктор, чтобы нельзя было создать напрямую

    public static final class Loading<T> extends Loadable<T> {
        private static final Loading<?> INSTANCE = new Loading<>();
        @SuppressWarnings("unchecked")
        public static <T> Loading<T> getInstance() {
            return (Loading<T>) INSTANCE;
        }
    }

    public static final class Success<T> extends Loadable<T> {
        private final T data;
        public Success(T data) {
            this.data = data;
        }
        public T getData() {
            return data;
        }
    }

    public static final class Error<T> extends Loadable<T> { // T здесь для единообразия, но не используется
        private final Throwable exception;
        public Error(Throwable exception) {
            this.exception = exception;
        }
        public Throwable getException() {
            return exception;
        }
    }
}