package com.example.projectquestonjava.feature.profile.presentation.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer; // Добавлен импорт для Observer
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

    private final MutableLiveData<ProfileUiState> _uiStateLiveData;
    public LiveData<ProfileUiState> uiStateLiveData;

    private final LiveData<UserAuth> userLiveData;
    private final LiveData<Gamification> gamificationLiveData;
    private final LiveData<List<GamificationBadgeCrossRef>> earnedBadgesRefsLiveData;
    private final LiveData<List<Badge>> allBadgesLiveData;
    private final LiveData<List<GamificationHistory>> recentHistoryLiveData;

    // Объявляем Observer для userIdSource здесь, чтобы можно было его удалить в onCleared
    private final Observer<Integer> userIdSourceObserver;


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

        _uiStateLiveData = new MutableLiveData<>(new ProfileUiState(true, false, null, null, 0, Collections.emptyList(), Collections.emptyList(), null));
        uiStateLiveData = _uiStateLiveData;
        logger.debug(TAG, "Initializing ProfileViewModel. Initial isLoading: " + Objects.requireNonNull(_uiStateLiveData.getValue()).isLoading());

        LiveData<Integer> userIdSource = userSessionManager.getUserIdLiveData();

        // Инициализируем userIdSourceObserver
        userIdSourceObserver = userIdVal -> {
            logger.debug(TAG, "UserSessionManager.getUserIdLiveData() emitted: " + userIdVal + ". HasActiveObs: " + userIdSource.hasActiveObservers());
            // Этот observer больше не нужен после первого срабатывания, если мы не ожидаем повторных изменений userId в сессии этого ViewModel
            // Но если userId может меняться и мы должны на это реагировать, то оставляем.
            // Для ProfileViewModel, обычно, userId не меняется в течение его жизни.
            // Если бы это был Application-scoped ViewModel, то да.
        };
        // Подписываемся временно (или постоянно, если нужно реагировать на смену пользователя)
        userIdSource.observeForever(userIdSourceObserver);


        userLiveData = Transformations.switchMap(userIdSource, userId -> {
            logger.info(TAG, "userLiveData switchMap TRIGGERED. userId: " + userId);
            if (userId == null || userId == UserSessionManager.NO_USER_ID) {
                logger.warn(TAG, "userLiveData: No user ID or invalid ID, returning MutableLiveData with null user.");
                MutableLiveData<UserAuth> emptyUserLiveData = new MutableLiveData<>();
                emptyUserLiveData.setValue(null); // Явно устанавливаем null, чтобы триггернуть combinator
                return emptyUserLiveData;
            }
            MutableLiveData<UserAuth> liveData = new MutableLiveData<>();
            Futures.addCallback(userAuthRepository.getUserById(userId), new FutureCallback<UserAuth>() {
                @Override public void onSuccess(UserAuth user) {
                    logger.debug(TAG, "userLiveData: User loaded for userId " + userId + ": " + (user != null ? user.getUsername() : "null"));
                    liveData.postValue(user);
                }
                @Override public void onFailure(@NonNull Throwable t) {
                    logger.error(TAG, "userLiveData: Error loading user data for userId " + userId, t);
                    liveData.postValue(null);
                    updateUiStateError("Ошибка загрузки данных пользователя.");
                }
            }, ioExecutor);
            return liveData;
        });
        logger.debug(TAG, "userLiveData initialized. HasActiveObservers (at init): " + userLiveData.hasActiveObservers());


        gamificationLiveData = Transformations.distinctUntilChanged(
                Transformations.switchMap(userIdSource, userId -> {
                    if (userId == null || userId == UserSessionManager.NO_USER_ID) {
                        return new MutableLiveData<>(null);
                    }
                    return gamificationRepository.getCurrentUserGamificationFlow(); // Этот Flow уже должен зависеть от userId внутри репозитория
                })
        );
        logger.debug(TAG, "gamificationLiveData initialized. HasActiveObservers (at init): " + gamificationLiveData.hasActiveObservers());

        earnedBadgesRefsLiveData = Transformations.distinctUntilChanged(
                Transformations.switchMap(userIdSource, userId -> { // Зависимость от userId для сброса, если пользователь меняется
                    if (userId == null || userId == UserSessionManager.NO_USER_ID) {
                        return new MutableLiveData<>(Collections.emptyList());
                    }
                    return badgeRepository.getEarnedBadgesFlow();
                })
        );
        logger.debug(TAG, "earnedBadgesRefsLiveData initialized. HasActiveObservers (at init): " + earnedBadgesRefsLiveData.hasActiveObservers());

        allBadgesLiveData = Transformations.distinctUntilChanged(badgeRepository.getAllBadgesFlow());
        logger.debug(TAG, "allBadgesLiveData initialized. HasActiveObservers (at init): " + allBadgesLiveData.hasActiveObservers());

        recentHistoryLiveData = Transformations.distinctUntilChanged(
                Transformations.switchMap(userIdSource, userId -> { // Зависимость от userId
                    if (userId == null || userId == UserSessionManager.NO_USER_ID) {
                        return new MutableLiveData<>(Collections.emptyList());
                    }
                    return Transformations.map(
                            gamificationHistoryRepository.getHistoryFlow(), // Этот Flow уже должен зависеть от gamificationId (который зависит от userId)
                            historyList -> {
                                if (historyList == null) return Collections.emptyList();
                                return historyList.stream().limit(RECENT_HISTORY_LIMIT).collect(Collectors.toList());
                            }
                    );
                })
        );
        logger.debug(TAG, "recentHistoryLiveData initialized. HasActiveObservers (at init): " + recentHistoryLiveData.hasActiveObservers());

        MediatorLiveData<Object> combinator = new MediatorLiveData<>();
        combinator.addSource(userLiveData, value -> { logger.debug(TAG, "Combinator: userLiveData changed (value " + (value != null ? "NOT null" : "is null") + ")"); combineAndSetUiState(); });
        combinator.addSource(gamificationLiveData, value -> { logger.debug(TAG, "Combinator: gamificationLiveData changed (value " + (value != null ? "NOT null" : "is null") + ")"); combineAndSetUiState(); });
        combinator.addSource(earnedBadgesRefsLiveData, value -> { logger.debug(TAG, "Combinator: earnedBadgesRefsLiveData changed (value " + (value != null ? "NOT null, size " + value.size() : "is null") + ")"); combineAndSetUiState(); });
        combinator.addSource(allBadgesLiveData, value -> { logger.debug(TAG, "Combinator: allBadgesLiveData changed (value " + (value != null ? "NOT null, size " + value.size() : "is null") + ")"); combineAndSetUiState(); });
        combinator.addSource(recentHistoryLiveData, value -> { logger.debug(TAG, "Combinator: recentHistoryLiveData changed (value " + (value != null ? "NOT null, size " + value.size() : "is null") + ")"); combineAndSetUiState(); });

        // Наблюдаем за combinator, чтобы он начал работать.
        // Фрагмент будет наблюдать за _uiStateLiveData, которое обновляется из combineAndSetUiState.
        combinator.observeForever(ignored -> {}); // Этот наблюдатель можно удалить в onCleared
    }
    // ... остальной код ViewModel (combineAndSetUiState, logout, etc.) ...

    private void combineAndSetUiState() {
        UserAuth user = userLiveData.getValue();
        Gamification gamification = gamificationLiveData.getValue();
        List<GamificationBadgeCrossRef> earnedRefs = earnedBadgesRefsLiveData.getValue();
        List<Badge> allBadges = allBadgesLiveData.getValue();
        List<GamificationHistory> history = recentHistoryLiveData.getValue();
        ProfileUiState currentState = _uiStateLiveData.getValue();

        logger.debug(TAG, "combineAndSetUiState CALLED. User: " + (user != null) +
                ", Gami: " + (gamification != null) +
                ", EarnedRefs: " + (earnedRefs != null ? earnedRefs.size() : "null") +
                ", AllBadges: " + (allBadges != null ? allBadges.size() : "null") +
                ", History: " + (history != null ? history.size() : "null"));
        logger.debug(TAG, "Active Observers -> User: " + userLiveData.hasActiveObservers() +
                ", Gami: " + gamificationLiveData.hasActiveObservers() +
                ", EarnedRefs: " + earnedBadgesRefsLiveData.hasActiveObservers() +
                ", AllBadges: " + allBadgesLiveData.hasActiveObservers() +
                ", History: " + recentHistoryLiveData.hasActiveObservers());

        boolean waitingForUser = (user == null && userLiveData.hasActiveObservers());
        boolean waitingForGami = (gamification == null && gamificationLiveData.hasActiveObservers());
        // Для списков, если они null, но наблюдатель активен, считаем что ждем. Пустой список - это уже данные.
        boolean waitingForEarnedRefs = (earnedRefs == null && earnedBadgesRefsLiveData.hasActiveObservers());
        boolean waitingForAllBadges = (allBadges == null && allBadgesLiveData.hasActiveObservers());
        boolean waitingForHistory = (history == null && recentHistoryLiveData.hasActiveObservers());

        boolean finalIsLoading = waitingForUser || waitingForGami || waitingForEarnedRefs || waitingForAllBadges || waitingForHistory;

        logger.debug(TAG, "combineAndSetUiState: finalIsLoading = " + finalIsLoading +
                " (waitingUser: " + waitingForUser +
                ", waitingGami: " + waitingForGami +
                ", waitingEarned: " + waitingForEarnedRefs +
                ", waitingAllBadges: " + waitingForAllBadges +
                ", waitingHistory: " + waitingForHistory + ")");

        List<Badge> recentEarnedBadges = Collections.emptyList();
        int earnedCount = 0;
        if (earnedRefs != null && allBadges != null) {
            earnedCount = earnedRefs.size();
            Map<Long, LocalDateTime> earnedBadgesMap = earnedRefs.stream()
                    .collect(Collectors.toMap(GamificationBadgeCrossRef::getBadgeId, GamificationBadgeCrossRef::getEarnedAt, (t1,t2) -> t1));
            recentEarnedBadges = allBadges.stream()
                    .filter(badge -> earnedBadgesMap.containsKey(badge.getId()))
                    .sorted(Comparator.comparing((Badge badge) -> earnedBadgesMap.get(badge.getId())).reversed())
                    .limit(RECENT_BADGES_LIMIT)
                    .collect(Collectors.toList());
        }

        String currentError = currentState != null ? currentState.getError() : null;
        String errorMsg = (!finalIsLoading && user == null && gamification == null && currentError == null) ?
                "Не удалось загрузить данные профиля" : currentError;

        _uiStateLiveData.postValue(new ProfileUiState(
                finalIsLoading,
                currentState != null ? currentState.isUpdatingAvatar() : false,
                user,
                gamification,
                earnedCount,
                recentEarnedBadges,
                history != null ? history : Collections.emptyList(),
                errorMsg
        ));
        logger.debug(TAG, "ProfileUiState posted. isLoading: " + finalIsLoading + ", Error: " + errorMsg +
                ", User: " + (user != null) + ", Gami: " + (gamification != null));
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
                    null
            ));
        }
    }

    private void updateUiStateLoading(boolean isLoading) {
        ProfileUiState current = _uiStateLiveData.getValue();
        if (current != null) {
            _uiStateLiveData.postValue(new ProfileUiState(
                    isLoading, current.isUpdatingAvatar(), current.getUser(), current.getGamification(),
                    current.getEarnedBadgesCount(), current.getRecentBadges(), current.getRecentHistory(),
                    isLoading ? null : current.getError()
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
        // Отписываемся от userIdSource, если использовали observeForever
        if (userIdSourceObserver != null) {
            userSessionManager.getUserIdLiveData().removeObserver(userIdSourceObserver);
        }
        // Отписка от MediatorLiveData, если на него была подписка observeForever
        // В данном случае, MediatorLiveData не имеет внешних наблюдателей, кроме как для инициации.
        // Его источники - это LiveData, которые должны управляться Lifecycle'ом Fragment'а,
        // который наблюдает за _uiStateLiveData.
        logger.debug(TAG, "ProfileViewModel cleared.");
    }

    public String getDaysString(int days) {
        if (days < 0) days = 0;
        int lastDigit = days % 10;
        int lastTwoDigits = days % 100;

        if (lastTwoDigits >= 11 && lastTwoDigits <= 19) {
            return "дней";
        }
        return switch (lastDigit) {
            case 1 -> "день";
            case 2, 3, 4 -> "дня";
            default -> "дней";
        };
    }
}