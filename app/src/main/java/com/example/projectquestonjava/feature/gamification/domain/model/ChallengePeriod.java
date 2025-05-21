package com.example.projectquestonjava.feature.gamification.domain.model;

// Переносим ChallengePeriod.kt
public enum ChallengePeriod {
    ONCE,       // Одноразовый челлендж
    DAILY,      // Ежедневный (прогресс сбрасывается каждый день)
    WEEKLY,     // Еженедельный (прогресс сбрасывается каждую неделю)
    MONTHLY,    // Ежемесячный (прогресс сбрасывается каждый месяц)
    EVENT       // Событийный (действует в рамках дат Challenge)
}