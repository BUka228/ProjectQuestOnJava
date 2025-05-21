package com.example.projectquestonjava.approach.calendar.domain.model;

import androidx.annotation.Nullable;
import com.example.projectquestonjava.core.data.model.core.Tag;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TaskInput {

    private Long id;
    private String title = "";
    private String description = "";
    private LocalDateTime dueDate = LocalDateTime.now();
    @Nullable
    private String recurrenceRule;
    private Set<Tag> selectedTags = Collections.emptySet();

    public TaskInput(Long id, String title, String description, LocalDateTime dueDate, @Nullable String recurrenceRule, Set<Tag> selectedTags) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.recurrenceRule = recurrenceRule;
        this.selectedTags = selectedTags != null ? selectedTags : Collections.emptySet();
    }

    public static final TaskInput EMPTY = new TaskInput(null, "", "", LocalDateTime.now(), null, Collections.emptySet());
}