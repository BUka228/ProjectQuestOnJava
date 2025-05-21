package com.example.projectquestonjava.feature.gamification.presentation.viewmodels;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.projectquestonjava.R;
import com.example.projectquestonjava.core.di.IODispatcher;
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
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import dagger.hilt.android.lifecycle.HiltViewModel;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    private final GamificationDataStoreManager gamificationDataStoreManager;
    private final DateTimeUtils dateTimeUtils;
    private final Executor ioExecutor;
    private final Logger logger;

    private final MutableLiveData<Integer> _selectedTab = new MutableLiveData<>(0);
    public LiveData<Integer> selectedTab = _selectedTab;

    // LiveData для основных данных
    public final LiveData<Gamification> gamificationState;
    public final LiveData<VirtualGarden> selectedPlantState;
    public final LiveData<PlantHealthState> plantHealthState;
    public final LiveData<Boolean> canWaterToday;
    public final LiveData<Pair<SurpriseTask, Reward>> surpriseTaskDetailsState; // Reward нужен для отображения имени
    private final MutableLiveData<DailyRewardsInfo> _dailyRewardsInfo = new MutableLiveData<>(null);
    public final LiveData<DailyRewardsInfo> dailyRewardsInfo = _dailyRewardsInfo;
    public final LiveData<ActiveChallengesSectionState> challengesSectionState;
    public final LiveData<ChallengeCardInfo> challengeToShowDetails = new MutableLiveData<>(null); // Для BottomSheet

    // LiveData для значков (для вкладки "Значки")
    public final LiveData<List<Badge>> allBadgesState;
    public final LiveData<List<GamificationBadgeCrossRef>> earnedBadgesState;

    // Для UI событий
    private final SingleLiveEvent<String> _errorMessageEvent = new SingleLiveEvent<>();
    public LiveData<String> errorMessageEvent = _errorMessageEvent;
    private final SingleLiveEvent<String> _successMessageEvent = new SingleLiveEvent<>();
    public LiveData<String> successMessageEvent = _successMessageEvent;
    private final MutableLiveData<Boolean> _isLoadingLiveData = new MutableLiveData<>(false); // Для общих операций
    public LiveData<Boolean> isLoadingLiveData = _isLoadingLiveData;


    @Inject
    public GamificationViewModel(
            GamificationRepository gamificationRepository,
            VirtualGardenRepository virtualGardenRepository,
            ChallengeRepository challengeRepository,
            SurpriseTaskRepository surpriseTaskRepository,
            BadgeRepository badgeRepository,
            RewardRepository rewardRepository, // Нужен для surpriseTaskDetailsState
            GetDailyRewardsUseCase getDailyRewardsUseCase,
            ClaimDailyRewardUseCase claimDailyRewardUseCase,
            AcceptSurpriseTaskUseCase acceptSurpriseTaskUseCase,
            SelectNewSurpriseTaskUseCase selectNewSurpriseTaskUseCase,
            ManuallyWaterPlantUseCase manuallyWaterPlantUseCase,
            GamificationDataStoreManager gamificationDataStoreManager,
            DateTimeUtils dateTimeUtils,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        // this.rewardRepository = rewardRepository;
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

        // 1. Gamification Profile
        gamificationState = Transformations.distinctUntilChanged(gamificationRepository.getCurrentUserGamificationFlow());

        // 2. Selected Plant & Health & CanWater
        // selectedPlantState
        LiveData<Long> selectedPlantIdFlow = gamificationDataStoreManager.getSelectedPlantIdFlow();
        LiveData<List<VirtualGarden>> allPlantsFlow = Transformations.switchMap(
                gamificationDataStoreManager.getGamificationIdFlow(),
                gamiId -> (gamiId == null || gamiId == -1L) ?
                        new MutableLiveData<>(Collections.emptyList()) :
                        virtualGardenRepository.getAllPlantsFlow()
        );

        MediatorLiveData<VirtualGarden> plantMediator = new MediatorLiveData<>();
        final Long[] currentSelectedId = {null};
        final List<VirtualGarden>[] currentAllPlants = new List[1];
        plantMediator.addSource(selectedPlantIdFlow, selectedId -> {
            currentSelectedId[0] = selectedId;
            updateSelectedPlant(plantMediator, currentSelectedId[0], currentAllPlants[0]);
        });
        plantMediator.addSource(allPlantsFlow, plants -> {
            currentAllPlants[0] = plants;
            updateSelectedPlant(plantMediator, currentSelectedId[0], currentAllPlants[0]);
        });
        selectedPlantState = Transformations.distinctUntilChanged(plantMediator);


        plantHealthState = Transformations.map(selectedPlantState, this::calculateHealthState);
        canWaterToday = Transformations.map(selectedPlantState, this::calculateCanWaterToday);

        // 3. Surprise Task Details
        LiveData<SurpriseTask> activeSurpriseTaskFlow = Transformations.switchMap(
                gamificationDataStoreManager.getGamificationIdFlow(),
                gamiId -> (gamiId == null || gamiId == -1L) ?
                        new MutableLiveData<>(null) :
                        surpriseTaskRepository.getActiveTaskForDateFlow(LocalDate.now())
        );
        LiveData<Set<Long>> hiddenIdsFlow = gamificationDataStoreManager.getHiddenExpiredTaskIdsFlow();

        MediatorLiveData<SurpriseTask> filteredSurpriseTaskMediator = new MediatorLiveData<>();
        final SurpriseTask[] currentActiveSurpriseTask = {null};
        final Set<Long>[] currentHiddenIds = new Set[1];

        filteredSurpriseTaskMediator.addSource(activeSurpriseTaskFlow, task -> {
            currentActiveSurpriseTask[0] = task;
            updateFilteredSurpriseTask(filteredSurpriseTaskMediator, currentActiveSurpriseTask[0], currentHiddenIds[0]);
        });
        filteredSurpriseTaskMediator.addSource(hiddenIdsFlow, hiddenIds -> {
            currentHiddenIds[0] = hiddenIds;
            updateFilteredSurpriseTask(filteredSurpriseTaskMediator, currentActiveSurpriseTask[0], currentHiddenIds[0]);
        });

        surpriseTaskDetailsState = Transformations.switchMap(filteredSurpriseTaskMediator, filteredTask -> {
            if (filteredTask == null) {
                return new MutableLiveData<>(null);
            }
            // Асинхронно загружаем Reward
            ListenableFuture<Reward> rewardFuture = rewardRepository.getRewardById(filteredTask.getRewardId());
            MutableLiveData<Pair<SurpriseTask, Reward>> resultLiveData = new MutableLiveData<>();
            Futures.addCallback(rewardFuture, new FutureCallback<Reward>() {
                @Override
                public void onSuccess(Reward reward) {
                    resultLiveData.postValue(new Pair<>(filteredTask, reward));
                }
                @Override
                public void onFailure(@NonNull Throwable t) {
                    logger.error(TAG, "Error loading reward for surprise task " + filteredTask.getId(), t);
                    resultLiveData.postValue(new Pair<>(filteredTask, null)); // Отправляем задачу без награды
                }
            }, ioExecutor);
            return resultLiveData;
        });


        // 5. Challenges Section State (через репозиторий и маппинг)
        challengesSectionState = Transformations.map(
                challengeRepository.getChallengesWithDetailsFlow(null), // null для всех статусов, фильтрация в calculate
                this::calculateChallengesSectionState
        );

        // 6. Badges (для вкладки "Значки")
        allBadgesState = Transformations.distinctUntilChanged(badgeRepository.getAllBadgesFlow());
        earnedBadgesState = Transformations.switchMap(
                gamificationDataStoreManager.getGamificationIdFlow(),
                gamiId -> (gamiId == null || gamiId == -1L) ?
                        new MutableLiveData<>(Collections.emptyList()) :
                        badgeRepository.getEarnedBadgesFlow()
        );

        // Начальная загрузка данных, которые не являются Flow
        loadNonFlowData();
    }

    private void updateSelectedPlant(MediatorLiveData<VirtualGarden> mediator, Long selectedId, List<VirtualGarden> allPlants) {
        if (allPlants == null) {
            mediator.setValue(null);
            return;
        }
        if (allPlants.isEmpty()) {
            mediator.setValue(null);
            return;
        }
        if (selectedId != null && selectedId != -1L) {
            VirtualGarden found = allPlants.stream().filter(p -> p.getId() == selectedId).findFirst().orElse(null);
            if (found != null) {
                mediator.setValue(found);
                return;
            }
        }
        // Если не выбран или выбранный не найден, берем последний
        mediator.setValue(allPlants.stream().max(Comparator.comparingLong(VirtualGarden::getId)).orElse(null));
    }

    private void updateFilteredSurpriseTask(MediatorLiveData<SurpriseTask> mediator, SurpriseTask task, Set<Long> hiddenIds) {
        if (task == null) {
            mediator.setValue(null);
            return;
        }
        boolean isExpired = LocalDateTime.now().isAfter(task.getExpirationTime());
        boolean isHidden = hiddenIds != null && hiddenIds.contains(task.getId());

        if (isHidden && isExpired && !task.isCompleted()) {
            logger.debug(TAG, "Task " + task.getId() + " is expired and hidden, filtering out.");
            mediator.setValue(null);
        } else {
            mediator.setValue(task);
        }
    }


    private void loadNonFlowData() {
        _isLoadingLiveData.setValue(true);
        ListenableFuture<DailyRewardsInfo> dailyRewardsFuture = getDailyRewardsUseCase.execute();
        Futures.addCallback(dailyRewardsFuture, new FutureCallback<DailyRewardsInfo>() {
            @Override
            public void onSuccess(DailyRewardsInfo info) {
                _dailyRewardsInfo.postValue(info);
                // Проверяем, нужно ли выбрать новую задачу-сюрприз только после загрузки DailyRewards
                checkAndSelectSurpriseTaskIfNeeded();
                _isLoadingLiveData.postValue(false);
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Failed to load daily rewards info", t);
                _errorMessageEvent.postValue("Ошибка загрузки ежедневных наград.");
                _dailyRewardsInfo.postValue(null); // Или дефолтное состояние
                checkAndSelectSurpriseTaskIfNeeded(); // Все равно проверяем сюрприз
                _isLoadingLiveData.postValue(false);
            }
        }, ioExecutor);
    }

    // --- Методы Действий ---
    public void selectTab(int tabIndex) {
        logger.debug(TAG, "Tab selected: " + tabIndex);
        _selectedTab.setValue(tabIndex);
    }

    public void waterPlant() {
        Boolean canWater = canWaterToday.getValue();
        Boolean isLoading = _isLoadingLiveData.getValue();
        if (canWater == null || !canWater) {
            logger.warn(TAG, "Attempted to water plant when not allowed.");
            _errorMessageEvent.setValue("Вы уже поливали растения сегодня.");
            return;
        }
        if (isLoading != null && isLoading) {
            logger.warn(TAG, "Attempted to water plant during another operation.");
            return;
        }
        logger.debug(TAG, "Initiating manual plant watering...");
        _isLoadingLiveData.setValue(true);
        _errorMessageEvent.setValue(null);
        _successMessageEvent.setValue(null);

        ListenableFuture<Void> waterFuture = manuallyWaterPlantUseCase.execute();
        Futures.addCallback(waterFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                logger.info(TAG, "Manual watering successful via UseCase.");
                _successMessageEvent.postValue("Растения политы!");
                _isLoadingLiveData.postValue(false);
                // Обновление healthState и canWaterToday произойдет через их LiveData,
                // так как manuallyWaterPlantUseCase обновит lastWatered в БД,
                // что вызовет обновление selectedPlantState.
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Manual watering failed via UseCase", t);
                _errorMessageEvent.postValue("Не удалось полить: " + t.getMessage());
                _isLoadingLiveData.postValue(false);
            }
        }, ioExecutor);
    }

    public void claimTodayReward() {
        logger.debug(TAG, "Attempting to claim today's reward.");
        _isLoadingLiveData.setValue(true);
        ListenableFuture<Void> claimFuture = claimDailyRewardUseCase.execute();
        Futures.addCallback(claimFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                logger.info(TAG, "Daily reward claimed successfully via UseCase.");
                _successMessageEvent.postValue("Награда получена!");
                loadNonFlowData(); // Перезагружаем DailyRewardsInfo
                _isLoadingLiveData.postValue(false);
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Failed to claim daily reward via UseCase", t);
                _errorMessageEvent.postValue("Не удалось получить награду: " + t.getMessage());
                _isLoadingLiveData.postValue(false);
            }
        }, ioExecutor);
    }

    public void acceptSurpriseTask(SurpriseTask task) {
        logger.debug(TAG, "Attempting to accept surprise task: " + task.getId());
        _isLoadingLiveData.setValue(true);
        ListenableFuture<Void> acceptFuture = acceptSurpriseTaskUseCase.execute(task);
        Futures.addCallback(acceptFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                logger.info(TAG, "Surprise task " + task.getId() + " accepted successfully via UseCase.");
                _successMessageEvent.postValue("Награда за сюрприз получена!");
                // Перезагрузка не нужна, т.к. activeSurpriseTaskFlow должен обновиться сам
                _isLoadingLiveData.postValue(false);
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Failed to accept surprise task " + task.getId() + " via UseCase", t);
                _errorMessageEvent.postValue("Не удалось получить награду: " + t.getMessage());
                _isLoadingLiveData.postValue(false);
            }
        }, ioExecutor);
    }

    public void hideExpiredSurpriseTask(long taskId) {
        logger.debug(TAG, "Requesting to hide expired task ID: " + taskId);
        // gamificationDataStoreManager.hideExpiredTaskId возвращает ListenableFuture<Void>
        ListenableFuture<Void> hideFuture = gamificationDataStoreManager.hideExpiredTaskId(taskId);
        Futures.addCallback(hideFuture, new FutureCallback<Void>() {
            @Override public void onSuccess(Void result) {}
            @Override public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Failed to hide task ID " + taskId + " in DataStore", t);
                _errorMessageEvent.postValue("Не удалось скрыть задачу.");
            }
        }, ioExecutor);
    }

    private void checkAndSelectSurpriseTaskIfNeeded() {
        ioExecutor.execute(() -> { // Используем ioExecutor
            logger.debug(TAG, "Checking if a new surprise task needs to be selected for today...");
            ListenableFuture<SurpriseTask> selectFuture = selectNewSurpriseTaskUseCase.execute(LocalDate.now());
            Futures.addCallback(selectFuture, new FutureCallback<SurpriseTask>() {
                @Override
                public void onSuccess(SurpriseTask selectedTask) {
                    if (selectedTask != null) {
                        logger.info(TAG, "SelectNewSurpriseTaskUseCase returned task " + selectedTask.getId() + ".");
                    } else {
                        logger.debug(TAG, "SelectNewSurpriseTaskUseCase returned null.");
                    }
                }
                @Override
                public void onFailure(@NonNull Throwable t) {
                    logger.error(TAG, "Error during selectNewSurpriseTaskUseCase execution", t);
                }
            }, ioExecutor);
        });
    }

    public void showChallengeDetails(ChallengeCardInfo challengeInfo) {
        logger.debug(TAG, "Showing details for challenge: " + challengeInfo.name());
        ((MutableLiveData<ChallengeCardInfo>)challengeToShowDetails).postValue(challengeInfo);
    }
    public void clearChallengeDetails() {
        logger.debug(TAG, "Clearing challenge details view.");
        ((MutableLiveData<ChallengeCardInfo>)challengeToShowDetails).postValue(null);
    }


    // --- Вспомогательные функции ---
    private PlantHealthState calculateHealthState(VirtualGarden plant) {
        if (plant == null) return PlantHealthState.HEALTHY;
        LocalDate lastWateredDate = dateTimeUtils.utcToLocalLocalDateTime(plant.getLastWatered()).toLocalDate();
        LocalDate today = LocalDate.now();
        long daysSinceWatered = ChronoUnit.DAYS.between(lastWateredDate, today);
        if (daysSinceWatered <= 1) return PlantHealthState.HEALTHY;
        if (daysSinceWatered == 2L) return PlantHealthState.NEEDSWATER;
        return PlantHealthState.WILTED;
    }

    private boolean calculateCanWaterToday(VirtualGarden plant) {
        if (plant == null) return false;
        LocalDate lastWateredDate = dateTimeUtils.utcToLocalLocalDateTime(plant.getLastWatered()).toLocalDate();
        return lastWateredDate.isBefore(LocalDate.now());
    }

    private ActiveChallengesSectionState calculateChallengesSectionState(List<ChallengeProgressFullDetails> allDetailsList) {
        if (allDetailsList == null || allDetailsList.isEmpty()) {
            return new ActiveChallengesSectionState(0, Collections.emptyList(), 0,0,0,0,0,0);
        }

        int dailyCompleted = 0; int dailyTotal = 0;
        int weeklyCompleted = 0; int weeklyTotal = 0;
        int urgentCount = 0; int nearCompletionCount = 0;
        LocalDateTime now = dateTimeUtils.currentLocalDateTime();
        LocalDate today = now.toLocalDate();
        LocalDate tomorrow = today.plusDays(1);
        int currentWeek = now.get(WeekFields.ISO.weekOfWeekBasedYear()); // ISO стандарт недели
        int currentYear = now.getYear();

        List<ChallengeCardInfo> challengeInfosToShow = new ArrayList<>();

        Map<Long, List<ChallengeProgressFullDetails>> groupedByChallengeId = allDetailsList.stream()
                .collect(Collectors.groupingBy(details -> details.getChallengeAndReward().getChallenge().getId()));

        for (Map.Entry<Long, List<ChallengeProgressFullDetails>> entry : groupedByChallengeId.entrySet()) {
            List<ChallengeProgressFullDetails> detailsForChallenge = entry.getValue();
            if (detailsForChallenge.isEmpty()) continue;

            Challenge challenge = detailsForChallenge.get(0).getChallengeAndReward().getChallenge();
            Reward reward = detailsForChallenge.get(0).getChallengeAndReward().getReward();

            float totalProgressValue = 0;
            float totalTargetValue = 0;
            for(ChallengeProgressFullDetails detail : detailsForChallenge){
                totalProgressValue += detail.getProgress().getProgress();
                totalTargetValue += detail.getRule().getTarget();
            }
            float overallProgress = (totalTargetValue > 0) ? (totalProgressValue / totalTargetValue) : 0f;
            overallProgress = Math.max(0f, Math.min(1f, overallProgress)); // Ограничиваем 0..1
            boolean isChallengeCompletedByProgress = overallProgress >= 1.0f;


            boolean actualIsCompleted = challenge.getStatus() == ChallengeStatus.COMPLETED;
            boolean isActive = challenge.getStatus() == ChallengeStatus.ACTIVE;

            LocalDateTime lastUpdateTime = detailsForChallenge.stream()
                    .map(d -> d.getProgress().getLastUpdated())
                    .max(LocalDateTime::compareTo)
                    .orElse(challenge.getStartDate());
            LocalDate lastUpdateDate = lastUpdateTime.toLocalDate();
            int lastUpdateWeek = lastUpdateTime.get(WeekFields.ISO.weekOfWeekBasedYear());
            int lastUpdateYear = lastUpdateTime.getYear();

            boolean shouldShowCard = false;

            if (challenge.getPeriod() == ChallengePeriod.DAILY) {
                dailyTotal++;
                if (actualIsCompleted && lastUpdateDate.isEqual(today)) {
                    dailyCompleted++; shouldShowCard = true;
                } else if (isActive) {
                    shouldShowCard = true;
                }
            } else if (challenge.getPeriod() == ChallengePeriod.WEEKLY) {
                weeklyTotal++;
                if (actualIsCompleted && lastUpdateYear == currentYear && lastUpdateWeek == currentWeek) {
                    weeklyCompleted++; shouldShowCard = true;
                } else if (isActive) {
                    shouldShowCard = true;
                }
            } else if (isActive) { // ONCE, EVENT, MONTHLY - показываем если активны
                shouldShowCard = true;
            }

            if (isActive) {
                LocalDate deadlineDate = challenge.getEndDate().toLocalDate();
                boolean isCurrentlyUrgent = deadlineDate.isEqual(today) || deadlineDate.isEqual(tomorrow);
                if (isCurrentlyUrgent) urgentCount++;
                if (overallProgress >= 0.8f && overallProgress < 1.0f) nearCompletionCount++;
                shouldShowCard = true; // Активные всегда показываем в пейджере
            }


            if (shouldShowCard) {
                boolean isUrgentForCard = isActive && (challenge.getEndDate().toLocalDate().isEqual(today) || challenge.getEndDate().toLocalDate().isEqual(tomorrow));
                String progressText = formatProgressText(detailsForChallenge);
                challengeInfosToShow.add(new ChallengeCardInfo(
                        challenge.getId(),
                        getIconForChallengeType(detailsForChallenge.get(0).getRule().getType()),
                        challenge.getName(),
                        challenge.getDescription(),
                        overallProgress,
                        progressText,
                        actualIsCompleted ? null : formatDeadlineText(challenge.getEndDate(), isUrgentForCard),
                        reward != null ? getIconForRewardType(reward.getRewardType()) : null,
                        reward != null ? reward.getName() : null,
                        isUrgentForCard,
                        challenge.getPeriod()
                ));
            }
        }

        challengeInfosToShow.sort(
                Comparator.comparing((ChallengeCardInfo c) -> !c.isUrgent()) // Сначала срочные (false придет первым)
                        .thenComparing(c -> !(c.progress() < 1.0f && c.progress() >= 0.8f)) // Затем близкие к завершению
                        .thenComparing(c -> !(c.progress() < 1.0f)) // Затем остальные активные
                        .thenComparing(Comparator.comparingDouble(ChallengeCardInfo::progress).reversed()) // Среди активных - по убыванию прогресса
        );


        return new ActiveChallengesSectionState(
                (int)allDetailsList.stream().filter(d -> d.getChallengeAndReward().getChallenge().getStatus() == ChallengeStatus.ACTIVE).count(),
                challengeInfosToShow,
                dailyCompleted, dailyTotal, weeklyCompleted, weeklyTotal,
                urgentCount, nearCompletionCount
        );
    }


    private String formatProgressText(List<ChallengeProgressFullDetails> details) {
        if (details.isEmpty()) return "?/?";
        // Если правил несколько, отображение прогресса может быть сложным.
        // Пока возьмем сумму прогресса и сумму таргетов по всем правилам.
        int currentSum = details.stream().mapToInt(d -> d.getProgress().getProgress()).sum();
        int targetSum = details.stream().mapToInt(d -> d.getRule().getTarget()).sum();
        ChallengeType firstType = details.get(0).getRule().getType();

        String unit = switch (firstType) {
            case TASK_COMPLETION -> " задач";
            case POMODORO_SESSION -> " Pomodoro";
            case DAILY_STREAK -> " дн. стрика";
            default -> "";
            // Добавить другие типы
        };
        return currentSum + "/" + targetSum + unit;
    }

    private String formatDeadlineText(LocalDateTime deadline, boolean isUrgent) {
        LocalDateTime now = dateTimeUtils.currentLocalDateTime(); // Используем dateTimeUtils
        long daysLeft = ChronoUnit.DAYS.between(now.toLocalDate(), deadline.toLocalDate());

        if (daysLeft < 0) return "Просрочено"; // Если уже прошло
        if (daysLeft == 0) return "Сегодня до " + deadline.format(DateTimeFormatter.ofPattern("HH:mm"));
        if (daysLeft == 1) return "Завтра до " + deadline.format(DateTimeFormatter.ofPattern("HH:mm"));
        if (isUrgent || daysLeft < 7) return "Ост. " + daysLeft + " " + getDaysStringSimple((int)daysLeft);
        return deadline.format(DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("ru")));
    }

    private String getDaysStringSimple(int days) {
        if (days % 10 == 1 && days % 100 != 11) return "день";
        if (days % 10 >= 2 && days % 10 <= 4 && (days % 100 < 10 || days % 100 >= 20)) return "дня";
        return "дней";
    }

    @DrawableRes
    private int getIconForChallengeType(ChallengeType type) {
        if (type == null) return R.drawable.help;
        return switch (type) {
            case TASK_COMPLETION -> R.drawable.check_box;
            case POMODORO_SESSION -> R.drawable.timer;
            case DAILY_STREAK -> R.drawable.local_fire_department;
            // Добавить другие типы
            default -> R.drawable.help;
        };
    }

    @DrawableRes
    private Integer getIconForRewardType(RewardType type) { // Возвращаем Integer, т.к. может быть null
        if (type == null) return null;
        return switch (type) {
            case COINS -> R.drawable.paid;
            case EXPERIENCE -> R.drawable.star; // Используем векторную иконку
            case BADGE -> R.drawable.badge;
            case PLANT -> R.drawable.grass;
            case THEME -> R.drawable.palette;
            default -> null;
        };
    }

    // Вспомогательный класс Pair для surpriseTaskDetailsState
    private static class Pair<F, S> {
        final F first;
        final S second;
        Pair(F f, S s) { first = f; second = s; }
    }
}