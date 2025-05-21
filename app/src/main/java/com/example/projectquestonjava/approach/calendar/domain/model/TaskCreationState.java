package com.example.projectquestonjava.approach.calendar.domain.model;

import androidx.annotation.Nullable;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TaskCreationState {
    private TaskInput taskInput = TaskInput.EMPTY;
    private boolean isDateDialogOpen = false;
    private boolean isTimeDialogOpen = false;
    private boolean isRecurrenceDialogOpen = false;
    private boolean isLoading = false;
    @Nullable
    private String error;
    @Nullable
    private TaskCreationEvent event;
    private boolean isEditMode = false;

    public TaskCreationState(TaskInput taskInput, boolean isDateDialogOpen, boolean isTimeDialogOpen, boolean isRecurrenceDialogOpen, boolean isLoading, @Nullable String error, @Nullable TaskCreationEvent event, boolean isEditMode) {
        this.taskInput = taskInput;
        this.isDateDialogOpen = isDateDialogOpen;
        this.isTimeDialogOpen = isTimeDialogOpen;
        this.isRecurrenceDialogOpen = isRecurrenceDialogOpen;
        this.isLoading = isLoading;
        this.error = error;
        this.event = event;
        this.isEditMode = isEditMode;
    }

    // Метод для создания копии с изменениями
    public TaskCreationState copy(
            @Nullable TaskInput taskInput,
            @Nullable Boolean isDateDialogOpen,
            @Nullable Boolean isTimeDialogOpen,
            @Nullable Boolean isRecurrenceDialogOpen,
            @Nullable Boolean isLoading,
            @Nullable String error,
            @Nullable TaskCreationEvent event,
            @Nullable Boolean isEditMode
    ) {
        return new TaskCreationState(
                taskInput != null ? taskInput : this.taskInput,
                isDateDialogOpen != null ? isDateDialogOpen : this.isDateDialogOpen,
                isTimeDialogOpen != null ? isTimeDialogOpen : this.isTimeDialogOpen,
                isRecurrenceDialogOpen != null ? isRecurrenceDialogOpen : this.isRecurrenceDialogOpen,
                isLoading != null ? isLoading : this.isLoading,
                error, // Просто передаем, может быть null
                event, // Просто передаем, может быть null
                isEditMode != null ? isEditMode : this.isEditMode
        );
    }
}