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

    public PlanningUiState copy(
            @Nullable Boolean isLoading,
            @Nullable String error,
            @Nullable String successMessage
    ) {
        return new PlanningUiState(
                isLoading != null ? isLoading : this.isLoading,
                error,
                successMessage
        );
    }

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