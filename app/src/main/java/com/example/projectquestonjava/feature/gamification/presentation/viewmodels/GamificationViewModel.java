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
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.core.utils.SingleLiveEvent; // Если используешь его
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
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.inject.Inject;

@HiltViewModel
public class GamificationViewModel extends ViewModel {

    private static final String TAG = "GamificationViewModel";

    // Use Cases
    private final GetDailyRewardsUseCase getDailyRewardsUseCase;
    private final ClaimDailyRewardUseCase claimDailyRewardUseCase;
    private final AcceptSurpriseTaskUseCase acceptSurpriseTaskUseCase;
    private final SelectNewSurpriseTaskUseCase selectNewSurpriseTaskUseCase;
    private final ManuallyWaterPlantUseCase manuallyWaterPlantUseCase;

    // Repositories
    private final GamificationRepository gamificationRepository;
    private final VirtualGardenRepository virtualGardenRepository;
    private final ChallengeRepository challengeRepository;
    private final SurpriseTaskRepository surpriseTaskRepository;
    private final BadgeRepository badgeRepository;
    private final RewardRepository rewardRepository;

    // Managers & Utils
    private final GamificationDataStoreManager gamificationDataStoreManager;
    private final DateTimeUtils dateTimeUtils;
    private final Executor ioExecutor;
    @Getter // Lombok для логгера, если он используется в адаптере
    public final Logger logger;


    // --- LiveData для UI ---
    private final MutableLiveData<Integer> _selectedTab = new MutableLiveData<>(0);
    public final LiveData<Integer> selectedTab = _selectedTab;

    // Используем SingleLiveEvent или другой механизм для одноразовых событий Snackbar
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
    public final LiveData<GamificationViewModel.Pair<SurpriseTask, Reward>> surpriseTaskDetailsState; // Используем внутренний Pair
    private final MutableLiveData<DailyRewardsInfo> _dailyRewardsInfo = new MutableLiveData<>(null);
    public final LiveData<DailyRewardsInfo> dailyRewardsInfo = _dailyRewardsInfo;
    public final LiveData<ActiveChallengesSectionState> challengesSectionState;
    private final MutableLiveData<ChallengeCardInfo> _challengeToShowDetails = new MutableLiveData<>(null);
    public final LiveData<ChallengeCardInfo> challengeToShowDetails = _challengeToShowDetails;

    public final LiveData<List<Badge>> allBadgesState;
    public final LiveData<List<GamificationBadgeCrossRef>> earnedBadgesState;

    // Observer'ы для отписки
    private final Observer<Gamification> gamificationObserverForDependentLoads;
    private final Observer<Long> selectedPlantIdObserver;
    private final Observer<List<VirtualGarden>> allPlantsObserver;
    private final Observer<SurpriseTask> activeSurpriseTaskObserver;
    private final Observer<Set<Long>> hiddenIdsObserver;

    // Для комбинирования selectedPlantState
    private final MediatorLiveData<VirtualGarden> selectedPlantMediator = new MediatorLiveData<>();
    private Long currentSelectedPlantIdFromStore = -1L; // Храним последнее значение из DataStore
    private List<VirtualGarden> currentAllPlantsFromRepo = Collections.emptyList(); // Храним последние растения

    // Для комбинирования surpriseTaskDetailsState
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
            Logger logger) {
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

        logger.info(TAG, "ViewModel initialized.");

        gamificationState = Transformations.distinctUntilChanged(gamificationRepository.getCurrentUserGamificationFlow());

        // Selected Plant Logic
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

        // Surprise Task Logic
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
            // Загружаем Reward для задачи
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
                _errorMessageEvent.postValue("Ошибка загрузки ежедневных наград.");
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
    public void clearErrorMessage() { _errorMessageEvent.setValue(null); }
    public void clearSuccessMessage() { _successMessageEvent.setValue(null); }

    public void waterPlant() {
        Boolean canWater = canWaterToday.getValue();
        Boolean isLoading = _isLoadingLiveData.getValue();
        if (canWater == null || !canWater) {
            _errorMessageEvent.postValue("Вы уже поливали растения сегодня."); return;
        }
        if (isLoading != null && isLoading) return;

        _isLoadingLiveData.setValue(true);
        _errorMessageEvent.setValue(null); _successMessageEvent.setValue(null);
        ListenableFuture<Void> future = manuallyWaterPlantUseCase.execute();
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override public void onSuccess(Void result) {
                _successMessageEvent.postValue("Растения политы!");
                _isLoadingLiveData.postValue(false);
            }
            @Override public void onFailure(@NonNull Throwable t) {
                _errorMessageEvent.postValue("Не удалось полить: " + t.getMessage());
                _isLoadingLiveData.postValue(false);
            }
        }, ioExecutor);
    }

    public void claimTodayReward() {
        _isLoadingLiveData.setValue(true);
        ListenableFuture<Void> future = claimDailyRewardUseCase.execute();
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override public void onSuccess(Void result) {
                _successMessageEvent.postValue("Награда получена!");
                loadDailyRewardsInfoInternal();
                _isLoadingLiveData.postValue(false);
            }
            @Override public void onFailure(@NonNull Throwable t) {
                _errorMessageEvent.postValue("Не удалось получить награду: " + t.getMessage());
                _isLoadingLiveData.postValue(false);
            }
        }, ioExecutor);
    }

    public void acceptSurpriseTask(SurpriseTask task) {
        _isLoadingLiveData.setValue(true);
        ListenableFuture<Void> future = acceptSurpriseTaskUseCase.execute(task);
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override public void onSuccess(Void result) {
                _successMessageEvent.postValue("Награда за сюрприз получена!");
                _isLoadingLiveData.postValue(false);
            }
            @Override public void onFailure(@NonNull Throwable t) {
                _errorMessageEvent.postValue("Не удалось получить награду: " + t.getMessage());
                _isLoadingLiveData.postValue(false);
            }
        }, ioExecutor);
    }

    public void hideExpiredSurpriseTask(long taskId) {
        Futures.addCallback(gamificationDataStoreManager.hideExpiredTaskId(taskId),
                new FutureCallback<Void>() {
                    @Override public void onSuccess(Void result) {}
                    @Override public void onFailure(@NonNull Throwable t) {
                        _errorMessageEvent.postValue("Не удалось скрыть задачу.");
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
        if (allDetailsList == null || allDetailsList.isEmpty()) {
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

        Map<Long, List<ChallengeProgressFullDetails>> grouped = allDetailsList.stream()
                .collect(Collectors.groupingBy(d -> d.getChallengeAndReward().getChallenge().getId()));

        for (List<ChallengeProgressFullDetails> detailsForOneChallenge : grouped.values()) {
            if (detailsForOneChallenge.isEmpty()) continue;
            Challenge challenge = detailsForOneChallenge.get(0).getChallengeAndReward().getChallenge();
            Reward reward = detailsForOneChallenge.get(0).getChallengeAndReward().getReward();
            float overallProgress = calculateOverallChallengeProgress(detailsForOneChallenge);
            boolean isChallengeCompletedByStatus = challenge.getStatus() == ChallengeStatus.COMPLETED;

            LocalDateTime lastUpdateTime = detailsForOneChallenge.stream()
                    .map(d -> d.getProgress().getLastUpdated())
                    .max(LocalDateTime::compareTo).orElse(challenge.getStartDate());
            boolean shouldShowCard = false;

            if (challenge.getPeriod() == ChallengePeriod.DAILY) {
                dailyTotal++;
                if (isChallengeCompletedByStatus && lastUpdateTime.toLocalDate().isEqual(today)) {
                    dailyCompleted++; shouldShowCard = true;
                } else if (challenge.getStatus() == ChallengeStatus.ACTIVE) {
                    shouldShowCard = true;
                }
            } else if (challenge.getPeriod() == ChallengePeriod.WEEKLY) {
                weeklyTotal++;
                if (isChallengeCompletedByStatus && lastUpdateTime.getYear() == currentYear && lastUpdateTime.get(WeekFields.ISO.weekOfWeekBasedYear()) == currentWeek) {
                    weeklyCompleted++; shouldShowCard = true;
                } else if (challenge.getStatus() == ChallengeStatus.ACTIVE) {
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
                challengeInfos.add(new ChallengeCardInfo(
                        challenge.getId(),
                        getIconResForChallengeType(detailsForOneChallenge.get(0).getRule().getType()),
                        challenge.getName(), challenge.getDescription(), overallProgress,
                        formatProgressTextJava(detailsForOneChallenge),
                        isChallengeCompletedByStatus ? null : formatDeadlineTextJava(challenge.getEndDate(), isUrgentForCard),
                        reward != null ? getIconResForRewardType(reward.getRewardType()) : null,
                        reward != null ? reward.getName() : null,
                        isUrgentForCard, challenge.getPeriod()
                ));
            }
        }
        challengeInfos.sort(
                Comparator.comparing((ChallengeCardInfo c) -> !c.isUrgent()) // Срочные вначале
                        .thenComparing(c -> !(c.progress() < 1.0f && c.progress() >= 0.8f)) // Близкие к завершению
                        .thenComparing(c -> !(c.progress() < 1.0f)) // Остальные активные
                        .thenComparing(Comparator.comparingDouble(ChallengeCardInfo::progress).reversed()) // Затем по убыванию прогресса
        );
        return new ActiveChallengesSectionState(
                (int) allDetailsList.stream().filter(d -> d.getChallengeAndReward().getChallenge().getStatus() == ChallengeStatus.ACTIVE).count(),
                challengeInfos, dailyCompleted, dailyTotal, weeklyCompleted, weeklyTotal, urgent, nearCompletion
        );
    }

    private float calculateOverallChallengeProgress(List<ChallengeProgressFullDetails> details) {
        if (details.isEmpty()) return 0f;
        // Если челлендж выполнен по статусу, считаем прогресс 100%
        if (details.get(0).getChallengeAndReward().getChallenge().getStatus() == ChallengeStatus.COMPLETED) {
            return 1.0f;
        }
        float totalProgressVal = 0; float totalTargetVal = 0;
        for (ChallengeProgressFullDetails detail : details) {
            totalProgressVal += detail.getProgress().getProgress();
            totalTargetVal += detail.getRule().getTarget();
        }
        float progress = (totalTargetVal > 0) ? (totalProgressVal / totalTargetVal) : 0f;
        return Math.max(0f, Math.min(1f, progress));
    }


    private String formatProgressTextJava(List<ChallengeProgressFullDetails> details) {
        if (details.isEmpty()) return "?/?";
        int currentSum = details.stream().mapToInt(d -> d.getProgress().getProgress()).sum();
        int targetSum = details.stream().mapToInt(d -> d.getRule().getTarget()).sum();
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
        // Для Compose было: if (isUrgent || daysLeft < 7)
        // Для XML, возможно, лучше всегда показывать дни, если не сегодня/завтра
        return "Ост. " + daysLeft + " " + getDaysStringSimpleJava((int)daysLeft);
    }
    private String getDaysStringSimpleJava(int days) {
        if (days % 10 == 1 && days % 100 != 11) return "день";
        if (days % 10 >= 2 && days % 10 <= 4 && (days % 100 < 10 || days % 100 >= 20)) return "дня";
        return "дней";
    }

    @DrawableRes private int getIconResForChallengeType(ChallengeType type) {
        if (type == null) return R.drawable.help;
        return switch (type) {
            case TASK_COMPLETION -> R.drawable.check_box;
            case POMODORO_SESSION -> R.drawable.timer;
            case DAILY_STREAK -> R.drawable.local_fire_department;
            case STATISTIC_REACHED -> R.drawable.trending_up;
            default -> R.drawable.help;
        };
    }
    @DrawableRes
    private Integer getIconResForRewardType(RewardType type) {
        if (type == null) return null;
        return switch (type) {
            case COINS -> R.drawable.paid;
            case EXPERIENCE -> R.drawable.star;
            case BADGE -> R.drawable.badge;
            case PLANT -> R.drawable.grass;
            case THEME -> R.drawable.palette;
        };
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Отписка от всех LiveData, на которые была подписка с observeForever
        gamificationState.removeObserver(gamificationObserverForDependentLoads);
        // Для selectedPlantMediator и surpriseTaskDetailsMediator отписка от источников произойдет автоматически,
        // если их источники (selectedPlantIdFlowInternal, allPlantsFlowInternal, activeSurpriseTaskFlowInternal, hiddenIdsFlowInternal)
        // корректно управляются Lifecycle'ом (например, если они сами LiveData или Flow, преобразованные в LiveData с Lifecycle).
        // Но если они были созданы через gamificationDataStoreManager.getPreferenceLiveData(...).observeForever(...),
        // то нужно отписаться от них явно.
        // Поскольку мы использовали Transformations.switchMap и Transformations.map для LiveData из DataStore,
        // они должны управляться Lifecycle'ом нормально.
        logger.debug(TAG, "GamificationViewModel cleared.");
    }

    /**
     * @param first Оставляем final для неизменяемости после создания
     */ // Вспомогательный класс Pair, если он не доступен глобально
        public record Pair<F, S>(@Getter F first, @Getter S second) {

    }
}