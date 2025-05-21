package com.example.projectquestonjava.approach.calendar.domain.model;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;
import com.example.projectquestonjava.core.data.model.core.Tag;
import com.example.projectquestonjava.core.data.model.core.Task;
import com.example.projectquestonjava.core.data.model.core.TaskTagCrossRef;
import com.example.projectquestonjava.approach.calendar.data.model.CalendarParams;
import java.util.List;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarTaskWithTagsAndPomodoro {

    @Embedded 
    private Task task;

    @Relation(
            parentColumn = "id", 
            entityColumn = "task_id",
            entity = CalendarParams.class
    )
    private CalendarParams calendarParams;

    @Relation(
            parentColumn = "id", 
            entityColumn = "id",
            entity = Tag.class,
            associateBy = @Junction(
                    value = TaskTagCrossRef.class,
                    parentColumn = "task_id",
                    entityColumn = "tag_id"
            )
    )
    private List<Tag> tags;

    private int pomodoroCount = 0;
}