package com.example.projectquestonjava.feature.gamification.presentation.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.feature.gamification.data.model.Reward;

import java.util.Locale;
import java.util.Objects;

public class DailyRewardAdapter extends ListAdapter<Reward, DailyRewardAdapter.RewardViewHolder> {

    private final OnDailyRewardClickListener listener;
    private int currentStreak;
    private boolean canClaimToday;
    private int todayStreakDay; // Фактический день стрика для "сегодня"
    private int currentWeekStartStreak; // Начальный день стрика для текущей отображаемой недели/набора

    public interface OnDailyRewardClickListener {
        void onDailyRewardClick(Reward reward, int rewardStreakDay, boolean isToday);
        void onClaimTodayRewardClick(); // Для кнопки "Получить"
    }

    public DailyRewardAdapter(@NonNull OnDailyRewardClickListener listener) {
        super(REWARD_DIFF_CALLBACK);
        this.listener = listener;
    }

    public void updateDailyState(int currentStreak, boolean canClaimToday, int todayStreakDay, int displayWeekStartStreak) {
        boolean changed = this.currentStreak != currentStreak ||
                this.canClaimToday != canClaimToday ||
                this.todayStreakDay != todayStreakDay ||
                this.currentWeekStartStreak != displayWeekStartStreak;
        this.currentStreak = currentStreak;
        this.canClaimToday = canClaimToday;
        this.todayStreakDay = todayStreakDay;
        this.currentWeekStartStreak = displayWeekStartStreak;
        if (changed) {
            notifyDataSetChanged(); // Простое обновление, т.к. меняется состояние всех элементов
        }
    }


    @NonNull
    @Override
    public RewardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_daily_reward, parent, false);
        return new RewardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RewardViewHolder holder, int position) {
        Reward reward = getItem(position);
        // Рассчитываем фактический день стрика для этого элемента награды
        int rewardActualStreakDay = currentWeekStartStreak + position;
        holder.bind(reward, rewardActualStreakDay, currentStreak, canClaimToday, todayStreakDay, listener);
    }

    static class RewardViewHolder extends RecyclerView.ViewHolder {
        LinearLayout itemLayout;
        FrameLayout iconBackground;
        ImageView iconView;
        ImageView checkView;
        TextView dayTextView;

        RewardViewHolder(@NonNull View itemView) {
            super(itemView);
            itemLayout = (LinearLayout) itemView; // Корневой элемент
            iconBackground = itemView.findViewById(R.id.frameLayout_reward_icon_background);
            iconView = itemView.findViewById(R.id.imageView_reward_icon);
            checkView = itemView.findViewById(R.id.imageView_reward_check);
            dayTextView = itemView.findViewById(R.id.textView_reward_day);
        }

        void bind(final Reward reward, final int rewardActualStreakDay, int currentStreak,
                  boolean canClaimToday, int todayStreakDay, final OnDailyRewardClickListener listener) {
            Context context = itemView.getContext();

            boolean isThisToday = rewardActualStreakDay == todayStreakDay;
            boolean isClaimed = rewardActualStreakDay <= currentStreak && (!isThisToday || (isThisToday && !canClaimToday));
            // Элемент "сегодняшний", если его день стрика совпадает с todayStreakDay И можно забрать награду
            boolean isTodayAndClaimable = isThisToday && canClaimToday;


            dayTextView.setText(String.format(Locale.getDefault(), "День %d", rewardActualStreakDay));

            // Иконка награды
            Drawable rewardIconDrawable = com.example.projectquestonjava.feature.gamification.presentation.utils.GamificationUiUtils.getIconForRewardTypeDrawable(reward.getRewardType(), context);
            if (rewardIconDrawable != null) {
                iconView.setImageDrawable(rewardIconDrawable);
            } else {
                iconView.setImageResource(R.drawable.help); // Default
            }

            // Стилизация
            int backgroundColor;
            int iconColor;
            int textColor;
            float scale = 1.0f;
            boolean showCheck = false;

            if (isClaimed) {
                backgroundColor = ContextCompat.getColor(context, R.color.secondaryLight); // Зеленый
                Drawable bg = ContextCompat.getDrawable(context, R.drawable.rounded_corner_background_12dp_with_alpha);
                if (bg instanceof GradientDrawable) ((GradientDrawable)bg.mutate()).setColor(backgroundColor); else iconBackground.setBackgroundColor(backgroundColor);
                iconBackground.setAlpha(0.15f);

                iconColor = ContextCompat.getColor(context, R.color.secondaryLight);
                textColor = ContextCompat.getColor(context, R.color.secondaryLight);
                showCheck = true;
            } else if (isTodayAndClaimable) {
                backgroundColor = ContextCompat.getColor(context, R.color.primaryLight);
                iconBackground.setBackgroundColor(backgroundColor);
                iconBackground.setAlpha(1f);

                iconColor = ContextCompat.getColor(context, R.color.onPrimaryLight);
                textColor = ContextCompat.getColor(context, R.color.onPrimaryContainerLight); // Для текста "День N"
                scale = 1.05f; // Небольшое увеличение для "сегодня"
                // Анимация пульсации для isTodayAndClaimable
                applyPulseAnimation(itemView);
            } else { // Будущие или пропущенные (если стрик сброшен)
                backgroundColor = ContextCompat.getColor(context, R.color.surfaceVariantLight);
                iconBackground.setBackgroundColor(backgroundColor);
                iconBackground.setAlpha(0.7f);

                iconColor = ContextCompat.getColor(context, R.color.onSurfaceVariantDark);
                textColor = ContextCompat.getColor(context, R.color.onSurfaceVariantDark);
                itemView.clearAnimation(); // Убираем анимацию, если была
            }

            ImageViewCompat.setImageTintList(iconView, ColorStateList.valueOf(iconColor));
            dayTextView.setTextColor(textColor);
            checkView.setVisibility(showCheck ? View.VISIBLE : View.GONE);

            itemView.setScaleX(scale);
            itemView.setScaleY(scale);

            itemView.setOnClickListener(v -> {
                if (isTodayAndClaimable) {
                    listener.onClaimTodayRewardClick();
                } else {
                    listener.onDailyRewardClick(reward, rewardActualStreakDay, isTodayAndClaimable);
                }
            });
        }

        private void applyPulseAnimation(View view) {
            AnimationSet set = new AnimationSet(true);
            ScaleAnimation scaleUp = new ScaleAnimation(1f, 1.03f, 1f, 1.03f,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            scaleUp.setDuration(800);
            scaleUp.setInterpolator(new AccelerateDecelerateInterpolator());
            scaleUp.setRepeatMode(Animation.REVERSE);
            scaleUp.setRepeatCount(Animation.INFINITE);
            set.addAnimation(scaleUp);
            view.startAnimation(set);
        }
    }

    private static final DiffUtil.ItemCallback<Reward> REWARD_DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Reward>() {
                @Override
                public boolean areItemsTheSame(@NonNull Reward oldItem, @NonNull Reward newItem) {
                    return oldItem.getId() == newItem.getId();
                }
                @Override
                public boolean areContentsTheSame(@NonNull Reward oldItem, @NonNull Reward newItem) {
                    return oldItem.equals(newItem);
                }
            };
}