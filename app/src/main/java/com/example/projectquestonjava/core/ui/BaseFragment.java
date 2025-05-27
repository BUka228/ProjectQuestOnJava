package com.example.projectquestonjava.core.ui;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.projectquestonjava.app.MainActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public abstract class BaseFragment extends Fragment {

    protected MainActivity getMainActivity() {
        if (getActivity() instanceof MainActivity) {
            return (MainActivity) getActivity();
        }
        return null;
    }

    protected MaterialToolbar getToolbar() {
        MainActivity activity = getMainActivity();
        return (activity != null) ? activity.getToolbar() : null;
    }

    // Предоставляем доступ к обоим типам FAB
    protected FloatingActionButton getStandardFab() {
        MainActivity activity = getMainActivity();
        return (activity != null) ? activity.getStandardFab() : null;
    }

    protected ExtendedFloatingActionButton getExtendedFab() {
        MainActivity activity = getMainActivity();
        return (activity != null) ? activity.getExtendedFab() : null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Используем post, чтобы убедиться, что View Activity созданы
        view.post(() -> {
            setupToolbar(); // Наследники переопределяют для настройки Toolbar
            setupFab();     // Наследники переопределяют для настройки нужного FAB
        });
    }

    /**
     * Вызывается для настройки Toolbar.
     * MainActivity устанавливает заголовок по умолчанию из NavGraph.
     * Фрагменты могут переопределить title, добавить меню и обработчики.
     */
    protected abstract void setupToolbar();

    /**
     * Вызывается для настройки Floating Action Button.
     * MainActivity по умолчанию скрывает оба FAB.
     * Фрагменты должны выбрать, какой FAB им нужен (standard или extended),
     * настроить его (иконка, текст, слушатель) и сделать видимым (fab.show()).
     */
    protected abstract void setupFab();
}