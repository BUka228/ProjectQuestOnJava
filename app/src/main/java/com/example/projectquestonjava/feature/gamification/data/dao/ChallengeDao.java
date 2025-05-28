package com.example.projectquestonjava.feature.gamification.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import com.example.projectquestonjava.feature.gamification.data.model.Challenge;
import com.example.projectquestonjava.feature.gamification.data.model.ChallengeRule;
import com.example.projectquestonjava.feature.gamification.data.model.GamificationChallengeProgress;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeProgressFullDetails;
import com.example.projectquestonjava.feature.gamification.domain.model.ChallengeStatus;
import com.google.common.util.concurrent.ListenableFuture;
import java.time.LocalDateTime;
import java.util.List;

@Dao
public interface ChallengeDao {

    @Query("SELECT * FROM Challenge WHERE id = :challengeId")
    ListenableFuture<Challenge> getChallengeById(long challengeId);

    @Query("SELECT * FROM Challenge WHERE status = :status ORDER BY end_date ASC")
    LiveData<List<Challenge>> getChallengesByStatusFlow(ChallengeStatus status);

    @Query("SELECT * FROM Challenge WHERE status = :status ORDER BY end_date ASC")
    ListenableFuture<List<Challenge>> getChallengesByStatus(ChallengeStatus status);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Long> insertChallenge(Challenge challenge);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Void> insertAllChallenges(List<Challenge> challenges);

    @Update
    ListenableFuture<Integer> updateChallenge(Challenge challenge);

    @Query("UPDATE Challenge SET status = :status WHERE id = :challengeId")
    ListenableFuture<Integer> updateChallengeStatus(long challengeId, ChallengeStatus status);

    @Delete
    ListenableFuture<Integer> deleteChallenge(Challenge challenge);

    @Query("SELECT * FROM challenge_rule WHERE challenge_id = :challengeId")
    ListenableFuture<List<ChallengeRule>> getChallengeRulesByChallengeId(long challengeId);

    @Query("SELECT * FROM challenge_rule WHERE id = :ruleId")
    ListenableFuture<ChallengeRule> getRuleById(long ruleId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Long> insertRule(ChallengeRule rule);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Void> insertAllRules(List<ChallengeRule> rules);

    @Update
    ListenableFuture<Integer> updateRule(ChallengeRule rule);

    @Query("DELETE FROM challenge_rule WHERE challenge_id = :challengeId")
    ListenableFuture<Integer> deleteRulesForChallenge(long challengeId);

    @Query("SELECT * FROM gamification_challenge_progress WHERE gamification_id = :gamificationId AND challenge_id = :challengeId AND rule_id = :ruleId")
    ListenableFuture<GamificationChallengeProgress> getProgress(long gamificationId, long challengeId, long ruleId);

    @Query("SELECT * FROM gamification_challenge_progress WHERE gamification_id = :gamificationId AND challenge_id = :challengeId")
    ListenableFuture<List<GamificationChallengeProgress>> getAllProgressForChallenge(long gamificationId, long challengeId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Void> insertOrUpdateProgress(GamificationChallengeProgress progress);

    @Transaction
    @Query("SELECT * FROM gamification_challenge_progress WHERE gamification_id = :gamificationId ORDER BY challenge_id, rule_id")
    LiveData<List<ChallengeProgressFullDetails>> getAllChallengesWithProgressFlow(long gamificationId);

    @Delete
    ListenableFuture<Integer> deleteProgress(GamificationChallengeProgress progress);

    @Query("UPDATE gamification_challenge_progress SET progress = 0, is_completed = 0 WHERE gamification_id = :gamificationId AND rule_id IN (SELECT id FROM challenge_rule WHERE period = :period) AND last_updated < :resetThreshold")
    ListenableFuture<Integer> resetPeriodicProgress(long gamificationId, String period, LocalDateTime resetThreshold);

    @Query("DELETE FROM gamification_challenge_progress WHERE gamification_id = :gamificationId")
    ListenableFuture<Integer> deleteProgressForGamification(long gamificationId);

    @Query("DELETE FROM gamification_challenge_progress WHERE challenge_id = :challengeId")
    ListenableFuture<Integer> deleteProgressForChallenge(long challengeId);


    @Query("SELECT * FROM Challenge WHERE status = :status ORDER BY end_date ASC")
    List<Challenge> getChallengesByStatusSync(ChallengeStatus status);

    @Query("SELECT * FROM challenge_rule WHERE challenge_id = :challengeId")
    List<ChallengeRule> getChallengeRulesByChallengeIdSync(long challengeId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateProgressSync(GamificationChallengeProgress progress);





    @Query("SELECT * FROM Challenge WHERE id = :challengeId")
    Challenge getChallengeByIdSync(long challengeId); // НОВЫЙ


    @Query("SELECT * FROM gamification_challenge_progress WHERE gamification_id = :gamificationId AND challenge_id = :challengeId AND rule_id = :ruleId")
    GamificationChallengeProgress getProgressSync(long gamificationId, long challengeId, long ruleId); // НОВЫЙ

    @Query("SELECT * FROM gamification_challenge_progress WHERE gamification_id = :gamificationId AND challenge_id = :challengeId")
    List<GamificationChallengeProgress> getAllProgressForChallengeSync(long gamificationId, long challengeId); // НОВЫЙ

    @Query("UPDATE Challenge SET status = :status WHERE id = :challengeId")
    int updateChallengeStatusSync(long challengeId, ChallengeStatus status);

}