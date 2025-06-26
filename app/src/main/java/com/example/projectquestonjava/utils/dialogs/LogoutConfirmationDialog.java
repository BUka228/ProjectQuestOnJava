package com.example.projectquestonjava.utils.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog; // Используем AlertDialog из androidx.appcompat
import androidx.fragment.app.DialogFragment;
import com.example.projectquestonjava.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder; // Используем MaterialAlertDialogBuilder

public class LogoutConfirmationDialog extends DialogFragment {

    private final Runnable onConfirm; // Используем Runnable для простоты

    // Конструктор, принимающий действие подтверждения
    public LogoutConfirmationDialog(@NonNull Runnable onConfirmAction) {
        this.onConfirm = onConfirmAction;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity(), R.style.Base_Theme_ProgressQuest); // Можно использовать стандартную тему или кастомную

        builder.setTitle("Выход из аккаунта")
                .setMessage("Вы уверены, что хотите выйти из своего аккаунта?")
                .setIcon(R.drawable.exit_to_app) // Убедитесь, что drawable exit_to_app есть (можно взять из material icons)
                .setPositiveButton("Выйти", (dialog, which) -> {
                    if (onConfirm != null) {
                        onConfirm.run();
                    }
                })
                .setNegativeButton("Отмена", (dialog, which) -> dismiss());

        AlertDialog dialog = builder.create();

        // Кастомизация цветов кнопок (если нужно, но MaterialAlertDialogBuilder уже должен их стилизовать)
        dialog.setOnShowListener(dialogInterface -> {
        });

        return dialog;
    }

}