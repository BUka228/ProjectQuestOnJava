package com.example.projectquestonjava.feature.gamification.domain.model;

public final class GamificationConstants {
    private GamificationConstants() {}

    public static final String POMODORO_FOCUS_XP_REWARD_VALUE = "10";
    public static final String POMODORO_FOCUS_COIN_REWARD_VALUE = "2";
    public static final int GROWTH_POINTS_PER_COMPLETED_FOCUS_SESSION = 2;
    public static final int MIN_FOCUS_DURATION_FOR_REWARD_MINUTES = 10;

    public static final String HISTORY_REASON_POMODORO_COMPLETED = "POMODORO_COMPLETED";
    public static final String HISTORY_REASON_TASK_COMPLETED = "TASK_COMPLETED";
    public static final String HISTORY_REASON_DAILY_REWARD = "DAILY_REWARD";
    public static final String HISTORY_REASON_CHALLENGE_COMPLETED = "CHALLENGE_COMPLETED";
    public static final String HISTORY_REASON_STORE_PURCHASE = "STORE_PURCHASE";
    public static final String HISTORY_REASON_SURPRISE_TASK_COMPLETED = "SURPRISE_TASK_COMPLETED";
}