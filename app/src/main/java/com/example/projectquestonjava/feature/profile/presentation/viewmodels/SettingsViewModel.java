package com.example.projectquestonjava.feature.profile.presentation.viewmodels; // Или общий пакет viewmodels

import android.os.Build; // Для проверки версии SDK
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.datastore.preferences.core.PreferencesKeys; // Для ключей DataStore
import androidx.datastore.preferences.core.Preferences;    // Для ключей DataStore

import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.managers.DataStoreManager;
import com.example.projectquestonjava.core.managers.SnackbarManager;
import com.example.projectquestonjava.core.managers.UserSessionManager;
import com.example.projectquestonjava.core.managers.WorkspaceSessionManager;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.data.managers.GamificationDataStoreManager;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.Objects;
import java.util.concurrent.Executor;
import javax.inject.Inject;

// AppTheme enum (как был определен ранее)
// MainSettingsScreenUiState data class (как был определен ранее)

@HiltViewModel
public class SettingsViewModel extends ViewModel {

    private static final String TAG = "SettingsViewModel";
    // Ключи DataStore
    private static final Preferences.Key<Integer> THEME_PREF_KEY = PreferencesKeys.intKey("app_theme_v2"); // Изменил ключ, чтобы не конфликтовал с PomodoroSettings
    private static final Preferences.Key<Boolean> DYNAMIC_COLOR_PREF_KEY = PreferencesKeys.booleanKey("dynamic_color_enabled_v2");
    private static final Preferences.Key<Boolean> NOTIFICATIONS_PREF_KEY = PreferencesKeys.booleanKey("notifications_enabled_v2");


    private final UserSessionManager userSessionManager;
    private final WorkspaceSessionManager workspaceSessionManager;
    private final GamificationDataStoreManager gamificationDataStoreManager;
    private final DataStoreManager dataStoreManager;
    private final SnackbarManager snackbarManager;
    private final Executor ioExecutor;
    private final Logger logger;

    private final MutableLiveData<MainSettingsScreenUiState> _uiState = new MutableLiveData<>(new MainSettingsScreenUiState());
    public LiveData<MainSettingsScreenUiState> uiStateLiveData = _uiState;

    // Обозреватели для LiveData из DataStoreManager
    private final Observer<Integer> themeObserver;
    private final Observer<Boolean> dynamicColorObserver;
    private final Observer<Boolean> notificationsObserver;

    @Inject
    public SettingsViewModel(
            UserSessionManager userSessionManager,
            WorkspaceSessionManager workspaceSessionManager,
            GamificationDataStoreManager gamificationDataStoreManager,
            DataStoreManager dataStoreManager,
            SnackbarManager snackbarManager,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.userSessionManager = userSessionManager;
        this.workspaceSessionManager = workspaceSessionManager;
        this.gamificationDataStoreManager = gamificationDataStoreManager;
        this.dataStoreManager = dataStoreManager;
        this.snackbarManager = snackbarManager;
        this.ioExecutor = ioExecutor;
        this.logger = logger;

        themeObserver = ordinal -> updateUiState(s -> s.copy(null,null,null,
                AppTheme.values()[ordinal != null ? ordinal : AppTheme.SYSTEM.ordinal()],
                null,null,null,null,null,false,false));

        dynamicColorObserver = enabled -> updateUiState(s -> s.copy(null,null,null,
                null, enabled, null, null, null, null, false, false));

        notificationsObserver = enabled -> updateUiState(s -> s.copy(null,null,null,
                null, null, enabled, null, null, null, false, false));

        loadSettings();
        logger.debug(TAG, "SettingsViewModel initialized.");
    }

    private void loadSettings() {
        updateUiState(s -> s.copy(true, null, null, null, null, null, null, null, null, false, true)); // isLoading = true, clearSuccess

        // Подписываемся на LiveData из DataStoreManager
        dataStoreManager.getPreferenceLiveData(THEME_PREF_KEY, AppTheme.SYSTEM.ordinal())
                .observeForever(themeObserver);
        dataStoreManager.getPreferenceLiveData(DYNAMIC_COLOR_PREF_KEY, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) // Динамический цвет по умолчанию включен на S+
                .observeForever(dynamicColorObserver);
        dataStoreManager.getPreferenceLiveData(NOTIFICATIONS_PREF_KEY, true)
                .observeForever(notificationsObserver);
        // isLoading будет сброшен, когда все три LiveData проэмитят свои первые значения
        // Это можно сделать через MediatorLiveData или просто после первой установки всех трех полей в uiState
        // Для простоты, предполагаем, что они быстро эмиттят, и isLoading сбросится в updateUiState.
        // Более надежно - отслеживать загрузку каждого.
        // После первой полной комбинации в _uiState, isLoading можно сбросить.
        // Небольшая задержка, чтобы LiveData успели проэмитить, прежде чем убирать isLoading
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            updateUiState(s -> s.copy(false, null, null, null, null, null, null, null, null, false, true));
        }, 200); // 200ms должно быть достаточно
    }

    private void updateUiState(SettingsUiUpdater updater) {
        MainSettingsScreenUiState current = _uiState.getValue();
        _uiState.postValue(updater.update(current != null ? current : new MainSettingsScreenUiState()));
    }
    @FunctionalInterface
    interface SettingsUiUpdater { MainSettingsScreenUiState update(MainSettingsScreenUiState currentState); }


    public void updateTheme(AppTheme theme) {
        updateUiState(s -> s.copy(null,null,null, theme, null,null,null,null,null, false,false));
        Futures.addCallback(dataStoreManager.saveValueFuture(THEME_PREF_KEY, theme.ordinal()),
                new FutureCallback<Void>() {
                    @Override public void onSuccess(Void result) { snackbarManager.showMessage("Тема '" + getThemeName(theme) + "' применена"); }
                    @Override public void onFailure(@NonNull Throwable t) { handleSaveError("темы", t); }
                }, ioExecutor);
    }
    private String getThemeName(AppTheme theme) {
        return switch (theme) {
            case SYSTEM -> "Системная";
            case LIGHT -> "Светлая";
            case DARK -> "Темная";
        };
    }


    public void updateDynamicColor(boolean enabled) {
        updateUiState(s -> s.copy(null,null,null, null, enabled,null,null,null,null, false,false));
        Futures.addCallback(dataStoreManager.saveValueFuture(DYNAMIC_COLOR_PREF_KEY, enabled),
                new FutureCallback<Void>() {
                    @Override public void onSuccess(Void result) {} // Обычно не нужен Snackbar для этого
                    @Override public void onFailure(@NonNull Throwable t) { handleSaveError("динамических цветов", t); }
                }, ioExecutor);
    }

    public void updateNotifications(boolean enabled) {
        updateUiState(s -> s.copy(null,null,null, null, null,enabled,null,null,null, false,false));
        Futures.addCallback(dataStoreManager.saveValueFuture(NOTIFICATIONS_PREF_KEY, enabled),
                new FutureCallback<Void>() {
                    @Override public void onSuccess(Void result) {}
                    @Override public void onFailure(@NonNull Throwable t) { handleSaveError("настроек уведомлений", t); }
                }, ioExecutor);
    }

    public void showThemeDialog() { updateUiState(s -> s.copy(null,null,null,null,null,null,true,null,null, false,false)); }
    public void dismissThemeDialog() { updateUiState(s -> s.copy(null,null,null,null,null,null,false,null,null, false,false)); }
    public void showLogoutDialog() { updateUiState(s -> s.copy(null,null,null,null,null,null,null,true,null, false,false)); }
    public void dismissLogoutDialog() { updateUiState(s -> s.copy(null,null,null,null,null,null,null,false,null, false,false)); }
    public void showDeleteAccountDialog() { updateUiState(s -> s.copy(null,null,null,null,null,null,null,null,true, false,false)); }
    public void dismissDeleteAccountDialog() { updateUiState(s -> s.copy(null,null,null,null,null,null,null,null,false, false,false)); }


    public void logout() {
        logger.info(TAG, "Logout initiated.");
        dismissLogoutDialog();
        updateUiState(s -> s.copy(true, null,null,null,null,null,null,null,null, false,true));
        ioExecutor.execute(() -> {
            try {
                Futures.getDone(userSessionManager.clearUserIdAsync());
                Futures.getDone(workspaceSessionManager.clearWorkspaceIdAsync());
                Futures.getDone(gamificationDataStoreManager.clearGamificationId());
                Futures.getDone(gamificationDataStoreManager.clearSelectedPlantId());
                Futures.getDone(gamificationDataStoreManager.clearHiddenExpiredTaskIds());
                // TODO: Очистка других DataStore ключей, если есть
                logger.info(TAG, "User session data cleared successfully.");
                snackbarManager.showMessage("Вы успешно вышли из аккаунта.");
                // Навигация на экран входа должна произойти в UI (например, MainActivity наблюдает за userIdFlow)
            } catch (Exception e) {
                logger.error(TAG, "Error during logout", e);
                handleOperationError("выхода из аккаунта", e);
            } finally {
                updateUiState(s -> s.copy(false, null,null,null,null,null,null,null,null, false,true));
            }
        });
    }

    public void clearCache() {
        updateUiState(s -> s.copy(true,null,null,null,null,null,null,null,null, false,true));
        ioExecutor.execute(() -> {
            try {
                // TODO: Реальная логика очистки кэша (файлы, Glide/Coil cache, etc.)
                Thread.sleep(1000); // Имитация
                snackbarManager.showMessage("Кэш успешно очищен (имитация).");
            } catch (Exception e) {
                handleOperationError("очистки кэша", e);
            } finally {
                updateUiState(s -> s.copy(false,null,null,null,null,null,null,null,null, false,true));
            }
        });
    }

    public void exportData() {
        updateUiState(s -> s.copy(true,null,null,null,null,null,null,null,null, false,true));
        ioExecutor.execute(() -> {
            try {
                // TODO: Реальная логика экспорта данных
                Thread.sleep(1500);
                snackbarManager.showMessage("Экспорт данных запущен (имитация).");
            } catch (Exception e) {
                handleOperationError("экспорта данных", e);
            } finally {
                updateUiState(s -> s.copy(false,null,null,null,null,null,null,null,null, false,true));
            }
        });
    }
    public void importData() {
        updateUiState(s -> s.copy(true,null,null,null,null,null,null,null,null, false,true));
        ioExecutor.execute(() -> {
            try {
                // TODO: Реальная логика импорта данных
                Thread.sleep(1500);
                snackbarManager.showMessage("Импорт данных запущен (имитация).");
            } catch (Exception e) {
                handleOperationError("импорта данных", e);
            } finally {
                updateUiState(s -> s.copy(false,null,null,null,null,null,null,null,null, false,true));
            }
        });
    }

    public void deleteAccount() {
        logger.warn(TAG, "Delete account initiated.");
        dismissDeleteAccountDialog();
        updateUiState(s -> s.copy(true,null,null,null,null,null,null,null,null, false,true));
        ioExecutor.execute(() -> {
            try {
                // !!! ВАЖНО: РЕАЛЬНАЯ ЛОГИКА УДАЛЕНИЯ АККАУНТА !!!
                // Это должно включать:
                // 1. Удаление данных пользователя из всех таблиц БД.
                //    (UserAuth, Workspace, Task, Gamification, Statistics, History, etc.)
                //    Лучше всего иметь UseCase для этого, который работает в транзакции.
                // 2. Очистку всех ключей DataStore, связанных с пользователем.
                // 3. Возможно, удаление файлов пользователя (аватары, экспорт).
                // 4. Выход из аккаунта (logout).
                Thread.sleep(2000); // Имитация
                // Futures.getDone(deleteAccountUseCase.execute()); // Пример
                Futures.getDone(userSessionManager.clearUserIdAsync()); // Как минимум, выход
                Futures.getDone(workspaceSessionManager.clearWorkspaceIdAsync());
                Futures.getDone(gamificationDataStoreManager.clearAllGamificationData()); // Нужен такой метод

                logger.info(TAG, "Account deleted successfully (simulation).");
                snackbarManager.showMessage("Аккаунт успешно удален.");
                // Навигация на экран регистрации/входа
            } catch (Exception e) {
                logger.error(TAG, "Failed to delete account", e);
                handleOperationError("удаления аккаунта", e);
            } finally {
                updateUiState(s -> s.copy(false,null,null,null,null,null,null,null,null, false,true));
            }
        });
    }

    private void handleSaveError(String settingName, Throwable t) {
        logger.error(TAG, "Failed to save " + settingName, t);
        updateUiState(s -> s.copy(null,null,null, AppTheme.valueOf("Ошибка сохранения " + settingName), null,null,null,null,null, true, false));
    }
    private void handleOperationError(String operationName, Throwable t) {
        updateUiState(s -> s.copy(null,null,null, AppTheme.valueOf("Ошибка " + operationName + ": " + t.getMessage()), null,null,null,null,null, true, false));
    }

    public void clearError() { updateUiState(s -> s.copy(null,null,null,null,null,null,null,null,null, true,false)); }
    public void clearSuccessMessage() { updateUiState(s -> s.copy(null,null,null,null,null,null,null,null,null, false,true)); }

    @Override
    protected void onCleared() {
        super.onCleared();

        dataStoreManager.getPreferenceLiveData(DYNAMIC_COLOR_PREF_KEY, true).removeObserver(dynamicColorObserver);
        dataStoreManager.getPreferenceLiveData(NOTIFICATIONS_PREF_KEY, true).removeObserver(notificationsObserver);
        logger.debug(TAG, "SettingsViewModel cleared.");
    }
}