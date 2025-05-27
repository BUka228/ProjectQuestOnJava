package com.example.projectquestonjava.feature.profile.presentation.viewmodels;

public class MainSettingsScreenUiState {
    public final boolean isLoading;
    public final String error;
    public final String successMessage;
    public final AppTheme selectedTheme;
    public final boolean useDynamicColor;
    public final boolean notificationsEnabled;
    public final boolean showThemeDialog;
    public final boolean showLogoutDialog;
    public final boolean showDeleteAccountDialog;

    public MainSettingsScreenUiState(boolean isLoading, String error, String successMessage,
                                     AppTheme selectedTheme, boolean useDynamicColor, boolean notificationsEnabled,
                                     boolean showThemeDialog, boolean showLogoutDialog, boolean showDeleteAccountDialog) {
        this.isLoading = isLoading;
        this.error = error;
        this.successMessage = successMessage;
        this.selectedTheme = selectedTheme;
        this.useDynamicColor = useDynamicColor;
        this.notificationsEnabled = notificationsEnabled;
        this.showThemeDialog = showThemeDialog;
        this.showLogoutDialog = showLogoutDialog;
        this.showDeleteAccountDialog = showDeleteAccountDialog;
    }

    // Конструктор по умолчанию
    public MainSettingsScreenUiState() {
        this(false, null, null, AppTheme.SYSTEM, true, true, false, false, false);
    }

    // Метод copy для удобства обновления (аналог Kotlin data class copy)
    public MainSettingsScreenUiState copy(
            Boolean isLoading, String error, String successMessage,
            AppTheme selectedTheme, Boolean useDynamicColor, Boolean notificationsEnabled,
            Boolean showThemeDialog, Boolean showLogoutDialog, Boolean showDeleteAccountDialog,
            boolean clearError, boolean clearSuccessMessage // Флаги для явной очистки
    ) {
        return new MainSettingsScreenUiState(
                isLoading != null ? isLoading : this.isLoading,
                clearError ? null : (error != null ? error : this.error),
                clearSuccessMessage ? null : (successMessage != null ? successMessage : this.successMessage),
                selectedTheme != null ? selectedTheme : this.selectedTheme,
                useDynamicColor != null ? useDynamicColor : this.useDynamicColor,
                notificationsEnabled != null ? notificationsEnabled : this.notificationsEnabled,
                showThemeDialog != null ? showThemeDialog : this.showThemeDialog,
                showLogoutDialog != null ? showLogoutDialog : this.showLogoutDialog,
                showDeleteAccountDialog != null ? showDeleteAccountDialog : this.showDeleteAccountDialog
        );
    }
}