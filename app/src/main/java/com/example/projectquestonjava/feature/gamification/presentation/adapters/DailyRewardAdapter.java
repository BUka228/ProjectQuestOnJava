package com.example.projectquestonjava.feature.gamification.presentation.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log; // Используем android.util.Log для простоты в адаптере
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
import com.example.projectquestonjava.feature.gamification.presentation.utils.GamificationUiUtils;

import java.util.Locale;
import java.util.Objects;

public class DailyRewardAdapter extends ListAdapter<Reward, DailyRewardAdapter.RewardViewHolder> {

    private static final String ADAPTER_TAG = "DailyRewardAdapter"; // Тег для логов

    private final OnDailyRewardClickListener listener;
    private int currentStreak;
    private boolean canClaimToday;
    private int todayStreakDay;
    private int currentWeekStartStreak;

    public interface OnDailyRewardClickListener {
        void onDailyRewardClick(Reward reward, int rewardStreakDay, boolean isTodayAndCanClaim); // Изменил параметр
        void onClaimTodayRewardClick();
    }

    public DailyRewardAdapter(@NonNull OnDailyRewardClickListener listener) {
        super(REWARD_DIFF_CALLBACK);
        this.listener = listener;
    }

    public void updateDailyState(int currentStreak, boolean canClaimToday, int todayStreakDay, int displayWeekStartStreak) {
        Log.d(ADAPTER_TAG, "updateDailyState: currentStreak=" + currentStreak + ", canClaimToday=" + canClaimToday + ", todayStreakDay=" + todayStreakDay + ", displayWeekStartStreak=" + displayWeekStartStreak);
        boolean changed = this.currentStreak != currentStreak ||
                this.canClaimToday != canClaimToday ||
                this.todayStreakDay != todayStreakDay ||
                this.currentWeekStartStreak != displayWeekStartStreak;
        this.currentStreak = currentStreak;
        this.canClaimToday = canClaimToday;
        this.todayStreakDay = todayStreakDay;
        this.currentWeekStartStreak = displayWeekStartStreak;
        if (changed) {
            notifyDataSetChanged();
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
        int rewardActualStreakDay = currentWeekStartStreak + position;
        Log.d(ADAPTER_TAG, "onBindViewHolder: position=" + position + ", rewardActualStreakDay=" + rewardActualStreakDay + ", rewardName=" + (reward != null ? reward.getName() : "null"));
        holder.bind(reward, rewardActualStreakDay, currentStreak, canClaimToday, todayStreakDay, listener);
    }

    static class RewardViewHolder extends RecyclerView.ViewHolder {
        FrameLayout iconBackground;
        ImageView iconView;
        ImageView checkView;
        TextView dayTextView;

        RewardViewHolder(@NonNull View itemView) {
            super(itemView);
            iconBackground = itemView.findViewById(R.id.frameLayout_reward_icon_background);
            iconView = itemView.findViewById(R.id.imageView_reward_icon);
            checkView = itemView.findViewById(R.id.imageView_reward_check);
            dayTextView = itemView.findViewById(R.id.textView_reward_day);
        }

        void bind(final Reward reward, final int rewardActualStreakDay, int currentStreak,
                  boolean canClaimToday, int todayStreakDay, final OnDailyRewardClickListener listener) {
            Context context = itemView.getContext();

            final boolean isThisTodayAndCanClaim = rewardActualStreakDay == todayStreakDay && canClaimToday;
            boolean isClaimed = rewardActualStreakDay <= currentStreak && !(isThisTodayAndCanClaim);
            boolean isFutureOrSkipped = rewardActualStreakDay > todayStreakDay || (rewardActualStreakDay < todayStreakDay && !isClaimed);

            Log.d(ADAPTER_TAG, "bind: rewardDay=" + rewardActualStreakDay +
                    ", todayStreakDay=" + todayStreakDay +
                    ", canClaimToday=" + canClaimToday +
                    ", currentStreak=" + currentStreak +
                    " -> isThisTodayAndCanClaim=" + isThisTodayAndCanClaim +
                    ", isClaimed=" + isClaimed +
                    ", isFutureOrSkipped=" + isFutureOrSkipped +
                    ", rewardName=" + (reward != null ? reward.getName() : "null"));


            dayTextView.setText(String.format(Locale.getDefault(), "День %d", rewardActualStreakDay));

            if (reward == null) { // Защита от null reward
                Log.e(ADAPTER_TAG, "bind: Reward object is null for rewardActualStreakDay=" + rewardActualStreakDay);
                // Можно установить какие-то дефолтные значения или скрыть элемент
                itemView.setVisibility(View.GONE); // Например, скрыть
                return;
            }
            itemView.setVisibility(View.VISIBLE);


            Drawable rewardIconDrawable = GamificationUiUtils.getIconForRewardTypeDrawable(reward.getRewardType(), context);
            if (rewardIconDrawable != null) {
                iconView.setImageDrawable(rewardIconDrawable);
            } else {
                iconView.setImageResource(R.drawable.help);
            }

            int backgroundColorId;
            int iconColorId;
            int textColorId;
            float alphaValue = 1.0f;
            float scale = 1.0f;
            boolean showCheck = false;

            if (isClaimed) {
                backgroundColorId = R.color.successContainerDark;
                iconColorId = R.color.onSuccessContainerDark;
                textColorId = R.color.onSuccessContainerDark;
                alphaValue = 0.7f;
                showCheck = true;
                itemView.clearAnimation();
            } else if (isThisTodayAndCanClaim) {
                backgroundColorId = R.color.primaryDark;
                iconColorId = R.color.onPrimaryDark;
                textColorId = R.color.onPrimaryDark;
                scale = 1.05f;
                applyPulseAnimation(itemView);
            } else {
                backgroundColorId = R.color.surfaceVariantDark;
                iconColorId = R.color.onSurfaceVariantDark;
                textColorId = R.color.onSurfaceVariantDark;
                alphaValue = isFutureOrSkipped ? 0.6f : 1.0f;
                itemView.clearAnimation();
            }

            Drawable background = ContextCompat.getDrawable(context, R.drawable.daily_reward_background);
            if (background != null) {
                Drawable mutatedBackground = background.mutate();
                if (mutatedBackground instanceof GradientDrawable) {
                    ((GradientDrawable) mutatedBackground).setColor(ContextCompat.getColor(context, backgroundColorId));
                } else {
                    mutatedBackground.setColorFilter(ContextCompat.getColor(context, backgroundColorId), PorterDuff.Mode.SRC_IN);
                }
                iconBackground.setBackground(mutatedBackground);
            }
            iconBackground.setAlpha(alphaValue);

            ImageViewCompat.setImageTintList(iconView, ColorStateList.valueOf(ContextCompat.getColor(context, iconColorId)));
            dayTextView.setTextColor(ContextCompat.getColor(context, textColorId));
            checkView.setVisibility(showCheck ? View.VISIBLE : View.GONE);

            itemView.setScaleX(scale);
            itemView.setScaleY(scale);

            itemView.setOnClickListener(v -> {
                Log.d(ADAPTER_TAG, "itemView onClick: rewardDay=" + rewardActualStreakDay + ", isThisTodayAndCanClaim=" + isThisTodayAndCanClaim);
                if (isThisTodayAndCanClaim) {
                    Log.d(ADAPTER_TAG, "Calling listener.onClaimTodayRewardClick()");
                    listener.onClaimTodayRewardClick();
                } else {
                    Log.d(ADAPTER_TAG, "Calling listener.onDailyRewardClick() for reward: " + reward.getName());
                    listener.onDailyRewardClick(reward, rewardActualStreakDay, isThisTodayAndCanClaim);
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