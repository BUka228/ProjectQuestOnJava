package com.example.projectquestonjava.feature.pomodoro.domain.model;

import lombok.Getter;


public abstract class TimerState {
    private TimerState() {}

    public static final class Idle extends TimerState {
        private static final Idle INSTANCE = new Idle();
        public static Idle getInstance() { return INSTANCE; }
    }

    @Getter
    public static final class Running extends TimerState {
        private final int remainingSeconds;
        private final int totalSeconds;
        private final SessionType type;
        private final int interruptions;

        public Running(int remainingSeconds, int totalSeconds, SessionType type, int interruptions) {
            this.remainingSeconds = remainingSeconds;
            this.totalSeconds = totalSeconds;
            this.type = type;
            this.interruptions = interruptions;
        }
        public Running(int remainingSeconds, int totalSeconds, SessionType type) {
            this(remainingSeconds, totalSeconds, type, 0);
        }


        public Running copy(Integer remainingSeconds, Integer totalSeconds, SessionType type, Integer interruptions) {
            return new Running(
                    remainingSeconds != null ? remainingSeconds : this.remainingSeconds,
                    totalSeconds != null ? totalSeconds : this.totalSeconds,
                    type != null ? type : this.type,
                    interruptions != null ? interruptions : this.interruptions
            );
        }
    }

    @Getter
    public static final class Paused extends TimerState {
        private final int remainingSeconds;
        private final int totalSeconds;
        private final SessionType type;
        private final int interruptions;

        public Paused(int remainingSeconds, int totalSeconds, SessionType type, int interruptions) {
            this.remainingSeconds = remainingSeconds;
            this.totalSeconds = totalSeconds;
            this.type = type;
            this.interruptions = interruptions;
        }
        public Paused(int remainingSeconds, int totalSeconds, SessionType type) {
            this(remainingSeconds, totalSeconds, type, 0);
        }


        public Paused copy(Integer remainingSeconds, Integer totalSeconds, SessionType type, Integer interruptions) {
            return new Paused(
                    remainingSeconds != null ? remainingSeconds : this.remainingSeconds,
                    totalSeconds != null ? totalSeconds : this.totalSeconds,
                    type != null ? type : this.type,
                    interruptions != null ? interruptions : this.interruptions
            );
        }
    }

    @Getter
    public static final class WaitingForConfirmation extends TimerState {
        private final SessionType type;
        private final int totalSeconds;
        private final int interruptions;

        public WaitingForConfirmation(SessionType type, int totalSeconds, int interruptions) {
            this.type = type;
            this.totalSeconds = totalSeconds;
            this.interruptions = interruptions;
        }
        public WaitingForConfirmation(SessionType type, int totalSeconds) {
            this(type, totalSeconds, 0);
        }

    }
}