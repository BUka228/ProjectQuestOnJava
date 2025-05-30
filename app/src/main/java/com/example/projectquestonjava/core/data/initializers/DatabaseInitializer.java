package com.example.projectquestonjava.core.data.initializers;

import androidx.sqlite.db.SupportSQLiteDatabase;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.core.data.model.enums.ApproachName;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengePeriod;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeStatus;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeType;
import com.example.projectquestonjava.feature.gamification.domain.model.RewardType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDateTime; // Для дат челленджей

@Singleton
public class DatabaseInitializer {
    private static final String TAG = "DbInitializer";

    private final Logger logger;

    // --- ID ЗНАЧКОВ ---
    private static final long BADGE_ID_STREAK_7 = 1L;
    private static final long BADGE_ID_STREAK_14 = 2L;
    private static final long BADGE_ID_STREAK_30 = 3L;
    private static final long BADGE_ID_POMODORO_5 = 4L; // Новый значок
    private static final long BADGE_ID_LEVEL_5 = 5L;    // Новый значок
    // ... остальные ID значков ...
    private static final long BADGE_ID_CHALLENGE_VETERAN_250 = 8L;
    private static final long BADGE_ID_CHALLENGE_CONQUEROR_1000 = 9L;
    private static final long BADGE_ID_CHALLENGE_HERO_WEEK = 10L;
    private static final long BADGE_ID_CHALLENGE_POMODORO_GURU = 11L;
    private static final long BADGE_ID_CHALLENGE_DRIVE_14 = 12L;
    private static final long BADGE_ID_CHALLENGE_LEVEL_25 = 13L;
    private static final long BADGE_ID_CHALLENGE_LEVEL_100 = 14L;


    // --- Ресурсы Drawable для Значков ---
    private static final int BADGE_DRAWABLE_STREAK_7 = R.drawable.streak_7;
    private static final int BADGE_DRAWABLE_STREAK_14 = R.drawable.streak_14;
    private static final int BADGE_DRAWABLE_STREAK_30 = R.drawable.streak_30;
    private static final int BADGE_DRAWABLE_POMODORO_5 = R.drawable.timer; // Используем существующую иконку
    private static final int BADGE_DRAWABLE_LEVEL_5 = R.drawable.star;     // Используем существующую иконку
    // ... остальные ресурсы значков ...
    private static final int BADGE_DRAWABLE_CHALLENGE_VETERAN_250 = R.drawable.badge;
    private static final int BADGE_DRAWABLE_CHALLENGE_CONQUEROR_1000 = R.drawable.badge;
    private static final int BADGE_DRAWABLE_CHALLENGE_HERO_WEEK = R.drawable.badge;
    private static final int BADGE_DRAWABLE_CHALLENGE_POMODORO_GURU = R.drawable.badge;
    private static final int BADGE_DRAWABLE_CHALLENGE_DRIVE_14 = R.drawable.badge;
    private static final int BADGE_DRAWABLE_CHALLENGE_LEVEL_25 = R.drawable.badge;
    private static final int BADGE_DRAWABLE_CHALLENGE_LEVEL_100 = R.drawable.badge;


    private static final long REWARD_ID_DAY_1 = 1L;
    private static final long REWARD_ID_DAY_2 = 2L;
    private static final long REWARD_ID_DAY_3 = 3L;
    private static final long REWARD_ID_DAY_4 = 4L;
    private static final long REWARD_ID_DAY_5 = 5L;
    private static final long REWARD_ID_DAY_6 = 6L;
    private static final long REWARD_ID_DAY_7_BADGE = 7L; // Основная награда за 7-й день - значок
    private static final long REWARD_ID_DAY_7_FALLBACK_COINS = 8L;
    // --- ID Наград ---
    // Ежедневные

    private static final long REWARD_ID_DAY_14_BADGE = 14L;
    private static final long REWARD_ID_DAY_30_BADGE = 30L;
    private static final long REWARD_ID_DAY_14_FALLBACK = 100L;
    private static final long REWARD_ID_DAY_30_FALLBACK = 101L;
    // Сюрпризы
    private static final long REWARD_ID_SURPRISE_201 = 201L;
    private static final long REWARD_ID_SURPRISE_230 = 230L;
    // Испытания
    private static final long REWARD_ID_CHALLENGE_C1 = 301L; // Первые шаги
    private static final long REWARD_ID_CHALLENGE_C2 = 302L; // Ежедневный фокус (XP)
    private static final long REWARD_ID_CHALLENGE_C3 = 303L; // Недельный марафон (Монеты)
    private static final long REWARD_ID_CHALLENGE_C4_BADGE = 304L; // За 7 дней стрика (уже было)
    private static final long REWARD_ID_CHALLENGE_C4_FALLBACK = 305L; // (уже было)
    private static final long REWARD_ID_CHALLENGE_POMODORO_STARTER_BADGE = 306L; // Новый для 5 Pomodoro
    private static final long REWARD_ID_CHALLENGE_POMODORO_STARTER_COINS = 307L; // Запасной для Pomodoro
    private static final long REWARD_ID_CHALLENGE_LEVEL_5_BADGE = 308L; // Новый для 5 уровня
    private static final long REWARD_ID_CHALLENGE_LEVEL_5_COINS = 309L; // Запасной для 5 уровня
    // ...
    private static final long REWARD_ID_CHALLENGE_C134_BADGE = 435L;
    private static final long REWARD_ID_CHALLENGE_C134_FALLBACK = 436L;


    // --- ID Испытаний ---
    private static final long CHALLENGE_ID_C1 = 1L; // Первые шаги
    private static final long CHALLENGE_ID_C2_DAILY_FOCUS = 2L; // Новый: Ежедневный фокус
    private static final long CHALLENGE_ID_C3_WEEKLY_MARATHON = 3L; // Новый: Недельный марафон
    private static final long CHALLENGE_ID_C4_7_DAY_STREAK = 4L; // (уже было, переименовал для ясности)
    private static final long CHALLENGE_ID_C5_POMODORO_STARTER = 5L; // Новый: 5 Pomodoro
    private static final long CHALLENGE_ID_C6_LEVEL_5_ACHIEVER = 6L; // Новый: Достичь 5 уровня
    // ... остальные ID испытаний ...
    private static final long CHALLENGE_ID_C143 = 143L;


    @Inject
    public DatabaseInitializer(Logger logger) {
        this.logger = logger;
    }

    public void initialize(SupportSQLiteDatabase db) {
        logger.info(TAG, "DatabaseInitializer: Starting GLOBAL data initialization...");
        db.beginTransaction();
        try {
            insertDefaultApproaches(db);
            logger.info(TAG, "DatabaseInitializer: Approaches inserted.");
            insertAllDefaultRewards(db); // Порядок важен: сначала награды
            logger.info(TAG, "DatabaseInitializer: Rewards inserted.");
            insertAllDefaultBadges(db);  // Затем значки (некоторые награды ссылаются на значки)
            logger.info(TAG, "DatabaseInitializer: Badges inserted.");
            insertDefaultStreakRewardDefinitions(db);
            logger.info(TAG, "DatabaseInitializer: Streak definitions inserted.");
            insertDefaultChallengesAndRules(db); // Затем челленджи (ссылаются на награды)
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
        db.execSQL("INSERT OR IGNORE INTO approach (id, name, description) VALUES (1, '" + ApproachName.CALENDAR.name() + "', 'Календарное планирование');");
        db.execSQL("INSERT OR IGNORE INTO approach (id, name, description) VALUES (2, '" + ApproachName.GTD.name() + "', 'Getting Things Done');");
        db.execSQL("INSERT OR IGNORE INTO approach (id, name, description) VALUES (3, '" + ApproachName.EISENHOWER.name() + "', 'Матрица Эйзенхауэра');");
        db.execSQL("INSERT OR IGNORE INTO approach (id, name, description) VALUES (4, '" + ApproachName.FROG.name() + "', 'Съешь лягушку');");
    }

    private void insertAllDefaultRewards(SupportSQLiteDatabase db) {
        // Ежедневные
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_DAY_1 + ", 'Стартовый капитал', 'Добро пожаловать! Небольшой бонус для начала.', '" + RewardType.COINS.name() + "', 'BASE*10*1.08');");
        // ...
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_DAY_2 + ", 'Немного опыта', 'За второй день.', '" + RewardType.EXPERIENCE.name() + "', '15');");
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_DAY_3 + ", 'Монетный дождь', 'Приятный бонус.', '" + RewardType.COINS.name() + "', '20');");
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_DAY_4 + ", 'Опытный искатель', 'Еще немного опыта.', '" + RewardType.EXPERIENCE.name() + "', '25');");
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_DAY_5 + ", 'Полпути к неделе', 'Почти у цели!', '" + RewardType.COINS.name() + "', '30');");
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_DAY_6 + ", 'Предвкушение', 'Завтра большой день!', '" + RewardType.EXPERIENCE.name() + "', '35');");
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_DAY_7_BADGE + ", 'Значок ''Неделя упорства''', 'Целая неделя ежедневных наград!', '" + RewardType.BADGE.name() + "', '" + BADGE_ID_STREAK_7 + "');");
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_DAY_7_FALLBACK_COINS + ", 'Награда за неделю', 'Значок недели уже есть! Вот монеты.', '" + RewardType.COINS.name() + "', '100');");
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_DAY_14_BADGE + ", 'Значок ''Двухнедельная стойкость''', 'Две недели подряд! Твоя дисциплина впечатляет.', '" + RewardType.BADGE.name() + "', '" + BADGE_ID_STREAK_14 + "');");
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_DAY_30_BADGE + ", 'Значок ''Месяц дисциплины''', 'Целый месяц ежедневных успехов!', '" + RewardType.BADGE.name() + "', '" + BADGE_ID_STREAK_30 + "');");
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_DAY_14_FALLBACK + ", 'Бонус стойкости', 'У тебя уже есть значок за 2 недели! Вот дополнительный бонус монетами.', '" + RewardType.COINS.name() + "', 'BASE*150*1.1');");
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_DAY_30_FALLBACK + ", 'Месячный капитал', 'Значок за месяц у тебя уже есть! Прими этот внушительный денежный бонус.', '" + RewardType.COINS.name() + "', 'BASE*300*1.1');");

        // Сюрпризы
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_SURPRISE_201 + ", 'Бонус за разминку', 'Забота о глазах.', '" + RewardType.EXPERIENCE.name() + "', 'BASE*5*1.05');");
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_SURPRISE_230 + ", 'Королевская осанка', 'За минуту прямой спины.', '" + RewardType.EXPERIENCE.name() + "', 'BASE*7*1.06');");

        // Испытания
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_CHALLENGE_C1 + ", 'Карманные деньги', 'Награда за первую задачу.', '" + RewardType.COINS.name() + "', 'BASE*15*1.05');");
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_CHALLENGE_C2 + ", 'Ежедневный опыт', 'Бонус за выполнение 3 задач за день.', '" + RewardType.EXPERIENCE.name() + "', 'BASE*25*1.05');");
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_CHALLENGE_C3 + ", 'Недельный бонус', 'Награда за выполнение 15 задач за неделю.', '" + RewardType.COINS.name() + "', 'BASE*100*1.1');");
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_CHALLENGE_C4_BADGE + ", 'Значок ''Первая Неделя''', 'За 7 дней стрика!', '" + RewardType.BADGE.name() + "', '" + BADGE_ID_STREAK_7 + "');");
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_CHALLENGE_C4_FALLBACK + ", 'Повторный успех (7 дней)', 'У тебя уже есть значок за 7 дней! Вот монеты.', '" + RewardType.COINS.name() + "', 'BASE*50*1.1');");
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_CHALLENGE_POMODORO_STARTER_BADGE + ", 'Значок ''Мастер Фокуса''', 'За 5 успешных Pomodoro сессий.', '" + RewardType.BADGE.name() + "', '" + BADGE_ID_POMODORO_5 + "');");
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_CHALLENGE_POMODORO_STARTER_COINS + ", 'Бонус Фокуса', 'Значок ''Мастер Фокуса'' уже есть! Держи монеты.', '" + RewardType.COINS.name() + "', 'BASE*30*1.08');");
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_CHALLENGE_LEVEL_5_BADGE + ", 'Значок ''Новичок+''', 'Достигнут 5 уровень!', '" + RewardType.BADGE.name() + "', '" + BADGE_ID_LEVEL_5 + "');");
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_CHALLENGE_LEVEL_5_COINS + ", 'Награда Новичка', 'Значок за 5 уровень уже есть! Получи монеты.', '" + RewardType.COINS.name() + "', 'BASE*75*1.08');");
        // ...
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_CHALLENGE_C134_BADGE + ", 'Значок ''Легенда 100''', 'Достигнут 100 уровень!', '" + RewardType.BADGE.name() + "', '" + BADGE_ID_CHALLENGE_LEVEL_100 + "');");
        db.execSQL("INSERT OR IGNORE INTO reward (id, name, description, reward_type, reward_value) VALUES (" + REWARD_ID_CHALLENGE_C134_FALLBACK + ", 'Дань Легенде', 'Значок за 100 уровень уже есть! Получи монеты.', '" + RewardType.COINS.name() + "', 'BASE*1000*1.15');");
    }

    private void insertAllDefaultBadges(SupportSQLiteDatabase db) {
        db.execSQL("INSERT OR IGNORE INTO badge (id, name, description, image_url, criteria) VALUES (" + BADGE_ID_STREAK_7 + ", 'Первая Неделя', 'Вы получали награду 7 дней подряд!', " + BADGE_DRAWABLE_STREAK_7 + ", 'Стрик 7 дней');");
        db.execSQL("INSERT OR IGNORE INTO badge (id, name, description, image_url, criteria) VALUES (" + BADGE_ID_STREAK_14 + ", 'Двухнедельная стойкость', 'Две недели подряд! Твоя дисциплина впечатляет.', " + BADGE_DRAWABLE_STREAK_14 + ", 'Стрик 14 дней');");
        db.execSQL("INSERT OR IGNORE INTO badge (id, name, description, image_url, criteria) VALUES (" + BADGE_ID_STREAK_30 + ", 'Месяц дисциплины', 'Целый месяц ежедневных успехов!', " + BADGE_DRAWABLE_STREAK_30 + ", 'Стрик 30 дней');");
        db.execSQL("INSERT OR IGNORE INTO badge (id, name, description, image_url, criteria) VALUES (" + BADGE_ID_POMODORO_5 + ", 'Мастер Фокуса', '5 успешных Pomodoro сессий.', " + BADGE_DRAWABLE_POMODORO_5 + ", '5 Pomodoro');");
        db.execSQL("INSERT OR IGNORE INTO badge (id, name, description, image_url, criteria) VALUES (" + BADGE_ID_LEVEL_5 + ", 'Новичок+', 'Достигнут 5 уровень.', " + BADGE_DRAWABLE_LEVEL_5 + ", '5 уровень');");
        // ...
        db.execSQL("INSERT OR IGNORE INTO badge (id, name, description, image_url, criteria) VALUES (" + BADGE_ID_CHALLENGE_VETERAN_250 + ", 'Ветеран 250', 'Выполнено 250 задач!', " + BADGE_DRAWABLE_CHALLENGE_VETERAN_250 + ", 'Завершить 250 задач');");
        db.execSQL("INSERT OR IGNORE INTO badge (id, name, description, image_url, criteria) VALUES (" + BADGE_ID_CHALLENGE_LEVEL_100 + ", 'Легенда 100', 'Достигнут 100 уровень!', " + BADGE_DRAWABLE_CHALLENGE_LEVEL_100 + ", 'Достичь 100 уровня');");
    }

    private void insertDefaultStreakRewardDefinitions(SupportSQLiteDatabase db) {
        db.execSQL("INSERT OR IGNORE INTO streak_reward_definition (streak_day, reward_id) VALUES (1, " + REWARD_ID_DAY_1 + ");");
        db.execSQL("INSERT OR IGNORE INTO streak_reward_definition (streak_day, reward_id) VALUES (2, " + REWARD_ID_DAY_2 + ");");
        db.execSQL("INSERT OR IGNORE INTO streak_reward_definition (streak_day, reward_id) VALUES (3, " + REWARD_ID_DAY_3 + ");");
        db.execSQL("INSERT OR IGNORE INTO streak_reward_definition (streak_day, reward_id) VALUES (4, " + REWARD_ID_DAY_4 + ");");
        db.execSQL("INSERT OR IGNORE INTO streak_reward_definition (streak_day, reward_id) VALUES (5, " + REWARD_ID_DAY_5 + ");");
        db.execSQL("INSERT OR IGNORE INTO streak_reward_definition (streak_day, reward_id) VALUES (6, " + REWARD_ID_DAY_6 + ");");
        db.execSQL("INSERT OR IGNORE INTO streak_reward_definition (streak_day, reward_id) VALUES (7, " + REWARD_ID_DAY_7_BADGE + ");");
        // Для 14 и 30 дней, если они остались, нужны соответствующие ID наград
    }

    private void insertDefaultChallengesAndRules(SupportSQLiteDatabase db) {
        String minDateString = "'MIN'";
        String maxDateString = "'MAX'";
        String now = "'" + LocalDateTime.now().toString() + "'"; // Для EVENT челленджей, если нужно текущее время
        String nextWeek = "'" + LocalDateTime.now().plusWeeks(1).toString() + "'";

        // 1. Первые шаги (ONCE)
        db.execSQL("INSERT OR IGNORE INTO Challenge (id, name, description, start_date, end_date, reward_id, status, period) VALUES (" + CHALLENGE_ID_C1 + ", 'Первые шаги', 'Заверши свою первую задачу', " + minDateString + ", " + maxDateString + ", " + REWARD_ID_CHALLENGE_C1 + ", '" + ChallengeStatus.ACTIVE.name() + "', '" + ChallengePeriod.ONCE.name() + "');");
        db.execSQL("INSERT OR IGNORE INTO challenge_rule (challenge_id, type, target, period, condition_json) VALUES (" + CHALLENGE_ID_C1 + ", '" + ChallengeType.TASK_COMPLETION.name() + "', 1, '" + ChallengePeriod.ONCE.name() + "', NULL);");

        // 2. Ежедневный фокус (DAILY)
        db.execSQL("INSERT OR IGNORE INTO Challenge (id, name, description, start_date, end_date, reward_id, status, period) VALUES (" + CHALLENGE_ID_C2_DAILY_FOCUS + ", 'Ежедневный фокус', 'Выполни 3 задачи сегодня', " + minDateString + ", " + maxDateString + ", " + REWARD_ID_CHALLENGE_C2 + ", '" + ChallengeStatus.ACTIVE.name() + "', '" + ChallengePeriod.DAILY.name() + "');");
        db.execSQL("INSERT OR IGNORE INTO challenge_rule (challenge_id, type, target, period, condition_json) VALUES (" + CHALLENGE_ID_C2_DAILY_FOCUS + ", '" + ChallengeType.TASK_COMPLETION.name() + "', 3, '" + ChallengePeriod.DAILY.name() + "', NULL);");

        // 3. Недельный марафон (WEEKLY)
        db.execSQL("INSERT OR IGNORE INTO Challenge (id, name, description, start_date, end_date, reward_id, status, period) VALUES (" + CHALLENGE_ID_C3_WEEKLY_MARATHON + ", 'Недельный марафон', 'Заверши 15 задач за неделю', " + minDateString + ", " + maxDateString + ", " + REWARD_ID_CHALLENGE_C3 + ", '" + ChallengeStatus.ACTIVE.name() + "', '" + ChallengePeriod.WEEKLY.name() + "');");
        db.execSQL("INSERT OR IGNORE INTO challenge_rule (challenge_id, type, target, period, condition_json) VALUES (" + CHALLENGE_ID_C3_WEEKLY_MARATHON + ", '" + ChallengeType.TASK_COMPLETION.name() + "', 15, '" + ChallengePeriod.WEEKLY.name() + "', NULL);");

        // 4. 7 дней стрик (ONCE) - уже было, проверяем ID
        db.execSQL("INSERT OR IGNORE INTO Challenge (id, name, description, start_date, end_date, reward_id, status, period) VALUES (" + CHALLENGE_ID_C4_7_DAY_STREAK + ", 'Первая Неделя Продуктивности', 'Удерживайте ежедневный стрик наград 7 дней подряд.', " + minDateString + ", " + maxDateString + ", " + REWARD_ID_CHALLENGE_C4_BADGE + ", '" + ChallengeStatus.ACTIVE.name() + "', '" + ChallengePeriod.ONCE.name() + "');");
        db.execSQL("INSERT OR IGNORE INTO challenge_rule (challenge_id, type, target, period, condition_json) VALUES (" + CHALLENGE_ID_C4_7_DAY_STREAK + ", '" + ChallengeType.DAILY_STREAK.name() + "', 7, '" + ChallengePeriod.ONCE.name() + "', NULL);");

        // 5. Pomodoro Стартер (ONCE)
        db.execSQL("INSERT OR IGNORE INTO Challenge (id, name, description, start_date, end_date, reward_id, status, period) VALUES (" + CHALLENGE_ID_C5_POMODORO_STARTER + ", 'Pomodoro Стартер', 'Заверши 5 полных Pomodoro сессий (фокус).', " + minDateString + ", " + maxDateString + ", " + REWARD_ID_CHALLENGE_POMODORO_STARTER_BADGE + ", '" + ChallengeStatus.ACTIVE.name() + "', '" + ChallengePeriod.ONCE.name() + "');");
        db.execSQL("INSERT OR IGNORE INTO challenge_rule (challenge_id, type, target, period, condition_json) VALUES (" + CHALLENGE_ID_C5_POMODORO_STARTER + ", '" + ChallengeType.POMODORO_SESSION.name() + "', 5, '" + ChallengePeriod.ONCE.name() + "', '{\"minDurationMinutes\": 20}');"); // Пример JSON условия

        // 6. Достичь 5 уровня (ONCE)
        db.execSQL("INSERT OR IGNORE INTO Challenge (id, name, description, start_date, end_date, reward_id, status, period) VALUES (" + CHALLENGE_ID_C6_LEVEL_5_ACHIEVER + ", 'Путь Новичка', 'Достигните 5-го уровня в профиле.', " + minDateString + ", " + maxDateString + ", " + REWARD_ID_CHALLENGE_LEVEL_5_BADGE + ", '" + ChallengeStatus.ACTIVE.name() + "', '" + ChallengePeriod.ONCE.name() + "');");
        db.execSQL("INSERT OR IGNORE INTO challenge_rule (challenge_id, type, target, period, condition_json) VALUES (" + CHALLENGE_ID_C6_LEVEL_5_ACHIEVER + ", '" + ChallengeType.LEVEL_REACHED.name() + "', 5, '" + ChallengePeriod.ONCE.name() + "', NULL);");

        // Пример EVENT челленджа
        // db.execSQL("INSERT OR IGNORE INTO Challenge (id, name, description, start_date, end_date, reward_id, status, period) VALUES (101, 'Весенний марафон задач', 'Выполни 10 задач в течении весеннего события!', " + now + ", " + nextWeek + ", 310, '" + ChallengeStatus.UPCOMING.name() + "', '" + ChallengePeriod.EVENT.name() + "');");
        // db.execSQL("INSERT OR IGNORE INTO challenge_rule (challenge_id, type, target, period, condition_json) VALUES (101, '" + ChallengeType.TASK_COMPLETION.name() + "', 10, '" + ChallengePeriod.EVENT.name() + "', NULL);");

    }
}