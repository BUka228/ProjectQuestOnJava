package com.example.projectquestonjava.feature.gamification.presentation.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.data.managers.GamificationDataStoreManager;
import com.example.projectquestonjava.feature.gamification.data.model.VirtualGarden;
import com.example.projectquestonjava.feature.gamification.domain.model.PlantHealthState;
import com.example.projectquestonjava.feature.gamification.domain.repository.VirtualGardenRepository;
import com.example.projectquestonjava.feature.gamification.domain.usecases.ManuallyWaterPlantUseCase;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import dagger.hilt.android.lifecycle.HiltViewModel;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.inject.Inject;

// Состояние UI для экрана Сада
class GardenScreenUiState { // Если еще не создан, убираем public
    public final boolean isLoading;
    public final String errorMessage;
    public final String successMessage;
    public final List<VirtualGarden> plants;
    public final long selectedPlantId;
    public final Map<Long, PlantHealthState> healthStates;
    public final boolean canWaterToday;
    public final boolean showWateringConfirmation;
    public final boolean showWateringPlantEffect;

    public GardenScreenUiState(boolean isLoading, String errorMessage, String successMessage,
                               List<VirtualGarden> plants, long selectedPlantId,
                               Map<Long, PlantHealthState> healthStates, boolean canWaterToday,
                               boolean showWateringConfirmation, boolean showWateringPlantEffect) {
        this.isLoading = isLoading;
        this.errorMessage = errorMessage;
        this.successMessage = successMessage;
        this.plants = plants != null ? plants : Collections.emptyList();
        this.selectedPlantId = selectedPlantId;
        this.healthStates = healthStates != null ? healthStates : Collections.emptyMap();
        this.canWaterToday = canWaterToday;
        this.showWateringConfirmation = showWateringConfirmation;
        this.showWateringPlantEffect = showWateringPlantEffect;
    }

    // Конструктор по умолчанию
    public GardenScreenUiState() {
        this(false, null, null, Collections.emptyList(), -1L,
                Collections.emptyMap(), false, false, false);
    }

    public GardenScreenUiState copy(
            Boolean isLoading, String errorMessage, String successMessage,
            List<VirtualGarden> plants, Long selectedPlantId,
            Map<Long, PlantHealthState> healthStates, Boolean canWaterToday,
            Boolean showWateringConfirmation, Boolean showWateringPlantEffect
    ) {
        return new GardenScreenUiState(
                isLoading != null ? isLoading : this.isLoading,
                errorMessage, // может быть null
                successMessage, // может быть null
                plants != null ? plants : this.plants,
                selectedPlantId != null ? selectedPlantId : this.selectedPlantId,
                healthStates != null ? healthStates : this.healthStates,
                canWaterToday != null ? canWaterToday : this.canWaterToday,
                showWateringConfirmation != null ? showWateringConfirmation : this.showWateringConfirmation,
                showWateringPlantEffect != null ? showWateringPlantEffect : this.showWateringPlantEffect
        );
    }
}


@HiltViewModel
public class GardenViewModel extends ViewModel {

    private static final String TAG = "GardenViewModel";

    private final GamificationDataStoreManager gamificationDataStoreManager;
    private final ManuallyWaterPlantUseCase manuallyWaterPlantUseCase;
    private final DateTimeUtils dateTimeUtils;
    private final Executor ioExecutor;
    private final Logger logger;

    private final MutableLiveData<GardenScreenUiState> _uiStateLiveData = new MutableLiveData<>(new GardenScreenUiState());
    public LiveData<GardenScreenUiState> uiStateLiveData = _uiStateLiveData;

    // LiveData для отдельных частей состояния, которые будут комбинироваться
    private final LiveData<List<VirtualGarden>> allPlantsLiveData;
    private final LiveData<Long> selectedPlantIdLiveData;

    // Таймер для сброса эффектов полива
    private java.util.Timer effectResetTimer; // Используем java.util.Timer

    @Inject
    public GardenViewModel(
            VirtualGardenRepository virtualGardenRepository,
            GamificationDataStoreManager gamificationDataStoreManager,
            ManuallyWaterPlantUseCase manuallyWaterPlantUseCase,
            DateTimeUtils dateTimeUtils,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.gamificationDataStoreManager = gamificationDataStoreManager;
        this.manuallyWaterPlantUseCase = manuallyWaterPlantUseCase;
        this.dateTimeUtils = dateTimeUtils;
        this.ioExecutor = ioExecutor;
        this.logger = logger;

        logger.info(TAG, "ViewModel initialized.");

        allPlantsLiveData = Transformations.switchMap(
                gamificationDataStoreManager.getGamificationIdFlow(), // LiveData<Long>
                gamiId -> (gamiId == null || gamiId == -1L) ?
                        new MutableLiveData<>(Collections.emptyList()) :
                        virtualGardenRepository.getAllPlantsFlow() // LiveData<List<VirtualGarden>>
        );

        selectedPlantIdLiveData = gamificationDataStoreManager.getSelectedPlantIdFlow(); // LiveData<Long>

        // Объединяем все источники для формирования _uiStateLiveData
        MediatorLiveData<Object> combinator = new MediatorLiveData<>();
        combinator.addSource(allPlantsLiveData, value -> combineAndSetUiState());
        combinator.addSource(selectedPlantIdLiveData, value -> combineAndSetUiState());
        // Добавляем _isLoadingLiveData как источник, чтобы UI обновлялся при его изменении
        // (хотя он не используется в combineAndSetUiState для вычисления, но триггерит обновление)
        // Либо, мы можем не добавлять его сюда, а обновлять _uiStateLiveData напрямую при изменении isLoading

        // Изначально загружаем данные, если нужно (allPlantsLiveData и selectedPlantIdLiveData начнут эмитить)
        // Состояние isLoading будет управляться методами waterPlant и т.д.
    }

    private void combineAndSetUiState() {
        List<VirtualGarden> plants = allPlantsLiveData.getValue();
        Long selectedId = selectedPlantIdLiveData.getValue();
        GardenScreenUiState currentState = _uiStateLiveData.getValue(); // Текущее состояние для сохранения флагов эффектов

        if (plants == null || selectedId == null) {
            // Если какие-то из основных данных еще не пришли, не обновляем или ставим isLoading
            _uiStateLiveData.postValue((currentState != null ? currentState : new GardenScreenUiState()).copy(
                    true, null,null, null, null, null, null, null, null
            ));
            return;
        }

        Map<Long, PlantHealthState> healthMap = calculateHealthStatesMap(plants);
        boolean canWater = calculateCanWaterToday(plants);

        _uiStateLiveData.postValue(new GardenScreenUiState(
                currentState != null ? currentState.isLoading : false, // Сохраняем текущий isLoading
                currentState != null ? currentState.errorMessage : null,
                currentState != null ? currentState.successMessage : null,
                plants,
                selectedId,
                healthMap,
                canWater,
                currentState != null ? currentState.showWateringConfirmation : false,
                currentState != null ? currentState.showWateringPlantEffect : false
        ));
        logger.debug(TAG, "UI State combined: " + plants.size() + " plants, selected: " + selectedId + ", canWater: " + canWater);
    }


    public void selectActivePlant(long plantId) {
        GardenScreenUiState current = _uiStateLiveData.getValue();
        if (current == null || plantId == current.selectedPlantId || current.isLoading) return;

        logger.debug(TAG, "Selecting active plant: " + plantId);
        // gamificationDataStoreManager.saveSelectedPlantId возвращает ListenableFuture<Void>
        ListenableFuture<Void> saveFuture = gamificationDataStoreManager.saveSelectedPlantId(plantId);
        Futures.addCallback(saveFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) { /* LiveData selectedPlantIdLiveData обновится автоматически */ }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Failed to save selected plant ID " + plantId, t);
                updateUiState(s -> s.copy(null, "Не удалось выбрать растение",null, null, null, null, null, null, null));
            }
        }, ioExecutor);
    }

    public void waterPlant() {
        GardenScreenUiState current = _uiStateLiveData.getValue();
        if (current == null || !current.canWaterToday || current.isLoading || current.showWateringConfirmation) {
            if (current != null && !current.canWaterToday) {
                logger.warn(TAG, "Attempted to water plant when not allowed (already watered).");
                updateUiState(s -> s.copy(null, "Вы уже поливали растения сегодня.",null, null, null, null, null, null, null));
            } else if (current != null && current.isLoading) {
                logger.warn(TAG, "Attempted to water plant during another operation.");
            }
            return;
        }
        logger.debug(TAG, "Initiating manual plant watering...");
        updateUiState(s -> s.copy(true, null, null, null, null, null, null, null, null));


        ListenableFuture<Void> waterFuture = manuallyWaterPlantUseCase.execute();
        Futures.addCallback(waterFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                logger.info(TAG, "Manual watering successful via UseCase.");
                triggerWateringEffects(); // Запускаем эффекты
                // isLoading будет сброшен после эффектов
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Manual watering failed via UseCase", t);
                updateUiState(s -> s.copy(false, "Не удалось полить: " + t.getMessage(),null, null, null, null, null, null, null));
            }
        }, ioExecutor);
    }

    private void triggerWateringEffects() {
        // Отменяем предыдущий таймер, если он был
        if (effectResetTimer != null) {
            effectResetTimer.cancel();
        }
        effectResetTimer = new java.util.Timer();

        updateUiState(s -> s.copy(null, null,null, null, null, null, null, true, true));

        // Запускаем задачи для сброса флагов через java.util.Timer
        effectResetTimer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                updateUiState(s -> s.copy(null, null,null, null, null, null, null, false, null));
            }
        }, 1500); // Длительность анимации галочки

        effectResetTimer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                updateUiState(s -> s.copy(false, null, null, null, null, null, null, null, false)); // Сбрасываем и isLoading
            }
        }, 1200 + 300); // Длительность эффекта на растениях + небольшая задержка после галочки
    }


    public void clearErrorMessage() { updateUiState(s -> s.copy(null, null,null, null, null, null, null, null, null)); }
    public void clearSuccessMessage() { updateUiState(s -> s.copy(null, null,null, null, null, null, null, null, null)); }

    private Map<Long, PlantHealthState> calculateHealthStatesMap(List<VirtualGarden> plants) {
        if (plants == null) return Collections.emptyMap();
        return plants.stream()
                .collect(Collectors.toMap(VirtualGarden::getId, this::calculateHealthState));
    }

    private PlantHealthState calculateHealthState(VirtualGarden plant) {
        if (plant == null) return PlantHealthState.HEALTHY;
        LocalDate lastWateredDate = dateTimeUtils.utcToLocalLocalDateTime(plant.getLastWatered()).toLocalDate();
        LocalDate today = dateTimeUtils.currentLocalDate(); // Используем dateTimeUtils
        long daysSinceWatered = ChronoUnit.DAYS.between(lastWateredDate, today);
        if (daysSinceWatered <= 1) return PlantHealthState.HEALTHY;
        if (daysSinceWatered == 2L) return PlantHealthState.NEEDSWATER;
        return PlantHealthState.WILTED;
    }

    private boolean calculateCanWaterToday(List<VirtualGarden> plants) {
        if (plants == null || plants.isEmpty()) return false;
        // Проверяем по дате полива ПЕРВОГО растения (или любого, т.к. все поливаются одновременно)
        VirtualGarden firstPlant = plants.get(0); // Если список не пуст, первый элемент есть
        LocalDate lastWateredDate = dateTimeUtils.utcToLocalLocalDateTime(firstPlant.getLastWatered()).toLocalDate();
        LocalDate today = dateTimeUtils.currentLocalDate();
        return lastWateredDate.isBefore(today);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (effectResetTimer != null) {
            effectResetTimer.cancel();
            effectResetTimer = null;
        }
        // Отписка от LiveData не нужна, т.к. ViewModel сама управляет их жизненным циклом
    }

    // Вспомогательный интерфейс для обновления UI State
    @FunctionalInterface
    private interface UiStateUpdater {
        GardenScreenUiState update(GardenScreenUiState currentState);
    }
    // Перегруженный метод для удобства
    private void updateUiState(UiStateUpdater updater) {
        GardenScreenUiState current = _uiStateLiveData.getValue();
        _uiStateLiveData.postValue(updater.update(current != null ? current : new GardenScreenUiState()));
    }
}