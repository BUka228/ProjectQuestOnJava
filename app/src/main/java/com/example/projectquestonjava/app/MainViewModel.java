package com.example.projectquestonjava.app;

import androidx.lifecycle.ViewModel;
import java.util.concurrent.Executor; // Для фоновых задач
import javax.inject.Inject;
import com.example.projectquestonjava.core.di.IODispatcher; // Ваша аннотация
import dagger.hilt.android.lifecycle.HiltViewModel;
// import kotlinx.coroutines.launch; // Удаляем корутины

@HiltViewModel
public class MainViewModel extends ViewModel {

    // private final Executor ioExecutor;

    @Inject
    public MainViewModel() {
        ensureDataInitialized();
    }

    public void ensureDataInitialized() {

    }
}