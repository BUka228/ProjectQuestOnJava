package com.example.projectquestonjava.feature.gamification.presentation.ui_parts;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.feature.gamification.presentation.screens.GamificationMainTabFragment;
import com.example.projectquestonjava.feature.gamification.presentation.utils.GamificationUiUtils;
import com.example.projectquestonjava.feature.gamification.presentation.viewmodels.ChallengeCardInfo;
import com.example.projectquestonjava.feature.gamification.presentation.viewmodels.GamificationViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ChallengeDetailsBottomSheetFragment extends BottomSheetDialogFragment {

    private GamificationViewModel sharedViewModel; // ViewModel от родительского GamificationFragment

    // Views
    private ImageView iconViewDetails, iconPeriod, iconDeadline, iconReward;
    private TextView nameViewDetails, descriptionViewDetails, progressTextViewDetails;
    private ProgressBar progressBarDetails;
    private TextView labelPeriod, valuePeriod, labelDeadline, valueDeadline, labelReward, valueReward;
    private LinearLayout layoutDetailsRoot; // Корневой layout для управления видимостью

    // Статический конструктор не обязателен, если мы всегда получаем данные из ViewModel
    public static ChallengeDetailsBottomSheetFragment newInstance() {
        return new ChallengeDetailsBottomSheetFragment();
    }

    public ChallengeDetailsBottomSheetFragment() {
        // Пустой конструктор
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Получаем ViewModel от родительского фрагмента (GamificationFragment)
        // или от Activity, если BottomSheet вызывается из Activity
        try {
            sharedViewModel = new ViewModelProvider(requireParentFragment()).get(GamificationViewModel.class);
        } catch (IllegalStateException e) {
            // Если родительский фрагмент не найден, пытаемся получить от Activity
            // Это может быть полезно, если BottomSheet вызывается из разных мест
            sharedViewModel = new ViewModelProvider(requireActivity()).get(GamificationViewModel.class);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            BottomSheetDialog dInternal = (BottomSheetDialog) d;
            View bottomSheetInternal = dInternal.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheetInternal != null) {
                // Раскрываем полностью, если нужно
                BottomSheetBehavior.from(bottomSheetInternal).setState(BottomSheetBehavior.STATE_EXPANDED);
                // Можно установить peekHeight, если разрешено частичное раскрытие
                // BottomSheetBehavior.from(bottomSheetInternal).setPeekHeight(desiredPeekHeight);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_challenge_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);

        sharedViewModel.challengeToShowDetails.observe(getViewLifecycleOwner(), info -> {
            if (info != null) {
                layoutDetailsRoot.setVisibility(View.VISIBLE);
                populateViews(info);
            } else {
                // Если информация о челлендже null, но BottomSheet все еще виден, скрываем его
                if (isVisible()) {
                    dismissAllowingStateLoss();
                }
            }
        });
    }

    private void bindViews(View view) {
        layoutDetailsRoot = view.findViewById(R.id.layout_challenge_details_root); // ID корневого LinearLayout в bottom_sheet_challenge_details
        iconViewDetails = view.findViewById(R.id.imageView_challenge_details_icon);
        nameViewDetails = view.findViewById(R.id.textView_challenge_details_name);
        descriptionViewDetails = view.findViewById(R.id.textView_challenge_details_description);
        progressBarDetails = view.findViewById(R.id.progressBar_challenge_details);
        progressTextViewDetails = view.findViewById(R.id.textView_challenge_details_progress_text);

        View detailItemPeriodView = view.findViewById(R.id.detail_item_period_bs); // Уникальные ID для BottomSheet
        iconPeriod = detailItemPeriodView.findViewById(R.id.imageView_detail_item_icon);
        labelPeriod = detailItemPeriodView.findViewById(R.id.textView_detail_item_label);
        valuePeriod = detailItemPeriodView.findViewById(R.id.textView_detail_item_value);

        View detailItemDeadlineView = view.findViewById(R.id.detail_item_deadline_bs);
        iconDeadline = detailItemDeadlineView.findViewById(R.id.imageView_detail_item_icon);
        labelDeadline = detailItemDeadlineView.findViewById(R.id.textView_detail_item_label);
        valueDeadline = detailItemDeadlineView.findViewById(R.id.textView_detail_item_value);
        // Скрываем по умолчанию, пока не будет данных
        detailItemDeadlineView.setVisibility(View.GONE);


        View detailItemRewardView = view.findViewById(R.id.detail_item_reward_bs);
        iconReward = detailItemRewardView.findViewById(R.id.imageView_detail_item_icon);
        labelReward = detailItemRewardView.findViewById(R.id.textView_detail_item_label);
        valueReward = detailItemRewardView.findViewById(R.id.textView_detail_item_value);
        // Скрываем по умолчанию
        detailItemRewardView.setVisibility(View.GONE);

    }

    private void populateViews(@NonNull ChallengeCardInfo info) {
        Context context = requireContext();
        boolean isCompleted = info.progress() >= 1.0f;
        int progressColor = ContextCompat.getColor(context,
                isCompleted ? R.color.secondaryLight : (info.isUrgent() ? R.color.errorLight : R.color.primaryLight)
        );

        iconViewDetails.setImageResource(info.iconResId());
        ImageViewCompat.setImageTintList(iconViewDetails, ColorStateList.valueOf(progressColor));
        nameViewDetails.setText(info.name());
        descriptionViewDetails.setText(info.description());

        progressBarDetails.setProgress((int) (info.progress() * 100));
        // progressBarDetails.setProgressTintList(ColorStateList.valueOf(progressColor)); // Для API 21+
        Drawable progressDrawable = ContextCompat.getDrawable(context, R.drawable.timer_progress_drawable); // Используем общий drawable
        if (progressDrawable != null) {
            Drawable wrapDrawable = progressDrawable.mutate();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                wrapDrawable.setColorFilter(new android.graphics.BlendModeColorFilter(progressColor, android.graphics.BlendMode.SRC_IN));
            } else {
                wrapDrawable.setColorFilter(progressColor, PorterDuff.Mode.SRC_IN);
            }
            progressBarDetails.setProgressDrawable(wrapDrawable);
        }

        progressTextViewDetails.setText(info.progressText());

        // Период
        iconPeriod.setImageResource(R.drawable.update);
        labelPeriod.setText("Период:");
        valuePeriod.setText(GamificationUiUtils.getLocalizedPeriodNameJava(info.period()));
        ImageViewCompat.setImageTintList(iconPeriod, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.onSurfaceVariantDark)));
        valuePeriod.setTextColor(ContextCompat.getColor(context, R.color.onSurfaceDark));


        // Срок
        View deadlineView = getView().findViewById(R.id.detail_item_deadline_bs);
        if (info.deadlineText() != null) {
            deadlineView.setVisibility(View.VISIBLE);
            iconDeadline.setImageResource(R.drawable.schedule);
            labelDeadline.setText("Срок:");
            valueDeadline.setText(info.deadlineText());
            int deadlineColor = ContextCompat.getColor(context, info.isUrgent() ? R.color.errorLight : R.color.onSurfaceDark);
            valueDeadline.setTextColor(deadlineColor);
            ImageViewCompat.setImageTintList(iconDeadline, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.onSurfaceVariantDark)));

        } else {
            deadlineView.setVisibility(View.GONE);
        }

        // Награда
        View rewardView = getView().findViewById(R.id.detail_item_reward_bs);
        if (info.rewardIconResId() != null && info.rewardName() != null) {
            rewardView.setVisibility(View.VISIBLE);
            iconReward.setImageResource(info.rewardIconResId());
            labelReward.setText("Награда:");
            valueReward.setText(info.rewardName());
            valueReward.setTextColor(ContextCompat.getColor(context, R.color.secondaryLight));
            ImageViewCompat.setImageTintList(iconReward, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.secondaryLight)));
        } else {
            rewardView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        // Сообщаем ViewModel, что BottomSheet закрыт
        if (sharedViewModel != null) {
            sharedViewModel.clearChallengeDetails();
        }
        // Если этот BottomSheet вызывался из GamificationMainTabFragment,
        // то можно также уведомить его, чтобы он сбросил флаг isChallengeDetailsSheetShown
        Fragment parent = getParentFragment();
        if (parent instanceof GamificationMainTabFragment) {
            ((GamificationMainTabFragment) parent).onChallengeDetailsSheetDismissed();
        } /*else if (parent instanceof ChallengesFragment) { // Если вызывается из ChallengesFragment
            //((ChallengesFragment) parent).onDetailsSheetDismissed(); // Нужен аналогичный метод
        }*/
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Очищаем View
        iconViewDetails = null; nameViewDetails = null; descriptionViewDetails = null;
        progressBarDetails = null; progressTextViewDetails = null;
        iconPeriod = null; labelPeriod = null; valuePeriod = null;
        iconDeadline = null; labelDeadline = null; valueDeadline = null;
        iconReward = null; labelReward = null; valueReward = null;
        layoutDetailsRoot = null;
    }
}