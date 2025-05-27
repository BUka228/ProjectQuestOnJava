package com.example.projectquestonjava.feature.gamification.presentation.utils; // Уточни пакет

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.example.projectquestonjava.R; // Важно для доступа к ресурсам drawable
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengePeriod;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeType; // Импорт ChallengeType
import com.example.projectquestonjava.feature.gamification.domain.model.RewardType;

public class GamificationUiUtils {

    private GamificationUiUtils() {
        // Приватный конструктор для утилитного класса
    }

    @Nullable
    public static Drawable getIconForRewardTypeDrawable(@Nullable RewardType rewardType, @NonNull Context context) {
        if (rewardType == null) {
            return ContextCompat.getDrawable(context, R.drawable.help);
        }
        @DrawableRes int resId;
        switch (rewardType) {
            case COINS: resId = R.drawable.paid; break;
            case EXPERIENCE: resId = R.drawable.star; break;
            case BADGE: resId = R.drawable.badge; break;
            case PLANT: resId = R.drawable.grass; break;
            case THEME: resId = R.drawable.palette; break;
            default: resId = R.drawable.help; break;
        }
        return ContextCompat.getDrawable(context, resId);
    }

    @DrawableRes
    public static int getIconResForRewardType(@Nullable RewardType rewardType) {
        if (rewardType == null) return R.drawable.help;
        return switch (rewardType) {
            case COINS -> R.drawable.paid;
            case EXPERIENCE -> R.drawable.star;
            case BADGE -> R.drawable.badge;
            case PLANT -> R.drawable.grass;
            case THEME -> R.drawable.palette;
        };
    }

    /**
     * Возвращает ресурс ID для иконки типа челленджа.
     * @param type Тип челленджа.
     * @return ID ресурса drawable.
     */
    @DrawableRes
    public static int getIconResForChallengeType(@Nullable ChallengeType type) {
        if (type == null) {
            return R.drawable.help; // Иконка по умолчанию, если тип неизвестен
        }
        switch (type) {
            case TASK_COMPLETION:
                return R.drawable.check_box; // Иконка выполненной задачи
            case POMODORO_SESSION:
                return R.drawable.timer; // Иконка таймера
            case DAILY_STREAK:
            case TASK_STREAK:
            case POMODORO_STREAK:
            case WATERING_STREAK:
                return R.drawable.local_fire_department; // Иконка огня для стриков
            case CUSTOM_EVENT:
                return R.drawable.emoji_events; // Иконка события/достижения
            case BADGE_COUNT:
                return R.drawable.badge; // Иконка значка
            case PLANT_MAX_STAGE:
                return R.drawable.grass; // Иконка растения
            case LEVEL_REACHED:
                return R.drawable.star; // Иконка звезды для уровня
            case RESOURCE_ACCUMULATED:
                return R.drawable.savings; // Иконка копилки/ресурсов
            case STATISTIC_REACHED:
                return R.drawable.trending_up; // Иконка графика/статистики
            default:
                return R.drawable.help; // Иконка по умолчанию для неизвестных или новых типов
        }
    }

    @NonNull
    public static String getLocalizedPeriodNameJava(@Nullable ChallengePeriod period) {
        if (period == null) return "Неизвестно";
        return switch (period) {
            case ONCE -> "Разовый";
            case DAILY -> "Дневной";
            case WEEKLY -> "Недельный";
            case MONTHLY -> "Месячный";
            case EVENT -> "Событие";
        };
    }

    @NonNull
    public static String getDaysStringJava(int days) {
        if (days < 0) days = 0;
        int lastDigit = days % 10;
        int lastTwoDigits = days % 100;

        if (lastTwoDigits >= 11 && lastTwoDigits <= 19) {
            return "дней";
        }
        return switch (lastDigit) {
            case 1 -> "день";
            case 2, 3, 4 -> "дня";
            default -> "дней";
        };
    }
}