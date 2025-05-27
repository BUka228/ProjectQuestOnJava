package com.example.projectquestonjava.feature.profile.presentation.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.feature.statistics.data.model.GamificationHistory;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Objects;

public class ActivityFeedAdapter extends ListAdapter<GamificationHistory, ActivityFeedAdapter.HistoryViewHolder> {

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(new Locale("ru"));
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(new Locale("ru"));

    public ActivityFeedAdapter() {
        super(HISTORY_DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_profile, parent, false);
        return new HistoryViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        GamificationHistory entry = getItem(position);
        holder.bind(entry, timeFormatter, dateTimeFormatter);
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        ImageView iconView;
        TextView reasonTextView, timestampTextView;
        FrameLayout xpChipLayout, coinsChipLayout;
        TextView xpValueTextView, coinsValueTextView;
        ImageView coinsIconView; // Если иконка монет нужна внутри чипа

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.imageView_history_icon);
            reasonTextView = itemView.findViewById(R.id.textView_history_reason);
            timestampTextView = itemView.findViewById(R.id.textView_history_timestamp);
            xpChipLayout = itemView.findViewById(R.id.chip_history_xp);
            xpValueTextView = itemView.findViewById(R.id.textView_history_xp_value);
            coinsChipLayout = itemView.findViewById(R.id.chip_history_coins);
            coinsValueTextView = itemView.findViewById(R.id.textView_history_coins_value);
            // coinsIconView = itemView.findViewById(R.id.imageView_history_coins_icon); // Если иконка внутри чипа
        }

        void bind(GamificationHistory entry, DateTimeFormatter timeFmt, DateTimeFormatter dateTimeFmt) {
            Context context = itemView.getContext();
            reasonTextView.setText(formatHistoryReasonStatic(entry.getReason(), entry.getRelatedEntityId()));
            iconView.setImageResource(getIconResForHistoryReasonStatic(entry.getReason()));

            if (entry.getTimestamp().toLocalDate().isEqual(LocalDate.now())) {
                timestampTextView.setText(entry.getTimestamp().format(timeFmt));
            } else {
                timestampTextView.setText(entry.getTimestamp().format(dateTimeFmt));
            }

            if (entry.getXpChange() != 0) {
                xpChipLayout.setVisibility(View.VISIBLE);
                xpValueTextView.setText(String.format(Locale.getDefault(), "%s%d XP",
                        entry.getXpChange() > 0 ? "+" : "", entry.getXpChange()));
                // Цвет фона xpChipLayout можно установить программно
                xpChipLayout.setBackgroundResource(R.drawable.chip_background_green);
            } else {
                xpChipLayout.setVisibility(View.GONE);
            }

            if (entry.getCoinsChange() != 0) {
                coinsChipLayout.setVisibility(View.VISIBLE);
                coinsValueTextView.setText(String.format(Locale.getDefault(), "%s%d",
                        entry.getCoinsChange() > 0 ? "+" : "", entry.getCoinsChange()));
                // Цвет фона coinsChipLayout можно установить программно
                coinsChipLayout.setBackgroundResource(R.drawable.chip_background_orange);
                // Если есть ImageView для иконки монет внутри chip_history_coins
                // ImageView coinsIcon = coinsChipLayout.findViewById(R.id.imageView_history_coins_icon);
                // coinsIcon.setVisibility(View.VISIBLE);
            } else {
                coinsChipLayout.setVisibility(View.GONE);
            }
        }

        private static String formatHistoryReasonStatic(String reason, Long relatedId) {
            if (reason == null) return "Неизвестное событие";
            return switch (reason.toUpperCase()) {
                case "TASK_COMPLETED" -> "Задача выполнена";
                case "DAILY_REWARD" -> "Ежедневная награда";
                case "CHALLENGE_COMPLETED" -> "Челлендж выполнен"; // Можно добавить имя челленджа, если ID есть и его можно зарезолвить
                case "STORE_PURCHASE" -> "Покупка в магазине";
                case "POMODORO_COMPLETED" -> "Сессия Pomodoro";
                default -> reason;
            };
        }

        private static int getIconResForHistoryReasonStatic(String reason) {
            if (reason == null) return R.drawable.help;
            return switch (reason.toUpperCase()) {
                case "TASK_COMPLETED" -> R.drawable.check_box;
                case "DAILY_REWARD" -> R.drawable.emoji_events;
                case "CHALLENGE_COMPLETED" -> R.drawable.military_tech;
                case "STORE_PURCHASE" -> R.drawable.savings;
                case "POMODORO_COMPLETED" -> R.drawable.timer;
                default -> R.drawable.help;
            };
        }
    }

    private static final DiffUtil.ItemCallback<GamificationHistory> HISTORY_DIFF_CALLBACK =
            new DiffUtil.ItemCallback<GamificationHistory>() {
                @Override
                public boolean areItemsTheSame(@NonNull GamificationHistory oldItem, @NonNull GamificationHistory newItem) {
                    return oldItem.getId() == newItem.getId();
                }
                @Override
                public boolean areContentsTheSame(@NonNull GamificationHistory oldItem, @NonNull GamificationHistory newItem) {
                    return oldItem.equals(newItem);
                }
            };
}