package com.example.projectquestonjava.feature.profile.presentation.viewmodels;

import android.os.Build;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Function;
import javax.inject.Inject;

@HiltViewModel
public class MainSettingsViewModel extends ViewModel {

    private static final String TAG = "MainSettingsViewModel";
    // Ключи DataStore (могут быть общими, если используются и в других местах)
    public static final Preferences.Key<Integer> THEME_PREF_KEY = PreferencesKeys.intKey("app_theme_global");
    public static final Preferences.Key<Boolean> DYNAMIC_COLOR_PREF_KEY = PreferencesKeys.booleanKey("dynamic_color_enabled_global");
    public static final Preferences.Key<Boolean> NOTIFICATIONS_PREF_KEY = PreferencesKeys.booleanKey("notifications_enabled_global");

    private final UserSessionManager userSessionManager;
    private final WorkspaceSessionManager workspaceSessionManager;
    private final GamificationDataStoreManager gamificationDataStoreManager;
    private final DataStoreManager dataStoreManager;
    private final SnackbarManager snackbarManager;
    private final Executor ioExecutor;
    private final Logger logger;

    private final MutableLiveData<MainSettingsScreenUiState> _uiState = new MutableLiveData<>(MainSettingsScreenUiState.createDefault());
    public LiveData<MainSettingsScreenUiState> uiStateLiveData = _uiState;

    private final Observer<Integer> themeObserver;
    private final Observer<Boolean> dynamicColorObserver;
    private final Observer<Boolean> notificationsObserver;

    @Inject
    public MainSettingsViewModel(
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

        themeObserver = ordinal -> updateUiState(builder -> builder.selectedTheme(
                (ordinal != null && ordinal >= 0 && ordinal < AppTheme.values().length) ? AppTheme.values()[ordinal] : AppTheme.SYSTEM
        ));
        dynamicColorObserver = enabled -> updateUiState(builder -> builder.useDynamicColor(
                enabled != null ? enabled : (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        ));
        notificationsObserver = enabled -> updateUiState(builder -> builder.notificationsEnabled(
                enabled != null ? enabled : true
        ));

        loadSettings();
        logger.debug(TAG, "ViewModel initialized.");
    }

    private void loadSettings() {
        updateUiState(builder -> builder.isLoading(true));

        dataStoreManager.getPreferenceLiveData(THEME_PREF_KEY, AppTheme.SYSTEM.ordinal())
                .observeForever(themeObserver);
        dataStoreManager.getPreferenceLiveData(DYNAMIC_COLOR_PREF_KEY, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                .observeForever(dynamicColorObserver);
        dataStoreManager.getPreferenceLiveData(NOTIFICATIONS_PREF_KEY, true)
                .observeForever(notificationsObserver);
        // Считаем загрузку завершенной после инициализации наблюдателей
        // (они сразу должны проэмитить начальные значения)
        updateUiState(builder -> builder.isLoading(false));
    }

    private void updateUiState(Function<MainSettingsScreenUiState.MainSettingsScreenUiStateBuilder, MainSettingsScreenUiState.MainSettingsScreenUiStateBuilder> updater) {
        MainSettingsScreenUiState current = _uiState.getValue();
        MainSettingsScreenUiState.MainSettingsScreenUiStateBuilder builder = (current != null ? current.toBuilder() : MainSettingsScreenUiState.builder());
        _uiState.postValue(updater.apply(builder).build());
    }

    public void updateTheme(AppTheme theme) {
        logger.debug(TAG, "Updating theme to: " + theme);
        updateUiState(builder -> builder.selectedTheme(theme));
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
        logger.debug(TAG, "Updating dynamic color to: " + enabled);
        updateUiState(builder -> builder.useDynamicColor(enabled));
        Futures.addCallback(dataStoreManager.saveValueFuture(DYNAMIC_COLOR_PREF_KEY, enabled),
                new FutureCallback<Void>() {
                    @Override public void onSuccess(Void result) { snackbarManager.showMessage("Динамические цвета " + (enabled ? "включены" : "выключены")); }
                    @Override public void onFailure(@NonNull Throwable t) { handleSaveError("динамических цветов", t); }
                }, ioExecutor);
    }

    public void updateNotifications(boolean enabled) {
        logger.debug(TAG, "Updating notifications to: " + enabled);
        updateUiState(builder -> builder.notificationsEnabled(enabled));
        Futures.addCallback(dataStoreManager.saveValueFuture(NOTIFICATIONS_PREF_KEY, enabled),
                new FutureCallback<Void>() {
                    @Override public void onSuccess(Void result) { snackbarManager.showMessage("Уведомления " + (enabled ? "включены" : "выключены"));}
                    @Override public void onFailure(@NonNull Throwable t) { handleSaveError("настроек уведомлений", t); }
                }, ioExecutor);
    }

    public void showThemeDialog() { updateUiState(builder -> builder.showThemeDialog(true)); }
    public void dismissThemeDialog() { updateUiState(builder -> builder.showThemeDialog(false)); }
    public void showLogoutDialog() { updateUiState(builder -> builder.showLogoutDialog(true)); }
    public void dismissLogoutDialog() { updateUiState(builder -> builder.showLogoutDialog(false)); }
    public void showDeleteAccountDialog() { updateUiState(builder -> builder.showDeleteAccountDialog(true)); }
    public void dismissDeleteAccountDialog() { updateUiState(builder -> builder.showDeleteAccountDialog(false)); }

    public void logout() {
        logger.info(TAG, "Logout initiated.");
        dismissLogoutDialog();
        updateUiState(builder -> builder.isLoading(true));
        ioExecutor.execute(() -> {
            try {
                Futures.getDone(userSessionManager.clearUserIdAsync());
                Futures.getDone(workspaceSessionManager.clearWorkspaceIdAsync());
                Futures.getDone(gamificationDataStoreManager.clearAllGamificationData());
                logger.info(TAG, "User session data cleared successfully.");
                snackbarManager.showMessage("Вы успешно вышли из аккаунта.");
                // Навигация на экран входа должна произойти в UI (например, MainActivity наблюдает за userIdFlow)
            } catch (Exception e) {
                logger.error(TAG, "Error during logout", e);
                handleOperationError("выхода из аккаунта", e);
            } finally {
                updateUiState(builder -> builder.isLoading(false));
            }
        });
    }

    public void clearCache() {
        updateUiState(builder -> builder.isLoading(true));
        ioExecutor.execute(() -> {
            try {
                Thread.sleep(1000); // Имитация
                snackbarManager.showMessage("Кэш успешно очищен (имитация).");
            } catch (Exception e) {
                handleOperationError("очистки кэша", e);
            } finally {
                updateUiState(builder -> builder.isLoading(false));
            }
        });
    }

    public void exportData() {
        updateUiState(builder -> builder.isLoading(true));
        ioExecutor.execute(() -> {
            try {
                Thread.sleep(1500); // Имитация
                snackbarManager.showMessage("Экспорт данных запущен (имитация).");
            } catch (Exception e) {
                handleOperationError("экспорта данных", e);
            } finally {
                updateUiState(builder -> builder.isLoading(false));
            }
        });
    }
    public void importData() {
        updateUiState(builder -> builder.isLoading(true));
        ioExecutor.execute(() -> {
            try {
                Thread.sleep(1500); // Имитация
                snackbarManager.showMessage("Импорт данных запущен (имитация).");
            } catch (Exception e) {
                handleOperationError("импорта данных", e);
            } finally {
                updateUiState(builder -> builder.isLoading(false));
            }
        });
    }

    public void deleteAccount() {
        logger.warn(TAG, "Delete account initiated.");
        dismissDeleteAccountDialog();
        updateUiState(builder -> builder.isLoading(true));
        ioExecutor.execute(() -> {
            try {
                // TODO: Реальная логика удаления аккаунта из БД и всех связанных данных
                Thread.sleep(2000); // Имитация
                Futures.getDone(userSessionManager.clearUserIdAsync());
                Futures.getDone(workspaceSessionManager.clearWorkspaceIdAsync());
                Futures.getDone(gamificationDataStoreManager.clearAllGamificationData());
                // MainActivity.deleteAllDatabases(applicationContext); // Если это статический метод
                logger.info(TAG, "Account deleted successfully (simulation).");
                snackbarManager.showMessage("Аккаунт успешно удален.");
            } catch (Exception e) {
                logger.error(TAG, "Failed to delete account", e);
                handleOperationError("удаления аккаунта", e);
            } finally {
                updateUiState(builder -> builder.isLoading(false));
            }
        });
    }

    private void handleSaveError(String settingName, Throwable t) {
        logger.error(TAG, "Failed to save " + settingName, t);
        updateUiState(builder -> builder);
    }
    private void handleOperationError(String operationName, Throwable t) {
        updateUiState(builder -> builder);
    }

    public void clearError() { updateUiState(builder -> builder); }
    public void clearSuccessMessage() { updateUiState(builder -> builder); }

    @Override
    protected void onCleared() {
        super.onCleared();
        dataStoreManager.getPreferenceLiveData(THEME_PREF_KEY, AppTheme.SYSTEM.ordinal()).removeObserver(themeObserver);
        dataStoreManager.getPreferenceLiveData(DYNAMIC_COLOR_PREF_KEY, true).removeObserver(dynamicColorObserver);
        dataStoreManager.getPreferenceLiveData(NOTIFICATIONS_PREF_KEY, true).removeObserver(notificationsObserver);
        logger.debug(TAG, "ViewModel cleared.");
    }
}