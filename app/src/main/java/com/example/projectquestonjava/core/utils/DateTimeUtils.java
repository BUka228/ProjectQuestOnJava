package com.example.projectquestonjava.core.utils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DateTimeUtils {
    private final ZoneId defaultZoneId;
    private final ZoneOffset utcZoneOffset = ZoneOffset.UTC;

    @Inject
    public DateTimeUtils() {
        this.defaultZoneId = ZoneId.systemDefault();
    }

    // Конструктор для тестов или специфической зоны
    public DateTimeUtils(ZoneId zoneId) {
        this.defaultZoneId = zoneId;
    }

    public LocalDateTime currentLocalDateTime() {
        return LocalDateTime.now(defaultZoneId);
    }

    public LocalDate currentLocalDate() {
        return LocalDate.now(defaultZoneId);
    }

    public LocalDateTime currentUtcDateTime() {
        return LocalDateTime.now(utcZoneOffset);
    }

    public Pair<Long, Long> calculateUtcDayBoundariesEpochSeconds(LocalDateTime dateTime) {
        LocalDate localDate = dateTime.toLocalDate();
        long startOfDayUtc = localDate.atStartOfDay().toEpochSecond(utcZoneOffset);
        long endOfDayUtc = localDate.atTime(LocalTime.MAX).toEpochSecond(utcZoneOffset);
        return new Pair<>(startOfDayUtc, endOfDayUtc);
    }

    public Pair<Long, Long> calculateUtcMonthBoundariesEpochSeconds(LocalDate localDate) {
        long startOfMonthUtc = localDate.withDayOfMonth(1).atStartOfDay().toEpochSecond(utcZoneOffset);
        long endOfMonthUtc = localDate.withDayOfMonth(localDate.lengthOfMonth())
                .atTime(LocalTime.MAX).toEpochSecond(utcZoneOffset);
        return new Pair<>(startOfMonthUtc, endOfMonthUtc);
    }

    public long calculateDurationUntilDue(LocalDateTime dueDate) {
        LocalDateTime nowLocal = currentLocalDateTime();
        Duration duration = Duration.between(nowLocal, dueDate);
        return duration.toMinutes();
    }

    public LocalDateTime utcToLocalLocalDateTime(LocalDateTime utcDateTime) {
        return utcDateTime.atZone(utcZoneOffset)
                .withZoneSameInstant(defaultZoneId)
                .toLocalDateTime();
    }

    public LocalDateTime localToUtcLocalDateTime(LocalDateTime localDateTime) {
        return localDateTime.atZone(defaultZoneId)
                .withZoneSameInstant(utcZoneOffset)
                .toLocalDateTime();
    }

    public LocalDateTime epochSecondsToLocalLocalDateTime(long epochSeconds) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), defaultZoneId);
    }

    public record Pair<F, S>(F first, S second) { }
}