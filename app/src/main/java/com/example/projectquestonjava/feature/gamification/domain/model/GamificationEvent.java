package com.example.projectquestonjava.feature.gamification.domain.model;

import com.example.projectquestonjava.core.data.model.core.Tag;
import java.util.List;
import java.util.Objects;

import lombok.Getter;


public abstract class GamificationEvent {
    private GamificationEvent() {}

    @Getter
    public static final class TaskCompleted extends GamificationEvent {
        private final long taskId;
        private final List<Tag> tags;

        public TaskCompleted(long taskId, List<Tag> tags) {
            this.taskId = taskId;
            this.tags = tags;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TaskCompleted that = (TaskCompleted) o;
            return taskId == that.taskId && Objects.equals(tags, that.tags);
        }

        @Override
        public int hashCode() {
            return Objects.hash(taskId, tags);
        }
    }

    @Getter
    public static final class PomodoroCompleted extends GamificationEvent {
        private final long sessionId;
        private final int durationSeconds;
        private final Long taskId; // Может быть null

        public PomodoroCompleted(long sessionId, int durationSeconds, Long taskId) {
            this.sessionId = sessionId;
            this.durationSeconds = durationSeconds;
            this.taskId = taskId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PomodoroCompleted that = (PomodoroCompleted) o;
            return sessionId == that.sessionId && durationSeconds == that.durationSeconds && Objects.equals(taskId, that.taskId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sessionId, durationSeconds, taskId);
        }
    }

    @Getter
    public static final class StreakUpdated extends GamificationEvent {
        private final int newStreakValue;

        public StreakUpdated(int newStreakValue) {
            this.newStreakValue = newStreakValue;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StreakUpdated that = (StreakUpdated) o;
            return newStreakValue == that.newStreakValue;
        }

        @Override
        public int hashCode() {
            return Objects.hash(newStreakValue);
        }
    }
}