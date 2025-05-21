package com.example.projectquestonjava.feature.pomodoro.domain.model;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull; // Для Parcelable CREATOR
import com.example.projectquestonjava.feature.pomodoro.domain.model.SessionType;
import java.time.LocalDateTime;
import java.util.Objects;

import lombok.Getter;

@Getter
public class InterruptedPhaseInfo implements Parcelable {
    private final long dbSessionId;
    private final SessionType type;
    private final LocalDateTime startTime; // UTC
    private final int interruptions;

    public InterruptedPhaseInfo(long dbSessionId, SessionType type, LocalDateTime startTime, int interruptions) {
        this.dbSessionId = dbSessionId;
        this.type = type;
        this.startTime = startTime;
        this.interruptions = interruptions;
    }

    protected InterruptedPhaseInfo(Parcel in) {
        dbSessionId = in.readLong();
        type = SessionType.valueOf(in.readString()); // SessionType должен быть Parcelable или иметь valueOf(String)
        startTime = (LocalDateTime) in.readSerializable(); // LocalDateTime сериализуем
        interruptions = in.readInt();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(dbSessionId);
        dest.writeString(type.name());
        dest.writeSerializable(startTime);
        dest.writeInt(interruptions);
    }

    @Override
    public int describeContents() {
        return 0;
    }


    public static final Creator<InterruptedPhaseInfo> CREATOR = new Creator<InterruptedPhaseInfo>() {
        @Override
        public InterruptedPhaseInfo createFromParcel(Parcel in) {
            return new InterruptedPhaseInfo(in);
        }

        @Override
        public InterruptedPhaseInfo[] newArray(int size) {
            return new InterruptedPhaseInfo[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InterruptedPhaseInfo that = (InterruptedPhaseInfo) o;
        return dbSessionId == that.dbSessionId &&
                interruptions == that.interruptions &&
                type == that.type &&
                Objects.equals(startTime, that.startTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dbSessionId, type, startTime, interruptions);
    }

    @NonNull
    @Override
    public String toString() {
        return "InterruptedPhaseInfo{" +
                "dbSessionId=" + dbSessionId +
                ", type=" + type +
                ", startTime=" + startTime +
                ", interruptions=" + interruptions +
                '}';
    }
}