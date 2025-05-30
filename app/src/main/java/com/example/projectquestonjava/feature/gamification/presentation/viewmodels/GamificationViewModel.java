package com.example.projectquestonjava.feature.gamification.presentation.viewmodels;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.managers.SnackbarManager; // Оставляем, т.к. он внедряется и может использоваться для showMessage
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.core.utils.SingleLiveEvent;
import com.example.projectquestonjava.feature.gamification.data.managers.GamificationDataStoreManager;
import com.example.projectquestonjava.feature.gamification.data.model.Badge;
import com.example.projectquestonjava.feature.gamification.data.model.Challenge;
import com.example.projectquestonjava.feature.gamification.data.model.Gamification;
import com.example.projectquestonjava.feature.gamification.data.model.GamificationBadgeCrossRef;
import com.example.projectquestonjava.feature.gamification.data.model.Reward;
import com.example.projectquestonjava.feature.gamification.data.model.SurpriseTask;
import com.example.projectquestonjava.feature.gamification.data.model.VirtualGarden;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengePeriod;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeProgressFullDetails;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeStatus;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeType;
import com.example.projectquestonjava.feature.gamification.domain.model.DailyRewardsInfo;
import com.example.projectquestonjava.feature.gamification.domain.model.PlantHealthState;
import com.example.projectquestonjava.feature.gamification.domain.model.RewardType;
import com.example.projectquestonjava.feature.gamification.domain.repository.BadgeRepository;
import com.example.projectquestonjava.feature.gamification.domain.repository.ChallengeRepository;
import com.example.projectquestonjava.feature.gamification.domain.repository.GamificationRepository;
import com.example.projectquestonjava.feature.gamification.domain.repository.RewardRepository;
import com.example.projectquestonjava.feature.gamification.domain.repository.SurpriseTaskRepository;
import com.example.projectquestonjava.feature.gamification.domain.repository.VirtualGardenRepository;
import com.example.projectquestonjava.feature.gamification.domain.usecases.AcceptSurpriseTaskUseCase;
import com.example.projectquestonjava.feature.gamification.domain.usecases.ClaimDailyRewardUseCase;
import com.example.projectquestonjava.feature.gamification.domain.usecases.GetDailyRewardsUseCase;
import com.example.projectquestonjava.feature.gamification.domain.usecases.ManuallyWaterPlantUseCase;
import com.example.projectquestonjava.feature.gamification.domain.usecases.SelectNewSurpriseTaskUseCase;
import com.example.projectquestonjava.feature.gamification.presentation.utils.GamificationUiUtils;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import dagger.hilt.android.lifecycle.HiltViewModel;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.inject.Inject;

@HiltViewModel
public class GamificationViewModel extends ViewModel {

    private static final String TAG = "GamificationViewModel";

    private final GetDailyRewardsUseCase getDailyRewardsUseCase;
    private final ClaimDailyRewardUseCase claimDailyRewardUseCase;
    private final AcceptSurpriseTaskUseCase acceptSurpriseTaskUseCase;
    private final SelectNewSurpriseTaskUseCase selectNewSurpriseTaskUseCase;
    private final ManuallyWaterPlantUseCase manuallyWaterPlantUseCase;

    private final GamificationRepository gamificationRepository;
    private final VirtualGardenRepository virtualGardenRepository;
    private final ChallengeRepository challengeRepository;
    private final SurpriseTaskRepository surpriseTaskRepository;
    private final BadgeRepository badgeRepository;
    private final RewardRepository rewardRepository;

    private final GamificationDataStoreManager gamificationDataStoreManager;
    private final DateTimeUtils dateTimeUtils;
    private final Executor ioExecutor;
    @Getter
    public final Logger logger;
    private final SnackbarManager snackbarManager; // Оставляем, используется для showMessage


    private final MutableLiveData<Integer> _selectedTab = new MutableLiveData<>(0);
    public final LiveData<Integer> selectedTab = _selectedTab;

    private final SingleLiveEvent<String> _errorMessageEvent = new SingleLiveEvent<>();
    public final LiveData<String> errorMessageEvent = _errorMessageEvent;
    private final SingleLiveEvent<String> _successMessageEvent = new SingleLiveEvent<>();
    public final LiveData<String> successMessageEvent = _successMessageEvent;

    private final MutableLiveData<Boolean> _isLoadingLiveData = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoadingLiveData = _isLoadingLiveData;

    public final LiveData<Gamification> gamificationState;
    public final LiveData<VirtualGarden> selectedPlantState;
    public final LiveData<PlantHealthState> plantHealthState;
    public final LiveData<Boolean> canWaterToday;
    public final LiveData<GamificationViewModel.Pair<SurpriseTask, Reward>> surpriseTaskDetailsState;
    private final MutableLiveData<DailyRewardsInfo> _dailyRewardsInfo = new MutableLiveData<>(null);
    public final LiveData<DailyRewardsInfo> dailyRewardsInfo = _dailyRewardsInfo;
    public final LiveData<ActiveChallengesSectionState> challengesSectionState;
    private final MutableLiveData<ChallengeCardInfo> _challengeToShowDetails = new MutableLiveData<>(null);
    public final LiveData<ChallengeCardInfo> challengeToShowDetails = _challengeToShowDetails;

    public final LiveData<List<Badge>> allBadgesState;
    public final LiveData<List<GamificationBadgeCrossRef>> earnedBadgesState;

    private final Observer<Gamification> gamificationObserverForDependentLoads;
    private final Observer<Long> selectedPlantIdObserver;
    private final Observer<List<VirtualGarden>> allPlantsObserver;
    private final Observer<SurpriseTask> activeSurpriseTaskObserver;
    private final Observer<Set<Long>> hiddenIdsObserver;

    private final MediatorLiveData<VirtualGarden> selectedPlantMediator = new MediatorLiveData<>();
    private Long currentSelectedPlantIdFromStore = -1L;
    private List<VirtualGarden> currentAllPlantsFromRepo = Collections.emptyList();

    private final MediatorLiveData<GamificationViewModel.Pair<SurpriseTask, Reward>> surpriseTaskDetailsMediator = new MediatorLiveData<>();
    private SurpriseTask currentActiveSurpriseTask = null;
    private Set<Long> currentHiddenIds = Collections.emptySet();


    @Inject
    public GamificationViewModel(
            GamificationRepository gamificationRepository,
            VirtualGardenRepository virtualGardenRepository,
            ChallengeRepository challengeRepository,
            SurpriseTaskRepository surpriseTaskRepository,
            BadgeRepository badgeRepository,
            RewardRepository rewardRepository,
            GetDailyRewardsUseCase getDailyRewardsUseCase,
            ClaimDailyRewardUseCase claimDailyRewardUseCase,
            AcceptSurpriseTaskUseCase acceptSurpriseTaskUseCase,
            SelectNewSurpriseTaskUseCase selectNewSurpriseTaskUseCase,
            ManuallyWaterPlantUseCase manuallyWaterPlantUseCase,
            GamificationDataStoreManager gamificationDataStoreManager,
            DateTimeUtils dateTimeUtils,
            @IODispatcher Executor ioExecutor,
            Logger logger,
            SnackbarManager snackbarManager) { // SnackbarManager остается
        this.gamificationRepository = gamificationRepository;
        this.virtualGardenRepository = virtualGardenRepository;
        this.challengeRepository = challengeRepository;
        this.surpriseTaskRepository = surpriseTaskRepository;
        this.badgeRepository = badgeRepository;
        this.rewardRepository = rewardRepository;
        this.getDailyRewardsUseCase = getDailyRewardsUseCase;
        this.claimDailyRewardUseCase = claimDailyRewardUseCase;
        this.acceptSurpriseTaskUseCase = acceptSurpriseTaskUseCase;
        this.selectNewSurpriseTaskUseCase = selectNewSurpriseTaskUseCase;
        this.manuallyWaterPlantUseCase = manuallyWaterPlantUseCase;
        this.gamificationDataStoreManager = gamificationDataStoreManager;
        this.dateTimeUtils = dateTimeUtils;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
        this.snackbarManager = snackbarManager; // SnackbarManager остается

        logger.info(TAG, "ViewModel initialized.");

        gamificationState = Transformations.distinctUntilChanged(gamificationRepository.getCurrentUserGamificationFlow());

        LiveData<Long> selectedPlantIdFlowInternal = gamificationDataStoreManager.getSelectedPlantIdFlow();
        LiveData<List<VirtualGarden>> allPlantsFlowInternal = Transformations.switchMap(
                gamificationDataStoreManager.getGamificationIdFlow(),
                gamiId -> (gamiId == null || gamiId == -1L) ?
                        new MutableLiveData<>(Collections.emptyList()) :
                        virtualGardenRepository.getAllPlantsFlow()
        );

        selectedPlantIdObserver = selectedId -> {
            currentSelectedPlantIdFromStore = selectedId;
            updateSelectedPlantMediator();
        };
        allPlantsObserver = plants -> {
            currentAllPlantsFromRepo = plants;
            updateSelectedPlantMediator();
        };

        selectedPlantMediator.addSource(selectedPlantIdFlowInternal, selectedPlantIdObserver);
        selectedPlantMediator.addSource(allPlantsFlowInternal, allPlantsObserver);
        selectedPlantState = Transformations.distinctUntilChanged(selectedPlantMediator);


        plantHealthState = Transformations.map(selectedPlantState, this::calculateHealthState);
        canWaterToday = Transformations.map(selectedPlantState, this::calculateCanWaterToday);

        LiveData<SurpriseTask> activeSurpriseTaskFlowInternal = Transformations.switchMap(
                gamificationDataStoreManager.getGamificationIdFlow(),
                gamiId -> (gamiId == null || gamiId == -1L) ?
                        new MutableLiveData<>(null) :
                        surpriseTaskRepository.getActiveTaskForDateFlow(LocalDate.now())
        );
        LiveData<Set<Long>> hiddenIdsFlowInternal = gamificationDataStoreManager.getHiddenExpiredTaskIdsFlow();

        activeSurpriseTaskObserver = task -> {
            currentActiveSurpriseTask = task;
            updateSurpriseDetailsMediator();
        };
        hiddenIdsObserver = hiddenIds -> {
            currentHiddenIds = hiddenIds != null ? hiddenIds : Collections.emptySet();
            updateSurpriseDetailsMediator();
        };
        surpriseTaskDetailsMediator.addSource(activeSurpriseTaskFlowInternal, activeSurpriseTaskObserver);
        surpriseTaskDetailsMediator.addSource(hiddenIdsFlowInternal, hiddenIdsObserver);
        surpriseTaskDetailsState = Transformations.distinctUntilChanged(surpriseTaskDetailsMediator);


        challengesSectionState = Transformations.map(
                challengeRepository.getChallengesWithDetailsFlow(null),
                this::calculateChallengesSectionStateJava
        );

        allBadgesState = Transformations.distinctUntilChanged(badgeRepository.getAllBadgesFlow());
        earnedBadgesState = Transformations.switchMap(
                gamificationDataStoreManager.getGamificationIdFlow(),
                gamiId -> (gamiId == null || gamiId == -1L) ?
                        new MutableLiveData<>(Collections.emptyList()) :
                        badgeRepository.getEarnedBadgesFlow()
        );

        gamificationObserverForDependentLoads = gamificationProfile -> {
            if (gamificationProfile != null) {
                logger.debug(TAG, "Gamification state changed/loaded. Reloading dependent data.");
                loadDailyRewardsInfoInternal();
                checkAndSelectSurpriseTaskIfNeededInternal();
            } else {
                logger.debug(TAG, "Gamification state is null. Resetting dependent data.");
                _dailyRewardsInfo.postValue(null);
            }
        };
        gamificationState.observeForever(gamificationObserverForDependentLoads);
    }

    private void updateSelectedPlantMediator() {
        Long selectedId = currentSelectedPlantIdFromStore;
        List<VirtualGarden> allPlants = currentAllPlantsFromRepo;

        if (allPlants == null || allPlants.isEmpty()) {
            selectedPlantMediator.setValue(null); return;
        }
        if (selectedId != null && selectedId != -1L) {
            VirtualGarden found = allPlants.stream().filter(p -> p.getId() == selectedId).findFirst().orElse(null);
            if (found != null) { selectedPlantMediator.setValue(found); return; }
        }
        selectedPlantMediator.setValue(allPlants.stream().max(Comparator.comparingLong(VirtualGarden::getId)).orElse(null));
    }

    private void updateSurpriseDetailsMediator() {
        SurpriseTask task = currentActiveSurpriseTask;
        Set<Long> hiddenIds = currentHiddenIds;

        if (task == null) {
            surpriseTaskDetailsMediator.setValue(null);
            return;
        }
        boolean isExpired = LocalDateTime.now().isAfter(task.getExpirationTime());
        boolean isHiddenAndShouldBe = hiddenIds.contains(task.getId()) && isExpired && !task.isCompleted();

        if (isHiddenAndShouldBe) {
            logger.debug(TAG, "Task " + task.getId() + " is expired and hidden, filtering out for details.");
            surpriseTaskDetailsMediator.setValue(null);
        } else {
            ListenableFuture<Reward> rewardFuture = rewardRepository.getRewardById(task.getRewardId());
            Futures.addCallback(rewardFuture, new FutureCallback<Reward>() {
                @Override
                public void onSuccess(Reward reward) {
                    surpriseTaskDetailsMediator.postValue(new Pair<>(task, reward));
                }
                @Override
                public void onFailure(@NonNull Throwable t) {
                    logger.error(TAG, "Error loading reward for surprise task " + task.getId(), t);
                    surpriseTaskDetailsMediator.postValue(new Pair<>(task, null));
                }
            }, ioExecutor);
        }
    }

    private void loadDailyRewardsInfoInternal() {
        _isLoadingLiveData.postValue(true);
        ListenableFuture<DailyRewardsInfo> future = getDailyRewardsUseCase.execute();
        Futures.addCallback(future, new FutureCallback<DailyRewardsInfo>() {
            @Override public void onSuccess(DailyRewardsInfo info) {
                _dailyRewardsInfo.postValue(info);
                logger.debug(TAG, "Daily rewards info loaded.");
                _isLoadingLiveData.postValue(false);
            }
            @Override public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Failed to load daily rewards info", t);
                _errorMessageEvent.postValue("Ошибка загрузки ежедневных наград."); // Пост в SingleLiveEvent
                _dailyRewardsInfo.postValue(null);
                _isLoadingLiveData.postValue(false);
            }
        }, ioExecutor);
    }

    private void checkAndSelectSurpriseTaskIfNeededInternal() {
        ioExecutor.execute(() -> {
            logger.debug(TAG, "Checking if a new surprise task needs to be selected...");
            ListenableFuture<SurpriseTask> future = selectNewSurpriseTaskUseCase.execute(LocalDate.now());
            Futures.addCallback(future, new FutureCallback<SurpriseTask>() {
                @Override public void onSuccess(SurpriseTask task) { /* LiveData обновится сам */ }
                @Override public void onFailure(@NonNull Throwable t) { logger.error(TAG, "Error selecting new surprise task", t); }
            }, ioExecutor);
        });
    }

    public void selectTab(int tabIndex) { _selectedTab.setValue(tabIndex); }
    public void clearErrorMessage() { _errorMessageEvent.setValue(null); } // Очистка SingleLiveEvent
    public void clearSuccessMessage() { _successMessageEvent.setValue(null); } // Очистка SingleLiveEvent

    public void waterPlant() {
        Boolean canWater = canWaterToday.getValue();
        Boolean isLoading = _isLoadingLiveData.getValue();
        if (canWater == null || !canWater) {
            snackbarManager.showMessage("Вы уже поливали растения сегодня."); // Используем SnackbarManager
            return;
        }
        if (isLoading != null && isLoading) return;

        _isLoadingLiveData.setValue(true);
        _errorMessageEvent.setValue(null); _successMessageEvent.setValue(null);
        ListenableFuture<Void> future = manuallyWaterPlantUseCase.execute();
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override public void onSuccess(Void result) {
                snackbarManager.showMessage("Растения политы!"); // Используем SnackbarManager
                _isLoadingLiveData.postValue(false);
            }
            @Override public void onFailure(@NonNull Throwable t) {
                snackbarManager.showMessage("Не удалось полить: " + t.getMessage()); // Используем SnackbarManager
                _isLoadingLiveData.postValue(false);
            }
        }, ioExecutor);
    }

    public void claimTodayReward() {
        _isLoadingLiveData.setValue(true);
        ListenableFuture<Void> future = claimDailyRewardUseCase.execute();
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override public void onSuccess(Void result) {
                snackbarManager.showMessage("Награда получена!"); // Используем SnackbarManager
                loadDailyRewardsInfoInternal();
                _isLoadingLiveData.postValue(false);
            }
            @Override public void onFailure(@NonNull Throwable t) {
                snackbarManager.showMessage("Не удалось получить награду: " + t.getMessage()); // Используем SnackbarManager
                _isLoadingLiveData.postValue(false);
            }
        }, ioExecutor);
    }

    public void acceptSurpriseTask(SurpriseTask task) {
        _isLoadingLiveData.setValue(true);
        ListenableFuture<Void> future = acceptSurpriseTaskUseCase.execute(task);
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override public void onSuccess(Void result) {
                snackbarManager.showMessage("Награда за сюрприз получена!"); // Используем SnackbarManager
                _isLoadingLiveData.postValue(false);
            }
            @Override public void onFailure(@NonNull Throwable t) {
                snackbarManager.showMessage("Не удалось получить награду: " + t.getMessage()); // Используем SnackbarManager
                _isLoadingLiveData.postValue(false);
            }
        }, ioExecutor);
    }

    public void hideExpiredSurpriseTask(long taskId) {
        Futures.addCallback(gamificationDataStoreManager.hideExpiredTaskId(taskId),
                new FutureCallback<Void>() {
                    @Override public void onSuccess(Void result) {}
                    @Override public void onFailure(@NonNull Throwable t) {
                        snackbarManager.showMessage("Не удалось скрыть задачу."); // Используем SnackbarManager
                    }
                }, ioExecutor);
    }

    public void showChallengeDetails(ChallengeCardInfo challengeInfo) {
        _challengeToShowDetails.setValue(challengeInfo);
    }
    public void clearChallengeDetails() {
        _challengeToShowDetails.setValue(null);
    }

    private PlantHealthState calculateHealthState(VirtualGarden plant) {
        if (plant == null) return PlantHealthState.HEALTHY;
        LocalDate lastWateredDate = dateTimeUtils.utcToLocalLocalDateTime(plant.getLastWatered()).toLocalDate();
        long daysSinceWatered = ChronoUnit.DAYS.between(lastWateredDate, LocalDate.now());
        if (daysSinceWatered <= 1) return PlantHealthState.HEALTHY;
        if (daysSinceWatered == 2) return PlantHealthState.NEEDSWATER;
        return PlantHealthState.WILTED;
    }

    private boolean calculateCanWaterToday(VirtualGarden plant) {
        if (plant == null) return false;
        LocalDate lastWateredDate = dateTimeUtils.utcToLocalLocalDateTime(plant.getLastWatered()).toLocalDate();
        return lastWateredDate.isBefore(LocalDate.now());
    }

    private ActiveChallengesSectionState calculateChallengesSectionStateJava(List<ChallengeProgressFullDetails> allDetailsList) {
        logger.debug(TAG, "calculateChallengesSectionStateJava: Input " + (allDetailsList == null ? "null" : allDetailsList.size()) + " full details.");
        if (allDetailsList == null || allDetailsList.isEmpty()) {
            logger.debug(TAG, "calculateChallengesSectionStateJava: Returning EMPTY state due to null/empty input.");
            return new ActiveChallengesSectionState();
        }
        int dailyCompleted = 0; int dailyTotal = 0;
        int weeklyCompleted = 0; int weeklyTotal = 0;
        int urgent = 0; int nearCompletion = 0;
        LocalDateTime now = dateTimeUtils.currentLocalDateTime();
        LocalDate today = now.toLocalDate();
        LocalDate tomorrow = today.plusDays(1);
        int currentWeek = now.get(WeekFields.ISO.weekOfWeekBasedYear());
        int currentYear = now.getYear();
        List<ChallengeCardInfo> challengeInfos = new ArrayList<>();

        Map<Long, List<ChallengeProgressFullDetails>> groupedByChallengeId = allDetailsList.stream()
                .filter(d -> d != null && d.getChallengeAndReward() != null && d.getChallengeAndReward().getChallenge() != null && d.getRule() != null && d.getProgress() != null)
                .collect(Collectors.groupingBy(d -> d.getChallengeAndReward().getChallenge().getId()));

        logger.debug(TAG, "calculateChallengesSectionStateJava: Grouped " + groupedByChallengeId.size() + " unique challenges.");

        for (Map.Entry<Long, List<ChallengeProgressFullDetails>> entry : groupedByChallengeId.entrySet()) {
            List<ChallengeProgressFullDetails> detailsForOneChallenge = entry.getValue();
            if (detailsForOneChallenge.isEmpty()) continue;

            Challenge challenge = detailsForOneChallenge.get(0).getChallengeAndReward().getChallenge();
            Reward reward = detailsForOneChallenge.get(0).getChallengeAndReward().getReward();

            float overallProgress = calculateOverallChallengeProgress(detailsForOneChallenge);
            boolean isChallengeCompletedByStatus = challenge.getStatus() == ChallengeStatus.COMPLETED;

            LocalDateTime lastUpdateTime = detailsForOneChallenge.stream()
                    .map(d -> d.getProgress().getLastUpdated())
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo).orElse(challenge.getStartDate());

            boolean shouldShowCard = false;

            if (challenge.getPeriod() == ChallengePeriod.DAILY) {
                dailyTotal++;
                if (isChallengeCompletedByStatus && lastUpdateTime != null && lastUpdateTime.toLocalDate().isEqual(today)) {
                    dailyCompleted++;
                }
                if (challenge.getStatus() == ChallengeStatus.ACTIVE || (isChallengeCompletedByStatus && lastUpdateTime != null && lastUpdateTime.toLocalDate().isEqual(today)) ) {
                    shouldShowCard = true;
                }
            } else if (challenge.getPeriod() == ChallengePeriod.WEEKLY) {
                weeklyTotal++;
                if (isChallengeCompletedByStatus && lastUpdateTime != null && lastUpdateTime.getYear() == currentYear && lastUpdateTime.get(WeekFields.ISO.weekOfWeekBasedYear()) == currentWeek) {
                    weeklyCompleted++;
                }
                if (challenge.getStatus() == ChallengeStatus.ACTIVE || (isChallengeCompletedByStatus && lastUpdateTime != null && lastUpdateTime.getYear() == currentYear && lastUpdateTime.get(WeekFields.ISO.weekOfWeekBasedYear()) == currentWeek) ) {
                    shouldShowCard = true;
                }
            } else if (challenge.getStatus() == ChallengeStatus.ACTIVE) {
                shouldShowCard = true;
            }

            if (challenge.getStatus() == ChallengeStatus.ACTIVE) {
                boolean isCurrentlyUrgent = challenge.getEndDate().toLocalDate().isEqual(today) ||
                        challenge.getEndDate().toLocalDate().isEqual(tomorrow);
                if (isCurrentlyUrgent) urgent++;
                if (overallProgress >= 0.8f && overallProgress < 1.0f) nearCompletion++;
            }

            if (shouldShowCard) {
                boolean isUrgentForCard = (challenge.getStatus() == ChallengeStatus.ACTIVE) &&
                        (challenge.getEndDate().toLocalDate().isEqual(today) || challenge.getEndDate().toLocalDate().isEqual(tomorrow));

                ChallengeType ruleType = detailsForOneChallenge.get(0).getRule() != null ?
                        detailsForOneChallenge.get(0).getRule().getType() :
                        ChallengeType.CUSTOM_EVENT;

                challengeInfos.add(new ChallengeCardInfo(
                        challenge.getId(),
                        GamificationUiUtils.getIconResForChallengeType(ruleType),
                        challenge.getName(), challenge.getDescription(), overallProgress,
                        formatProgressTextJava(detailsForOneChallenge),
                        isChallengeCompletedByStatus ? null : formatDeadlineTextJava(challenge.getEndDate(), isUrgentForCard),
                        reward != null ? GamificationUiUtils.getIconResForRewardType(reward.getRewardType()) : null,
                        reward != null ? reward.getName() : null,
                        isUrgentForCard, challenge.getPeriod()
                ));
            }
        }
        logger.debug(TAG, "calculateChallengesSectionStateJava: Generated " + challengeInfos.size() + " ChallengeCardInfo objects.");

        challengeInfos.sort(
                Comparator.comparing((ChallengeCardInfo c) -> !c.isUrgent())
                        .thenComparing(c -> !(c.progress() < 1.0f && c.progress() >= 0.8f))
                        .thenComparing(c -> !(c.progress() < 1.0f))
                        .thenComparing(Comparator.comparingDouble(ChallengeCardInfo::progress).reversed())
        );
        return new ActiveChallengesSectionState(
                (int) challengeInfos.stream().filter(ci -> ci.progress() < 1.0f).count(), // Подсчет только действительно активных (не 100% выполненных)
                challengeInfos, dailyCompleted, dailyTotal, weeklyCompleted, weeklyTotal, urgent, nearCompletion
        );
    }

    private float calculateOverallChallengeProgress(List<ChallengeProgressFullDetails> details) {
        if (details.isEmpty()) return 0f;
        if (details.get(0).getChallengeAndReward().getChallenge().getStatus() == ChallengeStatus.COMPLETED) {
            return 1.0f;
        }
        float totalProgressVal = 0; float totalTargetVal = 0;
        for (ChallengeProgressFullDetails detail : details) {
            if (detail.getProgress() != null && detail.getRule() != null) {
                totalProgressVal += detail.getProgress().getProgress();
                totalTargetVal += detail.getRule().getTarget();
            }
        }
        float progress = (totalTargetVal > 0) ? (totalProgressVal / totalTargetVal) : 0f;
        return Math.max(0f, Math.min(1f, progress));
    }

    private String formatProgressTextJava(List<ChallengeProgressFullDetails> details) {
        if (details.isEmpty() || details.get(0).getRule() == null || details.get(0).getProgress() == null) return "?/?";
        int currentSum = details.stream().filter(d -> d.getProgress() != null).mapToInt(d -> d.getProgress().getProgress()).sum();
        int targetSum = details.stream().filter(d -> d.getRule() != null).mapToInt(d -> d.getRule().getTarget()).sum();
        ChallengeType firstType = details.get(0).getRule().getType();
        String unit = "";
        if (firstType != null) {
            unit = switch (firstType) {
                case TASK_COMPLETION -> " задач";
                case POMODORO_SESSION -> " Pomodoro";
                case DAILY_STREAK -> " дн. стрика";
                default -> "";
            };
        }
        return currentSum + "/" + targetSum + unit;
    }

    private String formatDeadlineTextJava(LocalDateTime deadline, boolean isUrgent) {
        LocalDateTime now = dateTimeUtils.currentLocalDateTime();
        long daysLeft = ChronoUnit.DAYS.between(now.toLocalDate(), deadline.toLocalDate());
        daysLeft = Math.max(0, daysLeft);

        if (daysLeft == 0) return "Сегодня до " + deadline.format(DateTimeFormatter.ofPattern("HH:mm"));
        if (daysLeft == 1) return "Завтра до " + deadline.format(DateTimeFormatter.ofPattern("HH:mm"));
        return "Ост. " + daysLeft + " " + GamificationUiUtils.getDaysStringJava((int)daysLeft);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        gamificationState.removeObserver(gamificationObserverForDependentLoads);

        LiveData<Long> selectedPlantIdFlowInternal = gamificationDataStoreManager.getSelectedPlantIdFlow();
        LiveData<List<VirtualGarden>> allPlantsFlowInternal = Transformations.switchMap(
                gamificationDataStoreManager.getGamificationIdFlow(),
                gamiId -> (gamiId == null || gamiId == -1L) ?
                        new MutableLiveData<>(Collections.emptyList()) :
                        virtualGardenRepository.getAllPlantsFlow()
        );
        selectedPlantMediator.removeSource(selectedPlantIdFlowInternal);
        selectedPlantMediator.removeSource(allPlantsFlowInternal);

        LiveData<SurpriseTask> activeSurpriseTaskFlowInternal = Transformations.switchMap(
                gamificationDataStoreManager.getGamificationIdFlow(),
                gamiId -> (gamiId == null || gamiId == -1L) ?
                        new MutableLiveData<>(null) :
                        surpriseTaskRepository.getActiveTaskForDateFlow(LocalDate.now())
        );
        LiveData<Set<Long>> hiddenIdsFlowInternal = gamificationDataStoreManager.getHiddenExpiredTaskIdsFlow();
        surpriseTaskDetailsMediator.removeSource(activeSurpriseTaskFlowInternal);
        surpriseTaskDetailsMediator.removeSource(hiddenIdsFlowInternal);
        logger.debug(TAG, "GamificationViewModel cleared.");
    }

    public record Pair<F, S>(@Getter F first, @Getter S second) {}
}