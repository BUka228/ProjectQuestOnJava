package com.example.projectquestonjava.approach.calendar.domain.model;

import androidx.annotation.Nullable;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanningUiState {
    private boolean isLoading = false;
    @Nullable
    private String error;
    @Nullable
    private String successMessage;

    // Метод copy, имитирующий Kotlin data class copy
    public PlanningUiState copy(
            @Nullable Boolean isLoading, // Используем Boolean, чтобы можно было передать null (не менять)
            @Nullable String error,     // String уже nullable
            @Nullable String successMessage // String уже nullable
    ) {
        return new PlanningUiState(
                isLoading != null ? isLoading : this.isLoading,
                error, // Если null, то поле будет null
                successMessage // Если null, то поле будет null
        );
    }

    // Перегрузка copy для случая, когда нужно изменить только одно поле, передав null для остальных.
    // Это не обязательно, но может быть удобно.
    public PlanningUiState copyWithLoading(boolean isLoading) {
        return new PlanningUiState(isLoading, this.error, this.successMessage);
    }
    public PlanningUiState copyWithError(@Nullable String error) {
        return new PlanningUiState(this.isLoading, error, this.successMessage);
    }
    public PlanningUiState copyWithSuccessMessage(@Nullable String successMessage) {
        return new PlanningUiState(this.isLoading, this.error, successMessage);
    }
}