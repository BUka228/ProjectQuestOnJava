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
    private final Context context; // Нужен для доступа к ресурсам

    public PomodoroPhaseAdapter(@NonNull Context context) {
        super(DIFF_CALLBACK);
        this.context = context;
    }

    public void setCurrentPhaseIndex(int index) {
        if (this.currentPhaseIndex != index) {
            this.currentPhaseIndex = index;
            notifyDataSetChanged(); // Простое обновление для примера
        }
    }

    public void setCurrentTimerState(TimerState timerState) {
        if (!Objects.equals(this.currentTimerState, timerState)) {
            this.currentTimerState = timerState;
            notifyDataSetChanged(); // Простое обновление
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
        boolean isCurrent = position == currentPhaseIndex && (currentTimerState instanceof TimerState.Running || currentTimerState instanceof TimerState.Paused);
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
            int baseColorRes;
            int iconRes;

            switch (phase.getType()) {
                case FOCUS:
                    baseColorRes = R.color.primaryLight;
                    iconRes = R.drawable.timer;
                    break;
                case SHORT_BREAK:
                    baseColorRes = R.color.tertiaryLight;
                    iconRes = R.drawable.coffee;
                    break;
                case LONG_BREAK:
                    baseColorRes = R.color.secondaryContainerLight; // MaterialTheme.colorScheme.secondaryContainer
                    iconRes = R.drawable.self_improvement;
                    break;
                default:
                    baseColorRes = R.color.surfaceVariantLight;
                    iconRes = R.drawable.help;
            }

            int animatedColor;
            int iconAndTextColor;
            float scale = 1.0f;
            ColorStateList iconTint;

            if (isCompleted) {
                animatedColor = ContextCompat.getColor(context, baseColorRes);
                animatedColor = Color.argb(Math.round(Color.alpha(animatedColor) * 0.3f), Color.red(animatedColor), Color.green(animatedColor), Color.blue(animatedColor));
                iconAndTextColor = ContextCompat.getColor(context, baseColorRes);
                iconAndTextColor = Color.argb(Math.round(Color.alpha(iconAndTextColor) * 0.6f), Color.red(iconAndTextColor), Color.green(iconAndTextColor), Color.blue(iconAndTextColor));
            } else if (isCurrent) {
                animatedColor = ContextCompat.getColor(context, baseColorRes);
                iconAndTextColor = ContextCompat.getColor(context, getColorForState(baseColorRes, true, context)); // onColor
                scale = 1.1f;
            } else {
                animatedColor = ContextCompat.getColor(context, R.color.surfaceVariantLight);
                iconAndTextColor = ContextCompat.getColor(context, R.color.colorOnSurfaceVariantAlpha08);
            }

            cardView.setCardBackgroundColor(animatedColor);
            iconView.setImageResource(iconRes);
            ImageViewCompat.setImageTintList(iconView, ColorStateList.valueOf(iconAndTextColor));
            durationView.setText(String.format(Locale.getDefault(), "%dм", phase.getDurationSeconds() / 60));
            durationView.setTextColor(iconAndTextColor);
            durationView.setTypeface(null, isCurrent ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

            itemView.setScaleX(scale);
            itemView.setScaleY(scale);
        }

        private int getColorForState(int baseColorRes, boolean isCurrent, Context context) {
            // Простая логика для определения цвета контента на фоне
            // В идеале использовать MaterialColors.getColor(view, R.attr.colorOn[Primary/Secondary/Etc]Container)
            if (isCurrent) {
                if (baseColorRes == R.color.primaryLight) return ContextCompat.getColor(context, R.color.onPrimaryLight);
                if (baseColorRes == R.color.tertiaryLight) return ContextCompat.getColor(context, R.color.onTertiaryLight);
                if (baseColorRes == R.color.secondaryContainerLight) return ContextCompat.getColor(context, R.color.onSecondaryContainerLight);
            }
            return ContextCompat.getColor(context, R.color.colorOnSurfaceVariantAlpha05);
        }
    }

    private static final DiffUtil.ItemCallback<PomodoroPhase> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<PomodoroPhase>() {
                @Override
                public boolean areItemsTheSame(@NonNull PomodoroPhase oldItem, @NonNull PomodoroPhase newItem) {
                    // Фазы обычно не меняют своего "ID" в рамках одного цикла
                    return oldItem.getType() == newItem.getType() &&
                            oldItem.getPhaseNumberInCycle() == newItem.getPhaseNumberInCycle() &&
                            oldItem.getTotalFocusSessionIndex() == newItem.getTotalFocusSessionIndex();
                }

                @Override
                public boolean areContentsTheSame(@NonNull PomodoroPhase oldItem, @NonNull PomodoroPhase newItem) {
                    return oldItem.equals(newItem); // Data class сам сгенерирует equals
                }
            };
}