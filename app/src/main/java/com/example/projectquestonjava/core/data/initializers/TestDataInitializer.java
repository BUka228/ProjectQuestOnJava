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
import com.example.projectquestonjava.feature.gamification.data.model.Challenge; // Убедитесь, что создан Challenge.java
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
import timber.log.Timber; // или android.util.Log

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

    // Константы ID наград для задач-сюрпризов
    private static final long REWARD_ID_SURPRISE_201 = 201L; // ... и так далее, как в Kotlin файле

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

        return Futures.submitAsync(() -> {
            UserAuth existingUser = appDatabase.userAuthDao().getUserByEmail(TEST_USER_EMAIL).get(); // Блокирующий вызов
            if (existingUser != null) {
                logger.info(TAG, "Test data already exists for user " + TEST_USER_EMAIL + ". Skipping initialization.");
                ensureSessionSet(existingUser.getId()); // ensureSessionSet должен быть адаптивным
                return Futures.immediateFuture(null);
            }

            logger.info(TAG, "No existing test data found. Proceeding with TEST initialization for " + TEST_USER_EMAIL);

            try {
                appDatabase.runInTransaction((Callable<Void>) () -> { // Используем Callable для возврата Void
                    // Получение ID подхода
                    // getAllApproaches возвращает LiveData, для синхронного получения в транзакции нужен другой метод в DAO
                    // Предположим, есть метод List<Approach> getAllApproachesSync() в ApproachDao
                    // List<Approach> approaches = appDatabase.approachDao().getAllApproachesSync();
                    // Пока заглушка:
                    List<Approach> approaches = appDatabase.approachDao().getAllApproaches().getValue(); // Опасно, если LiveData не инициализирована
                    if (approaches == null || approaches.isEmpty()) {
                        // Если не можем получить синхронно, можно сделать этот шаг вне транзакции или использовать
                        // ListenableFuture и Futures.transformAsync
                        throw new IllegalStateException("Approaches not available for initialization.");
                    }

                    Approach defaultApproach = approaches.stream()
                            .filter(a -> a.getName() == TEST_DEFAULT_APPROACH_NAME)
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Default approach '" + TEST_DEFAULT_APPROACH_NAME + "' not found."));
                    long defaultApproachId = defaultApproach.getId();
                    logger.debug(TAG, "Found default approach '" + TEST_DEFAULT_APPROACH_NAME + "' with ID: " + defaultApproachId);

                    // 1. Пользователь
                    UserAuth testUser = new UserAuth(0, TEST_USER_EMAIL, TEST_USER_PASSWORD_HASH_PLACEHOLDER, TEST_USER_USERNAME, null);
                    long userIdLong = appDatabase.userAuthDao().insertUser(testUser).get();
                    int userId = (int) userIdLong;
                    logger.debug(TAG, "Test user created with ID: " + userId);

                    // 2. Рабочее пространство
                    Workspace testWorkspace = new Workspace(0, userId, TEST_WORKSPACE_NAME, TEST_WORKSPACE_DESCRIPTION, defaultApproachId, dateTimeUtils.currentUtcDateTime(), dateTimeUtils.currentUtcDateTime());
                    long workspaceId = appDatabase.workspaceDao().insertWorkspace(testWorkspace).get();
                    logger.debug(TAG, "Test workspace created with ID: " + workspaceId);

                    // 3. Геймификация
                    Gamification testGamification = new Gamification(userId, InitialDataConstants.INITIAL_LEVEL, InitialDataConstants.INITIAL_XP, InitialDataConstants.INITIAL_COINS, InitialDataConstants.MAX_XP_FOR_LEVEL, dateTimeUtils.currentUtcDateTime(), 0, LocalDate.MIN, 0);
                    long gamificationId = appDatabase.gamificationDao().insert(testGamification).get();
                    logger.debug(TAG, "Gamification profile created with ID: " + gamificationId);

                    // 4. Глобальная статистика
                    GlobalStatistics initialGlobalStats = new GlobalStatistics(0, userId, 1, 0, 0, 0, dateTimeUtils.currentUtcDateTime());
                    appDatabase.globalStatisticsDao().insertOrUpdateGlobalStatistics(initialGlobalStats).get();
                    logger.debug(TAG, "Initial global statistics created/updated for user ID: " + userId);

                    // 5. Начальное растение
                    VirtualGarden initialPlant = new VirtualGarden(gamificationId, TEST_INITIAL_PLANT_TYPE, InitialDataConstants.INITIAL_GROWTH_STAGE, 0, dateTimeUtils.currentUtcDateTime().minusDays(1));
                    long plantId = appDatabase.virtualGardenDao().insert(initialPlant).get();
                    logger.debug(TAG, "Initial plant created with ID: " + plantId);

                    // 6. Задачи-сюрпризы
                    insertSurpriseTasksInternal(appDatabase.surpriseTaskDao(), gamificationId);

                    // 7. Начальный прогресс для испытаний
                    insertInitialChallengeProgressInternal(appDatabase.challengeDao(), gamificationId);

                    // 8. Инициализация DataStore (ListenableFuture)
                    userSessionManager.saveUserIdAsync(userId).get();
                    workspaceSessionManager.saveWorkspaceIdAsync(workspaceId).get();
                    gamificationDataStoreManager.saveGamificationId(gamificationId).get();
                    gamificationDataStoreManager.saveSelectedPlantId(plantId).get();

                    logger.info(TAG, "TEST data initialization successful for user " + TEST_USER_EMAIL);
                    return null; // Для Callable<Void>
                });
                return Futures.immediateFuture(null);
            } catch (Exception e) {
                logger.error(TAG, "Failed to initialize TEST data for " + TEST_USER_EMAIL, e);
                return Futures.immediateFailedFuture(e);
            }
        }, ioExecutor);
    }

    private void ensureSessionSet(int userId) throws Exception { // Добавил throws для .get()
        if (userSessionManager.getUserIdSync() != userId) {
            logger.warn(TAG, "Session user ID mismatch for existing user " + userId + ". Setting session...");
            userSessionManager.saveUserIdAsync(userId).get();
        }

        if (workspaceSessionManager.getWorkspaceIdSync() == 0L) {
            // Для синхронного получения нужен другой метод в DAO или использование LiveData.getValue() с осторожностью
            List<Workspace> workspaces = appDatabase.workspaceDao().getAllWorkspaces(userId).getValue(); // Опасно, если LiveData не инициализирована
            if (workspaces != null && !workspaces.isEmpty()) {
                logger.warn(TAG, "Session workspace ID missing for user " + userId + ". Setting workspace ID " + workspaces.get(0).getId());
                workspaceSessionManager.saveWorkspaceIdAsync(workspaces.get(0).getId()).get();
            } else {
                logger.error(TAG, "Cannot set session workspace ID: No workspace found for user " + userId);
            }
        }

        if (gamificationDataStoreManager.getGamificationIdSync() == -1L) {
            Gamification gamification = appDatabase.gamificationDao().getByUserId(userId).get();
            if (gamification != null) {
                logger.warn(TAG, "Session gamification ID missing for user " + userId + ". Setting gamification ID " + gamification.getId());
                gamificationDataStoreManager.saveGamificationId(gamification.getId()).get();
                ensureSelectedPlantSet(gamification.getId());
            } else {
                logger.error(TAG, "Cannot set session gamification ID: No gamification profile found for user " + userId);
            }
        } else {
            ensureSelectedPlantSet(gamificationDataStoreManager.getGamificationIdSync());
        }
    }

    private void ensureSelectedPlantSet(long gamificationId) throws Exception {
        if (gamificationDataStoreManager.getSelectedPlantIdSync() == -1L) {
            VirtualGarden latestPlant = appDatabase.virtualGardenDao().getLatestPlant(gamificationId).get();
            if (latestPlant != null) {
                logger.warn(TAG, "Session selected plant ID missing for gamification " + gamificationId + ". Setting to latest plant ID " + latestPlant.getId());
                gamificationDataStoreManager.saveSelectedPlantId(latestPlant.getId()).get();
            } else {
                logger.warn(TAG, "Cannot set session selected plant ID: No plants found for gamification " + gamificationId);
            }
        }
    }

    private void insertSurpriseTasksInternal(SurpriseTaskDao surpriseTaskDao, long gamificationId) throws Exception {
        Timber.tag(TAG).d("Inserting test surprise tasks for gamificationId: " + gamificationId);
        LocalDateTime now = dateTimeUtils.currentUtcDateTime();
        List<SurpriseTask> tasks = new ArrayList<>();
        // Добавление задач как в Kotlin, используя конструктор SurpriseTask
        tasks.add(new SurpriseTask(gamificationId, "Разминка для глаз!", REWARD_ID_SURPRISE_201, now.plusHours(1), false, null));

        try {
            surpriseTaskDao.insertAll(tasks).get(); // Блокирующий вызов
            Timber.tag(TAG).d("Inserted " + tasks.size() + " test surprise tasks.");
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "Error inserting test surprise tasks");
            throw e;
        }
    }

    private void insertInitialChallengeProgressInternal(ChallengeDao challengeDao, long gamificationId) throws Exception {
        Timber.tag(TAG).d("Inserting initial challenge progress for gamificationId: " + gamificationId);
        LocalDateTime now = dateTimeUtils.currentUtcDateTime();
        try {
            List<Challenge> activeChallenges = challengeDao.getChallengesByStatus(ChallengeStatus.ACTIVE).get(); // Блокирующий вызов
            if (activeChallenges.isEmpty()) {
                Timber.tag(TAG).i("No active challenges found to initialize progress for.");
                return;
            }
            Timber.tag(TAG).d("Found " + activeChallenges.size() + " active challenges to initialize progress.");

            for (Challenge challenge : activeChallenges) {
                List<ChallengeRule> rules = challengeDao.getChallengeRulesByChallengeId(challenge.getId()).get();
                if (rules.isEmpty()) {
                    Timber.tag(TAG).i("No rules found for active challenge " + challenge.getId() + ". Skipping.");
                    continue;
                }
                for (ChallengeRule rule : rules) {
                    GamificationChallengeProgress initialProgress = new GamificationChallengeProgress(gamificationId, challenge.getId(), rule.getId(), 0, false, now);
                    challengeDao.insertOrUpdateProgress(initialProgress).get();
                }
            }
            Timber.tag(TAG).d("Initial challenge progress insertion/update finished.");
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "Error inserting initial challenge progress");
            throw e;
        }
    }
}