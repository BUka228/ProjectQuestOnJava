package com.example.projectquestonjava.app;

import androidx.lifecycle.ViewModel;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MainViewModel extends ViewModel {

    @Inject
    public MainViewModel() {
        ensureDataInitialized();
    }

    public void ensureDataInitialized() {

    }
}