package com.example.projectquestonjava.feature.gamification.presentation.adapters; // Уточни пакет

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.feature.gamification.presentation.utils.GamificationUiUtils;
import com.example.projectquestonjava.feature.gamification.presentation.viewmodels.ChallengeCardInfo;
import com.google.android.material.card.MaterialCardView;
import java.util.Objects;

public class ActiveChallengesPagerAdapter extends ListAdapter<ChallengeCardInfo, ActiveChallengesPagerAdapter.ChallengePagerViewHolder> {

    private final OnChallengePagerItemClickListener listener;

    public interface OnChallengePagerItemClickListener {
        void onChallengePagerItemClicked(ChallengeCardInfo info);
    }

    public ActiveChallengesPagerAdapter(@NonNull OnChallengePagerItemClickListener listener) {
        super(CHALLENGE_INFO_DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChallengePagerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_challenge_card_pager, parent, false);
        return new ChallengePagerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChallengePagerViewHolder holder, int position) {
        ChallengeCardInfo info = getItem(position);
        if (info != null) {
            holder.bind(info, listener);
        }
    }

    static class ChallengePagerViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView iconView, deadlineIconView, rewardIconView;
        TextView nameView, periodView, descriptionView, progressTextView, deadlineTextView, rewardNameView;
        ProgressBar progressBar;
        LinearLayout deadlineLayout, rewardLayout;

        ChallengePagerViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_challenge_pager_item_gamif); // ID из макета
            iconView = itemView.findViewById(R.id.imageView_challenge_pager_icon_gamif);
            nameView = itemView.findViewById(R.id.textView_challenge_pager_name_gamif);
            periodView = itemView.findViewById(R.id.textView_challenge_pager_period_gamif);
            descriptionView = itemView.findViewById(R.id.textView_challenge_pager_description_gamif);
            progressBar = itemView.findViewById(R.id.progressBar_challenge_pager_gamif);
            progressTextView = itemView.findViewById(R.id.textView_challenge_pager_progress_text_gamif);
            deadlineLayout = itemView.findViewById(R.id.layout_challenge_pager_deadline_gamif);
            deadlineIconView = itemView.findViewById(R.id.imageView_challenge_pager_deadline_icon_gamif);
            deadlineTextView = itemView.findViewById(R.id.textView_challenge_pager_deadline_text_gamif);
            rewardLayout = itemView.findViewById(R.id.layout_challenge_pager_reward_gamif);
            rewardIconView = itemView.findViewById(R.id.imageView_challenge_pager_reward_icon_gamif);
            rewardNameView = itemView.findViewById(R.id.textView_challenge_pager_reward_name_gamif);
        }

        void bind(final ChallengeCardInfo info, final OnChallengePagerItemClickListener listener) {
            Context context = itemView.getContext();

            nameView.setText(info.name());
            periodView.setText(GamificationUiUtils.getLocalizedPeriodNameJava(info.period()));
            descriptionView.setText(info.description());
            descriptionView.setVisibility(info.description() != null && !info.description().isEmpty() ? View.VISIBLE : View.GONE);

            progressBar.setProgress((int) (info.progress() * 100));
            progressTextView.setText(info.progressText());

            boolean isCompleted = info.progress() >= 1.0f;
            int progressColor = ContextCompat.getColor(context,
                    isCompleted ? R.color.secondaryLight : (info.isUrgent() ? R.color.errorLight : R.color.primaryLight)
            );

            iconView.setImageResource(info.iconResId());
            ImageViewCompat.setImageTintList(iconView, ColorStateList.valueOf(progressColor));

            // Установка цвета ProgressBar
            Drawable progressDrawable = ContextCompat.getDrawable(context, R.drawable.timer_progress_drawable); // Используем общий drawable
            if (progressDrawable != null) {
                Drawable wrapDrawable = progressDrawable.mutate();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    wrapDrawable.setColorFilter(new android.graphics.BlendModeColorFilter(progressColor, android.graphics.BlendMode.SRC_IN));
                } else {
                    wrapDrawable.setColorFilter(progressColor, PorterDuff.Mode.SRC_IN);
                }
                progressBar.setProgressDrawable(wrapDrawable);
            }
            // Установка фона ProgressBar (трека)
            Drawable trackDrawable = ContextCompat.getDrawable(context, R.drawable.timer_progress_track_drawable);
            if (trackDrawable != null) {
                progressBar.setProgressBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.surfaceVariantLight))); // Пример цвета трека
            }


            if (info.deadlineText() != null) {
                deadlineLayout.setVisibility(View.VISIBLE);
                deadlineTextView.setText(info.deadlineText());
                int deadlineColor = ContextCompat.getColor(context, info.isUrgent() ? R.color.errorLight : R.color.onSurfaceVariantDark);
                deadlineTextView.setTextColor(deadlineColor);
                ImageViewCompat.setImageTintList(deadlineIconView, ColorStateList.valueOf(deadlineColor));
            } else {
                deadlineLayout.setVisibility(View.GONE);
            }

            if (info.rewardIconResId() != null) {
                rewardLayout.setVisibility(View.VISIBLE);
                rewardIconView.setImageResource(info.rewardIconResId());
                ImageViewCompat.setImageTintList(rewardIconView, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.secondaryLight)));
                if (info.rewardName() != null && info.deadlineText() == null) {
                    rewardNameView.setText(info.rewardName());
                    rewardNameView.setVisibility(View.VISIBLE);
                } else {
                    rewardNameView.setVisibility(View.GONE);
                }
            } else {
                rewardLayout.setVisibility(View.GONE);
            }

            // Настройка фона и рамки карточки
            cardView.setStrokeColor(ContextCompat.getColor(context, (info.isUrgent() && !isCompleted) ? R.color.errorLight : android.R.color.transparent));
            // cardView.setCardBackgroundColor(...); // Если нужно менять цвет фона карточки

            cardView.setOnClickListener(v -> listener.onChallengePagerItemClicked(info));
        }
    }

    private static final DiffUtil.ItemCallback<ChallengeCardInfo> CHALLENGE_INFO_DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ChallengeCardInfo>() {
                @Override
                public boolean areItemsTheSame(@NonNull ChallengeCardInfo oldItem, @NonNull ChallengeCardInfo newItem) {
                    return oldItem.id() == newItem.id();
                }

                @Override
                public boolean areContentsTheSame(@NonNull ChallengeCardInfo oldItem, @NonNull ChallengeCardInfo newItem) {
                    // Сравниваем все поля ChallengeCardInfo
                    return Objects.equals(oldItem.name(), newItem.name()) &&
                            Objects.equals(oldItem.description(), newItem.description()) &&
                            oldItem.iconResId() == newItem.iconResId() &&
                            Float.compare(oldItem.progress(), newItem.progress()) == 0 &&
                            Objects.equals(oldItem.progressText(), newItem.progressText()) &&
                            Objects.equals(oldItem.deadlineText(), newItem.deadlineText()) &&
                            Objects.equals(oldItem.rewardIconResId(), newItem.rewardIconResId()) &&
                            Objects.equals(oldItem.rewardName(), newItem.rewardName()) &&
                            oldItem.isUrgent() == newItem.isUrgent() &&
                            oldItem.period().equals(newItem.period());
                }
            };
}