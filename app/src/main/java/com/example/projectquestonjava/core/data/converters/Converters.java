package com.example.projectquestonjava.core.data.converters;

import androidx.room.TypeConverter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class Converters {
    private final ZoneOffset zoneOffset = ZoneOffset.UTC;

    @TypeConverter
    public LocalDateTime fromTimestamp(Long value) {
        return value == null ? null : LocalDateTime.ofEpochSecond(value, 0, zoneOffset);
    }

    @TypeConverter
    public Long toTimestamp(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toEpochSecond(zoneOffset);
    }

    @TypeConverter
    public LocalDate fromEpochDay(Long value) {
        return value == null ? null : LocalDate.ofEpochDay(value);
    }

    @TypeConverter
    public Long toEpochDay(LocalDate date) {
        return date == null ? null : date.toEpochDay();
    }
}