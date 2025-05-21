package com.example.projectquestonjava.feature.gamification.presentation.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.data.model.Challenge;
import com.example.projectquestonjava.feature.gamification.data.model.Reward;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengePeriod;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeProgressFullDetails;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeStatus;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeType;
import com.example.projectquestonjava.feature.gamification.domain.model.RewardType;
import com.example.projectquestonjava.feature.gamification.domain.repository.ChallengeRepository;

import dagger.hilt.android.lifecycle.HiltViewModel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;


enum ChallengeSortOption { // Если еще не создан
    DEADLINE_ASC, DEADLINE_DESC,
    PROGRESS_DESC, PROGRESS_ASC,
    REWARD_VALUE_DESC, REWARD_VALUE_ASC,
    NAME_ASC, NAME_DESC
}

enum ChallengeFilterOption { // Если еще не создан
    ALL, ACTIVE, COMPLETED, EXPIRED, MISSED, URGENT,
    HIGH_REWARD, HAS_BADGE_REWARD, HAS_COIN_REWARD, HAS_XP_REWARD
}

class ChallengesScreenUiState { // Если еще не создан
    public final boolean isLoading;
    public final String error;
    public final ChallengePeriod selectedPeriod;
    public final ChallengeSortOption sortOption;
    public final Set<ChallengeFilterOption> filterOptions;
    public final List<ChallengeProgressFullDetails> challenges;

    public ChallengesScreenUiState(boolean isLoading, String error, ChallengePeriod selectedPeriod, ChallengeSortOption sortOption, Set<ChallengeFilterOption> filterOptions, List<ChallengeProgressFullDetails> challenges) {
        this.isLoading = isLoading;
        this.error = error;
        this.selectedPeriod = selectedPeriod;
        this.sortOption = sortOption;
        this.filterOptions = filterOptions;
        this.challenges = challenges;
    }

    // Конструктор по умолчанию
    public ChallengesScreenUiState() {
        this.isLoading = true;
        this.error = null;
        this.selectedPeriod = null;
        this.sortOption = ChallengeSortOption.DEADLINE_ASC;
        this.filterOptions = Collections.singleton(ChallengeFilterOption.ACTIVE);
        this.challenges = Collections.emptyList();
    }
}

// Вспомогательный класс для объединения источников триггера
class ChallengesDisplaySettings {
    final ChallengePeriod selectedPeriod;
    final ChallengeSortOption sortOption;
    final Set<ChallengeFilterOption> filterOptions;

    ChallengesDisplaySettings(ChallengePeriod period, ChallengeSortOption sort, Set<ChallengeFilterOption> filters) {
        this.selectedPeriod = period;
        this.sortOption = sort;
        this.filterOptions = filters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChallengesDisplaySettings that = (ChallengesDisplaySettings) o;
        return selectedPeriod == that.selectedPeriod && sortOption == that.sortOption && Objects.equals(filterOptions, that.filterOptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(selectedPeriod, sortOption, filterOptions);
    }
}


@HiltViewModel
public class ChallengesViewModel extends ViewModel {

    private static final String TAG = "ChallengesViewModel";

    private final Executor ioExecutor; // Для фоновых операций фильтрации/сортировки
    private final Logger logger;

    private final MutableLiveData<Boolean> _isLoadingLiveData = new MutableLiveData<>(true);
    private final MutableLiveData<String> _errorLiveData = new MutableLiveData<>(null);
    private final MutableLiveData<ChallengePeriod> _selectedPeriodLiveData = new MutableLiveData<>(null);
    private final MutableLiveData<ChallengeSortOption> _sortOptionLiveData = new MutableLiveData<>(ChallengeSortOption.DEADLINE_ASC);
    private final MutableLiveData<Set<ChallengeFilterOption>> _filterOptionsLiveData = new MutableLiveData<>(Collections.singleton(ChallengeFilterOption.ACTIVE));

    // LiveData для всех деталей челленджей (исходные данные)
    private final LiveData<List<ChallengeProgressFullDetails>> allChallengesDetailsLiveData;

    // LiveData для настроек отображения
    private final MediatorLiveData<ChallengesDisplaySettings> displaySettingsLiveData = new MediatorLiveData<>();

    // Итоговое LiveData для UI
    public final LiveData<ChallengesScreenUiState> uiStateLiveData;

    @Inject
    public ChallengesViewModel(
            ChallengeRepository challengeRepository,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.ioExecutor = ioExecutor;
        this.logger = logger;

        // Загружаем все детали челленджей один раз
        // ChallengeRepository.getChallengesWithDetailsFlow(null) должен возвращать LiveData
        allChallengesDetailsLiveData = Transformations.distinctUntilChanged(challengeRepository.getChallengesWithDetailsFlow(null));


        // Настраиваем displaySettingsLiveData для отслеживания изменений в фильтрах/сортировке/периоде
        displaySettingsLiveData.addSource(_selectedPeriodLiveData, period -> updateDisplaySettings());
        displaySettingsLiveData.addSource(_sortOptionLiveData, sort -> updateDisplaySettings());
        displaySettingsLiveData.addSource(_filterOptionsLiveData, filters -> updateDisplaySettings());
        updateDisplaySettings(); // Инициализация

        // uiStateLiveData зависит от isLoading, error, displaySettings и allChallengesDetails
        MediatorLiveData<ChallengesScreenUiState> tempUiState = new MediatorLiveData<>();
        tempUiState.addSource(_isLoadingLiveData, isLoading -> combineUiState(tempUiState));
        tempUiState.addSource(_errorLiveData, error -> combineUiState(tempUiState));
        tempUiState.addSource(displaySettingsLiveData, settings -> combineUiState(tempUiState));
        tempUiState.addSource(allChallengesDetailsLiveData, details -> {
            // При получении новых данных сбрасываем isLoading
            _isLoadingLiveData.setValue(false);
            combineUiState(tempUiState);
        });
        // Инициализация isLoading
        _isLoadingLiveData.setValue(true); // Начинаем с загрузки
        uiStateLiveData = tempUiState;
    }

    private void updateDisplaySettings() {
        ChallengePeriod period = _selectedPeriodLiveData.getValue();
        ChallengeSortOption sort = _sortOptionLiveData.getValue();
        Set<ChallengeFilterOption> filters = _filterOptionsLiveData.getValue();
        if (sort != null && filters != null) { // period может быть null
            displaySettingsLiveData.setValue(new ChallengesDisplaySettings(period, sort, filters));
        }
    }

    private void combineUiState(MediatorLiveData<ChallengesScreenUiState> mediator) {
        Boolean isLoading = _isLoadingLiveData.getValue();
        String error = _errorLiveData.getValue();
        ChallengesDisplaySettings settings = displaySettingsLiveData.getValue();
        List<ChallengeProgressFullDetails> allChallenges = allChallengesDetailsLiveData.getValue();

        if (isLoading == null || settings == null) { // Ждем инициализации всех источников
            return;
        }

        if (isLoading) { // Если все еще грузим (например, allChallenges еще null)
            mediator.setValue(new ChallengesScreenUiState(true, error, settings.selectedPeriod, settings.sortOption, settings.filterOptions, Collections.emptyList()));
            return;
        }

        if (allChallenges == null) { // Данные не пришли, но загрузка завершена (возможно, с ошибкой)
            if (error == null) { // Если ошибки нет, но данных нет, ставим ошибку
                error = "Не удалось загрузить данные испытаний.";
            }
            mediator.setValue(new ChallengesScreenUiState(false, error, settings.selectedPeriod, settings.sortOption, settings.filterOptions, Collections.emptyList()));
            return;
        }

        // Выполняем фильтрацию и сортировку в фоновом потоке
        final String currentError = error; // для лямбды
        ioExecutor.execute(() -> {
            List<ChallengeProgressFullDetails> filtered = filterChallenges(allChallenges, settings.selectedPeriod, settings.filterOptions);
            List<ChallengeProgressFullDetails> sorted = sortChallenges(filtered, settings.sortOption);
            // Обновляем LiveData из фонового потока
            mediator.postValue(new ChallengesScreenUiState(false, currentError, settings.selectedPeriod, settings.sortOption, settings.filterOptions, sorted));
        });
    }


    private List<ChallengeProgressFullDetails> filterChallenges(
            List<ChallengeProgressFullDetails> challenges,
            ChallengePeriod period,
            Set<ChallengeFilterOption> filters) {
        if (challenges == null) return Collections.emptyList();
        Stream<ChallengeProgressFullDetails> stream = challenges.stream();

        if (period != null) {
            stream = stream.filter(details -> details.getChallengeAndReward().getChallenge().getPeriod() == period);
        }

        if (filters != null && !filters.isEmpty() && !filters.contains(ChallengeFilterOption.ALL)) {
            LocalDateTime now = LocalDateTime.now();
            LocalDate today = now.toLocalDate();
            LocalDate tomorrow = today.plusDays(1);

            stream = stream.filter(details -> {
                Challenge challenge = details.getChallengeAndReward().getChallenge();
                Reward reward = details.getChallengeAndReward().getReward();
                boolean isCompleted = challenge.getStatus() == ChallengeStatus.COMPLETED;
                boolean isExpired = challenge.getStatus() == ChallengeStatus.EXPIRED;
                boolean isActive = challenge.getStatus() == ChallengeStatus.ACTIVE;
                boolean isUrgent = isActive && (challenge.getEndDate().toLocalDate().isEqual(today) || challenge.getEndDate().toLocalDate().isEqual(tomorrow));

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
                        case ALL: match = true; break; // ALL не должен быть здесь, если !filters.contains(ALL)
                    }
                    if (!match) return false; // Если хоть один фильтр не пройден
                }
                return true; // Все активные фильтры пройдены
            });
        }
        return stream.collect(Collectors.toList());
    }

    private boolean isHighReward(RewardType type, String valueStr) {
        // Логика оценки "ценности"
        if (type == null || valueStr == null) return false;
        try {
            return switch (type) {
                case COINS -> Integer.parseInt(valueStr) >= 50;
                case EXPERIENCE -> Integer.parseInt(valueStr) >= 50;
                case BADGE, PLANT, THEME -> true;
                default -> false;
            };
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private List<ChallengeProgressFullDetails> sortChallenges(
            List<ChallengeProgressFullDetails> challenges,
            ChallengeSortOption sortOption) {
        if (challenges == null) return Collections.emptyList();
        List<ChallengeProgressFullDetails> sortedList = new ArrayList<>(challenges); // Копируем для сортировки

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
                default -> 0;
            };
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // --- Методы для UI ---
    public void selectPeriod(ChallengePeriod period) {
        logger.debug(TAG, "Period selected: " + period);
        _selectedPeriodLiveData.setValue(period);
    }

    public void updateSortOption(ChallengeSortOption option) {
        logger.debug(TAG, "Sort option selected: " + option);
        _sortOptionLiveData.setValue(option);
    }

    public void toggleFilterOption(ChallengeFilterOption option) {
        Set<ChallengeFilterOption> currentFilters = new HashSet<>(Objects.requireNonNull(_filterOptionsLiveData.getValue()));
        Set<ChallengeFilterOption> newFilters;
        if (option == ChallengeFilterOption.ALL) {
            newFilters = Collections.singleton(ChallengeFilterOption.ALL);
        } else if (currentFilters.contains(ChallengeFilterOption.ALL)) {
            newFilters = Collections.singleton(option);
        } else if (currentFilters.contains(option)) {
            currentFilters.remove(option);
            newFilters = currentFilters.isEmpty() ? Collections.singleton(ChallengeFilterOption.ACTIVE) : currentFilters;
        } else {
            currentFilters.remove(ChallengeFilterOption.ALL);
            currentFilters.add(option);
            newFilters = currentFilters;
        }
        logger.debug(TAG, "Filter options updated: " + newFilters);
        _filterOptionsLiveData.setValue(newFilters);
    }

    public void resetFiltersAndSort() {
        logger.debug(TAG, "Resetting filters and sort");
        _sortOptionLiveData.setValue(ChallengeSortOption.DEADLINE_ASC);
        _filterOptionsLiveData.setValue(Collections.singleton(ChallengeFilterOption.ACTIVE));
    }

    public void clearError() {
        _errorLiveData.setValue(null);
    }
}