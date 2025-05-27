package com.example.projectquestonjava.feature.profile.presentation.viewmodels;

import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.projectquestonjava.core.data.model.core.UserAuth;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.managers.SnackbarManager;
import com.example.projectquestonjava.core.managers.UserSessionManager;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.profile.domain.usecases.RemoveAvatarUseCase;
import com.example.projectquestonjava.feature.profile.domain.usecases.UpdateAvatarUseCase;
import com.example.projectquestonjava.feature.profile.domain.usecases.UpdatePasswordUseCase;
import com.example.projectquestonjava.feature.profile.domain.usecases.UpdateUsernameUseCase;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import dagger.hilt.android.lifecycle.HiltViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Function;

import javax.inject.Inject;

@HiltViewModel
public class ProfileEditViewModel extends ViewModel {

    private static final String TAG = "ProfileEditViewModel";

    private final UserSessionManager userSessionManager;
    private final UpdateUsernameUseCase updateUsernameUseCase;
    private final UpdateAvatarUseCase updateAvatarUseCase;
    private final UpdatePasswordUseCase updatePasswordUseCase;
    private final RemoveAvatarUseCase removeAvatarUseCase;
    private final SnackbarManager snackbarManager;
    private final Executor ioExecutor;
    private final Logger logger;

    // LiveData для UI состояния
    private final MutableLiveData<ProfileEditUiState> _uiState = new MutableLiveData<>(ProfileEditUiState.createDefault());
    public final LiveData<ProfileEditUiState> uiStateLiveData = _uiState;

    // Оригинальные значения для отслеживания изменений
    private String originalUsername = "";
    private String originalAvatarUrl = null;


    @Inject
    public ProfileEditViewModel(
            UserSessionManager userSessionManager,
            UpdateUsernameUseCase updateUsernameUseCase,
            UpdateAvatarUseCase updateAvatarUseCase,
            UpdatePasswordUseCase updatePasswordUseCase,
            RemoveAvatarUseCase removeAvatarUseCase,
            SnackbarManager snackbarManager,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.userSessionManager = userSessionManager;
        this.updateUsernameUseCase = updateUsernameUseCase;
        this.updateAvatarUseCase = updateAvatarUseCase;
        this.updatePasswordUseCase = updatePasswordUseCase;
        this.removeAvatarUseCase = removeAvatarUseCase;
        this.snackbarManager = snackbarManager;
        this.ioExecutor = ioExecutor;
        this.logger = logger;

        loadInitialUserData();
    }

    private void loadInitialUserData() {
        logger.debug(TAG, "Loading initial user data...");
        updateUiState(s -> s.toBuilder().isLoading(true).build().toBuilder());

        ListenableFuture<UserAuth> userFuture = userSessionManager.getCurrentUser();
        Futures.addCallback(userFuture, new FutureCallback<UserAuth>() {
            @Override
            public void onSuccess(@Nullable UserAuth user) {
                if (user != null) {
                    originalUsername = user.getUsername();
                    originalAvatarUrl = user.getAvatarUrl();
                    _uiState.postValue(
                            ProfileEditUiState.createDefault().toBuilder()
                                    .isLoading(false)
                                    .user(user)
                                    .editedUsername(user.getUsername())
                                    // selectedAvatarUri остается null, т.к. мы загружаем существующий
                                    .hasUnsavedChanges(false) // Изначально нет несохраненных изменений
                                    .build()
                    );
                    logger.debug(TAG, "User data loaded: " + user.getUsername());
                } else {
                    _uiState.postValue(
                            Objects.requireNonNull(_uiState.getValue()).toBuilder()
                                    .isLoading(false)
                                    .generalError("Не удалось загрузить данные пользователя.")
                                    .build()
                    );
                    logger.error(TAG, "Failed to load user data: User is null.");
                }
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Failed to load user data", t);
                _uiState.postValue(
                        Objects.requireNonNull(_uiState.getValue()).toBuilder()
                                .isLoading(false)
                                .generalError("Ошибка загрузки: " + t.getMessage())
                                .build()
                );
            }
        }, ioExecutor);
    }

    // --- Обработчики изменения полей ---
    public void onUsernameChange(String newName) {
        ProfileEditUiState current = _uiState.getValue();
        if (current == null) return;
        updateUiState(s -> s.toBuilder()
                .editedUsername(newName)
                .usernameError(null) // Сбрасываем ошибку при вводе
                .hasUnsavedChanges(checkIfUsernameChanged(newName, current.getUser()))
                .build().toBuilder()
        );
    }

    public void onCurrentPasswordChange(String password) {
        updateUiState(s -> s.toBuilder().currentPassword(password).currentPasswordError(null).passwordUpdateError(null).hasUnsavedChanges(true).build().toBuilder());
    }
    public void onNewPasswordChange(String password) {
        updateUiState(s -> s.toBuilder().newPassword(password).newPasswordError(null).passwordUpdateError(null).hasUnsavedChanges(true).build().toBuilder());
    }
    public void onConfirmPasswordChange(String password) {
        updateUiState(s -> s.toBuilder().confirmPassword(password).confirmPasswordError(null).passwordUpdateError(null).hasUnsavedChanges(true).build().toBuilder());
    }

    public void toggleShowPasswordFields() {
        ProfileEditUiState current = _uiState.getValue();
        if (current == null) return;
        boolean willShow = !current.isShowPasswordFields();
        updateUiState(s -> s.toBuilder()
                .showPasswordFields(willShow)
                .currentPassword(willShow ? s.getCurrentPassword() : "") // Сбрасываем при скрытии
                .newPassword(willShow ? s.getNewPassword() : "")
                .confirmPassword(willShow ? s.getConfirmPassword() : "")
                .currentPasswordError(null)
                .newPasswordError(null)
                .confirmPasswordError(null)
                .passwordUpdateError(null)
                .hasUnsavedChanges(willShow || s.isHasUnsavedChanges()) // Если открыли - есть изменения
                .build().toBuilder()
        );
    }

    // --- Аватар ---
    public void onAvatarSelected(Uri uri) {
        logger.info(TAG, "New avatar selected by user: " + uri);
        updateUiState(s -> s.toBuilder()
                .selectedAvatarUri(uri) // Показываем предпросмотр
                .isAvatarLoading(true)
                .avatarUpdateError(null)
                .build().toBuilder()
        );

        ListenableFuture<String> updateFuture = updateAvatarUseCase.execute(uri);
        Futures.addCallback(updateFuture, new FutureCallback<String>() {
            @Override
            public void onSuccess(String newAvatarPath) {
                logger.info(TAG, "Avatar updated successfully. New path: " + newAvatarPath);
                originalAvatarUrl = newAvatarPath; // Обновляем оригинальный URL
                updateUiState(s -> s.toBuilder()
                        .isAvatarLoading(false)
                        .user(s.getUser() != null ? s.getUser().copyAvatarUrl(newAvatarPath) : null)
                        .selectedAvatarUri(null) // Убираем предпросмотр
                        .hasUnsavedChanges(true) // Были изменения
                        .build().toBuilder()
                );
                snackbarManager.showMessage("Аватар обновлен!");
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Failed to update avatar", t);
                updateUiState(s -> s.toBuilder()
                        .isAvatarLoading(false)
                        .selectedAvatarUri(null) // Убираем предпросмотр при ошибке
                        .avatarUpdateError("Не удалось обновить аватар: " + t.getMessage())
                        .build().toBuilder()
                );
            }
        }, ioExecutor);
    }

    public void requestAvatarRemoval() {
        ProfileEditUiState s = _uiState.getValue();
        if (s != null && (s.isAvatarLoading() || s.isRemovingAvatar())) return;
        updateUiState(state -> state.toBuilder().showAvatarDeleteConfirm(true).build().toBuilder());
    }

    public void dismissAvatarRemovalDialog() {
        updateUiState(state -> state.toBuilder().showAvatarDeleteConfirm(false).build().toBuilder());
    }

    public void onAvatarDeleteDialogShown() { // Вызывается из фрагмента после показа диалога
        updateUiState(state -> state.toBuilder().showAvatarDeleteConfirm(false).build().toBuilder());
    }

    public void confirmAvatarRemoval() {
        updateUiState(s -> s.toBuilder()
                .showAvatarDeleteConfirm(false)
                .isRemovingAvatar(true)
                .avatarUpdateError(null)
                .selectedAvatarUri(null) // Сбрасываем предпросмотр, если был
                .build().toBuilder()
        );
        ListenableFuture<Void> removeFuture = removeAvatarUseCase.execute();
        Futures.addCallback(removeFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                logger.info(TAG, "Avatar removed successfully.");
                originalAvatarUrl = null; // Обновляем оригинальный URL
                updateUiState(s -> s.toBuilder()
                        .isRemovingAvatar(false)
                        .user(s.getUser() != null ? s.getUser().copyAvatarUrl(null) : null)
                        .hasUnsavedChanges(true)
                        .build().toBuilder()
                );
                snackbarManager.showMessage("Аватар удален.");
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Failed to remove avatar", t);
                updateUiState(s -> s.toBuilder()
                        .isRemovingAvatar(false)
                        .avatarUpdateError("Не удалось удалить аватар: " + t.getMessage())
                        .build().toBuilder()
                );
            }
        }, ioExecutor);
    }

    // --- Сохранение ---
    public void saveChanges() {
        ProfileEditUiState currentState = _uiState.getValue();
        if (currentState == null || currentState.getUser() == null) {
            snackbarManager.showMessage("Данные пользователя еще не загружены.");
            return;
        }
        if (currentState.isAvatarLoading() || currentState.isRemovingAvatar() || currentState.isSavingPassword() || currentState.isSavingUsername()) {
            snackbarManager.showMessage("Подождите завершения текущей операции.");
            return;
        }

        // Проверяем, есть ли вообще изменения, которые нужно сохранить
        boolean usernameChanged = checkIfUsernameChanged(currentState.getEditedUsername(), currentState.getUser());
        boolean passwordFieldsFilled = currentState.isShowPasswordFields() &&
                (!currentState.getCurrentPassword().isEmpty() || !currentState.getNewPassword().isEmpty() || !currentState.getConfirmPassword().isEmpty());
        // hasUnsavedChanges уже должен корректно отслеживать изменения аватара

        if (!usernameChanged && !passwordFieldsFilled && !currentState.isHasUnsavedChanges()) {
            logger.debug(TAG, "No actual changes to save.");
            snackbarManager.showMessage("Нет изменений для сохранения.");
            return;
        }

        updateUiState(s -> s.toBuilder()
                .isSavingUsername(usernameChanged)
                .isSavingPassword(passwordFieldsFilled) // Только если поля заполнены
                .usernameError(null).currentPasswordError(null).newPasswordError(null)
                .confirmPasswordError(null).generalError(null).passwordUpdateError(null)
                .avatarUpdateError(null) // Сбрасываем ошибку аватара тоже
                .build().toBuilder()
        );

        logger.debug(TAG, "Attempting to save changes (username: " + usernameChanged + ", password: " + passwordFieldsFilled + ")");

        List<ListenableFuture<?>> operations = new ArrayList<>();
        final StringBuilder combinedErrorBuilder = new StringBuilder();

        if (usernameChanged) {
            ListenableFuture<Void> usernameFuture = updateUsernameUseCase.execute(currentState.getEditedUsername().trim());
            operations.add(Futures.catchingAsync(usernameFuture, Exception.class, e -> {
                handleOperationError("Имя пользователя", e, combinedErrorBuilder);
                updateUiState(s -> s.toBuilder().usernameError(e instanceof IllegalArgumentException ? e.getMessage() : "Ошибка").build().toBuilder());
                return Futures.immediateFailedFuture(e); // Важно вернуть проваленный Future
            }, MoreExecutors.directExecutor()));
        }

        if (passwordFieldsFilled) {
            if (validatePasswordFields(currentState)) {
                ListenableFuture<Void> passwordFuture = updatePasswordUseCase.execute(
                        currentState.getCurrentPassword(), currentState.getNewPassword()
                );
                operations.add(Futures.catchingAsync(passwordFuture, Exception.class, e -> {
                    handleOperationError("Пароль", e, combinedErrorBuilder);
                    if (e.getMessage() != null && e.getMessage().toLowerCase().contains("неверный текущий пароль")) {
                        updateUiState(s -> s.toBuilder().currentPasswordError(e.getMessage()).build().toBuilder());
                    } else if (e instanceof IllegalArgumentException) {
                        updateUiState(s -> s.toBuilder().newPasswordError(e.getMessage()).build().toBuilder());
                    } else {
                        updateUiState(s -> s.toBuilder().passwordUpdateError("Ошибка смены пароля").build().toBuilder());
                    }
                    return Futures.immediateFailedFuture(e);
                }, MoreExecutors.directExecutor()));
            } else {
                operations.add(Futures.immediateFailedFuture(new IllegalArgumentException("Password validation failed")));
            }
        }

        if (operations.isEmpty()) {
            // Это может произойти, если hasUnsavedChanges было true только из-за аватара,
            // который уже сохранен/удален.
            logger.info(TAG, "No username or password changes to save, but avatar might have changed.");
            updateUiState(s -> s.toBuilder()
                    .isSavingUsername(false).isSavingPassword(false)
                    .saveSuccess(true) // Считаем успехом, если только аватар изменился
                    .hasUnsavedChanges(false) // Все изменения (если были) применены
                    .build().toBuilder()
            );
            snackbarManager.showMessage("Изменения сохранены.");
            return;
        }

        ListenableFuture<List<Object>> allOperationsFuture = Futures.allAsList(operations);
        Futures.addCallback(allOperationsFuture, new FutureCallback<List<Object>>() {
            @Override
            public void onSuccess(List<Object> result) {
                logger.info(TAG, "Profile changes (username/password) saved successfully.");
                UserAuth currentUser = Objects.requireNonNull(_uiState.getValue()).getUser();
                UserAuth updatedUser;
                if (usernameChanged && currentUser != null) {
                    updatedUser = currentUser.copyUsername(currentState.getEditedUsername().trim());
                    originalUsername = updatedUser.getUsername(); // Обновляем оригинал
                } else {
                    updatedUser = currentUser;
                }
                updateUiState(s -> s.toBuilder()
                        .isSavingUsername(false).isSavingPassword(false)
                        .user(updatedUser)
                        .currentPassword(passwordFieldsFilled ? "" : s.getCurrentPassword())
                        .newPassword(passwordFieldsFilled ? "" : s.getNewPassword())
                        .confirmPassword(passwordFieldsFilled ? "" : s.getConfirmPassword())
                        .showPasswordFields(passwordFieldsFilled ? false : s.isShowPasswordFields())
                        .saveSuccess(true)
                        .hasUnsavedChanges(false) // Все сохранено
                        .build().toBuilder()
                );
                snackbarManager.showMessage("Профиль сохранен!");
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.warn(TAG, "One or more profile save operations failed. Combined Error: " + combinedErrorBuilder.toString());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    updateUiState(s -> s.toBuilder()
                            .isSavingUsername(false).isSavingPassword(false)
                            .generalError(!combinedErrorBuilder.isEmpty() ? combinedErrorBuilder.toString().trim() : "Не удалось сохранить изменения.")
                            .build().toBuilder()
                    );
                }
            }
        }, ioExecutor);
    }

    private boolean checkIfUsernameChanged(String newUsername, @Nullable UserAuth currentUser) {
        if (currentUser == null) return !newUsername.trim().isEmpty(); // Если пользователя нет, любое имя - изменение
        return !Objects.equals(currentUser.getUsername(), newUsername.trim());
    }

    private boolean validatePasswordFields(ProfileEditUiState state) {
        boolean isValid = true;
        if (state.getCurrentPassword().isEmpty()) {
            updateUiState(s -> s.toBuilder().currentPasswordError("Введите текущий пароль").build().toBuilder()); isValid = false;
        }
        if (state.getNewPassword().isEmpty()) {
            updateUiState(s -> s.toBuilder().newPasswordError("Введите новый пароль").build().toBuilder()); isValid = false;
        } else if (state.getNewPassword().length() < 8) {
            updateUiState(s -> s.toBuilder().newPasswordError("Минимум 8 символов").build().toBuilder()); isValid = false;
        }
        if (!state.getNewPassword().equals(state.getConfirmPassword())) {
            updateUiState(s -> s.toBuilder().confirmPasswordError("Пароли не совпадают").build().toBuilder()); isValid = false;
        }
        return isValid;
    }

    private void handleOperationError(String operationName, Throwable e, StringBuilder errorBuilder) {
        String message = operationName + ": " + (e.getMessage() != null ? e.getMessage() : "Ошибка");
        if (errorBuilder.length() > 0) errorBuilder.append("\n");
        errorBuilder.append(message);
    }

    public void onNavigatedBack() {
        updateUiState(s -> s.toBuilder().saveSuccess(false).build().toBuilder());
    }
    public void clearGeneralError() { updateUiState(s -> s.toBuilder().generalError(null).build().toBuilder()); }
    public void clearAvatarError() { updateUiState(s -> s.toBuilder().avatarUpdateError(null).build().toBuilder()); }
    public void clearPasswordError() { // Сбрасывает общую ошибку обновления пароля
        updateUiState(s -> s.toBuilder().passwordUpdateError(null).build().toBuilder());
    }

    // Вспомогательный метод для обновления LiveData
    private void updateUiState(Function<ProfileEditUiState, ProfileEditUiState.ProfileEditUiStateBuilder> updater) {
        ProfileEditUiState current = _uiState.getValue();
        if (current == null) current = ProfileEditUiState.createDefault();
        _uiState.postValue(updater.apply(current).build());
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        logger.debug(TAG, "ProfileEditViewModel cleared.");
    }
}