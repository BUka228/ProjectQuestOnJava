package com.example.projectquestonjava.feature.gamification.domain.model;

// Переносим ChallengeStatus.kt
public enum ChallengeStatus {
    ACTIVE,     // Челлендж активен и доступен для выполнения
    COMPLETED,  // Челлендж успешно выполнен пользователем
    EXPIRED,    // Срок выполнения челленджа истек
    UPCOMING,   // Челлендж еще не начался (для будущих событий)
    INACTIVE    // Челлендж неактивен/отключен
}