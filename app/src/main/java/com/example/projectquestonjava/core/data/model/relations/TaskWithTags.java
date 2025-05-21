package com.example.projectquestonjava.core.data.model.relations;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import com.example.projectquestonjava.core.data.model.core.Tag;
import com.example.projectquestonjava.core.data.model.core.Task;
import com.example.projectquestonjava.core.data.model.core.TaskTagCrossRef;

import java.util.List;

public class TaskWithTags {

    @Embedded
    public Task task;

    @Relation(
            parentColumn = "id",
            entity = Tag.class,
            entityColumn = "id",
            associateBy = @Junction(
                    value = TaskTagCrossRef.class,
                    parentColumn = "task_id",
                    entityColumn = "tag_id"
            )
    )
    public List<Tag> tags;

    // Конструктор, если нужен
    public TaskWithTags(Task task, List<Tag> tags) {
        this.task = task;
        this.tags = tags;
    }


    public Task getTask() {
        return task;
    }

    public List<Tag> getTags() {
        return tags;
    }
}