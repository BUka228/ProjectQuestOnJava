package com.example.projectquestonjava.core.data.initializers;

import com.example.projectquestonjava.core.data.database.AppDatabase;
import com.example.projectquestonjava.core.data.model.core.Approach;
import com.example.projectquestonjava.core.data.model.core.UserAuth;
import com.example.projectquestonjava.core.data.model.core.Workspace;
import com.example.projectquestonjava.core.data.model.enums.ApproachName;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.managers.UserSessionManager;
import com.example.projectquestonjava.core.managers.WorkspaceSessionManager;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.data.dao.ChallengeDao;
import com.example.projectquestonjava.feature.gamification.data.dao.SurpriseTaskDao;
import com.example.projectquestonjava.feature.gamification.data.managers.GamificationDataStoreManager;
import com.example.projectquestonjava.feature.gamification.data.model.Challenge;
import com.example.projectquestonjava.feature.gamification.data.model.ChallengeRule;
import com.example.projectquestonjava.feature.gamification.data.model.Gamification;
import com.example.projectquestonjava.feature.gamification.data.model.GamificationChallengeProgress;
import com.example.projectquestonjava.feature.gamification.data.model.SurpriseTask;
import com.example.projectquestonjava.feature.gamification.data.model.VirtualGarden;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeStatus;
import com.example.projectquestonjava.feature.gamification.domain.model.PlantType;
import com.example.projectquestonjava.feature.statistics.data.model.GlobalStatistics;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TestDataInitializer {
    private static final String TAG = "TestDataInitializer";
    private static final String TEST_USER_EMAIL = "test@example.com";
    private static final String TEST_USER_PASSWORD_HASH_PLACEHOLDER = "$2a$12$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKL";
    private static final String TEST_USER_USERNAME = "Искатель Прогресса";
    private static final String TEST_WORKSPACE_NAME = "Основное пространство";
    private static final String TEST_WORKSPACE_DESCRIPTION = "Мои задачи и цели";
    private static final ApproachName TEST_DEFAULT_APPROACH_NAME = ApproachName.CALENDAR;
    private static final PlantType TEST_INITIAL_PLANT_TYPE = PlantType.SUNFLOWER;

    private static final long REWARD_ID_SURPRISE_201 = 201L;
    // ... (остальные константы наград)

    private final AppDatabase appDatabase;
    private final UserSessionManager userSessionManager;
    private final WorkspaceSessionManager workspaceSessionManager;
    private final GamificationDataStoreManager gamificationDataStoreManager;
    private final DateTimeUtils dateTimeUtils;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public TestDataInitializer(
            AppDatabase appDatabase,
            UserSessionManager userSessionManager,
            WorkspaceSessionManager workspaceSessionManager,
            GamificationDataStoreManager gamificationDataStoreManager,
            DateTimeUtils dateTimeUtils,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.appDatabase = appDatabase;
        this.userSessionManager = userSessionManager;
        this.workspaceSessionManager = workspaceSessionManager;
        this.gamificationDataStoreManager = gamificationDataStoreManager;
        this.dateTimeUtils = dateTimeUtils;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    public ListenableFuture<Void> initializeTestDataIfEmpty() {
        logger.debug(TAG, "Attempting to initialize TEST data for " + TEST_USER_EMAIL + "...");

        // Проверка пользователя выполняется на ioExecutor
        return Futures.submitAsync(() -> {
            UserAuth existingUser = appDatabase.userAuthDao().getUserByEmailSync(TEST_USER_EMAIL); // Используем Sync метод
            if (existingUser != null) {
                logger.info(TAG, "Test data already exists for user " + TEST_USER_EMAIL + ". Ensuring session is set.");
                return ensureSessionSetAsync(existingUser.getId());
            }

            logger.info(TAG, "No existing test data found. Proceeding with TEST initialization for " + TEST_USER_EMAIL);

            // Вся инициализация данных происходит в транзакции на ioExecutor
            return Futures.submit(() -> {
                appDatabase.runInTransaction((Callable<Void>) () -> {
                    logger.debug(TAG, "Starting transaction for TEST data initialization.");
                    List<Approach> approaches = appDatabase.approachDao().getAllApproachesSync();
                    if (approaches == null || approaches.isEmpty()) {
                        throw new IllegalStateException("Approaches not available for initialization.");
                    }
                    Approach defaultApproach = approaches.stream()
                            .filter(a -> a.getName() == TEST_DEFAULT_APPROACH_NAME)
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Default approach not found."));
                    long defaultApproachId = defaultApproach.getId();

                    UserAuth testUser = new UserAuth(0, TEST_USER_EMAIL, TEST_USER_PASSWORD_HASH_PLACEHOLDER, TEST_USER_USERNAME, null);
                    long userIdLong = appDatabase.userAuthDao().insertUserSync(testUser); // SYNC
                    int userId = (int) userIdLong;
                    logger.debug(TAG, "User created with ID: " + userId);

                    Workspace testWorkspace = new Workspace(0, userId, TEST_WORKSPACE_NAME, TEST_WORKSPACE_DESCRIPTION, defaultApproachId, dateTimeUtils.currentUtcDateTime(), dateTimeUtils.currentUtcDateTime());
                    long workspaceId = appDatabase.workspaceDao().insertWorkspaceSync(testWorkspace); // SYNC
                    logger.debug(TAG, "Workspace created with ID: " + workspaceId);

                    Gamification testGamification = new Gamification(userId, InitialDataConstants.INITIAL_LEVEL, InitialDataConstants.INITIAL_XP, InitialDataConstants.INITIAL_COINS, InitialDataConstants.MAX_XP_FOR_LEVEL, dateTimeUtils.currentUtcDateTime(), 0, LocalDate.MIN, 0);
                    long gamificationId = appDatabase.gamificationDao().insertSync(testGamification); // SYNC
                    logger.debug(TAG, "Gamification profile created with ID: " + gamificationId);

                    GlobalStatistics initialGlobalStats = new GlobalStatistics(0, userId, 1, 0, 0, 0, dateTimeUtils.currentUtcDateTime());
                    appDatabase.globalStatisticsDao().insertOrUpdateGlobalStatisticsSync(initialGlobalStats); // SYNC
                    logger.debug(TAG, "Global statistics created for user ID: " + userId);

                    VirtualGarden initialPlant = new VirtualGarden(gamificationId, TEST_INITIAL_PLANT_TYPE, InitialDataConstants.INITIAL_GROWTH_STAGE, 0, dateTimeUtils.currentUtcDateTime().minusDays(1));
                    long plantId = appDatabase.virtualGardenDao().insertSync(initialPlant); // SYNC
                    logger.debug(TAG, "Initial plant created with ID: " + plantId);

                    insertSurpriseTasksInternalSync(appDatabase.surpriseTaskDao(), gamificationId); // SYNC
                    insertInitialChallengeProgressInternalSync(appDatabase.challengeDao(), gamificationId); // SYNC

                    // DataStore операции остаются асинхронными, но мы дожидаемся их завершения
                    userSessionManager.saveUserIdAsync(userId).get();
                    workspaceSessionManager.saveWorkspaceIdAsync(workspaceId).get();
                    gamificationDataStoreManager.saveGamificationId(gamificationId).get();
                    gamificationDataStoreManager.saveSelectedPlantId(plantId).get();

                    logger.info(TAG, "TEST data initialization (full) successful for user " + TEST_USER_EMAIL);
                    return null;
                });
                return null; // для Futures.submit
            }, ioExecutor);
        }, ioExecutor);
    }

    private ListenableFuture<Void> ensureSessionSetAsync(int userId) {
        logger.info(TAG, "ensureSessionSetAsync started for userId: " + userId);
        List<ListenableFuture<?>> operations = new ArrayList<>();

        operations.add(Futures.transformAsync(userSessionManager.getUserIdFuture(), currentSessionUserId -> {
            if (currentSessionUserId == null || currentSessionUserId != userId) {
                return userSessionManager.saveUserIdAsync(userId);
            }
            return Futures.immediateFuture(null);
        }, ioExecutor));

        operations.add(Futures.transformAsync(workspaceSessionManager.getWorkspaceIdFuture(), currentWsId -> {
            if (currentWsId == null || currentWsId == WorkspaceSessionManager.NO_WORKSPACE_ID) {
                // Используем синхронный вызов DAO внутри submit, так как ioExecutor уже есть
                return Futures.submit(() -> {
                    List<Workspace> workspaces = appDatabase.workspaceDao().getAllWorkspacesSync(userId);
                    if (workspaces != null && !workspaces.isEmpty()) {
                        return Futures.getDone(workspaceSessionManager.saveWorkspaceIdAsync(workspaces.get(0).getId()));
                    }
                    logger.error(TAG, "ensureSessionSetAsync: No workspace found in DB for user " + userId);
                    return null;
                }, ioExecutor);
            }
            return Futures.immediateFuture(null);
        }, ioExecutor));

        operations.add(Futures.transformAsync(gamificationDataStoreManager.getGamificationIdFuture(), currentGamiId -> {
            if (currentGamiId == null || currentGamiId == -1L) {
                return Futures.submitAsync(() -> { // Для вложенных асинхронных операций
                    Gamification gamification = appDatabase.gamificationDao().getByUserIdSync(userId); // SYNC
                    if (gamification != null) {
                        ListenableFuture<Void> saveGamiIdFuture = gamificationDataStoreManager.saveGamificationId(gamification.getId());
                        return Futures.transformAsync(saveGamiIdFuture, v -> ensureSelectedPlantSetAsyncInternal(gamification.getId()), ioExecutor);
                    }
                    logger.error(TAG, "ensureSessionSetAsync: No gamification profile found in DB for user " + userId);
                    return Futures.immediateFuture(null);
                }, ioExecutor);
            } else {
                return ensureSelectedPlantSetAsyncInternal(currentGamiId);
            }
        }, ioExecutor));

        return Futures.transform(Futures.allAsList(operations), input -> {
            logger.info(TAG, "ensureSessionSetAsync finished for userId: " + userId);
            return null;
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Void> ensureSelectedPlantSetAsyncInternal(long gamificationId) {
        return Futures.transformAsync(gamificationDataStoreManager.getSelectedPlantIdFuture(), selectedPlantId -> {
            if (selectedPlantId == null || selectedPlantId == -1L) {
                return Futures.submitAsync(() -> { // Для DAO вызова
                    VirtualGarden latestPlant = appDatabase.virtualGardenDao().getLatestPlantSync(gamificationId); // SYNC
                    if (latestPlant != null) {
                        return gamificationDataStoreManager.saveSelectedPlantId(latestPlant.getId());
                    }
                    logger.warn(TAG, "ensureSelectedPlantSetAsyncInternal: No plants for gamification " + gamificationId);
                    return Futures.immediateFuture(null);
                }, ioExecutor);
            }
            return Futures.immediateFuture(null);
        }, ioExecutor);
    }

    private void insertSurpriseTasksInternalSync(SurpriseTaskDao surpriseTaskDao, long gamificationId) {
        logger.debug(TAG, "Inserting test surprise tasks (SYNC) for gamificationId: " + gamificationId);
        LocalDateTime now = dateTimeUtils.currentUtcDateTime();
        List<SurpriseTask> tasks = new ArrayList<>();
        tasks.add(new SurpriseTask(gamificationId, "Разминка для глаз!", REWARD_ID_SURPRISE_201, now.plusHours(1), false, null));
        // ...
        surpriseTaskDao.insertAllSync(tasks); // SYNC
        logger.debug(TAG, "Inserted " + tasks.size() + " test surprise tasks (SYNC).");
    }

    private void insertInitialChallengeProgressInternalSync(ChallengeDao challengeDao, long gamificationId) {
        logger.debug(TAG, "Inserting initial challenge progress (SYNC) for gamificationId: " + gamificationId);
        LocalDateTime now = dateTimeUtils.currentUtcDateTime();
        List<Challenge> activeChallenges = challengeDao.getChallengesByStatusSync(ChallengeStatus.ACTIVE); // SYNC
        if (activeChallenges.isEmpty()) {
            logger.info(TAG, "No active challenges found (SYNC).");
            return;
        }
        for (Challenge challenge : activeChallenges) {
            List<ChallengeRule> rules = challengeDao.getChallengeRulesByChallengeIdSync(challenge.getId()); // SYNC
            if (rules.isEmpty()) continue;
            for (ChallengeRule rule : rules) {
                GamificationChallengeProgress progress = new GamificationChallengeProgress(gamificationId, challenge.getId(), rule.getId(), 0, false, now);
                challengeDao.insertOrUpdateProgressSync(progress); // SYNC
            }
        }
        logger.debug(TAG, "Initial challenge progress (SYNC) finished.");
    }
}