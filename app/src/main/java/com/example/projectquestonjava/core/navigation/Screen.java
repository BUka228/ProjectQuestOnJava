package com.example.projectquestonjava.core.navigation;

// Пока простой enum. Аргументы и createRoute можно добавить позже,
// если будет использоваться такой же подход к навигации, как в Compose.
// Для Navigation Component обычно используются ID из nav_graph.
public enum Screen {
    DASHBOARD("dashboard"), // route здесь больше для совместимости с BottomNav, если он его использует
    CALENDAR_TASK_CREATION("calendarTaskCreation"),
    CALENDAR_PLANNING("calendarPlanning"),
    POMODORO("pomodoro"),
    TIMER_SETTINGS("timerSettings"),
    GAMIFICATION("gamification"),
    GARDEN("garden"),
    CHALLENGES("challenges"),
    PROFILE("profile"),
    PROFILE_EDIT("profileEdit"),
    STATISTICS("statistics"),
    SETTINGS("settings");

    public final String route;

    Screen(String route) {
        this.route = route;
    }

    // Для Navigation Component обычно используются ID ресурсов
    // public int getNavigationId() { ... }
}