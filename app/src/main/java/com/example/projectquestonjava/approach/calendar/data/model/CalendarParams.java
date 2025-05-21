package com.example.projectquestonjava.approach.calendar.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

import com.example.projectquestonjava.core.data.model.core.Task;

@Entity(
        tableName = "calendar_params",
        primaryKeys = {"task_id"},
        foreignKeys = {
                @ForeignKey(
                        entity = Task.class,
                        parentColumns = {"id"},
                        childColumns = {"task_id"},
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {@Index("task_id")}
)
public class CalendarParams {

    @ColumnInfo(name = "task_id")
    private final long taskId;

    @ColumnInfo(name = "event_id")
    private final String eventId;

    @ColumnInfo(name = "is_all_day")
    private final boolean isAllDay;

    @ColumnInfo(name = "recurrence_rule")
    private final String recurrenceRule; // Может быть null

    public CalendarParams(long taskId, String eventId, boolean isAllDay, String recurrenceRule) {
        this.taskId = taskId;
        this.eventId = eventId;
        this.isAllDay = isAllDay;
        this.recurrenceRule = recurrenceRule;
    }

    // Getters
    public long getTaskId() {
        return taskId;
    }

    public String getEventId() {
        return eventId;
    }

    public boolean isAllDay() {
        return isAllDay;
    }

    public String getRecurrenceRule() {
        return recurrenceRule;
    }

}
