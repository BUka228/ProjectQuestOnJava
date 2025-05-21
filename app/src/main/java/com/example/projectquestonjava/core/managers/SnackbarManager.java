package com.example.projectquestonjava.core.managers;

import androidx.lifecycle.LiveData;
import com.example.projectquestonjava.core.utils.SingleLiveEvent;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SnackbarManager {

    private final SingleLiveEvent<SnackbarMessage> _messagesEvent = new SingleLiveEvent<>();
    public LiveData<SnackbarMessage> getMessagesEvent() {
        return _messagesEvent;
    }

    @Inject
    public SnackbarManager() {}

    /**
     * Показывает Snackbar с сообщением и стандартной длительностью.
     * Этот метод безопасен для вызова из любого потока.
     * @param message Сообщение для Snackbar.
     * @param duration Длительность показа (SnackbarMessage.LENGTH_SHORT, SnackbarMessage.LENGTH_LONG).
     */
    public void showMessage(String message, int duration) {
        _messagesEvent.postValue(new SnackbarMessage(message, duration));
    }

    /**
     * Показывает Snackbar с сообщением и длительностью по умолчанию (LENGTH_SHORT).
     * @param message Сообщение для Snackbar.
     */
    public void showMessage(String message) {
        showMessage(message, SnackbarMessage.LENGTH_SHORT);
    }
}