package com.example.projectquestonjava.feature.gamification.data.repository;

import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.data.dao.StreakRewardDefinitionDao;
import com.example.projectquestonjava.feature.gamification.data.model.StreakRewardDefinition;
import com.example.projectquestonjava.feature.gamification.domain.repository.StreakRewardDefinitionRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StreakRewardDefinitionRepositoryImpl implements StreakRewardDefinitionRepository {

    private static final String TAG = "StreakRewardDefRepo";
    private final StreakRewardDefinitionDao streakRewardDefinitionDao;
    private final Logger logger;
    private final Executor ioExecutor;

    @Inject
    public StreakRewardDefinitionRepositoryImpl(
            StreakRewardDefinitionDao streakRewardDefinitionDao,
            Logger logger,
            @com.example.projectquestonjava.core.di.IODispatcher Executor ioExecutor) {
        this.streakRewardDefinitionDao = streakRewardDefinitionDao;
        this.logger = logger;
        this.ioExecutor = ioExecutor;
    }

    @Override
    public ListenableFuture<StreakRewardDefinition> getRewardDefinitionForStreak(int streakDay) {
        logger.debug(TAG, "Getting reward definition for streakDay=" + streakDay);
        return Futures.catchingAsync(
                streakRewardDefinitionDao.getRewardDefinitionForStreak(streakDay),
                Exception.class,
                e -> {
                    logger.error(TAG, "Error getting definition for streakDay=" + streakDay, e);
                    return null;
                },
                ioExecutor
        );
    }

    @Override
    public ListenableFuture<List<StreakRewardDefinition>> getRewardDefinitionsForStreakRange(int startStreak, int endStreak) {
        logger.debug(TAG, "Getting reward definitions for range " + startStreak + ".." + endStreak);
        return Futures.catchingAsync(
                streakRewardDefinitionDao.getRewardDefinitionsForStreakRange(startStreak, endStreak),
                Exception.class,
                e -> {
                    logger.error(TAG, "Error getting definitions for range " + startStreak + ".." + endStreak, e);
                    throw new RuntimeException("Failed to get definitions for range", e);
                },
                ioExecutor
        );
    }

    @Override
    public ListenableFuture<Void> insertDefinition(StreakRewardDefinition definition) {
        logger.debug(TAG, "Inserting definition for streakDay=" + definition.getStreakDay());
        return Futures.catchingAsync(
                streakRewardDefinitionDao.insert(definition),
                Exception.class,
                e -> {
                    logger.error(TAG, "Error inserting definition for streakDay=" + definition.getStreakDay(), e);
                    throw new RuntimeException("Failed to insert definition", e);
                },
                ioExecutor
        );
    }

    @Override
    public ListenableFuture<Void> insertAllDefinitions(List<StreakRewardDefinition> definitions) {
        logger.debug(TAG, "Inserting " + definitions.size() + " definitions");
        return Futures.catchingAsync(
                streakRewardDefinitionDao.insertAll(definitions),
                Exception.class,
                e -> {
                    logger.error(TAG, "Error inserting all definitions", e);
                    throw new RuntimeException("Failed to insert all definitions", e);
                },
                ioExecutor
        );
    }
}