package com.example.projectquestonjava.feature.pomodoro.domain.logic;

import com.example.projectquestonjava.feature.pomodoro.domain.model.PomodoroPhase;
import com.example.projectquestonjava.feature.pomodoro.domain.model.SessionType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PomodoroCycleGenerator {

    // Константы теперь static final
    public static final int FOCUS_DURATION_MINUTES = 25;
    public static final int SHORT_BREAK_DURATION_MINUTES = 5;
    public static final int LONG_BREAK_DURATION_MINUTES = 15;
    public static final int INTERVAL_BEFORE_LONG_BREAK = 4;
    public static final int MIN_FOCUS_SESSION_FOR_TAIL_MINUTES = 10;

    @Inject
    public PomodoroCycleGenerator() {
    }

    public List<PomodoroPhase> generatePhases(int estimatedTotalMinutes) {
        if (estimatedTotalMinutes < MIN_FOCUS_SESSION_FOR_TAIL_MINUTES) {
            if (estimatedTotalMinutes > 0) {
                List<PomodoroPhase> singlePhaseList = new ArrayList<>();
                singlePhaseList.add(new PomodoroPhase(SessionType.FOCUS, estimatedTotalMinutes * 60, 1, 1));
                return singlePhaseList;
            } else {
                return Collections.emptyList();
            }
        }

        List<PomodoroPhase> phases = new ArrayList<>();
        int remainingMinutes = estimatedTotalMinutes;
        int focusSessionsInCurrentShortCycle = 0;
        int totalFocusSessionOrder = 0;

        while (true) {
            if (remainingMinutes >= MIN_FOCUS_SESSION_FOR_TAIL_MINUTES) {
                totalFocusSessionOrder++;
                focusSessionsInCurrentShortCycle++;

                int currentFocusDuration;
                if (remainingMinutes >= FOCUS_DURATION_MINUTES) {
                    currentFocusDuration = FOCUS_DURATION_MINUTES;
                } else {
                    currentFocusDuration = remainingMinutes;
                }
                phases.add(new PomodoroPhase(
                        SessionType.FOCUS,
                        currentFocusDuration * 60,
                        focusSessionsInCurrentShortCycle,
                        totalFocusSessionOrder
                ));
                remainingMinutes -= currentFocusDuration;
            } else {
                break; // Выходим из цикла, если оставшегося времени недостаточно для минимальной фокус-сессии
            }

            // Определяем тип и длительность перерыва
            boolean isTimeForLongBreak = focusSessionsInCurrentShortCycle % INTERVAL_BEFORE_LONG_BREAK == 0;
            int breakDurationToAdd = isTimeForLongBreak ? LONG_BREAK_DURATION_MINUTES : SHORT_BREAK_DURATION_MINUTES;
            SessionType breakTypeToAdd = isTimeForLongBreak ? SessionType.LONG_BREAK : SessionType.SHORT_BREAK;

            // Проверяем, был ли предыдущий фокус стандартной длины
            int lastFocusDurationMinutes = 0;
            // Ищем последнюю фокусную сессию
            for (int i = phases.size() - 1; i >= 0; i--) {
                if (phases.get(i).isFocus()) {
                    lastFocusDurationMinutes = phases.get(i).getDurationSeconds() / 60;
                    break;
                }
            }


            if (remainingMinutes > 0 || (remainingMinutes == 0 && lastFocusDurationMinutes == FOCUS_DURATION_MINUTES)) {
                // Добавляем перерыв, если он помещается или если предыдущий фокус был полным
                if (remainingMinutes >= breakDurationToAdd || (lastFocusDurationMinutes == FOCUS_DURATION_MINUTES && remainingMinutes == 0) ) {
                    phases.add(new PomodoroPhase(breakTypeToAdd, breakDurationToAdd * 60, 0, 0));
                    remainingMinutes -= breakDurationToAdd; // remainingMinutes может стать отрицательным, если перерыв "выходит" за общее время
                    remainingMinutes = Math.max(0, remainingMinutes); // Не даем уйти в глубокий минус, если перерыв был добавлен "в долг"
                } else {
                    break;
                }
            } else {
                break;
            }


            // Если после перерыва осталось слишком мало времени для новой фокус-сессии, завершаем цикл
            if (remainingMinutes < MIN_FOCUS_SESSION_FOR_TAIL_MINUTES) {
                break;
            }
        }
        return phases;
    }
}