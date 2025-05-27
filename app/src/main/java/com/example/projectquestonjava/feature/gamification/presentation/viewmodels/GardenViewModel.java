package com.example.projectquestonjava.feature.gamification.presentation.viewmodels;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.managers.SnackbarManager;
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
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Function; // Для java.util.function.Function
import java.util.stream.Collectors;
import javax.inject.Inject;

@HiltViewModel
public class GardenViewModel extends ViewModel {

    private static final String TAG = "GardenViewModel";

    private final VirtualGardenRepository virtualGardenRepository;
    private final GamificationDataStoreManager gamificationDataStoreManager;
    private final ManuallyWaterPlantUseCase manuallyWaterPlantUseCase;
    private final DateTimeUtils dateTimeUtils;
    private final Executor ioExecutor;
    private final Logger logger;
    private final SnackbarManager snackbarManager;

    private final MutableLiveData<GardenScreenUiState> _uiStateLiveData =
            new MutableLiveData<>(GardenScreenUiState.createDefault());
    public final LiveData<GardenScreenUiState> uiStateLiveData = _uiStateLiveData;

    // Источники данных для MediatorLiveData
    private final LiveData<List<VirtualGarden>> allPlantsLiveData;
    private final LiveData<Long> selectedPlantIdFromDataStoreLiveData;

    private java.util.Timer effectResetTimer;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    // Обозреватели для отписки в onCleared
    private final Observer<List<VirtualGarden>> allPlantsObserver;
    private final Observer<Long> selectedPlantIdObserver;
    private final Observer<Object> combinatorObserver; // Пустой наблюдатель для активации MediatorLiveData

    @Inject
    public GardenViewModel(
            VirtualGardenRepository virtualGardenRepository,
            GamificationDataStoreManager gamificationDataStoreManager,
            ManuallyWaterPlantUseCase manuallyWaterPlantUseCase,
            DateTimeUtils dateTimeUtils,
            @IODispatcher Executor ioExecutor,
            Logger logger,
            SnackbarManager snackbarManager) {
        this.virtualGardenRepository = virtualGardenRepository;
        this.gamificationDataStoreManager = gamificationDataStoreManager;
        this.manuallyWaterPlantUseCase = manuallyWaterPlantUseCase;
        this.dateTimeUtils = dateTimeUtils;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
        this.snackbarManager = snackbarManager;

        logger.info(TAG, "GardenViewModel initialized.");

        LiveData<Long> gamificationIdLiveData = gamificationDataStoreManager.getGamificationIdFlow();

        allPlantsLiveData = Transformations.switchMap(gamificationIdLiveData, gamiId -> {
            if (gamiId == null || gamiId == -1L) {
                return new MutableLiveData<>(Collections.emptyList());
            }
            return virtualGardenRepository.getAllPlantsFlow();
        });

        selectedPlantIdFromDataStoreLiveData = gamificationDataStoreManager.getSelectedPlantIdFlow();

        MediatorLiveData<Object> combinator = new MediatorLiveData<>();

        allPlantsObserver = plants -> combineAndSetUiState();
        selectedPlantIdObserver = selectedId -> combineAndSetUiState();
        combinatorObserver = ignored -> {}; // Пустой наблюдатель

        combinator.addSource(allPlantsLiveData, allPlantsObserver);
        combinator.addSource(selectedPlantIdFromDataStoreLiveData, selectedPlantIdObserver);

        // Начальная установка isLoading
        updateUiState(currentState -> currentState.withLoading(true));
        // Наблюдаем за MediatorLiveData, чтобы он был активен
        combinator.observeForever(combinatorObserver);
    }

    private void combineAndSetUiState() {
        List<VirtualGarden> plants = allPlantsLiveData.getValue();
        Long selectedIdFromStore = selectedPlantIdFromDataStoreLiveData.getValue();
        GardenScreenUiState currentState = _uiStateLiveData.getValue(); // Для сохранения флагов эффектов

        if (plants == null || selectedIdFromStore == null) {
            // Если основные данные еще не пришли, не обновляем или оставляем isLoading,
            // если он не был сброшен другим процессом (например, ошибкой)
            if (currentState == null || currentState.isLoading()) {
                _uiStateLiveData.postValue(GardenScreenUiState.createDefault().withLoading(true));
            }
            return;
        }

        long finalSelectedPlantId = selectedIdFromStore;
        if (finalSelectedPlantId == -1L && !plants.isEmpty()) {
            finalSelectedPlantId = plants.stream()
                    .max(Comparator.comparingLong(VirtualGarden::getId))
                    .map(VirtualGarden::getId)
                    .orElse(-1L);
            if (finalSelectedPlantId != -1L) {
                final long idToSave = finalSelectedPlantId;
                ioExecutor.execute(() -> gamificationDataStoreManager.saveSelectedPlantId(idToSave));
            }
        }

        Map<Long, PlantHealthState> healthMap = calculateHealthStatesMap(plants);
        boolean canWater = calculateCanWaterToday(plants);

        GardenScreenUiState.GardenScreenUiStateBuilder newStateBuilder = (currentState != null ? currentState.toBuilder() : GardenScreenUiState.builder())
                .isLoading(false)
                .plants(plants)
                .selectedPlantId(finalSelectedPlantId)
                .healthStates(healthMap)
                .canWaterToday(canWater);

        // Ошибку и сообщение об успехе не сбрасываем здесь, они управляются действиями
        _uiStateLiveData.postValue(newStateBuilder.build());
        logger.debug(TAG, "UI State combined: " + plants.size() + " plants, selected: " + finalSelectedPlantId + ", canWater: " + canWater);
    }

    public void selectActivePlant(long plantId) {
        GardenScreenUiState current = _uiStateLiveData.getValue();
        if (current == null || plantId == current.getSelectedPlantId() || current.isLoading()) return;

        logger.debug(TAG, "Selecting active plant: " + plantId);
        ListenableFuture<Void> saveFuture = gamificationDataStoreManager.saveSelectedPlantId(plantId);
        Futures.addCallback(saveFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                logger.info(TAG, "Selected plant ID " + plantId + " saved to DataStore.");
                // _selectedPlantIdFromDataStoreLiveData обновится, что вызовет combineAndSetUiState
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Failed to save selected plant ID " + plantId, t);
                updateUiState(s -> s.withErrorMessage("Не удалось выбрать растение"));
            }
        }, ioExecutor);
    }

    public void waterPlant() {
        GardenScreenUiState current = _uiStateLiveData.getValue();
        if (current == null) return;

        if (!current.isCanWaterToday()) {
            logger.warn(TAG, "Attempted to water plant when not allowed (already watered).");
            snackbarManager.showMessage("Вы уже поливали растения сегодня.");
            return;
        }
        if (current.isLoading() || current.isShowWateringConfirmation()) {
            logger.warn(TAG, "Attempted to water plant during another operation or effect.");
            return;
        }

        logger.debug(TAG, "Initiating manual plant watering...");
        updateUiState(s -> s.withLoading(true).withErrorMessage(null).withSuccessMessage(null));

        ListenableFuture<Void> waterFuture = manuallyWaterPlantUseCase.execute();
        Futures.addCallback(waterFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                logger.info(TAG, "Manual watering successful via UseCase.");
                triggerWateringEffects();
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Manual watering failed via UseCase", t);
                updateUiState(s -> s.withLoading(false).withErrorMessage("Не удалось полить: " + t.getMessage()));
            }
        }, ioExecutor);
    }

    private void triggerWateringEffects() {
        if (effectResetTimer != null) {
            effectResetTimer.cancel();
            effectResetTimer.purge();
        }
        effectResetTimer = new java.util.Timer();

        updateUiState(s -> s.withShowWateringConfirmation(true).withShowWateringPlantEffect(true));

        uiHandler.postDelayed(() -> {
            updateUiState(s -> s.withShowWateringConfirmation(false));
        }, 1500);

        uiHandler.postDelayed(() -> {
            // Сбрасываем isLoading здесь, после завершения всех эффектов
            updateUiState(s -> s.withLoading(false)
                    .withShowWateringPlantEffect(false)
                    .withSuccessMessage("Растения политы!"));
            // snackbarManager.showMessage("Растения политы!"); // Можно и здесь, если нет гонки с successMessage
        }, 1200 + 300 + 100); // Длительность эффекта + задержки
    }

    public void clearErrorMessage() {
        updateUiState(s -> s.withErrorMessage(null));
    }
    public void clearSuccessMessage() {
        updateUiState(s -> s.withSuccessMessage(null));
    }

    private Map<Long, PlantHealthState> calculateHealthStatesMap(List<VirtualGarden> plants) {
        if (plants == null) return Collections.emptyMap();
        return plants.stream()
                .collect(Collectors.toMap(VirtualGarden::getId, this::calculateHealthState));
    }

    private PlantHealthState calculateHealthState(VirtualGarden plant) {
        if (plant == null) return PlantHealthState.HEALTHY;
        LocalDate lastWateredDate = dateTimeUtils.utcToLocalLocalDateTime(plant.getLastWatered()).toLocalDate();
        LocalDate today = dateTimeUtils.currentLocalDate();
        long daysSinceWatered = ChronoUnit.DAYS.between(lastWateredDate, today);
        if (daysSinceWatered <= 1) return PlantHealthState.HEALTHY;
        if (daysSinceWatered == 2L) return PlantHealthState.NEEDSWATER;
        return PlantHealthState.WILTED;
    }

    private boolean calculateCanWaterToday(List<VirtualGarden> plants) {
        if (plants == null || plants.isEmpty()) return false;
        VirtualGarden firstPlant = plants.get(0);
        LocalDate lastWateredDate = dateTimeUtils.utcToLocalLocalDateTime(firstPlant.getLastWatered()).toLocalDate();
        LocalDate today = dateTimeUtils.currentLocalDate();
        return lastWateredDate.isBefore(today);
    }

    // Используем Function для передачи логики обновления
    private void updateUiState(Function<GardenScreenUiState, GardenScreenUiState> updater) {
        GardenScreenUiState current = _uiStateLiveData.getValue();
        _uiStateLiveData.postValue(updater.apply(current != null ? current : GardenScreenUiState.createDefault()));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (effectResetTimer != null) {
            effectResetTimer.cancel();
            effectResetTimer.purge();
            effectResetTimer = null;
        }
        // Отписываемся от LiveData, на которые была подписка с observeForever
        if (allPlantsLiveData != null && allPlantsObserver != null) {
            allPlantsLiveData.removeObserver(allPlantsObserver);
        }
        if (selectedPlantIdFromDataStoreLiveData != null && selectedPlantIdObserver != null) {
            selectedPlantIdFromDataStoreLiveData.removeObserver(selectedPlantIdObserver);
        }
        // MediatorLiveData сам отпишется от источников, если на него нет активных наблюдателей.
        // Но если мы использовали observeForever на самом MediatorLiveData, то от него тоже нужно отписаться.
        // В данном случае `combinator` был локальным, и мы подписались на его источники.

        logger.debug(TAG, "GardenViewModel cleared.");
    }
}