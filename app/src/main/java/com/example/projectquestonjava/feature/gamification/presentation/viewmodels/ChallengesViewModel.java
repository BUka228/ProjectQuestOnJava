package com.example.projectquestonjava.feature.gamification.presentation.viewmodels;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.projectquestonjava.R; // Для иконок в ChallengeCardInfo
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.data.model.Challenge;
import com.example.projectquestonjava.feature.gamification.data.model.Reward;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengePeriod;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeProgressFullDetails;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeStatus;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeType;
import com.example.projectquestonjava.feature.gamification.domain.model.RewardType;
import com.example.projectquestonjava.feature.gamification.domain.repository.ChallengeRepository;
import com.example.projectquestonjava.feature.gamification.presentation.utils.GamificationUiUtils;
import dagger.hilt.android.lifecycle.HiltViewModel;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;



@HiltViewModel
public class ChallengesViewModel extends ViewModel {

    private static final String TAG = "ChallengesViewModel";

    private final ChallengeRepository challengeRepository;
    private final Executor ioExecutor;
    @Getter
    private final Logger logger;
    private final DateTimeUtils dateTimeUtils; // Добавляем DateTimeUtils

    private final MutableLiveData<ChallengesScreenUiState> _uiStateLiveData =
            new MutableLiveData<>(ChallengesScreenUiState.createDefault());
    public final LiveData<ChallengesScreenUiState> uiStateLiveData = _uiStateLiveData;

    // Триггеры для обновления
    private final MutableLiveData<ChallengePeriod> _selectedPeriodTrigger = new MutableLiveData<>(null); // null = "Все"
    private final MutableLiveData<ChallengeSortOption> _sortOptionTrigger = new MutableLiveData<>(ChallengeSortOption.DEADLINE_ASC);
    private final MutableLiveData<Set<ChallengeFilterOption>> _filterOptionsTrigger =
            new MutableLiveData<>(Collections.singleton(ChallengeFilterOption.ACTIVE));

    // LiveData для всех челленджей (основной источник данных от репозитория)
    private final LiveData<List<ChallengeProgressFullDetails>> allChallengesDetailsLiveData;

    // Mediator для объединения триггеров и данных
    private final MediatorLiveData<Object> loadDataMediator = new MediatorLiveData<>();
    private final ScheduledExecutorService debounceExecutor = new ScheduledThreadPoolExecutor(1);
    private ScheduledFuture<?> scheduledLoadTask;

    private final MutableLiveData<ChallengeCardInfo> _challengeToShowDetails = new MutableLiveData<>(null);
    public LiveData<ChallengeCardInfo> getChallengeToShowDetails() { // Геттер для LiveData
        return _challengeToShowDetails;
    }


    @Inject
    public ChallengesViewModel(
            ChallengeRepository challengeRepository,
            @IODispatcher Executor ioExecutor,
            DateTimeUtils dateTimeUtils, // Внедряем
            Logger logger) {
        this.challengeRepository = challengeRepository;
        this.ioExecutor = ioExecutor;
        this.dateTimeUtils = dateTimeUtils; // Сохраняем
        this.logger = logger;

        logger.debug(TAG, "ChallengesViewModel initialized.");

        // Получаем LiveData из репозитория
        // Передаем null, чтобы получить все челленджи, фильтрация будет на стороне ViewModel
        allChallengesDetailsLiveData = challengeRepository.getChallengesWithDetailsFlow(null);

        // Настраиваем MediatorLiveData для отслеживания изменений
        loadDataMediator.addSource(allChallengesDetailsLiveData, details -> scheduleDataProcessing());
        loadDataMediator.addSource(_selectedPeriodTrigger, period -> scheduleDataProcessing());
        loadDataMediator.addSource(_sortOptionTrigger, sort -> scheduleDataProcessing());
        loadDataMediator.addSource(_filterOptionsTrigger, filters -> scheduleDataProcessing());

        // Наблюдатель для активации MediatorLiveData
        loadDataMediator.observeForever(ignored -> {});

        // Первоначальная загрузка (или обновление, если данные уже есть)
        scheduleDataProcessing();
    }

    private void scheduleDataProcessing() {
        if (scheduledLoadTask != null && !scheduledLoadTask.isDone()) {
            scheduledLoadTask.cancel(false);
        }
        // Устанавливаем isLoading в true перед запуском задачи
        _uiStateLiveData.postValue(
                Objects.requireNonNull(_uiStateLiveData.getValue()).toBuilder()
                        .isLoading(true)
                        .error(null) // Сбрасываем ошибку
                        .build()
        );

        scheduledLoadTask = debounceExecutor.schedule(() -> {
            List<ChallengeProgressFullDetails> currentDetails = allChallengesDetailsLiveData.getValue();
            ChallengePeriod currentPeriod = _selectedPeriodTrigger.getValue();
            ChallengeSortOption currentSort = _sortOptionTrigger.getValue();
            Set<ChallengeFilterOption> currentFilters = _filterOptionsTrigger.getValue();

            if (currentDetails == null) { // Данные еще не загружены из репозитория
                logger.debug(TAG, "All challenge details are still null, waiting for repository.");
                // isLoading уже true, просто выходим
                return;
            }
            if (currentSort == null || currentFilters == null) { // Настройки еще не пришли (маловероятно)
                logger.warn(TAG, "Sort or Filter options are null, cannot process.");
                _uiStateLiveData.postValue(
                        Objects.requireNonNull(_uiStateLiveData.getValue()).toBuilder()
                                .isLoading(false)
                                .error("Ошибка настроек отображения")
                                .build()
                );
                return;
            }

            ioExecutor.execute(() -> { // Выполняем фильтрацию и сортировку в фоновом потоке
                try {
                    List<ChallengeProgressFullDetails> filtered = filterChallenges(currentDetails, currentPeriod, currentFilters);
                    List<ChallengeProgressFullDetails> sorted = sortChallenges(filtered, currentSort);

                    _uiStateLiveData.postValue(new ChallengesScreenUiState(
                            false, // isLoading = false
                            null,  // error = null
                            currentPeriod,
                            currentSort,
                            currentFilters,
                            sorted
                    ));
                    logger.debug(TAG, "Processed and posted UI state. Filtered: " + filtered.size() + ", Sorted: " + sorted.size());
                } catch (Exception e) {
                    logger.error(TAG, "Error processing challenges data", e);
                    _uiStateLiveData.postValue(
                            Objects.requireNonNull(_uiStateLiveData.getValue()).toBuilder()
                                    .isLoading(false)
                                    .error("Ошибка обработки испытаний: " + e.getMessage())
                                    .build()
                    );
                }
            });
        }, 150, TimeUnit.MILLISECONDS); // Небольшой debounce
    }



    private List<ChallengeProgressFullDetails> filterChallenges(
            @NonNull List<ChallengeProgressFullDetails> challenges,
            @Nullable ChallengePeriod period,
            @NonNull Set<ChallengeFilterOption> filters
    ) {
        Stream<ChallengeProgressFullDetails> stream = challenges.stream();

        if (period != null) {
            stream = stream.filter(details -> details.getChallengeAndReward().getChallenge().getPeriod() == period);
        }

        if (!filters.contains(ChallengeFilterOption.ALL)) {
            LocalDateTime now = dateTimeUtils.currentLocalDateTime(); // Используем dateTimeUtils
            LocalDate today = now.toLocalDate();
            LocalDate tomorrow = today.plusDays(1);

            stream = stream.filter(details -> {
                Challenge challenge = details.getChallengeAndReward().getChallenge();
                Reward reward = details.getChallengeAndReward().getReward();
                boolean isCompleted = challenge.getStatus() == ChallengeStatus.COMPLETED;
                boolean isExpired = challenge.getStatus() == ChallengeStatus.EXPIRED;
                boolean isActive = challenge.getStatus() == ChallengeStatus.ACTIVE;
                boolean isUrgent = isActive && (challenge.getEndDate().toLocalDate().isEqual(today) ||
                        challenge.getEndDate().toLocalDate().isEqual(tomorrow));

                for (ChallengeFilterOption filter : filters) {
                    boolean match = false;
                    switch (filter) {
                        case ACTIVE: if (isActive) match = true; break;
                        case COMPLETED: if (isCompleted) match = true; break;
                        case EXPIRED: if (isExpired) match = true; break;
                        case MISSED: if (isExpired && !isCompleted) match = true; break;
                        case URGENT: if (isUrgent) match = true; break;
                        case HIGH_REWARD: if (isHighReward(reward.getRewardType(), reward.getRewardValue())) match = true; break;
                        case HAS_BADGE_REWARD: if (reward.getRewardType() == RewardType.BADGE) match = true; break;
                        case HAS_COIN_REWARD: if (reward.getRewardType() == RewardType.COINS) match = true; break;
                        case HAS_XP_REWARD: if (reward.getRewardType() == RewardType.EXPERIENCE) match = true; break;
                        // ALL не должен быть здесь, если он не единственный фильтр
                    }
                    if (!match) return false;
                }
                return true;
            });
        }
        return stream.collect(Collectors.toList());
    }

    private boolean isHighReward(RewardType type, String valueStr) {
        if (type == null || valueStr == null) return false;
        try {
            return switch (type) {
                case COINS, EXPERIENCE -> Integer.parseInt(valueStr) >= 50; // Примерный порог
                case BADGE, PLANT, THEME -> true; // Всегда считаем ценными
            };
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private List<ChallengeProgressFullDetails> sortChallenges(
            List<ChallengeProgressFullDetails> challenges,
            ChallengeSortOption sortOption
    ) {
        List<ChallengeProgressFullDetails> sortedList = new ArrayList<>(challenges);
        Comparator<ChallengeProgressFullDetails> comparator = null;
        switch (sortOption) {
            case DEADLINE_ASC: comparator = Comparator.comparing(d -> d.getChallengeAndReward().getChallenge().getEndDate()); break;
            case DEADLINE_DESC: comparator = Comparator.comparing((ChallengeProgressFullDetails d) -> d.getChallengeAndReward().getChallenge().getEndDate()).reversed(); break;
            case PROGRESS_DESC:
                comparator = Comparator.comparingDouble((ChallengeProgressFullDetails d) -> {
                    float progressVal = d.getProgress().getProgress();
                    float targetVal = d.getRule().getTarget();
                    return targetVal > 0 ? (double) progressVal / targetVal : 0.0;
                }).reversed();
                break;
            case PROGRESS_ASC:
                comparator = Comparator.comparingDouble(d -> {
                    float progressVal = d.getProgress().getProgress();
                    float targetVal = d.getRule().getTarget();
                    return targetVal > 0 ? (double) progressVal / targetVal : 0.0;
                });
                break;
            case REWARD_VALUE_DESC: comparator = Comparator.comparingInt((ChallengeProgressFullDetails d) -> calculateRewardSortValue(d.getChallengeAndReward().getReward().getRewardType(), d.getChallengeAndReward().getReward().getRewardValue())).reversed(); break;
            case REWARD_VALUE_ASC: comparator = Comparator.comparingInt(d -> calculateRewardSortValue(d.getChallengeAndReward().getReward().getRewardType(), d.getChallengeAndReward().getReward().getRewardValue())); break;
            case NAME_ASC: comparator = Comparator.comparing(d -> d.getChallengeAndReward().getChallenge().getName()); break;
            case NAME_DESC: comparator = Comparator.comparing((ChallengeProgressFullDetails d) -> d.getChallengeAndReward().getChallenge().getName()).reversed(); break;
        }
        if (comparator != null) {
            sortedList.sort(comparator);
        }
        return sortedList;
    }

    private int calculateRewardSortValue(RewardType type, String valueStr) {
        if (type == null || valueStr == null) return 0;
        try {
            return switch (type) {
                case COINS, EXPERIENCE -> Integer.parseInt(valueStr);
                case BADGE -> 1000;
                case PLANT -> 1500;
                case THEME -> 2000;
            };
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // --- Методы для UI ---
    public void selectPeriod(@Nullable ChallengePeriod period) {
        logger.debug(TAG, "Period selected via UI: " + period);
        _selectedPeriodTrigger.setValue(period);
    }

    public void updateSortOption(ChallengeSortOption option) {
        logger.debug(TAG, "Sort option selected: " + option);
        _sortOptionTrigger.setValue(option);
    }

    public void toggleFilterOption(ChallengeFilterOption option) {
        Set<ChallengeFilterOption> currentFilters = new HashSet<>(Objects.requireNonNull(_filterOptionsTrigger.getValue()));
        Set<ChallengeFilterOption> newFilters;
        if (option == ChallengeFilterOption.ALL) {
            newFilters = Collections.singleton(ChallengeFilterOption.ALL);
        } else if (currentFilters.contains(ChallengeFilterOption.ALL)) {
            newFilters = Collections.singleton(option);
        } else if (currentFilters.contains(option)) {
            currentFilters.remove(option);
            newFilters = currentFilters.isEmpty() ? Collections.singleton(ChallengeFilterOption.ACTIVE) : currentFilters;
        } else {
            currentFilters.remove(ChallengeFilterOption.ALL); // Убираем ALL, если он был
            currentFilters.add(option);
            newFilters = currentFilters;
        }
        logger.debug(TAG, "Filter options updated: " + newFilters);
        _filterOptionsTrigger.setValue(newFilters);
    }

    public void resetFiltersAndSort() {
        logger.debug(TAG, "Resetting filters and sort");
        _sortOptionTrigger.setValue(ChallengeSortOption.DEADLINE_ASC);
        _filterOptionsTrigger.setValue(Collections.singleton(ChallengeFilterOption.ACTIVE));
        // _selectedPeriodTrigger можно тоже сбросить на null (Все), если нужно
        // _selectedPeriodTrigger.setValue(null);
    }

    public void retryLoad() {
        logger.debug(TAG, "Retry load triggered.");
        triggerLoadWithDebounce(); // Просто перезапускаем загрузку с текущими параметрами
    }

    private void triggerLoadWithDebounce() {
        // Отменяем предыдущую запланированную задачу, если она есть
        if (scheduledLoadTask != null && !scheduledLoadTask.isDone()) {
            scheduledLoadTask.cancel(false); // false - не прерывать, если уже выполняется
        }

        // Планируем новую задачу с задержкой
        scheduledLoadTask = debounceExecutor.schedule(() -> {
            // Этот код будет выполнен после задержки debounce
            // Получаем актуальные значения триггеров
            ChallengePeriod currentPeriod = _selectedPeriodTrigger.getValue();
            ChallengeSortOption currentSort = _sortOptionTrigger.getValue();
            Set<ChallengeFilterOption> currentFilters = _filterOptionsTrigger.getValue();
            List<ChallengeProgressFullDetails> allDetails = allChallengesDetailsLiveData.getValue(); // Получаем текущий список

            // Проверяем, что все необходимые данные для обработки есть
            if (allDetails == null) { // Если основные данные еще не загружены
                logger.debug(TAG, "Debounced: All challenge details are still null, likely waiting for initial repository load. Setting UI to loading.");
                // Устанавливаем isLoading, если еще не установлен, и выходим
                // Это предотвратит NPE при первой загрузке, когда allChallengesDetailsLiveData еще не имеет значения
                _uiStateLiveData.postValue(
                        Objects.requireNonNull(_uiStateLiveData.getValue()).toBuilder()
                                .isLoading(true)
                                .error(null) // Сбрасываем предыдущую ошибку
                                .build()
                );
                return; // Выходим, если основные данные не готовы
            }

            if (currentSort == null || currentFilters == null) {
                logger.warn(TAG, "Debounced: Sort or Filter options are null, cannot process data.");
                _uiStateLiveData.postValue(
                        Objects.requireNonNull(_uiStateLiveData.getValue()).toBuilder()
                                .isLoading(false) // Загрузка была, но настройки невалидны
                                .error("Ошибка настроек фильтрации/сортировки")
                                .build()
                );
                return;
            }

            logger.debug(TAG, "Debounced: Processing data with Period=" + currentPeriod + ", Sort=" + currentSort + ", Filters=" + currentFilters.size());

            // Устанавливаем isLoading в true перед началом фактической обработки
            // (если еще не установлен)
            if (_uiStateLiveData.getValue() != null && !_uiStateLiveData.getValue().isLoading()) {
                _uiStateLiveData.postValue(
                        _uiStateLiveData.getValue().toBuilder()
                                .isLoading(true)
                                .error(null)
                                .build()
                );
            }


            // Выполняем ресурсоемкую фильтрацию и сортировку на ioExecutor
            ioExecutor.execute(() -> {
                try {
                    List<ChallengeProgressFullDetails> filtered = filterChallenges(allDetails, currentPeriod, currentFilters);
                    List<ChallengeProgressFullDetails> sorted = sortChallenges(filtered, currentSort);

                    // Обновляем UI с отфильтрованными и отсортированными данными
                    _uiStateLiveData.postValue(new ChallengesScreenUiState(
                            false, // isLoading = false
                            null,  // error = null
                            currentPeriod,
                            currentSort,
                            currentFilters,
                            sorted
                    ));
                    logger.debug(TAG, "Debounced: UI state updated. Filtered: " + filtered.size() + ", Sorted: " + sorted.size());
                } catch (Exception e) {
                    logger.error(TAG, "Debounced: Error processing challenges data in background", e);
                    _uiStateLiveData.postValue(
                            Objects.requireNonNull(_uiStateLiveData.getValue()).toBuilder()
                                    .isLoading(false)
                                    .error("Ошибка обработки испытаний: " + e.getMessage())
                                    .build()
                    );
                }
            });

        }, 150, TimeUnit.MILLISECONDS);
    }

    public void showChallengeDetailsFromFullDetails(ChallengeProgressFullDetails details) {
        Challenge challenge = details.getChallengeAndReward().getChallenge();
        Reward reward = details.getChallengeAndReward().getReward();

        float totalProgressVal = details.getProgress().getProgress();
        float totalTargetVal = details.getRule().getTarget();
        float overallProgress = (totalTargetVal > 0) ? (totalProgressVal / totalTargetVal) : 0f;

        ChallengeCardInfo cardInfo = new ChallengeCardInfo(
                challenge.getId(),
                GamificationUiUtils.getIconResForChallengeType(details.getRule().getType()),
                challenge.getName(),
                challenge.getDescription(),
                overallProgress,
                formatProgressTextJava(Collections.singletonList(details)), // Передаем список для форматирования
                challenge.getStatus() == ChallengeStatus.COMPLETED ? null : formatDeadlineTextJava(challenge.getEndDate(), isChallengeUrgent(challenge)),
                reward != null ? GamificationUiUtils.getIconResForRewardType(reward.getRewardType()) : null,
                reward != null ? reward.getName() : null,
                isChallengeUrgent(challenge),
                challenge.getPeriod()
        );
        _challengeToShowDetails.setValue(cardInfo);
    }

    private boolean isChallengeUrgent(Challenge challenge) {
        if (challenge.getStatus() != ChallengeStatus.ACTIVE) return false;
        LocalDate today = dateTimeUtils.currentLocalDate();
        LocalDate deadlineDate = challenge.getEndDate().toLocalDate();
        return deadlineDate.isEqual(today) || deadlineDate.isEqual(today.plusDays(1));
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
        if (deadline == null) return null;
        LocalDateTime now = dateTimeUtils.currentLocalDateTime();
        long daysLeft = ChronoUnit.DAYS.between(now.toLocalDate(), deadline.toLocalDate());
        daysLeft = Math.max(0, daysLeft);

        if (daysLeft == 0) return "Сегодня до " + deadline.format(DateTimeFormatter.ofPattern("HH:mm"));
        if (daysLeft == 1) return "Завтра до " + deadline.format(DateTimeFormatter.ofPattern("HH:mm"));
        return "Ост. " + daysLeft + " " + GamificationUiUtils.getDaysStringJava((int)daysLeft);
    }

    public void clearChallengeDetails() {
        _challengeToShowDetails.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (debounceExecutor != null && !debounceExecutor.isShutdown()) {
            debounceExecutor.shutdownNow();
        }
        // Отписка от MediatorLiveData не обязательна, если его источники управляются LifecycleOwner'ом
        // Но если источники были observeForever, то от них нужно отписаться.
        // В нашем случае, allChallengesDetailsLiveData - это LiveData от репозитория,
        // а триггеры - MutableLiveData, управляемые этой ViewModel.
        logger.debug(TAG, "ChallengesViewModel cleared.");
    }
}