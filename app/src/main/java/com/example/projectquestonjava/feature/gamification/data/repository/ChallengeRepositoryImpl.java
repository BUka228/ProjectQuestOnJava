package com.example.projectquestonjava.feature.gamification.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.data.dao.ChallengeDao;
import com.example.projectquestonjava.feature.gamification.data.managers.GamificationDataStoreManager;
import com.example.projectquestonjava.feature.gamification.data.model.Challenge;
import com.example.projectquestonjava.feature.gamification.data.model.ChallengeRule;
import com.example.projectquestonjava.feature.gamification.data.model.GamificationChallengeProgress;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeProgressFullDetails;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeStatus;
import com.example.projectquestonjava.feature.gamification.domain.repository.ChallengeRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ChallengeRepositoryImpl implements ChallengeRepository {

    private static final String TAG = "ChallengeRepositoryImpl";
    private final ChallengeDao challengeDao;
    private final GamificationDataStoreManager gamificationDataStoreManager;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public ChallengeRepositoryImpl(
            ChallengeDao challengeDao,
            GamificationDataStoreManager gamificationDataStoreManager,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.challengeDao = challengeDao;
        this.gamificationDataStoreManager = gamificationDataStoreManager;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    // --- Helper for executing DAO calls that depend on gamificationId ---
    private <T> ListenableFuture<T> executeWithGamificationId(GamificationIdFunction<T> function) {
        return Futures.transformAsync(
                gamificationDataStoreManager.getGamificationIdFuture(), // Получаем Future<Long>
                gamificationId -> {
                    if (gamificationId == null || gamificationId == -1L) {
                        logger.warn(TAG, "Gamification ID not found for operation.");
                        // Можно бросить исключение или вернуть Future с ошибкой/пустым значением
                        return Futures.immediateFailedFuture(new IllegalStateException("Gamification ID not found"));
                    }
                    return function.apply(gamificationId);
                },
                ioExecutor // Executor для transformAsync
        );
    }

    @FunctionalInterface
    private interface GamificationIdFunction<R> {
        ListenableFuture<R> apply(long gamificationId);
    }


    @Override
    public ListenableFuture<Challenge> getChallenge(long challengeId) {
        logger.debug(TAG, "Getting challenge definition id=" + challengeId);
        return challengeDao.getChallengeById(challengeId);
    }

    @Override
    public ListenableFuture<List<ChallengeRule>> getChallengeRules(long challengeId) {
        logger.debug(TAG, "Getting rules for challenge id=" + challengeId);
        return challengeDao.getChallengeRulesByChallengeId(challengeId);
    }

    @Override
    public ListenableFuture<ChallengeRule> getRule(long ruleId) {
        logger.debug(TAG, "Getting rule by id=" + ruleId);
        return challengeDao.getRuleById(ruleId);
    }

    @Override
    public ListenableFuture<Void> updateChallengeStatus(long challengeId, ChallengeStatus status) {
        logger.debug(TAG, "Updating status for challenge " + challengeId + " to " + status);
        return Futures.transform(challengeDao.updateChallengeStatus(challengeId, status), count -> null, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<GamificationChallengeProgress> getProgressForRule(long challengeId, long ruleId) {
        return executeWithGamificationId(gamiId -> {
            logger.debug(TAG, "Getting progress for rule " + ruleId + " (Challenge " + challengeId + ", GamiID " + gamiId + ")");
            return challengeDao.getProgress(gamiId, challengeId, ruleId);
        });
    }

    @Override
    public ListenableFuture<List<GamificationChallengeProgress>> getAllProgressForChallenge(long challengeId) {
        return executeWithGamificationId(gamiId -> {
            logger.debug(TAG, "Getting all progress for challenge " + challengeId + " (GamiID " + gamiId + ")");
            return challengeDao.getAllProgressForChallenge(gamiId, challengeId);
        });
    }

    @Override
    public ListenableFuture<Void> insertOrUpdateProgress(GamificationChallengeProgress progress) {
        // Проверка gamificationId должна быть здесь, перед вызовом DAO
        return executeWithGamificationId(currentGamificationId -> {
            if (progress.getGamificationId() != currentGamificationId) {
                logger.error(TAG, "Mismatch Gamification ID during progress update. Progress GamiID: " + progress.getGamificationId() + ", Current GamiID: " + currentGamificationId);
                return Futures.immediateFailedFuture(new IllegalStateException("Gamification ID mismatch"));
            }
            GamificationChallengeProgress progressToSave = new GamificationChallengeProgress(
                    progress.getGamificationId(),
                    progress.getChallengeId(),
                    progress.getRuleId(),
                    progress.getProgress(),
                    progress.isCompleted(),
                    LocalDateTime.now() // Обновляем lastUpdated
            );
            logger.debug(TAG, "Inserting/Updating progress: gamiId=" + progressToSave.getGamificationId() + ", challengeId=" + progressToSave.getChallengeId() + ", ruleId=" + progressToSave.getRuleId());
            return challengeDao.insertOrUpdateProgress(progressToSave);
        });
    }

    @Override
    public LiveData<List<Challenge>> getActiveChallengesFlow() {
        logger.debug(TAG, "Getting active challenges LiveData");
        // DAO возвращает LiveData<List<Challenge>> для определенного статуса.
        // Здесь gamificationId не нужен, так как определения челленджей общие.
        return challengeDao.getChallengesByStatusFlow(ChallengeStatus.ACTIVE);
    }

    @Override
    public ListenableFuture<List<Challenge>> getActiveChallenges() {
        logger.debug(TAG, "Getting active challenges future");
        return challengeDao.getChallengesByStatus(ChallengeStatus.ACTIVE);
    }

    @Override
    public LiveData<List<ChallengeProgressFullDetails>> getChallengesWithDetailsFlow(ChallengeStatus filterStatus) {
        logger.debug(TAG, "Getting challenges with details LiveData (filter: " + filterStatus + ")");
        return Transformations.switchMap(gamificationDataStoreManager.getGamificationIdFlow(), gamificationId -> {
            if (gamificationId == null || gamificationId == -1L) {
                logger.warn(TAG, "No gamification ID for challenges with details LiveData.");
                return new LiveData<List<ChallengeProgressFullDetails>>(Collections.emptyList()) {};
            }
            LiveData<List<ChallengeProgressFullDetails>> allDetails = challengeDao.getAllChallengesWithProgressFlow(gamificationId);
            if (filterStatus == null) {
                return allDetails;
            }
            return Transformations.map(allDetails, list -> {
                if (list == null) return Collections.emptyList();
                return list.stream()
                        .filter(details -> details.getChallengeAndReward().getChallenge().getStatus() == filterStatus)
                        .collect(Collectors.toList());
            });
        });
    }

    @Override
    public ListenableFuture<Void> resetPeriodicProgress(String period, LocalDateTime resetThreshold) {
        return executeWithGamificationId(gamiId -> {
            logger.debug(TAG, "Resetting " + period + " progress for gamiId=" + gamiId + " (before " + resetThreshold + ")");
            return Futures.transform(challengeDao.resetPeriodicProgress(gamiId, period, resetThreshold), count -> null, MoreExecutors.directExecutor());
        });
    }

    @Override
    public ListenableFuture<Void> deleteProgressForGamification(long gamificationId) {
        // Этот метод принимает gamificationId, поэтому не использует executeWithGamificationId
        logger.debug(TAG, "Deleting ALL challenge progress for gamificationId=" + gamificationId);
        return Futures.transform(challengeDao.deleteProgressForGamification(gamificationId), count -> null, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<Void> deleteProgressForChallenge(long challengeId) {
        logger.warn(TAG, "Deleting ALL progress entries for challengeId=" + challengeId);
        return Futures.transform(challengeDao.deleteProgressForChallenge(challengeId), count -> null, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<Void> deleteRulesForChallenge(long challengeId) {
        logger.debug(TAG, "Deleting rules for challengeId=" + challengeId);
        return Futures.transform(challengeDao.deleteRulesForChallenge(challengeId), count -> null, MoreExecutors.directExecutor());
    }

    @Override
    public List<Challenge> getActiveChallengesSync() {
        logger.debug(TAG, "SYNC Getting active challenges");
        return challengeDao.getChallengesByStatusSync(ChallengeStatus.ACTIVE); // DAO должен иметь getChallengesByStatusSync
    }

    @Override
    public Challenge getChallengeByIdSync(long challengeId) {
        logger.debug(TAG, "SYNC Getting challenge by id=" + challengeId);
        return challengeDao.getChallengeByIdSync(challengeId); // DAO должен иметь getChallengeByIdSync
    }

    @Override
    public List<ChallengeRule> getChallengeRulesSync(long challengeId) {
        logger.debug(TAG, "SYNC Getting rules for challenge id=" + challengeId);
        return challengeDao.getChallengeRulesByChallengeIdSync(challengeId); // DAO должен иметь getChallengeRulesByChallengeIdSync
    }

    @Override
    public GamificationChallengeProgress getProgressForRuleSync(long challengeId, long ruleId) throws IOException {
        long gamificationId = gamificationDataStoreManager.getGamificationIdSync(); // Получаем ID здесь
        if (gamificationId == -1L) {
            logger.warn(TAG, "SYNC Cannot get progress for rule: Gamification ID not found.");
            return null;
        }
        logger.debug(TAG, "SYNC Getting progress for rule " + ruleId + " (Challenge " + challengeId + ", GamiID " + gamificationId + ")");
        return challengeDao.getProgressSync(gamificationId, challengeId, ruleId); // DAO должен иметь getProgressSync
    }

    @Override
    public List<GamificationChallengeProgress> getAllProgressForChallengeSync(long gamificationId, long challengeId) {
        logger.debug(TAG, "SYNC Getting all progress for challenge " + challengeId + " (GamiID " + gamificationId + ")");
        return challengeDao.getAllProgressForChallengeSync(gamificationId, challengeId); // DAO должен иметь getAllProgressForChallengeSync
    }

    @Override
    public void insertOrUpdateProgressSync(GamificationChallengeProgress progress) throws IOException {
        long currentGamificationId = gamificationDataStoreManager.getGamificationIdSync();
        if (progress.getGamificationId() != currentGamificationId) {
            logger.error(TAG, "SYNC Mismatch Gamification ID during progress update. Progress GamiID: " + progress.getGamificationId() + ", Current GamiID: " + currentGamificationId);
            throw new IllegalStateException("Gamification ID mismatch");
        }
        GamificationChallengeProgress progressToSave = new GamificationChallengeProgress(
                progress.getGamificationId(), progress.getChallengeId(), progress.getRuleId(),
                progress.getProgress(), progress.isCompleted(), LocalDateTime.now()
        );
        logger.debug(TAG, "SYNC Inserting/Updating progress for rule " + progress.getRuleId());
        challengeDao.insertOrUpdateProgressSync(progressToSave); // DAO должен иметь insertOrUpdateProgressSync
    }

    @Override
    public void updateChallengeStatusSync(long challengeId, ChallengeStatus status) {
        logger.debug(TAG, "SYNC Updating status for challenge " + challengeId + " to " + status);
        challengeDao.updateChallengeStatusSync(challengeId, status); // DAO должен иметь updateChallengeStatusSync
    }
}