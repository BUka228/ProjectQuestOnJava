package com.example.projectquestonjava.feature.pomodoro.domain.model;

import android.os.Parcel;
import android.os.Parcelable;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PomodoroPhase implements Parcelable {
    private final SessionType type;
    private final int durationSeconds;
    private final int phaseNumberInCycle;
    private final int totalFocusSessionIndex;

    public boolean isFocus() {
        return type == SessionType.FOCUS;
    }

    public boolean isShortBreak() {
        return type == SessionType.SHORT_BREAK;
    }

    public boolean isLongBreak() {
        return type == SessionType.LONG_BREAK;
    }

    public boolean isBreak() {
        return type == SessionType.SHORT_BREAK || type == SessionType.LONG_BREAK;
    }

    // Реализация Parcelable
    protected PomodoroPhase(Parcel in) {
        type = SessionType.valueOf(in.readString());
        durationSeconds = in.readInt();
        phaseNumberInCycle = in.readInt();
        totalFocusSessionIndex = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(type.name());
        dest.writeInt(durationSeconds);
        dest.writeInt(phaseNumberInCycle);
        dest.writeInt(totalFocusSessionIndex);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PomodoroPhase> CREATOR = new Creator<PomodoroPhase>() {
        @Override
        public PomodoroPhase createFromParcel(Parcel in) {
            return new PomodoroPhase(in);
        }

        @Override
        public PomodoroPhase[] newArray(int size) {
            return new PomodoroPhase[size];
        }
    };
}