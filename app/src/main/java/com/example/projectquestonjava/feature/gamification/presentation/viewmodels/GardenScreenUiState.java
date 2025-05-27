package com.example.projectquestonjava.feature.gamification.presentation.viewmodels; // Уточни пакет

import androidx.annotation.Nullable;
import com.example.projectquestonjava.feature.gamification.data.model.VirtualGarden;
import com.example.projectquestonjava.feature.gamification.domain.model.PlantHealthState;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder(toBuilder = true)
public class GardenScreenUiState {

    @With boolean isLoading;
    @With @Nullable String errorMessage;
    @With @Nullable String successMessage;
    @With List<VirtualGarden> plants;
    @With long selectedPlantId;
    @With Map<Long, PlantHealthState> healthStates;
    @With boolean canWaterToday;
    @With boolean showWateringConfirmation;
    @With boolean showWateringPlantEffect;

    public static GardenScreenUiState createDefault() {
        return GardenScreenUiState.builder()
                .isLoading(false)
                .errorMessage(null)
                .successMessage(null)
                .plants(Collections.emptyList())
                .selectedPlantId(-1L)
                .healthStates(Collections.emptyMap())
                .canWaterToday(false)
                .showWateringConfirmation(false)
                .showWateringPlantEffect(false)
                .build();
    }
}