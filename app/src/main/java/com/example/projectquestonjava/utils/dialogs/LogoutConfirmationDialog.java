package com.example.projectquestonjava.utils.dialogs; // Убедитесь, что пакет правильный

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


    // Статический метод newInstance не обязателен, если мы всегда передаем listener через конструктор
    // при вызове new LogoutConfirmationDialog(...).show(...)
    // Но если хотим передавать что-то через Bundle, то он нужен.
    // Для простоты, пока оставим конструктор.

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Используем MaterialAlertDialogBuilder для Material3 стиля
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity(), R.style.Base_Theme_ProgressQuest); // Можно использовать стандартную тему или кастомную

        // Инфлейтим кастомный макет, если он есть (как в ProfileScreen)
        // Или используем стандартные методы setTitle, setMessage, setIcon
        // Предположим, что у нас есть простой диалог без сложного макета,
        // как это было определено в ProfileScreen.kt
        // <item name="alertDialogTheme">@style/ThemeOverlay.Material3.MaterialAlertDialog.Centered</item> в themes.xml

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
            // Можно получить кнопки и изменить их цвет, если стандартные не подходят
            // Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            // if (positiveButton != null) {
            //     positiveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.errorLight));
            // }
        });

        return dialog;
    }

    // Если был бы кастомный макет (как в ProfileScreen.kt для LogoutConfirmationDialog):
    // @NonNull
    // @Override
    // public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    //     AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
    //     LayoutInflater inflater = requireActivity().getLayoutInflater();
    //     View dialogView = inflater.inflate(R.layout.dialog_custom_confirmation, null); // Пример макета

    //     ImageView iconView = dialogView.findViewById(R.id.dialog_icon);
    //     TextView titleView = dialogView.findViewById(R.id.dialog_title);
    //     TextView messageView = dialogView.findViewById(R.id.dialog_message);

    //     iconView.setImageResource(R.drawable.exit_to_app); // Убедитесь, что иконка есть
    //     titleView.setText("Выход из аккаунта");
    //     messageView.setText("Вы уверены, что хотите выйти из своего аккаунта?");

    //     builder.setView(dialogView)
    //            .setPositiveButton("Выйти", (dialog, which) -> {
    //                if (onConfirm != null) {
    //                    onConfirm.run();
    //                }
    //            })
    //            .setNegativeButton("Отмена", (dialog, which) -> dismiss());
    //     return builder.create();
    // }
}