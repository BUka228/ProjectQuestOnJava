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
import android.view.ViewTreeObserver;
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
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.presentation.screens.ChallengesFragment;
import com.example.projectquestonjava.feature.gamification.presentation.screens.GamificationMainTabFragment;
import com.example.projectquestonjava.feature.gamification.presentation.utils.GamificationUiUtils;
import com.example.projectquestonjava.feature.gamification.presentation.viewmodels.ChallengeCardInfo;
import com.example.projectquestonjava.feature.gamification.presentation.viewmodels.GamificationViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject; // Для логгера по умолчанию, если ViewModel не получена

@AndroidEntryPoint
public class ChallengeDetailsBottomSheetFragment extends BottomSheetDialogFragment {

    private GamificationViewModel sharedViewModel; // Может быть null, если не удалось получить

    @Inject // Внедряем логгер напрямую во фрагмент
    Logger logger;

    private ImageView iconViewDetails, iconPeriod, iconDeadline, iconReward;
    private TextView nameViewDetails, descriptionViewDetails, progressTextViewDetails;
    private ProgressBar progressBarDetails;
    private TextView labelPeriod, valuePeriod, labelDeadline, valueDeadline, labelReward, valueReward;
    private LinearLayout layoutDetailsRoot;
    private View detailItemDeadlineViewRoot, detailItemRewardViewRoot;


    public static ChallengeDetailsBottomSheetFragment newInstance() {
        return new ChallengeDetailsBottomSheetFragment();
    }

    public ChallengeDetailsBottomSheetFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            // Пытаемся получить ViewModel, привязанную к Activity, как наиболее общий случай для shared ViewModel
            sharedViewModel = new ViewModelProvider(requireActivity()).get(GamificationViewModel.class);
            // Логгер уже должен быть внедрен Hilt'ом к этому моменту.
            // Если sharedViewModel получен, можно использовать его логгер, если он специфичен,
            // но logger, внедренный в фрагмент, должен быть доступен.
            if (logger != null) {
                logger.debug("ChallengeDetailsBS", "ViewModel obtained from Activity. VM Hash: " + (sharedViewModel != null ? sharedViewModel.hashCode() : "null VM object"));
            } else {
                android.util.Log.e("ChallengeDetailsBS", "Logger was not injected!");
            }
            if (sharedViewModel == null && logger != null) {
                logger.error("ChallengeDetailsBS", "Failed to obtain GamificationViewModel from Activity, sharedViewModel is null.");
            }

        } catch (Exception e) {
            // Эта ошибка означает, что Hilt не смог создать GamificationViewModel даже для Activity
            if (logger != null) {
                logger.error("ChallengeDetailsBS", "CRITICAL: Cannot create an instance of GamificationViewModel for Activity!", e);
            } else {
                android.util.Log.e("ChallengeDetailsBS", "CRITICAL: Cannot create an instance of GamificationViewModel for Activity! Logger also null.", e);
            }
            // Закрываем диалог, так как без ViewModel он бесполезен
            if (isAdded()) {
                dismissAllowingStateLoss();
            }
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
                BottomSheetBehavior.from(bottomSheetInternal).setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog instanceof BottomSheetDialog) {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialog;
            View bottomSheetInternal = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);

            if (bottomSheetInternal != null) {
                final BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheetInternal);
                bottomSheetInternal.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (bottomSheetInternal.getViewTreeObserver().isAlive()) {
                            bottomSheetInternal.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    }
                });
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (logger != null) logger.debug("ChallengeDetailsBS", "onCreateView");
        return inflater.inflate(R.layout.bottom_sheet_challenge_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (logger != null) logger.debug("ChallengeDetailsBS", "onViewCreated");
        bindViews(view);

        if (sharedViewModel == null) {
            if (logger != null) logger.error("ChallengeDetailsBS", "sharedViewModel is NULL in onViewCreated. Cannot observe.");
            // Диалог мог быть уже закрыт в onCreate, но на всякий случай
            if (isAdded() && !isStateSaved()) { // Добавил !isStateSaved()
                dismissAllowingStateLoss();
            }
            return;
        }

        sharedViewModel.challengeToShowDetails.observe(getViewLifecycleOwner(), info -> {
            if (logger != null) logger.debug("ChallengeDetailsBS", "challengeToShowDetails observed. Info is " + (info == null ? "null" : "NOT NULL"));
            if (info != null) {
                if (logger != null) logger.info("ChallengeDetailsBS", "Populating views with Challenge ID: " + info.id() + ", Name: " + info.name());
                layoutDetailsRoot.setVisibility(View.VISIBLE);
                populateViews(info);
            } else {
                if (logger != null) logger.warn("ChallengeDetailsBS", "ChallengeInfo is null. If BottomSheet is visible, it will appear empty or attempt to dismiss.");
                if (isVisible()) { // Если он все еще видим, а данных нет, закрываем.
                    dismissAllowingStateLoss();
                }
            }
        });
    }

    private void bindViews(View view) {
        layoutDetailsRoot = view.findViewById(R.id.layout_challenge_details_root);
        iconViewDetails = view.findViewById(R.id.imageView_challenge_details_icon);
        nameViewDetails = view.findViewById(R.id.textView_challenge_details_name);
        descriptionViewDetails = view.findViewById(R.id.textView_challenge_details_description);
        progressBarDetails = view.findViewById(R.id.progressBar_challenge_details);
        progressTextViewDetails = view.findViewById(R.id.textView_challenge_details_progress_text);

        View detailItemPeriodView = view.findViewById(R.id.detail_item_period_bs);
        iconPeriod = detailItemPeriodView.findViewById(R.id.imageView_detail_item_icon);
        labelPeriod = detailItemPeriodView.findViewById(R.id.textView_detail_item_label);
        valuePeriod = detailItemPeriodView.findViewById(R.id.textView_detail_item_value);

        detailItemDeadlineViewRoot = view.findViewById(R.id.detail_item_deadline_bs);
        iconDeadline = detailItemDeadlineViewRoot.findViewById(R.id.imageView_detail_item_icon);
        labelDeadline = detailItemDeadlineViewRoot.findViewById(R.id.textView_detail_item_label);
        valueDeadline = detailItemDeadlineViewRoot.findViewById(R.id.textView_detail_item_value);

        detailItemRewardViewRoot = view.findViewById(R.id.detail_item_reward_bs);
        iconReward = detailItemRewardViewRoot.findViewById(R.id.imageView_detail_item_icon);
        labelReward = detailItemRewardViewRoot.findViewById(R.id.textView_detail_item_label);
        valueReward = detailItemRewardViewRoot.findViewById(R.id.textView_detail_item_value);

        if (logger != null) logger.debug("ChallengeDetailsBS", "Views bound.");
    }

    private void populateViews(@NonNull ChallengeCardInfo info) {
        // ... (код populateViews остается без изменений, но с проверками logger != null перед вызовом) ...
        if (logger != null) logger.debug("ChallengeDetailsBS", "populateViews START for Challenge ID: " + info.id());
        Context context = getContext();
        if (context == null) {
            if (logger != null) logger.error("ChallengeDetailsBS", "Context is null in populateViews. Cannot proceed.");
            return;
        }

        boolean isCompleted = info.progress() >= 1.0f;
        int progressColor = ContextCompat.getColor(context,
                isCompleted ? R.color.secondaryLight : (info.isUrgent() ? R.color.errorLight : R.color.primaryLight)
        );

        iconViewDetails.setImageResource(info.iconResId());
        ImageViewCompat.setImageTintList(iconViewDetails, ColorStateList.valueOf(progressColor));
        nameViewDetails.setText(info.name());
        descriptionViewDetails.setText(info.description());


        progressBarDetails.setProgress((int) (info.progress() * 100));
        Drawable progressDrawable = ContextCompat.getDrawable(context, R.drawable.timer_progress_drawable);
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


        iconPeriod.setImageResource(R.drawable.update);
        labelPeriod.setText("Период:");
        valuePeriod.setText(GamificationUiUtils.getLocalizedPeriodNameJava(info.period()));
        ImageViewCompat.setImageTintList(iconPeriod, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.onSurfaceVariantDark)));
        valuePeriod.setTextColor(ContextCompat.getColor(context, R.color.onSurfaceDark));


        if (info.deadlineText() != null) {
            detailItemDeadlineViewRoot.setVisibility(View.VISIBLE);
            iconDeadline.setImageResource(R.drawable.schedule);
            labelDeadline.setText("Срок:");
            valueDeadline.setText(info.deadlineText());
            int deadlineColor = ContextCompat.getColor(context, info.isUrgent() ? R.color.errorLight : R.color.onSurfaceDark);
            valueDeadline.setTextColor(deadlineColor);
            ImageViewCompat.setImageTintList(iconDeadline, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.onSurfaceVariantDark)));
        } else {
            detailItemDeadlineViewRoot.setVisibility(View.GONE);
        }


        if (info.rewardIconResId() != null && info.rewardName() != null) {
            detailItemRewardViewRoot.setVisibility(View.VISIBLE);
            iconReward.setImageResource(info.rewardIconResId());
            labelReward.setText("Награда:");
            valueReward.setText(info.rewardName());
            valueReward.setTextColor(ContextCompat.getColor(context, R.color.secondaryLight));
            ImageViewCompat.setImageTintList(iconReward, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.secondaryLight)));
        } else {
            detailItemRewardViewRoot.setVisibility(View.GONE);
        }
        if (logger != null) logger.debug("ChallengeDetailsBS", "populateViews END");
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (logger != null) logger.debug("ChallengeDetailsBS", "onDismiss called.");
        if (sharedViewModel != null) {
            sharedViewModel.clearChallengeDetails();
        }
        Fragment parent = getParentFragment();
        if (parent instanceof GamificationMainTabFragment) {
            ((GamificationMainTabFragment) parent).onChallengeDetailsSheetDismissed();
        } else if (parent instanceof ChallengesFragment) {
            ((ChallengesFragment) parent).onDetailsSheetDismissed();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (logger != null) logger.debug("ChallengeDetailsBS", "onDestroyView");
        // ... (очистка View)
        iconViewDetails = null; nameViewDetails = null; descriptionViewDetails = null;
        progressBarDetails = null; progressTextViewDetails = null;
        iconPeriod = null; labelPeriod = null; valuePeriod = null;
        iconDeadline = null; labelDeadline = null; valueDeadline = null;
        iconReward = null; labelReward = null; valueReward = null;
        layoutDetailsRoot = null; detailItemDeadlineViewRoot = null; detailItemRewardViewRoot = null;
    }
}