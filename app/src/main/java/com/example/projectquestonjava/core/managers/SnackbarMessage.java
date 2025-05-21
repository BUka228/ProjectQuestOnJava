package com.example.projectquestonjava.core.managers;

import com.google.android.material.snackbar.Snackbar;

public class SnackbarMessage {
    private final String message;
    private final int duration; // Теперь int

    public SnackbarMessage(String message, int duration) {
        this.message = message;
        this.duration = duration;
    }

    public String getMessage() {
        return message;
    }

    public int getDuration() {
        return duration;
    }

    // Стандартные длительности для удобства
    public static final int LENGTH_SHORT = Snackbar.LENGTH_SHORT;
    public static final int LENGTH_LONG = Snackbar.LENGTH_LONG;
    public static final int LENGTH_INDEFINITE = Snackbar.LENGTH_INDEFINITE;
}