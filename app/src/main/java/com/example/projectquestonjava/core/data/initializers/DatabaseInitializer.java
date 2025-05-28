package com.example.projectquestonjava.core.data.initializers;

import androidx.sqlite.db.SupportSQLiteDatabase;
import com.example.projectquestonjava.R; // Убедитесь, что R-файл доступен
import com.example.projectquestonjava.core.data.model.enums.ApproachName;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengePeriod;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeStatus;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeType;
import com.example.projectquestonjava.feature.gamification.domain.model.RewardType;

import javax.inject.Inject;
import javax.inject.Singleton; // Если DatabaseInitializer должен быть Singleton

// Если Timber не настроен для Java, можно использовать android.util.Log
// import android.util.Log;

@Singleton // Предполагаем, что инициализатор - синглтон
public class DatabaseInitializer {
    private static final String TAG = "DbInitializer";

    private final Logger logger;

    // --- КОНСТАНТЫ ID ЗНАЧКОВ ---
    private static final long BADGE_ID_STREAK_7 = 1L;
    private static final long BADGE_ID_STREAK_14 = 2L;
    private static final long BADGE_ID_STREAK_30 = 3L;
    private static final long BADGE_ID_CHALLENGE_VETERAN_250 = 8L;
    private static final long BADGE_ID_CHALLENGE_CONQUEROR_1000 = 9L;
    private static final long BADGE_ID_CHALLENGE_HERO_WEEK = 10L;
    private static final long BADGE_ID_CHALLENGE_POMODORO_GURU = 11L;
    private static final long BADGE_ID_CHALLENGE_DRIVE_14 = 12L;
    private static final long BADGE_ID_CHALLENGE_LEVEL_25 = 13L;
    private static final long BADGE_ID_CHALLENGE_LEVEL_100 = 14L;

    // --- КОНСТАНТЫ Ресурсов Drawable для Значков ---
    // Важно: R.drawable.имя_файла (без расширения)
    private static final int BADGE_DRAWABLE_STREAK_7 = R.drawable.streak_7;
    private static final int BADGE_DRAWABLE_STREAK_14 = R.drawable.streak_14;
    private static final int BADGE_DRAWABLE_STREAK_30 = R.drawable.streak_30;
    private static final int BADGE_DRAWABLE_CHALLENGE_VETERAN_250 = R.drawable.badge; // Замените на правильные ID
    private static final int BADGE_DRAWABLE_CHALLENGE_CONQUEROR_1000 = R.drawable.badge;
    private static final int BADGE_DRAWABLE_CHALLENGE_HERO_WEEK = R.drawable.badge;
    private static final int BADGE_DRAWABLE_CHALLENGE_POMODORO_GURU = R.drawable.badge;
    private static final int BADGE_DRAWABLE_CHALLENGE_DRIVE_14 = R.drawable.badge;
    private static final int BADGE_DRAWABLE_CHALLENGE_LEVEL_25 = R.drawable.badge;
    private static final int BADGE_DRAWABLE_CHALLENGE_LEVEL_100 = R.drawable.badge;

    // --- КОНСТАНТЫ ID Наград ---
    // Ежедневные (1-30)
    private static final long REWARD_ID_DAY_1 = 1L; private static final long REWARD_ID_DAY_2 = 2L; // ... и так далее
    private static final long REWARD_ID_DAY_14_BADGE = 14L;
    private static final long REWARD_ID_DAY_30_BADGE = 30L;
    // Запасные
    private static final long REWARD_ID_DAY_14_FALLBACK = 100L;
    private static final long REWARD_ID_DAY_30_FALLBACK = 101L;
    // Сюрпризы (201-230)
    private static final long REWARD_ID_SURPRISE_201 = 201L; // ... и так далее
    private static final long REWARD_ID_SURPRISE_230 = 230L;
    // Испытания (301+)
    private static final long REWARD_ID_CHALLENGE_C1 = 301L; // ... и так далее
    private static final long REWARD_ID_CHALLENGE_C4_BADGE = 304L;
    private static final long REWARD_ID_CHALLENGE_C4_FALLBACK = 305L;
    // ... остальные ID наград для испытаний ...
    private static final long REWARD_ID_CHALLENGE_C13_BADGE = 313L; private static final long REWARD_ID_CHALLENGE_C13_FALLBACK = 314L;
    private static final long REWARD_ID_CHALLENGE_C15_BADGE = 316L; private static final long REWARD_ID_CHALLENGE_C15_FALLBACK = 317L;
    private static final long REWARD_ID_CHALLENGE_C32_BADGE = 332L; private static final long REWARD_ID_CHALLENGE_C32_FALLBACK = 333L;
    private static final long REWARD_ID_CHALLENGE_C52_BADGE = 352L; private static final long REWARD_ID_CHALLENGE_C52_FALLBACK = 353L;
    private static final long REWARD_ID_CHALLENGE_C92_BADGE = 392L; private static final long REWARD_ID_CHALLENGE_C92_FALLBACK = 393L;
    private static final long REWARD_ID_CHALLENGE_C132_BADGE = 432L; private static final long REWARD_ID_CHALLENGE_C132_FALLBACK = 433L;
    private static final long REWARD_ID_CHALLENGE_C134_BADGE = 435L; private static final long REWARD_ID_CHALLENGE_C134_FALLBACK = 436L;

    // --- КОНСТАНТЫ ID Испытаний ---
    private static final long CHALLENGE_ID_C1 = 1L; // ... и так далее
    private static final long CHALLENGE_ID_C4 = 4L;
    // ... остальные ID испытаний ...
    private static final long CHALLENGE_ID_C143 = 143L;


    @Inject
    public DatabaseInitializer(Logger logger) {
        this.logger = logger;
    }

    // DatabaseInitializer.java
    public void initialize(SupportSQLiteDatabase db) {
        logger.info(TAG, "DatabaseInitializer: Starting GLOBAL data initialization...");
        db.beginTransaction();
        try {
            insertDefaultApproaches(db);
            logger.info(TAG, "DatabaseInitializer: Approaches inserted.");
            insertAllDefaultRewards(db);
            logger.info(TAG, "DatabaseInitializer: Rewards inserted.");
            insertAllDefaultBadges(db);
            logger.info(TAG, "DatabaseInitializer: Badges inserted.");
            insertDefaultStreakRewardDefinitions(db);
            logger.info(TAG, "DatabaseInitializer: Streak definitions inserted.");
            insertDefaultChallengesAndRules(db);
            logger.info(TAG, "DatabaseInitializer: Challenges and rules inserted.");

            db.setTransactionSuccessful();
            logger.info(TAG, "DatabaseInitializer: Default GLOBAL data transaction successful.");
        } catch (Exception e) {
            logger.error(TAG, "DatabaseInitializer: Error inserting default GLOBAL data", e);
        } finally {
            db.endTransaction();
            logger.info(TAG, "DatabaseInitializer: Default GLOBAL data transaction ended.");
        }
    }

    private void insertDefaultApproaches(SupportSQLiteDatabase db) {
        db.execSQL("INSERT OR IGNORE INTO approach (id, name, description) VALUES (1, '" + ApproachName.CALENDAR.name() + "', 'Calendar Approach');");
        db.execSQL("INSERT OR IGNORE INTO approach (id, name, description) VALUES (2, '" + ApproachName.GTD.name() + "', 'Getting Things Done');");
        db.execSQL("INSERT OR IGNORE INTO approach (id, name, description) VALUES (3, '" + ApproachName.EISENHOWER.name() + "', 'Eisenhower Matrix');");
        db.execSQL("INSERT OR IGNORE INTO approach (id, name, description) VALUES (4, '" + ApproachName.FROG.name() + "', 'Eat The Frog');");
    }

    private void insertAllDefaultRewards(SupportSQLiteDatabase db) {
        // Ежедневные
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_DAY_1 + ", 'Стартовый капитал', 'Добро пожаловать! Небольшой бонус для начала.', '" + RewardType.COINS.name() + "', 'BASE*10*1.08');");
        // ... и так далее для всех REWARD_ID_DAY_...
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_DAY_14_BADGE + ", 'Значок ''Двухнедельная стойкость''', 'Две недели подряд! Твоя дисциплина впечатляет.', '" + RewardType.BADGE.name() + "', '" + BADGE_ID_STREAK_14 + "');");
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_DAY_30_BADGE + ", 'Значок ''Месяц дисциплины''', 'Целый месяц ежедневных успехов!', '" + RewardType.BADGE.name() + "', '" + BADGE_ID_STREAK_30 + "');");
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_DAY_14_FALLBACK + ", 'Бонус стойкости', 'У тебя уже есть значок за 2 недели! Вот дополнительный бонус монетами.', '" + RewardType.COINS.name() + "', 'BASE*150*1.1');");
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_DAY_30_FALLBACK + ", 'Месячный капитал', 'Значок за месяц у тебя уже есть! Прими этот внушительный денежный бонус.', '" + RewardType.COINS.name() + "', 'BASE*300*1.1');");


        // Сюрпризы
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_SURPRISE_201 + ", 'Бонус за разминку', 'Забота о глазах.', '" + RewardType.EXPERIENCE.name() + "', 'BASE*5*1.05');");
        // ... и так далее для всех REWARD_ID_SURPRISE_...
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_SURPRISE_230 + ", 'Королевская осанка', 'За минуту прямой спины.', '" + RewardType.EXPERIENCE.name() + "', 'BASE*7*1.06');");

        // Испытания
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_CHALLENGE_C1 + ", 'Карманные деньги', 'Награда за первую задачу.', '" + RewardType.COINS.name() + "', 'BASE*15*1.05');");
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_CHALLENGE_C4_BADGE + ", 'Значок ''Первая Неделя''', 'За 7 дней стрика!', '" + RewardType.BADGE.name() + "', '" + BADGE_ID_STREAK_7 + "');");
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_CHALLENGE_C4_FALLBACK + ", 'Повторный успех', 'У тебя уже есть значок за 7 дней! Вот монеты.', '" + RewardType.COINS.name() + "', 'BASE*50*1.1');");
        // ... и так далее для всех REWARD_ID_CHALLENGE_...
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_CHALLENGE_C134_BADGE + ", 'Значок ''Легенда 100''', 'Достигнут 100 уровень!', '" + RewardType.BADGE.name() + "', '" + BADGE_ID_CHALLENGE_LEVEL_100 + "');");
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_CHALLENGE_C134_FALLBACK + ", 'Дань Легенде', 'Значок за 100 уровень уже есть! Получи монеты.', '" + RewardType.COINS.name() + "', 'BASE*1000*1.15');");

    }

    private void insertAllDefaultBadges(SupportSQLiteDatabase db) {
        db.execSQL("INSERT OR IGNORE INTO badge (id, name, description, image_url, criteria) VALUES (" + BADGE_ID_STREAK_7 + ", 'Первая Неделя', 'Вы получали награду 7 дней подряд!', " + BADGE_DRAWABLE_STREAK_7 + ", 'Стрик 7 дней');");
        db.execSQL("INSERT OR IGNORE INTO badge (id, name, description, image_url, criteria) VALUES (" + BADGE_ID_STREAK_14 + ", 'Двухнедельная стойкость', 'Две недели подряд! Твоя дисциплина впечатляет.', " + BADGE_DRAWABLE_STREAK_14 + ", 'Стрик 14 дней');");
        db.execSQL("INSERT OR IGNORE INTO badge (id, name, description, image_url, criteria) VALUES (" + BADGE_ID_STREAK_30 + ", 'Месяц дисциплины', 'Целый месяц ежедневных успехов!', " + BADGE_DRAWABLE_STREAK_30 + ", 'Стрик 30 дней');");
        db.execSQL("INSERT OR IGNORE INTO badge (id, name, description, image_url, criteria) VALUES (" + BADGE_ID_CHALLENGE_VETERAN_250 + ", 'Ветеран 250', 'Выполнено 250 задач!', " + BADGE_DRAWABLE_CHALLENGE_VETERAN_250 + ", 'Завершить 250 задач');");
        // ... и так далее для всех значков
        db.execSQL("INSERT OR IGNORE INTO badge (id, name, description, image_url, criteria) VALUES (" + BADGE_ID_CHALLENGE_LEVEL_100 + ", 'Легенда 100', 'Достигнут 100 уровень!', " + BADGE_DRAWABLE_CHALLENGE_LEVEL_100 + ", 'Достичь 100 уровня');");
    }

    private void insertDefaultStreakRewardDefinitions(SupportSQLiteDatabase db) {
        // Связываем дни 1-30 с их ОСНОВНЫМИ наградами
        db.execSQL("INSERT OR IGNORE INTO streak_reward_definition (streak_day, reward_id) VALUES (1, " + REWARD_ID_DAY_1 + ");");
        // ... и так далее для всех дней до 30 ...
        db.execSQL("INSERT OR IGNORE INTO streak_reward_definition (streak_day, reward_id) VALUES (14, " + REWARD_ID_DAY_14_BADGE + ");");
        db.execSQL("INSERT OR IGNORE INTO streak_reward_definition (streak_day, reward_id) VALUES (30, " + REWARD_ID_DAY_30_BADGE + ");");
    }

    private void insertDefaultChallengesAndRules(SupportSQLiteDatabase db) {
        String minDateString = "'MIN'"; // LocalDateTime.MIN.toString() не будет работать в SQL напрямую
        String maxDateString = "'MAX'"; // LocalDateTime.MAX.toString()

        // Онбординг
        db.execSQL("INSERT OR IGNORE INTO Challenge (id, name, description, start_date, end_date, reward_id, status, period) VALUES (" + CHALLENGE_ID_C1 + ", 'Первые шаги', 'Заверши свою первую задачу', " + minDateString + ", " + maxDateString + ", " + REWARD_ID_CHALLENGE_C1 + ", '" + ChallengeStatus.ACTIVE.name() + "', '" + ChallengePeriod.ONCE.name() + "');");
        db.execSQL("INSERT OR IGNORE INTO challenge_rule (challenge_id, type, target, period, condition_json) VALUES (" + CHALLENGE_ID_C1 + ", '" + ChallengeType.TASK_COMPLETION.name() + "', 1, '" + ChallengePeriod.ONCE.name() + "', NULL);");

    }
}