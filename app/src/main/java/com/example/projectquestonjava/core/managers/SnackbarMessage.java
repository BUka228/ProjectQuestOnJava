package com.example.projectquestonjava.core.managers;

import androidx.lifecycle.LiveData;
import com.example.projectquestonjava.core.utils.SingleLiveEvent;
import javax.inject.Inject;
import javax.inject.Singleton;
// import kotlinx.coroutines.flow.MutableSharedFlow; // Удаляем
// import kotlinx.coroutines.flow.SharedFlow; // Удаляем
// import kotlinx.coroutines.flow.asSharedFlow; // Удаляем

@Singleton
public class SnackbarManager {

    // Используем SingleLiveEvent для событий Snackbar
    private final SingleLiveEvent<SnackbarMessage> _messagesEvent = new SingleLiveEvent<>();
    public LiveData<SnackbarMessage> getMessagesEvent() {
        return _messagesEvent;
    }

    @Inject
    public SnackbarManager() {}

    // Этот метод будет вызываться из ViewModel (которые теперь на Java)
    // Он должен быть синхронным, т.к. SingleLiveEvent.setValue() должен вызываться из MainThread
    // или postValue() из любого потока.
    // ViewModel'и будут вызывать его из главного потока после завершения асинхронных операций.
    public void showMessage(String message) {
        // Длительность по умолчанию для Material Components Snackbar
        _messagesEvent.postValue(new SnackbarMessage(message, SnackbarMessage.LENGTH_SHORT));
    }

    public void showMessage(String message, int materialDuration) {
        _messagesEvent.postValue(new SnackbarMessage(message, materialDuration));
    }

    // Если где-то в Kotlin UI (App.kt) все еще ожидается SnackbarDuration из Compose,
    // можно оставить метод, который принимает его, но внутри он будет конвертироваться.
    // Но лучше переделать App.kt для работы с int длительностью.
    /*
    public void showMessage(String message, androidx.compose.material3.SnackbarDuration composeDuration) {
        int materialDuration = composeDuration == androidx.compose.material3.SnackbarDuration.Long ?
                SnackbarMessage.LENGTH_LONG : SnackbarMessage.LENGTH_SHORT;
        _messagesEvent.postValue(new SnackbarMessage(message, materialDuration));
    }
    */
}