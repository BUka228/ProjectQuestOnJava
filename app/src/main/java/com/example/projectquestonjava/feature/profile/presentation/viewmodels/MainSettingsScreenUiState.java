package com.example.projectquestonjava.feature.profile.presentation.viewmodels;

import androidx.annotation.Nullable;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder(toBuilder = true)
public class MainSettingsScreenUiState {
    @With @Builder.Default boolean isLoading = false;
    @With @Nullable String error = null;
    @With @Nullable String successMessage = null;

    @With @Builder.Default AppTheme selectedTheme = AppTheme.SYSTEM;
    @With @Builder.Default boolean useDynamicColor = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S;
    @With @Builder.Default boolean notificationsEnabled = true;

    // Состояния диалогов
    @With @Builder.Default boolean showThemeDialog = false;
    @With @Builder.Default boolean showLogoutDialog = false;
    @With @Builder.Default boolean showDeleteAccountDialog = false;

    public static MainSettingsScreenUiState createDefault() {
        return MainSettingsScreenUiState.builder().build();
    }
}