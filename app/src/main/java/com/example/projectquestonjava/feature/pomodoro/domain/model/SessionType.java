package com.example.projectquestonjava.feature.pomodoro.domain.model;

import android.os.Parcel;
import android.os.Parcelable;

public enum SessionType implements Parcelable {
    FOCUS,
    SHORT_BREAK,
    LONG_BREAK;

    public boolean isFocus() {
        return this == FOCUS;
    }

    public boolean isShortBreak() {
        return this == SHORT_BREAK;
    }

    public boolean isLongBreak() {
        return this == LONG_BREAK;
    }

    public boolean isBreak() {
        return this == SHORT_BREAK || this == LONG_BREAK;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SessionType> CREATOR = new Creator<SessionType>() {
        @Override
        public SessionType createFromParcel(Parcel in) {
            return SessionType.valueOf(in.readString());
        }

        @Override
        public SessionType[] newArray(int size) {
            return new SessionType[size];
        }
    };
}