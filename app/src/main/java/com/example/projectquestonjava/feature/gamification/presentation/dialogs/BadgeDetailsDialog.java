package com.example.projectquestonjava.feature.gamification.presentation.dialogs;

import android.app.Dialog;
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
import com.example.projectquestonjava.feature.gamification.data.model.Badge;

public class BadgeDetailsDialog extends DialogFragment {
    private static final String ARG_BADGE_NAME = "badge_name";
    private static final String ARG_BADGE_DESC = "badge_desc";
    private static final String ARG_BADGE_IMAGE_RES = "badge_image_res";
    private static final String ARG_BADGE_CRITERIA = "badge_criteria";

    public static BadgeDetailsDialog newInstance(Badge badge) {
        BadgeDetailsDialog dialog = new BadgeDetailsDialog();
        Bundle args = new Bundle();
        args.putString(ARG_BADGE_NAME, badge.getName());
        args.putString(ARG_BADGE_DESC, badge.getDescription());
        args.putInt(ARG_BADGE_IMAGE_RES, badge.getImageUrl());
        args.putString(ARG_BADGE_CRITERIA, badge.getCriteria());
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_badge_details, null); // Создать этот макет

        ImageView badgeIcon = dialogView.findViewById(R.id.imageView_dialog_badge_icon);
        TextView badgeName = dialogView.findViewById(R.id.textView_dialog_badge_name);
        TextView badgeDescription = dialogView.findViewById(R.id.textView_dialog_badge_description);
        TextView badgeCriteria = dialogView.findViewById(R.id.textView_dialog_badge_criteria);

        if (getArguments() != null) {
            badgeName.setText(getArguments().getString(ARG_BADGE_NAME));
            badgeDescription.setText(getArguments().getString(ARG_BADGE_DESC));
            badgeIcon.setImageResource(getArguments().getInt(ARG_BADGE_IMAGE_RES, R.drawable.star)); // Дефолтная иконка
            badgeCriteria.setText("Критерий: " + getArguments().getString(ARG_BADGE_CRITERIA));
        }

        builder.setView(dialogView)
                .setPositiveButton("Понятно", (d, id) -> d.dismiss());
        return builder.create();
    }
}