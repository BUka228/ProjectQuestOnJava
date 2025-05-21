package com.example.projectquestonjava.feature.profile.presentation.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.projectquestonjava.core.data.model.core.UserAuth;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.domain.repository.UserAuthRepository;
import com.example.projectquestonjava.core.managers.UserSessionManager;
import com.example.projectquestonjava.core.managers.WorkspaceSessionManager;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.data.managers.GamificationDataStoreManager;
import com.example.projectquestonjava.feature.gamification.data.model.Badge;
import com.example.projectquestonjava.feature.gamification.data.model.Gamification;
import com.example.projectquestonjava.feature.gamification.data.model.GamificationBadgeCrossRef;
import com.example.projectquestonjava.feature.gamification.domain.repository.BadgeRepository;
import com.example.projectquestonjava.feature.gamification.domain.repository.GamificationRepository;
import com.example.projectquestonjava.feature.statistics.data.model.GamificationHistory;
import com.example.projectquestonjava.feature.statistics.domain.repository.GamificationHistoryRepository;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import dagger.hilt.android.lifecycle.HiltViewModel;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.inject.Inject;

// Data class ProfileUiState (если еще не создан глобально)
class ProfileUiState { // Убираем public, если используется только здесь
    public final boolean isLoading;
    public final boolean isUpdatingAvatar; // Оставляем, если обновление аватара происходит здесь
    public final UserAuth user;
    public final Gamification gamification;
    public final int earnedBadgesCount;
    public final List<Badge> recentBadges;
    public final List<GamificationHistory> recentHistory;
    public final String error;

    public ProfileUiState(boolean isLoading, boolean isUpdatingAvatar, UserAuth user, Gamification gamification,
                          int earnedBadgesCount, List<Badge> recentBadges, List<GamificationHistory> recentHistory, String error) {
        this.isLoading = isLoading;
        this.isUpdatingAvatar = isUpdatingAvatar;
        this.user = user;
        this.gamification = gamification;
        this.earnedBadgesCount = earnedBadgesCount;
        this.recentBadges = recentBadges != null ? recentBadges : Collections.emptyList();
        this.recentHistory = recentHistory != null ? recentHistory : Collections.emptyList();
        this.error = error;
    }

    // Конструктор по умолчанию
    public ProfileUiState() {
        this(true, false, null, null, 0, Collections.emptyList(), Collections.emptyList(), null);
    }
    // Метод copy для удобства обновления
    public ProfileUiState copy(Boolean isLoading, Boolean isUpdatingAvatar, UserAuth user, Gamification gamification,
                               Integer earnedBadgesCount, List<Badge> recentBadges, List<GamificationHistory> recentHistory,
                               String error, boolean clearError) { // clearError для явного сброса ошибки
        return new ProfileUiState(
                isLoading != null ? isLoading : this.isLoading,
                isUpdatingAvatar != null ? isUpdatingAvatar : this.isUpdatingAvatar,
                user != null ? user : this.user,
                gamification != null ? gamification : this.gamification,
                earnedBadgesCount != null ? earnedBadgesCount : this.earnedBadgesCount,
                recentBadges != null ? recentBadges : this.recentBadges,
                recentHistory != null ? recentHistory : this.recentHistory,
                clearError ? null : (error != null ? error : this.error)
        );
    }
}


@HiltViewModel
public class ProfileViewModel extends ViewModel {

    private static final String TAG = "ProfileViewModel";
    private static final int RECENT_BADGES_LIMIT = 5;
    private static final int RECENT_HISTORY_LIMIT = 5;

    private final UserSessionManager userSessionManager;
    private final WorkspaceSessionManager workspaceSessionManager; // Для logout
    private final GamificationDataStoreManager gamificationDataStoreManager; // Для logout
    // updateUsernameUseCase был в Kotlin, но здесь не используется напрямую в public API
    private final Executor ioExecutor;
    private final Logger logger;

    private final MutableLiveData<ProfileUiState> _uiStateLiveData = new MutableLiveData<>(new ProfileUiState());
    public LiveData<ProfileUiState> uiStateLiveData = _uiStateLiveData;

    // LiveData для каждого источника данных
    private final LiveData<UserAuth> userLiveData;
    private final LiveData<Gamification> gamificationLiveData;
    private final LiveData<List<GamificationBadgeCrossRef>> earnedBadgesRefsLiveData;
    private final LiveData<List<Badge>> allBadgesLiveData;
    private final LiveData<List<GamificationHistory>> recentHistoryLiveData;


    @Inject
    public ProfileViewModel(
            UserAuthRepository userAuthRepository,
            GamificationRepository gamificationRepository,
            BadgeRepository badgeRepository,
            GamificationHistoryRepository gamificationHistoryRepository,
            UserSessionManager userSessionManager,
            WorkspaceSessionManager workspaceSessionManager,
            GamificationDataStoreManager gamificationDataStoreManager,
            // UpdateUsernameUseCase updateUsernameUseCase, // Не нужен здесь, если нет прямого вызова
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.userSessionManager = userSessionManager;
        this.workspaceSessionManager = workspaceSessionManager;
        this.gamificationDataStoreManager = gamificationDataStoreManager;
        this.ioExecutor = ioExecutor;
        this.logger = logger;

        logger.debug(TAG, "Initializing ProfileViewModel.");

        // Инициализация LiveData источников
        userLiveData = Transformations.switchMap(userSessionManager.getUserIdLiveData(), userId -> {
            if (userId == null || userId == UserSessionManager.NO_USER_ID) {
                return new MutableLiveData<>(null);
            }
            // UserAuthRepository.getUserById возвращает ListenableFuture
            // Преобразуем его в LiveData
            MutableLiveData<UserAuth> userAuthLd = new MutableLiveData<>();
            Futures.addCallback(userAuthRepository.getUserById(userId), new FutureCallback<UserAuth>() {
                @Override public void onSuccess(UserAuth user) { userAuthLd.postValue(user); }
                @Override public void onFailure(@NonNull Throwable t) {
                    logger.error(TAG, "Error loading user data in switchMap", t);
                    userAuthLd.postValue(null);
                    updateUiState(s -> s.copy(null, null, null, null, null, null, null, "Ошибка загрузки пользователя", false));
                }
            }, ioExecutor);
            return userAuthLd;
        });

        gamificationLiveData = gamificationRepository.getCurrentUserGamificationFlow(); // Уже LiveData
        earnedBadgesRefsLiveData = badgeRepository.getEarnedBadgesFlow(); // Уже LiveData
        allBadgesLiveData = badgeRepository.getAllBadgesFlow(); // Уже LiveData

        recentHistoryLiveData = Transformations.map(
                gamificationHistoryRepository.getHistoryFlow(), // Уже LiveData
                historyList -> {
                    if (historyList == null) return Collections.emptyList();
                    return historyList.stream().limit(RECENT_HISTORY_LIMIT).collect(Collectors.toList());
                }
        );

        // Объединяем все источники для формирования _uiStateLiveData
        MediatorLiveData<Object> combinator = new MediatorLiveData<>();
        combinator.addSource(userLiveData, value -> combineAndSetUiState());
        combinator.addSource(gamificationLiveData, value -> combineAndSetUiState());
        combinator.addSource(earnedBadgesRefsLiveData, value -> combineAndSetUiState());
        combinator.addSource(allBadgesLiveData, value -> combineAndSetUiState());
        combinator.addSource(recentHistoryLiveData, value -> combineAndSetUiState());

        // Изначально isLoading = true (установлено в конструкторе ProfileUiState)
        // Сбрасываем isLoading, когда все основные LiveData получили начальные значения
        // Это упрощенный подход; в идеале нужно отслеживать загрузку каждого источника.
        // Для простоты, сбросим isLoading после первой комбинации.
        _uiStateLiveData.addSource(combinator, combinedValue -> {
            // Этот коллбэк вызывается, когда любой из источников изменился.
            // Мы уже обновили uiState в combineAndSetUiState.
            // Здесь можно просто залогировать или ничего не делать, если combineAndSetUiState
            // уже правильно обновляет uiState.
        });
    }

    private void combineAndSetUiState() {
        UserAuth user = userLiveData.getValue();
        Gamification gamification = gamificationLiveData.getValue();
        List<GamificationBadgeCrossRef> earnedRefs = earnedBadgesRefsLiveData.getValue();
        List<Badge> allBadges = allBadgesLiveData.getValue();
        List<GamificationHistory> history = recentHistoryLiveData.getValue();
        ProfileUiState currentState = _uiStateLiveData.getValue(); // Для сохранения isUpdatingAvatar

        // Устанавливаем isLoading в false, только если все источники уже имеют значения
        // (хотя бы начальные, например, null или пустые списки)
        boolean stillLoadingSources = user == null && gamification == null && earnedRefs == null && allBadges == null && history == null;
        // Если какой-то источник еще не проэмитил значение, считаем, что грузимся.
        // Более точная проверка - если все источники были хоть раз вызваны.


        List<Badge> recentEarnedBadges = Collections.emptyList();
        int earnedCount = 0;
        if (earnedRefs != null && allBadges != null) {
            earnedCount = earnedRefs.size();
            Map<Long, LocalDateTime> earnedBadgesMap = earnedRefs.stream()
                    .collect(Collectors.toMap(GamificationBadgeCrossRef::getBadgeId, GamificationBadgeCrossRef::getEarnedAt));
            recentEarnedBadges = allBadges.stream()
                    .filter(badge -> earnedBadgesMap.containsKey(badge.getId()))
                    .sorted(Comparator.comparing((Badge badge) -> earnedBadgesMap.get(badge.getId())).reversed())
                    .limit(RECENT_BADGES_LIMIT)
                    .collect(Collectors.toList());
        }

        String errorMsg = (user == null && gamification == null && (currentState == null || currentState.error == null) && !stillLoadingSources) ?
                "Не удалось загрузить данные профиля" :
                (currentState != null ? currentState.error : null);


        _uiStateLiveData.postValue(new ProfileUiState(
                stillLoadingSources, // isLoading
                currentState != null ? currentState.isUpdatingAvatar : false,
                user,
                gamification,
                earnedCount,
                recentEarnedBadges,
                history != null ? history : Collections.emptyList(),
                errorMsg
        ));
        logger.debug(TAG, "ProfileUiState combined and posted.");
    }

    // avatarSelected не используется в этой ViewModel, он в ProfileEditViewModel
    // public void onAvatarSelected(Uri uri) { ... }

    public void logout() {
        logger.info(TAG, "Performing logout...");
        updateUiState(s -> s.copy(true, null, null, null, null, null, null, null, false));
        ioExecutor.execute(() -> {
            try {
                // Используем ListenableFuture и get() на фоновом потоке
                Futures.getDone(userSessionManager.clearUserIdAsync());
                Futures.getDone(workspaceSessionManager.clearWorkspaceIdAsync());
                Futures.getDone(gamificationDataStoreManager.clearGamificationId());
                Futures.getDone(gamificationDataStoreManager.clearSelectedPlantId());
                Futures.getDone(gamificationDataStoreManager.clearHiddenExpiredTaskIds());
                logger.info(TAG, "User session cleared.");
                // Навигация на экран входа должна произойти в UI на основе изменения состояния (например, userIdFlow)
            } catch (Exception e) {
                logger.error(TAG, "Error during logout data clearing", e);
                updateUiState(s -> s.copy(null, null, null, null, null, null, null, "Ошибка выхода.", false));
            } finally {
                // Сбрасываем isLoading уже после того, как UI среагирует на очистку сессии (например, через userIdFlow)
                // Здесь можно не сбрасывать, так как после logout обычно происходит навигация.
                updateUiState(s -> s.copy(false, null, null, null, null, null, null, null, false));
            }
        });
    }

    public void clearError() {
        updateUiState(s -> s.copy(null, null, null, null, null, null, null, null, true));
    }

    // Вспомогательный интерфейс для обновления UI State
    @FunctionalInterface
    private interface UiStateUpdaterProfile {
        ProfileUiState update(ProfileUiState currentState);
    }

    private void updateUiState(UiStateUpdaterProfile updater) {
        ProfileUiState current = _uiStateLiveData.getValue();
        _uiStateLiveData.postValue(updater.update(current != null ? current : new ProfileUiState()));
    }
}