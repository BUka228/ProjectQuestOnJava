package com.example.projectquestonjava.feature.gamification.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.data.dao.VirtualGardenDao;
import com.example.projectquestonjava.feature.gamification.data.managers.GamificationDataStoreManager;
import com.example.projectquestonjava.feature.gamification.data.model.VirtualGarden;
import com.example.projectquestonjava.feature.gamification.domain.repository.VirtualGardenRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class VirtualGardenRepositoryImpl implements VirtualGardenRepository {

    private static final String TAG = "VirtualGardenRepository";
    private final VirtualGardenDao virtualGardenDao;
    private final GamificationDataStoreManager gamificationDataStoreManager;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public VirtualGardenRepositoryImpl(
            VirtualGardenDao virtualGardenDao,
            GamificationDataStoreManager gamificationDataStoreManager,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.virtualGardenDao = virtualGardenDao;
        this.gamificationDataStoreManager = gamificationDataStoreManager;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    @Override
    public LiveData<List<VirtualGarden>> getAllPlantsFlow() {
        logger.debug(TAG, "Getting all plants LiveData for current user");
        return Transformations.switchMap(gamificationDataStoreManager.getGamificationIdFlow(), gamificationId -> {
            if (gamificationId == null || gamificationId == -1L) {
                logger.warn(TAG, "Cannot get all plants, no gamification ID.");
                return new LiveData<List<VirtualGarden>>(Collections.emptyList()) {};
            }
            return virtualGardenDao.getAllPlantsFlow(gamificationId);
        });
    }

    @Override
    public LiveData<VirtualGarden> getPlantFlow(long plantId) {
        logger.debug(TAG, "Getting plant LiveData for plantId=" + plantId);
        return virtualGardenDao.getPlantByIdFlow(plantId);
    }

    @Override
    public ListenableFuture<VirtualGarden> getPlant(long plantId) {
        logger.debug(TAG, "Getting plant future for plantId=" + plantId);
        return virtualGardenDao.getPlantById(plantId);
    }

    @Override
    public ListenableFuture<VirtualGarden> getLatestPlant() {
        return Futures.transformAsync(
                gamificationDataStoreManager.getGamificationIdFuture(),
                gamificationId -> {
                    if (gamificationId == null || gamificationId == -1L) {
                        logger.warn(TAG, "Cannot get latest plant, no gamification ID.");
                        return Futures.immediateFuture(null);
                    }
                    logger.debug(TAG, "Getting latest plant for gamiId=" + gamificationId);
                    return virtualGardenDao.getLatestPlant(gamificationId);
                },
                ioExecutor
        );
    }

    @Override
    public ListenableFuture<Long> insertPlant(VirtualGarden plant) {
        logger.debug(TAG, "Inserting plant: gamiId=" + plant.getGamificationId() + ", type=" + plant.getPlantType());
        return virtualGardenDao.insert(plant);
    }

    @Override
    public ListenableFuture<Void> updatePlant(VirtualGarden plant) {
        logger.debug(TAG, "Updating plant: id=" + plant.getId());
        return Futures.transform(virtualGardenDao.update(plant), count -> null, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<Void> updateLastWateredForAllUserPlants(long gamificationId, LocalDateTime timestamp) {
        logger.debug(TAG, "Updating lastWatered for all plants of user " + gamificationId + " to " + timestamp);
        return Futures.transform(virtualGardenDao.updateLastWateredForAllUserPlants(gamificationId, timestamp), count -> null, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<Void> deleteVirtualGardenForGamification(long gamificationId) {
        logger.debug(TAG, "Deleting virtual garden for gamificationId=" + gamificationId);
        return Futures.transform(virtualGardenDao.deleteVirtualGardenForGamification(gamificationId), count -> null, MoreExecutors.directExecutor());
    }


    @Override
    public ListenableFuture<List<VirtualGarden>> getAllPlantsFuture() {
        logger.debug(TAG, "Getting all plants future for current user");
        return Futures.transformAsync(
                gamificationDataStoreManager.getGamificationIdFuture(),
                gamificationId -> {
                    if (gamificationId == null || gamificationId == -1L) {
                        logger.warn(TAG, "Cannot get all plants future, no gamification ID.");
                        return Futures.immediateFuture(Collections.emptyList());
                    }
                    return virtualGardenDao.getAllPlants(gamificationId);
                },
                ioExecutor
        );
    }
}