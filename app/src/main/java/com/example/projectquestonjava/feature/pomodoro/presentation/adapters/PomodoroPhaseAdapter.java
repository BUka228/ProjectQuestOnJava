package com.example.projectquestonjava.feature.pomodoro.presentation.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.feature.pomodoro.domain.model.PomodoroPhase;
import com.example.projectquestonjava.feature.pomodoro.domain.model.SessionType;
import com.example.projectquestonjava.feature.pomodoro.domain.model.TimerState;
import com.google.android.material.card.MaterialCardView;
import java.util.Locale;
import java.util.Objects;

public class PomodoroPhaseAdapter extends ListAdapter<PomodoroPhase, PomodoroPhaseAdapter.PhaseViewHolder> {

    private int currentPhaseIndex = -1;
    private TimerState currentTimerState = TimerState.Idle.getInstance();
    private final Context context;

    public PomodoroPhaseAdapter(@NonNull Context context) {
        super(DIFF_CALLBACK);
        this.context = context;
    }

    public void setCurrentPhaseIndex(int index) {
        if (this.currentPhaseIndex != index) {
            int oldIndex = this.currentPhaseIndex;
            this.currentPhaseIndex = index;
            if (oldIndex != -1) notifyItemChanged(oldIndex);
            if (index != -1) notifyItemChanged(index);
        }
    }

    public void setCurrentTimerState(TimerState timerState) {
        if (!Objects.equals(this.currentTimerState, timerState)) {
            this.currentTimerState = timerState;
            // Обновляем только текущую и, возможно, предыдущую фазу, если индекс изменился
            if (currentPhaseIndex != -1) {
                notifyItemChanged(currentPhaseIndex);
                if (currentPhaseIndex > 0 && timerState instanceof TimerState.WaitingForConfirmation) {
                    // Если текущая стала WaitingForConfirmation, предыдущая могла стать completed
                    notifyItemChanged(currentPhaseIndex -1);
                }
            } else if (timerState instanceof TimerState.Idle && getItemCount() > 0) {
                // Если перешли в Idle, возможно, нужно перерисовать первую фазу (если она есть)
                notifyItemChanged(0);
            }
        }
    }

    @NonNull
    @Override
    public PhaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_phase_indicator, parent, false);
        return new PhaseViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull PhaseViewHolder holder, int position) {
        PomodoroPhase phase = getItem(position);
        boolean isCurrent = position == currentPhaseIndex && !(currentTimerState instanceof TimerState.Idle); // Не выделяем, если Idle
        boolean isCompleted = position < currentPhaseIndex ||
                (position == currentPhaseIndex && currentTimerState instanceof TimerState.WaitingForConfirmation &&
                        ((TimerState.WaitingForConfirmation) currentTimerState).getType() == phase.getType());
        holder.bind(phase, isCurrent, isCompleted, context);
    }

    static class PhaseViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView iconView;
        TextView durationView;

        public PhaseViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_phase_indicator);
            iconView = itemView.findViewById(R.id.imageView_phase_icon);
            durationView = itemView.findViewById(R.id.textView_phase_duration);
        }

        void bind(PomodoroPhase phase, boolean isCurrent, boolean isCompleted, Context context) {
            @ColorInt int baseColor;
            @ColorInt int onBaseColor;
            @DrawableRes int iconRes;

            SessionType type = phase.getType();
            if (type == null) type = SessionType.FOCUS; // Fallback

            switch (type) {
                case FOCUS:
                    baseColor = ContextCompat.getColor(context, R.color.primaryDark);
                    onBaseColor = ContextCompat.getColor(context, R.color.onPrimaryDark);
                    iconRes = R.drawable.timer;
                    break;
                case SHORT_BREAK:
                    baseColor = ContextCompat.getColor(context, R.color.tertiaryDark);
                    onBaseColor = ContextCompat.getColor(context, R.color.onTertiaryDark);
                    iconRes = R.drawable.coffee;
                    break;
                case LONG_BREAK:
                    baseColor = ContextCompat.getColor(context, R.color.secondaryContainerDark);
                    onBaseColor = ContextCompat.getColor(context, R.color.onSecondaryContainerDark);
                    iconRes = R.drawable.self_improvement;
                    break;
                default: // Fallback
                    baseColor = ContextCompat.getColor(context, R.color.surfaceVariantDark);
                    onBaseColor = ContextCompat.getColor(context, R.color.onSurfaceVariantDark);
                    iconRes = R.drawable.help;
            }

            @ColorInt int cardBackgroundColor;
            @ColorInt int iconAndTextColor;
            float scale = 1.0f;

            if (isCompleted) {
                // Полупрозрачный основной цвет для фона
                cardBackgroundColor = Color.argb(Math.round(Color.alpha(baseColor) * 0.2f), // Меньше альфа
                        Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor));
                // Полупрозрачный цвет текста/иконки
                iconAndTextColor = Color.argb(Math.round(Color.alpha(onBaseColor) * 0.5f), // Меньше альфа
                        Color.red(onBaseColor), Color.green(onBaseColor), Color.blue(onBaseColor));

            } else if (isCurrent) {
                cardBackgroundColor = baseColor;
                iconAndTextColor = onBaseColor;
                scale = 1.1f;
            } else { // Будущие фазы
                cardBackgroundColor = ContextCompat.getColor(context, R.color.surfaceContainerLowestDark); // Более темный фон для неактивных
                iconAndTextColor = ContextCompat.getColor(context, R.color.onSurfaceVariantDark);
            }

            cardView.setCardBackgroundColor(cardBackgroundColor);
            iconView.setImageResource(iconRes);
            ImageViewCompat.setImageTintList(iconView, ColorStateList.valueOf(iconAndTextColor));
            durationView.setText(String.format(Locale.getDefault(), "%dм", phase.getDurationSeconds() / 60));
            durationView.setTextColor(iconAndTextColor);
            durationView.setTypeface(null, isCurrent ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

            itemView.setScaleX(scale);
            itemView.setScaleY(scale);
        }
    }

    private static final DiffUtil.ItemCallback<PomodoroPhase> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<PomodoroPhase>() {
                @Override
                public boolean areItemsTheSame(@NonNull PomodoroPhase oldItem, @NonNull PomodoroPhase newItem) {
                    return oldItem.getType() == newItem.getType() &&
                            oldItem.getPhaseNumberInCycle() == newItem.getPhaseNumberInCycle() &&
                            oldItem.getTotalFocusSessionIndex() == newItem.getTotalFocusSessionIndex();
                }

                @Override
                public boolean areContentsTheSame(@NonNull PomodoroPhase oldItem, @NonNull PomodoroPhase newItem) {
                    return oldItem.equals(newItem);
                }
            };
}