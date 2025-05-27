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
import com.example.projectquestonjava.core.managers.SnackbarManager;
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

// ProfileUiState data class (как был определен ранее)

@HiltViewModel
public class ProfileViewModel extends ViewModel {

    private static final String TAG = "ProfileViewModel";
    private static final int RECENT_BADGES_LIMIT = 5;
    private static final int RECENT_HISTORY_LIMIT = 5;

    private final UserAuthRepository userAuthRepository;
    private final GamificationRepository gamificationRepository;
    private final BadgeRepository badgeRepository;
    private final GamificationHistoryRepository gamificationHistoryRepository;
    private final UserSessionManager userSessionManager;
    private final WorkspaceSessionManager workspaceSessionManager;
    private final GamificationDataStoreManager gamificationDataStoreManager;
    private final Executor ioExecutor;
    private final Logger logger;
    private final SnackbarManager snackbarManager;


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
            @IODispatcher Executor ioExecutor,
            Logger logger,
            SnackbarManager snackbarManager) {
        this.userAuthRepository = userAuthRepository;
        this.gamificationRepository = gamificationRepository;
        this.badgeRepository = badgeRepository;
        this.gamificationHistoryRepository = gamificationHistoryRepository;
        this.userSessionManager = userSessionManager;
        this.workspaceSessionManager = workspaceSessionManager;
        this.gamificationDataStoreManager = gamificationDataStoreManager;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
        this.snackbarManager = snackbarManager;

        logger.debug(TAG, "Initializing ProfileViewModel.");

        userLiveData = Transformations.switchMap(userSessionManager.getUserIdLiveData(), userId -> {
            if (userId == null || userId == UserSessionManager.NO_USER_ID) return new MutableLiveData<>(null);
            MutableLiveData<UserAuth> liveData = new MutableLiveData<>();
            Futures.addCallback(userAuthRepository.getUserById(userId), new FutureCallback<UserAuth>() {
                @Override public void onSuccess(UserAuth user) { liveData.postValue(user); }
                @Override public void onFailure(@NonNull Throwable t) {
                    logger.error(TAG, "Error loading user data", t);
                    liveData.postValue(null);
                    updateUiStateError("Ошибка загрузки пользователя");
                }
            }, ioExecutor);
            return liveData;
        });

        gamificationLiveData = gamificationRepository.getCurrentUserGamificationFlow();
        earnedBadgesRefsLiveData = badgeRepository.getEarnedBadgesFlow();
        allBadgesLiveData = badgeRepository.getAllBadgesFlow();
        recentHistoryLiveData = Transformations.map(
                gamificationHistoryRepository.getHistoryFlow(),
                historyList -> {
                    if (historyList == null) return Collections.emptyList();
                    return historyList.stream().limit(RECENT_HISTORY_LIMIT).collect(Collectors.toList());
                }
        );

        MediatorLiveData<Object> combinator = new MediatorLiveData<>();
        combinator.addSource(userLiveData, value -> combineAndSetUiState());
        combinator.addSource(gamificationLiveData, value -> combineAndSetUiState());
        combinator.addSource(earnedBadgesRefsLiveData, value -> combineAndSetUiState());
        combinator.addSource(allBadgesLiveData, value -> combineAndSetUiState());
        combinator.addSource(recentHistoryLiveData, value -> combineAndSetUiState());

        // Первоначальная установка isLoading, сбрасывается в combineAndSetUiState
        _uiStateLiveData.setValue(new ProfileUiState(true, false, null, null, 0, Collections.emptyList(), Collections.emptyList(), null));
    }

    private void combineAndSetUiState() {
        UserAuth user = userLiveData.getValue();
        Gamification gamification = gamificationLiveData.getValue();
        List<GamificationBadgeCrossRef> earnedRefs = earnedBadgesRefsLiveData.getValue();
        List<Badge> allBadges = allBadgesLiveData.getValue();
        List<GamificationHistory> history = recentHistoryLiveData.getValue();
        ProfileUiState currentState = _uiStateLiveData.getValue(); // Для сохранения isUpdatingAvatar и error

        // Определяем, завершилась ли загрузка всех основных источников
        boolean stillLoadingSources = (user == null && userLiveData.hasActiveObservers()) ||
                (gamification == null && gamificationLiveData.hasActiveObservers()) ||
                (earnedRefs == null && earnedBadgesRefsLiveData.hasActiveObservers()) ||
                (allBadges == null && allBadgesLiveData.hasActiveObservers()) ||
                (history == null && recentHistoryLiveData.hasActiveObservers());


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

        String currentError = currentState != null ? currentState.getError() : null;
        String errorMsg = (user == null && gamification == null && currentError == null && !stillLoadingSources) ?
                "Не удалось загрузить данные профиля" : currentError;

        _uiStateLiveData.postValue(new ProfileUiState(
                stillLoadingSources,
                currentState != null ? currentState.isUpdatingAvatar() : false,
                user,
                gamification,
                earnedCount,
                recentEarnedBadges,
                history != null ? history : Collections.emptyList(),
                errorMsg
        ));
        if (!stillLoadingSources) {
            logger.debug(TAG, "ProfileUiState combined and posted.");
        }
    }

    public void logout() {
        logger.info(TAG, "Performing logout...");
        updateUiStateLoading(true);
        ioExecutor.execute(() -> {
            try {
                Futures.getDone(userSessionManager.clearUserIdAsync());
                Futures.getDone(workspaceSessionManager.clearWorkspaceIdAsync());
                Futures.getDone(gamificationDataStoreManager.clearGamificationId());
                Futures.getDone(gamificationDataStoreManager.clearSelectedPlantId());
                Futures.getDone(gamificationDataStoreManager.clearHiddenExpiredTaskIds());
                logger.info(TAG, "User session data cleared successfully.");
                // Навигация на экран входа должна произойти в UI (например, через Activity.finish() или NavController)
                // Здесь мы просто сигнализируем об успехе, если нужно
                // _uiStateLiveData.postValue(_uiStateLiveData.getValue().copy(false, ..., "Выход выполнен", false));
            } catch (Exception e) {
                logger.error(TAG, "Error during logout data clearing", e);
                updateUiStateError("Ошибка выхода из аккаунта.");
            } finally {
                updateUiStateLoading(false);
            }
        });
    }

    public void clearError() {
        ProfileUiState current = _uiStateLiveData.getValue();
        if (current != null && current.getError() != null) {
            _uiStateLiveData.postValue(new ProfileUiState(
                    current.isLoading(), current.isUpdatingAvatar(), current.getUser(), current.getGamification(),
                    current.getEarnedBadgesCount(), current.getRecentBadges(), current.getRecentHistory(),
                    null // Очищаем ошибку
            ));
        }
    }

    private void updateUiStateLoading(boolean isLoading) {
        ProfileUiState current = _uiStateLiveData.getValue();
        if (current != null) {
            _uiStateLiveData.postValue(new ProfileUiState(
                    isLoading, current.isUpdatingAvatar(), current.getUser(), current.getGamification(),
                    current.getEarnedBadgesCount(), current.getRecentBadges(), current.getRecentHistory(),
                    isLoading ? null : current.getError() // Сбрасываем ошибку при начале загрузки
            ));
        }
    }
    private void updateUiStateError(String errorMsg) {
        ProfileUiState current = _uiStateLiveData.getValue();
        if (current != null) {
            _uiStateLiveData.postValue(new ProfileUiState(
                    false, current.isUpdatingAvatar(), current.getUser(), current.getGamification(),
                    current.getEarnedBadgesCount(), current.getRecentBadges(), current.getRecentHistory(),
                    errorMsg
            ));
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Отписываемся от LiveData, если использовали observeForever
        // В данном случае, LiveData из репозиториев скорее всего привязаны к Lifecycle фрагмента
        // и не требуют явной отписки здесь. Но если бы были прямые подписки observeForever, их нужно отменить.
        logger.debug(TAG, "ProfileViewModel cleared.");
    }
}