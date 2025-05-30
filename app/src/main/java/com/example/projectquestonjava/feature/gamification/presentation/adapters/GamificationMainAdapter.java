package com.example.projectquestonjava.feature.gamification.presentation.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.core.commonUi.components.WormPagerIndicatorView;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.core.utils.PlantResources;
import com.example.projectquestonjava.feature.gamification.data.model.Gamification;
import com.example.projectquestonjava.feature.gamification.data.model.Reward;
import com.example.projectquestonjava.feature.gamification.data.model.SurpriseTask;
import com.example.projectquestonjava.feature.gamification.data.model.VirtualGarden;
import com.example.projectquestonjava.feature.gamification.domain.model.DailyRewardsInfo;
import com.example.projectquestonjava.feature.gamification.domain.model.PlantHealthState;
import com.example.projectquestonjava.feature.gamification.presentation.utils.GamificationUiUtils;
import com.example.projectquestonjava.feature.gamification.presentation.viewmodels.ActiveChallengesSectionState;
import com.example.projectquestonjava.feature.gamification.presentation.viewmodels.ChallengeCardInfo;
import com.example.projectquestonjava.feature.gamification.presentation.viewmodels.GamificationViewModel;
import com.google.android.material.card.MaterialCardView;
// Убрал ненужные импорты TabLayout, если они были
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import lombok.Setter;

public class GamificationMainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // Изменяем порядок VIEW_TYPE констант
    private static final int VIEW_TYPE_PLANT_STATS = 0;
    private static final int VIEW_TYPE_DAILY_REWARDS = 1;
    private static final int VIEW_TYPE_CHALLENGES = 2;
    private static final int VIEW_TYPE_SURPRISE_TASK = 3;
    @Setter
    private Gamification gamificationData;
    @Setter
    private VirtualGarden plantData;
    @Setter
    private PlantHealthState plantHealthState = PlantHealthState.HEALTHY;
    private boolean canWaterPlant = false;
    @Setter
    private GamificationViewModel.Pair<SurpriseTask, Reward> surpriseTaskData;
    @Setter
    private DailyRewardsInfo dailyRewardsData;
    private ActiveChallengesSectionState activeChallengesData;

    private final GamificationItemClickListener listener;
    private final Context context;
    private final Logger logger;

    public interface GamificationItemClickListener {
        void onPlantWidgetClick();
        void onWaterPlantClick();
        void onAcceptSurpriseTaskClick(SurpriseTask task);
        void onHideSurpriseTaskClick(long taskId);
        void onClaimDailyRewardClick();
        void onViewAllChallengesClick();
        void onChallengeCardClick(ChallengeCardInfo challengeInfo);
        void onDailyRewardItemClick(Reward reward, int rewardStreakDay, boolean isToday);
    }

    public GamificationMainAdapter(GamificationItemClickListener listener, Context context, Logger logger) {
        this.listener = listener;
        this.context = context;
        this.logger = logger;
        this.activeChallengesData = new ActiveChallengesSectionState();
    }

    public void setCanWater(boolean canWater) { this.canWaterPlant = canWater; }

    public void setActiveChallengesData(ActiveChallengesSectionState data) {
        this.activeChallengesData = data != null ? data : new ActiveChallengesSectionState();
    }

    public void notifyDataSetChangedHack() {
        new Handler(Looper.getMainLooper()).post(this::notifyDataSetChanged);
    }

    @Override
    public int getItemViewType(int position) {
        return position; // Используем позицию как тип
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case VIEW_TYPE_PLANT_STATS:
                return new PlantStatsViewHolder(inflater.inflate(R.layout.item_gamification_header_stats_plant, parent, false));
            case VIEW_TYPE_DAILY_REWARDS: // Порядок изменен
                return new DailyRewardsViewHolder(inflater.inflate(R.layout.item_gamification_daily_rewards, parent, false), listener, context);
            case VIEW_TYPE_CHALLENGES:    // Порядок изменен
                return new ActiveChallengesViewHolder(inflater.inflate(R.layout.item_gamification_active_challenges, parent, false), listener, context);
            case VIEW_TYPE_SURPRISE_TASK: // Порядок изменен
                return new SurpriseTaskViewHolder(inflater.inflate(R.layout.item_gamification_surprise_task, parent, false));
            default:
                throw new IllegalArgumentException("Invalid view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_PLANT_STATS:
                ((PlantStatsViewHolder) holder).bind(plantData, gamificationData, plantHealthState, canWaterPlant, listener);
                break;
            case VIEW_TYPE_DAILY_REWARDS: // Порядок изменен
                ((DailyRewardsViewHolder) holder).bind(dailyRewardsData);
                break;
            case VIEW_TYPE_CHALLENGES:    // Порядок изменен
                ((ActiveChallengesViewHolder) holder).bind(activeChallengesData);
                break;
            case VIEW_TYPE_SURPRISE_TASK: // Порядок изменен
                ((SurpriseTaskViewHolder) holder).bind(surpriseTaskData, listener);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }

    // --- ViewHolder-ы (остаются без изменений, кроме порядка их вызова) ---
    static class PlantStatsViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView miniStatsCard, plantWidgetCard;
        TextView levelValue, xpValue, xpMax, coinsValue;
        ProgressBar xpProgressBar;
        ImageView plantImageView, coinsIcon, levelIcon, xpIconContainerIcon;
        com.google.android.material.floatingactionbutton.FloatingActionButton fabWaterPlant;

        PlantStatsViewHolder(@NonNull View itemView) {
            super(itemView);
            miniStatsCard = itemView.findViewById(R.id.card_mini_stats_gamif);
            levelValue = miniStatsCard.findViewById(R.id.textView_level_value_gamif);
            xpValue = miniStatsCard.findViewById(R.id.textView_xp_value_gamif);
            xpMax = miniStatsCard.findViewById(R.id.textView_xp_max_gamif);
            coinsValue = miniStatsCard.findViewById(R.id.textView_coins_value_gamif);
            xpProgressBar = miniStatsCard.findViewById(R.id.progressBar_xp_gamif);
            coinsIcon = miniStatsCard.findViewById(R.id.imageView_info_icon_coins);
            levelIcon = miniStatsCard.findViewById(R.id.imageView_info_icon_level);
            xpIconContainerIcon = miniStatsCard.findViewById(R.id.imageView_xp_icon_container);

            plantWidgetCard = itemView.findViewById(R.id.card_plant_widget_gamif);
            plantImageView = plantWidgetCard.findViewById(R.id.imageView_plant_gamif);
            fabWaterPlant = plantWidgetCard.findViewById(R.id.fab_water_plant_gamif);
        }

        void bind(@Nullable VirtualGarden plant, @Nullable Gamification gamification,
                  PlantHealthState healthState, boolean canWater,
                  final GamificationItemClickListener listener) {
            Context context = itemView.getContext();
            if (gamification != null) {
                miniStatsCard.setVisibility(View.VISIBLE);
                levelValue.setText(String.valueOf(gamification.getLevel()));
                xpValue.setText(String.valueOf(gamification.getExperience()));
                xpMax.setText(String.format(Locale.getDefault(), "/ %d", gamification.getMaxExperienceForLevel()));
                coinsValue.setText(String.valueOf(gamification.getCoins()));
                if (gamification.getMaxExperienceForLevel() > 0) {
                    xpProgressBar.setProgress((int) ((float) gamification.getExperience() / gamification.getMaxExperienceForLevel() * 100));
                } else {
                    xpProgressBar.setProgress(0);
                }
                if(levelIcon != null) levelIcon.setImageResource(R.drawable.star);
                if(xpIconContainerIcon != null) xpIconContainerIcon.setImageResource(R.drawable.trending_up);
                if(coinsIcon != null) coinsIcon.setImageResource(R.drawable.paid);

            } else {
                miniStatsCard.setVisibility(View.GONE);
            }

            if (plant != null) {
                plantWidgetCard.setVisibility(View.VISIBLE);
                plantImageView.setImageResource(PlantResources.getPlantImageResId(plant.getPlantType(), plant.getGrowthStage(), healthState));
                fabWaterPlant.setEnabled(canWater);
                fabWaterPlant.setOnClickListener(v -> listener.onWaterPlantClick());
                plantWidgetCard.setOnClickListener(v -> listener.onPlantWidgetClick());
                fabWaterPlant.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(context, canWater ? R.color.primaryContainerLight : R.color.surfaceVariantDark)
                ));
            } else {
                plantWidgetCard.setVisibility(View.GONE);
            }
        }
    }

    static class SurpriseTaskViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView iconView, rewardIconView;
        TextView titleView, descriptionView, rewardNameView, timerView;
        FrameLayout actionStatusContainer;

        SurpriseTaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_surprise_task_gamif);
            iconView = itemView.findViewById(R.id.imageView_surprise_icon_gamif);
            // titleView отсутствует в макете, убрал
            descriptionView = itemView.findViewById(R.id.textView_surprise_description_gamif);
            rewardIconView = itemView.findViewById(R.id.imageView_surprise_reward_icon_gamif);
            rewardNameView = itemView.findViewById(R.id.textView_surprise_reward_name_gamif);
            timerView = itemView.findViewById(R.id.textView_surprise_timer_gamif);
            actionStatusContainer = itemView.findViewById(R.id.frameLayout_surprise_action_status_gamif);
        }

        void bind(@Nullable GamificationViewModel.Pair<SurpriseTask, Reward> data, final GamificationItemClickListener listener) {
            if (data == null || data.getFirst() == null) {
                cardView.setVisibility(View.GONE);
                return;
            }
            cardView.setVisibility(View.VISIBLE);
            SurpriseTask task = data.getFirst();
            Reward reward = data.getSecond();

            descriptionView.setText(task.getDescription());
            if (reward != null) {
                rewardNameView.setText(reward.getName());
                Drawable rewardIconDr = GamificationUiUtils.getIconForRewardTypeDrawable(reward.getRewardType(), itemView.getContext());
                rewardIconView.setImageDrawable(rewardIconDr);
            } else {
                rewardNameView.setText("Ценная награда");
                rewardIconView.setImageResource(R.drawable.help);
            }

            actionStatusContainer.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(itemView.getContext());
            LocalDateTime now = LocalDateTime.now();
            boolean isExpired = now.isAfter(task.getExpirationTime());

            if (task.isCompleted()) {
                View statusView = inflater.inflate(R.layout.view_surprise_task_status, actionStatusContainer, false);
                ((ImageView) statusView.findViewById(R.id.status_icon_surprise)).setImageResource(R.drawable.check_circle);
                ((TextView) statusView.findViewById(R.id.status_text_surprise)).setText("Получено!");
                timerView.setVisibility(View.GONE);
                actionStatusContainer.addView(statusView);
            } else if (isExpired) {
                View statusView = inflater.inflate(R.layout.view_surprise_task_status_with_hide, actionStatusContainer, false);
                ((ImageView) statusView.findViewById(R.id.status_icon_surprise_expired)).setImageResource(R.drawable.warning);
                ((TextView) statusView.findViewById(R.id.status_text_surprise_expired)).setText("Истекло");
                statusView.findViewById(R.id.button_hide_surprise_expired).setOnClickListener(v -> listener.onHideSurpriseTaskClick(task.getId()));
                timerView.setVisibility(View.GONE);
                actionStatusContainer.addView(statusView);
            } else {
                Button acceptBtn = (Button) inflater.inflate(R.layout.button_accept_surprise_task, actionStatusContainer, false);
                acceptBtn.setOnClickListener(v -> listener.onAcceptSurpriseTaskClick(task));
                actionStatusContainer.addView(acceptBtn);

                long remainingMinutes = ChronoUnit.MINUTES.between(now, task.getExpirationTime());
                timerView.setText(String.format(Locale.getDefault(), "Осталось: %s", formatRemainingTimeStatic(remainingMinutes)));
                timerView.setTextColor(ContextCompat.getColor(itemView.getContext(),
                        remainingMinutes < 15 ? R.color.errorLight : R.color.onPrimaryContainerDark)); // Предполагая, что цвет для onPrimaryContainerDark существует
                timerView.setVisibility(View.VISIBLE);
            }
        }
        private String formatRemainingTimeStatic(long minutes) {
            if (minutes <= 0) return "0 мин";
            long hours = minutes / 60;
            long mins = minutes % 60;
            if (hours > 0 && mins > 0) return hours + " ч " + mins + " мин";
            if (hours > 0) return hours + " ч";
            return mins + " мин";
        }
    }

    static class DailyRewardsViewHolder extends RecyclerView.ViewHolder {
        TextView streakValueText, streakDaysText;
        RecyclerView rewardsRecyclerView;
        DailyRewardAdapter dailyRewardAdapter;

        DailyRewardsViewHolder(@NonNull View itemView, final GamificationItemClickListener globalListener, Context context) {
            super(itemView);
            streakValueText = itemView.findViewById(R.id.textView_daily_streak_value_gamif);
            streakDaysText = itemView.findViewById(R.id.textView_daily_streak_days_gamif);
            rewardsRecyclerView = itemView.findViewById(R.id.recyclerView_daily_rewards_gamif);

            dailyRewardAdapter = new DailyRewardAdapter(new DailyRewardAdapter.OnDailyRewardClickListener() {
                @Override public void onDailyRewardClick(Reward reward, int rewardStreakDay, boolean isToday) {
                    globalListener.onDailyRewardItemClick(reward, rewardStreakDay, isToday);
                }
                @Override public void onClaimTodayRewardClick() {
                    globalListener.onClaimDailyRewardClick();
                }
            });
            rewardsRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
            rewardsRecyclerView.setAdapter(dailyRewardAdapter);
        }

        void bind(@Nullable DailyRewardsInfo info) {
            if (info == null) { itemView.setVisibility(View.GONE); return; }
            itemView.setVisibility(View.VISIBLE);
            streakValueText.setText(String.valueOf(info.getCurrentStreak()));
            streakDaysText.setText(GamificationUiUtils.getDaysStringJava(info.getCurrentStreak()));
            dailyRewardAdapter.submitList(info.getRewards() != null ? info.getRewards() : Collections.emptyList());
            dailyRewardAdapter.updateDailyState(info.getCurrentStreak(), info.isCanClaimToday(), info.getTodayStreakDay(), ((info.getTodayStreakDay() -1) / 7) * 7 + 1);
        }
    }

    static class ActiveChallengesViewHolder extends RecyclerView.ViewHolder {
        ImageButton viewAllButton;
        LinearLayout summaryIndicatorsLayout;
        ViewPager2 challengesViewPager;
        WormPagerIndicatorView pagerIndicatorView;
        ActiveChallengesPagerAdapter pagerAdapter;
        Context context;

        ActiveChallengesViewHolder(@NonNull View itemView, final GamificationItemClickListener listener, Context context) {
            super(itemView);
            this.context = context;
            viewAllButton = itemView.findViewById(R.id.button_view_all_challenges_gamif);
            summaryIndicatorsLayout = itemView.findViewById(R.id.layout_challenges_summary_indicators_gamif);
            challengesViewPager = itemView.findViewById(R.id.viewPager_active_challenges_gamif);
            pagerIndicatorView = itemView.findViewById(R.id.worm_pager_indicator_challenges_gamif);

            pagerAdapter = new ActiveChallengesPagerAdapter(listener::onChallengeCardClick);
            challengesViewPager.setAdapter(pagerAdapter);

            pagerIndicatorView.attachToViewPager(challengesViewPager);
            viewAllButton.setOnClickListener(v -> listener.onViewAllChallengesClick());
        }

        void bind(@Nullable ActiveChallengesSectionState state) {
            if (state == null || state.getTotalActiveCount() == 0) {
                itemView.setVisibility(View.GONE);
                return;
            }
            itemView.setVisibility(View.VISIBLE);

            pagerAdapter.submitList(state.getAllActiveChallengesInfo());
            pagerIndicatorView.setPageCount(state.getAllActiveChallengesInfo().size());
            pagerIndicatorView.setVisibility(state.getAllActiveChallengesInfo().size() > 1 ? View.VISIBLE : View.GONE);

            summaryIndicatorsLayout.removeAllViews();
            if (state.getDailyProgressText() != null) {
                summaryIndicatorsLayout.addView(createSummaryIndicatorView(R.drawable.edit_calendar, state.getDailyProgressText(), "Ежедневные"));
            }
            if (state.getWeeklyProgressText() != null) {
                summaryIndicatorsLayout.addView(createSummaryIndicatorView(R.drawable.calendar_view_week, state.getWeeklyProgressText(), "Еженедельные"));
            }
            if (state.getUrgentCount() > 0) {
                summaryIndicatorsLayout.addView(createSummaryIndicatorView(R.drawable.local_fire_department, String.valueOf(state.getUrgentCount()), "Срочные", ContextCompat.getColor(context, R.color.errorLight)));
            }
            if (state.getNearCompletionCount() > 0) {
                summaryIndicatorsLayout.addView(createSummaryIndicatorView(R.drawable.star, String.valueOf(state.getNearCompletionCount()), "Близко", ContextCompat.getColor(context, R.color.secondaryLight)));
            }
        }

        private View createSummaryIndicatorView(int iconRes, String text, String contentDesc, @Nullable Integer tintColor) {
            LinearLayout indicatorLayout = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.view_summary_indicator, summaryIndicatorsLayout, false);
            ImageView icon = indicatorLayout.findViewById(R.id.imageView_summary_indicator_icon);
            TextView textView = indicatorLayout.findViewById(R.id.textView_summary_indicator_text);
            icon.setImageResource(iconRes);
            if (tintColor != null) {
                ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(tintColor));
                textView.setTextColor(tintColor);
            } else {
                ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.onSurfaceVariantDark)));
                textView.setTextColor(ContextCompat.getColor(context, R.color.onSurfaceVariantDark));
            }
            textView.setText(text);
            icon.setContentDescription(contentDesc);
            return indicatorLayout;
        }
        private View createSummaryIndicatorView(int iconRes, String text, String contentDesc) {
            return createSummaryIndicatorView(iconRes, text, contentDesc, null);
        }
    }
}