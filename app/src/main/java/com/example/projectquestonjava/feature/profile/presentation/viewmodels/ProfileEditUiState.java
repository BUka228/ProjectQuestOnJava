package com.example.projectquestonjava.feature.profile.presentation.viewmodels; // Уточни пакет

import android.net.Uri;
import androidx.annotation.Nullable;
import com.example.projectquestonjava.core.data.model.core.UserAuth;
import lombok.Builder;
import lombok.Value;
import lombok.With; // Если используешь @With на полях

@Value
@Builder(toBuilder = true)
public class ProfileEditUiState {
    @With boolean isLoading;
    @With boolean isSavingUsername;
    @With boolean isSavingPassword;
    @With boolean isAvatarLoading;
    @With boolean isRemovingAvatar;
    @With @Nullable UserAuth user;
    @With String editedUsername;
    @With @Nullable String usernameError;
    @With String currentPassword;
    @With String newPassword;
    @With String confirmPassword;
    @With @Nullable String currentPasswordError;
    @With @Nullable String newPasswordError;
    @With @Nullable String confirmPasswordError;
    @With boolean showPasswordFields;
    @With boolean saveSuccess; // Для сигнала о успешном сохранении и навигации
    @With @Nullable String generalError;
    @With @Nullable String avatarUpdateError;
    @With @Nullable String passwordUpdateError; // Общая ошибка для пароля (не валидация полей)
    @With boolean showAvatarDeleteConfirm;
    @With @Nullable Uri selectedAvatarUri; // Для предпросмотра нового аватара
    @With boolean hasUnsavedChanges;

    public static ProfileEditUiState createDefault() {
        return ProfileEditUiState.builder()
                .isLoading(true)
                .isSavingUsername(false)
                .isSavingPassword(false)
                .isAvatarLoading(false)
                .isRemovingAvatar(false)
                .user(null)
                .editedUsername("")
                .usernameError(null)
                .currentPassword("")
                .newPassword("")
                .confirmPassword("")
                .currentPasswordError(null)
                .newPasswordError(null)
                .confirmPasswordError(null)
                .showPasswordFields(false)
                .saveSuccess(false)
                .generalError(null)
                .avatarUpdateError(null)
                .passwordUpdateError(null)
                .showAvatarDeleteConfirm(false)
                .selectedAvatarUri(null)
                .hasUnsavedChanges(false)
                .build();
    }
}