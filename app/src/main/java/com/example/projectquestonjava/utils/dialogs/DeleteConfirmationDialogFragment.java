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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.example.projectquestonjava.R;

public class DeleteConfirmationDialogFragment extends DialogFragment {

    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_MESSAGE = "arg_message";
    private static final String ARG_POSITIVE_BUTTON_TEXT = "arg_positive_button_text";
    private static final String ARG_ICON_RES_ID = "arg_icon_res_id";

    private ConfirmClickListener positiveClickListener;

    public interface ConfirmClickListener {
        void onConfirmClick();
    }

    public static DeleteConfirmationDialogFragment newInstance(
            String title,
            String message,
            String positiveButtonText,
            int iconResId,
            ConfirmClickListener listener
    ) {
        DeleteConfirmationDialogFragment fragment = new DeleteConfirmationDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        args.putString(ARG_POSITIVE_BUTTON_TEXT, positiveButtonText);
        args.putInt(ARG_ICON_RES_ID, iconResId);
        fragment.setArguments(args);
        fragment.positiveClickListener = listener; // Устанавливаем listener
        return fragment;
    }
    public DeleteConfirmationDialogFragment() {
        // Пустой конструктор для DialogFragment
    }

    // Добавляем конструктор для установки listener'a, если newInstance не используется
    public DeleteConfirmationDialogFragment(ConfirmClickListener listener) {
        this.positiveClickListener = listener;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity(), com.google.android.material.R.style.Theme_MaterialComponents_DayNight_Dialog_Alert_Bridge);
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_delete_confirmation, null);

        ImageView iconView = dialogView.findViewById(R.id.imageView_dialog_icon);
        TextView titleView = dialogView.findViewById(R.id.textView_dialog_title);
        TextView messageView = dialogView.findViewById(R.id.textView_dialog_message);

        String title = "Подтверждение";
        String message = "Вы уверены?";
        String positiveButtonText = "Да";
        int iconResId = R.drawable.warning; // Дефолтная иконка

        if (getArguments() != null) {
            title = getArguments().getString(ARG_TITLE, title);
            message = getArguments().getString(ARG_MESSAGE, message);
            positiveButtonText = getArguments().getString(ARG_POSITIVE_BUTTON_TEXT, positiveButtonText);
            iconResId = getArguments().getInt(ARG_ICON_RES_ID, iconResId);
        }

        iconView.setImageResource(iconResId);
        titleView.setText(title);
        messageView.setText(message);

        builder.setView(dialogView)
                .setPositiveButton(positiveButtonText, (dialog, which) -> {
                    if (positiveClickListener != null) {
                        positiveClickListener.onConfirmClick();
                    }
                })
                .setNegativeButton("Отмена", (dialog, which) -> dismiss());

        return builder.create();
    }
}