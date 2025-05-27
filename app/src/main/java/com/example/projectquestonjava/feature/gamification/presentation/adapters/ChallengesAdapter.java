package com.example.projectquestonjava.feature.gamification.presentation.adapters;

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
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.feature.gamification.data.model.Challenge;
import com.example.projectquestonjava.feature.gamification.data.model.Reward;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeProgressFullDetails;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeStatus;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeType;
import com.example.projectquestonjava.feature.gamification.presentation.utils.GamificationUiUtils; // Для иконок и форматирования
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;

public class ChallengesAdapter extends ListAdapter<ChallengeProgressFullDetails, ChallengesAdapter.ChallengeItemViewHolder> {

    private final OnChallengeItemClickListener listener;
    private final Context context; // Нужен для доступа к ресурсам

    public interface OnChallengeItemClickListener {
        void onChallengeItemClicked(ChallengeProgressFullDetails challengeDetails);
    }

    public ChallengesAdapter(@NonNull OnChallengeItemClickListener listener, @NonNull Context context) {
        super(CHALLENGE_FULL_DETAILS_DIFF_CALLBACK);
        this.listener = listener;
        this.context = context;
    }

    @NonNull
    @Override
    public ChallengeItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_challenge_list, parent, false);
        return new ChallengeItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChallengeItemViewHolder holder, int position) {
        ChallengeProgressFullDetails item = getItem(position);
        if (item != null) {
            holder.bind(item, listener, context);
        }
    }

    static class ChallengeItemViewHolder extends RecyclerView.ViewHolder {
        ImageView iconView, statusIndicatorView, deadlineIconView, rewardIconView;
        TextView nameView, descriptionView, progressTextView, deadlineTextView, rewardNameView;
        ProgressBar progressBar;
        LinearLayout deadlineLayout, rewardLayout;

        ChallengeItemViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.imageView_challenge_list_icon);
            nameView = itemView.findViewById(R.id.textView_challenge_list_name);
            statusIndicatorView = itemView.findViewById(R.id.imageView_challenge_list_status_indicator);
            descriptionView = itemView.findViewById(R.id.textView_challenge_list_description);
            progressBar = itemView.findViewById(R.id.progressBar_challenge_list);
            progressTextView = itemView.findViewById(R.id.textView_challenge_list_progress_text);
            deadlineLayout = itemView.findViewById(R.id.layout_challenge_list_deadline);
            deadlineIconView = itemView.findViewById(R.id.imageView_challenge_list_deadline_icon);
            deadlineTextView = itemView.findViewById(R.id.textView_challenge_list_deadline_text);
            rewardLayout = itemView.findViewById(R.id.layout_challenge_list_reward);
            rewardIconView = itemView.findViewById(R.id.imageView_challenge_list_reward_icon);
            rewardNameView = itemView.findViewById(R.id.textView_challenge_list_reward_name);
        }

        void bind(final ChallengeProgressFullDetails details, final OnChallengeItemClickListener listener, Context context) {
            Challenge challenge = details.getChallengeAndReward().getChallenge();
            Reward reward = details.getChallengeAndReward().getReward();

            nameView.setText(challenge.getName());
            descriptionView.setText(challenge.getDescription());
            descriptionView.setVisibility(challenge.getDescription() != null && !challenge.getDescription().isEmpty() ? View.VISIBLE : View.GONE);

            // Рассчитываем прогресс (аналогично ViewModel)
            float totalProgressValue = details.getProgress().getProgress();
            float totalTargetValue = details.getRule().getTarget();
            float overallProgress = (totalTargetValue > 0) ? (totalProgressValue / totalTargetValue) : 0f;
            overallProgress = Math.max(0f, Math.min(1f, overallProgress));

            progressBar.setProgress((int) (overallProgress * 100));
            progressTextView.setText(
                    String.format(Locale.getDefault(), "%d/%d %s",
                            details.getProgress().getProgress(),
                            details.getRule().getTarget(),
                            getUnitForChallengeType(details.getRule().getType())
                    )
            );

            // Цвета и иконки статуса
            LocalDateTime now = LocalDateTime.now();
            boolean isUrgent = challenge.getStatus() == ChallengeStatus.ACTIVE &&
                    (challenge.getEndDate().toLocalDate().isEqual(now.toLocalDate()) ||
                            challenge.getEndDate().toLocalDate().isEqual(now.toLocalDate().plusDays(1)));

            int progressColorRes;
            int statusIconRes = 0; // 0 означает нет иконки
            int statusIconTintRes = 0;

            switch (challenge.getStatus()) {
                case COMPLETED:
                    progressColorRes = R.color.secondaryLight;
                    statusIconRes = R.drawable.check_circle;
                    statusIconTintRes = R.color.secondaryLight;
                    break;
                case EXPIRED:
                    progressColorRes = R.color.onSurfaceVariantDark; // Серый для истекших
                    statusIconRes = R.drawable.warning;
                    statusIconTintRes = R.color.errorLight; // Полупрозрачный красный
                    break;
                case ACTIVE:
                    progressColorRes = isUrgent ? R.color.errorLight : R.color.primaryLight;
                    if (isUrgent) {
                        statusIconRes = R.drawable.warning;
                        statusIconTintRes = R.color.errorLight;
                    }
                    break;
                default: // UPCOMING, INACTIVE
                    progressColorRes = R.color.surfaceVariantDark; // Нейтральный
                    // Для UPCOMING можно добавить иконку часов
                    break;
            }

            iconView.setImageResource(GamificationUiUtils.getIconResForChallengeType(details.getRule().getType()));
            ImageViewCompat.setImageTintList(iconView, ColorStateList.valueOf(ContextCompat.getColor(context, progressColorRes)));

            // Установка цвета ProgressBar
            Drawable progressDrawable = ContextCompat.getDrawable(context, R.drawable.timer_progress_drawable);
            if (progressDrawable != null) {
                Drawable wrapDrawable = progressDrawable.mutate();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    wrapDrawable.setColorFilter(new android.graphics.BlendModeColorFilter(ContextCompat.getColor(context, progressColorRes), android.graphics.BlendMode.SRC_IN));
                } else {
                    wrapDrawable.setColorFilter(ContextCompat.getColor(context, progressColorRes), PorterDuff.Mode.SRC_IN);
                }
                progressBar.setProgressDrawable(wrapDrawable);
            }

            if (statusIconRes != 0) {
                statusIndicatorView.setVisibility(View.VISIBLE);
                statusIndicatorView.setImageResource(statusIconRes);
                ImageViewCompat.setImageTintList(statusIndicatorView, ColorStateList.valueOf(ContextCompat.getColor(context, statusIconTintRes)));
            } else {
                statusIndicatorView.setVisibility(View.GONE);
            }

            // Дедлайн
            if (challenge.getStatus() == ChallengeStatus.ACTIVE || challenge.getStatus() == ChallengeStatus.UPCOMING) {
                deadlineLayout.setVisibility(View.VISIBLE);
                deadlineTextView.setText(formatDeadlineTextListStatic(challenge.getEndDate(), isUrgent));
                int deadlineColor = ContextCompat.getColor(context, isUrgent ? R.color.errorLight : R.color.onSurfaceVariantDark);
                deadlineTextView.setTextColor(deadlineColor);
                ImageViewCompat.setImageTintList(deadlineIconView, ColorStateList.valueOf(deadlineColor));
            } else {
                deadlineLayout.setVisibility(View.GONE);
            }

            // Награда
            if (reward != null) {
                rewardLayout.setVisibility(View.VISIBLE);
                rewardIconView.setImageResource(GamificationUiUtils.getIconResForRewardType(reward.getRewardType()));
                rewardNameView.setText(reward.getName());
            } else {
                rewardLayout.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> listener.onChallengeItemClicked(details));
        }

        private String getUnitForChallengeType(ChallengeType type) {
            if (type == null) return "";
            return switch (type) {
                case TASK_COMPLETION -> "задач";
                case POMODORO_SESSION -> "Pomodoro";
                case DAILY_STREAK, TASK_STREAK, POMODORO_STREAK, WATERING_STREAK -> "дн. стрика";
                case LEVEL_REACHED -> "уровень";
                case BADGE_COUNT -> "значка(ов)";
                case PLANT_MAX_STAGE -> "растение";
                // Для RESOURCE_ACCUMULATED и CUSTOM_EVENT можно возвращать пустую строку или специфичный текст
                default -> "";
            };
        }

        private String formatDeadlineTextListStatic(LocalDateTime deadline, boolean isUrgent) {
            if (deadline == null) return null;
            LocalDateTime now = LocalDateTime.now();
            long daysLeft = ChronoUnit.DAYS.between(now.toLocalDate(), deadline.toLocalDate());
            daysLeft = Math.max(0, daysLeft);

            if (daysLeft == 0) return "Сегодня до " + deadline.format(DateTimeFormatter.ofPattern("HH:mm"));
            if (daysLeft == 1) return "Завтра до " + deadline.format(DateTimeFormatter.ofPattern("HH:mm"));
            return "Ост. " + daysLeft + " " + GamificationUiUtils.getDaysStringJava((int)daysLeft);
        }
    }

    private static final DiffUtil.ItemCallback<ChallengeProgressFullDetails> CHALLENGE_FULL_DETAILS_DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ChallengeProgressFullDetails>() {
                @Override
                public boolean areItemsTheSame(@NonNull ChallengeProgressFullDetails oldItem, @NonNull ChallengeProgressFullDetails newItem) {
                    // Сравниваем по ID челленджа и ID правила, так как это уникальная комбинация для прогресса
                    return oldItem.getChallengeAndReward().getChallenge().getId() == newItem.getChallengeAndReward().getChallenge().getId() &&
                            oldItem.getRule().getId() == newItem.getRule().getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull ChallengeProgressFullDetails oldItem, @NonNull ChallengeProgressFullDetails newItem) {
                    return oldItem.equals(newItem); // Используем auto-generated equals из record/data class
                }
            };
}