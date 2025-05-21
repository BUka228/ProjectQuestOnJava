package com.example.projectquestonjava.core.data.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.projectquestonjava.approach.calendar.data.dao.CalendarTaskDao;
import com.example.projectquestonjava.approach.calendar.data.model.CalendarParams;
import com.example.projectquestonjava.core.data.converters.Converters;
import com.example.projectquestonjava.core.data.dao.ApproachDao;
import com.example.projectquestonjava.core.data.dao.TagDao;
import com.example.projectquestonjava.core.data.dao.TaskDao;
import com.example.projectquestonjava.core.data.dao.TaskTagCrossRefDao;
import com.example.projectquestonjava.core.data.model.core.Approach;
import com.example.projectquestonjava.core.data.model.core.TaskTagCrossRef;
import com.example.projectquestonjava.core.data.model.core.UserAuth;
import com.example.projectquestonjava.core.data.model.core.Workspace;
import com.example.projectquestonjava.feature.statistics.data.dao.TaskStatisticsDao;
import com.example.projectquestonjava.feature.statistics.data.model.TaskStatistics;

import okhttp3.Challenge;


@Database(
        entities = {
                // Core Entities
                UserAuth.class,
                Workspace.class,
                Approach.class,
                Task.class,
                Tag.class,
                TaskTagCrossRef.class,
                SubtaskRelation.class,
                PublicCommitment.class,
                Witness.class,
                // Params Entities
                GTDParams.class,
                EisenhowerParams.class,
                FrogParams.class,
                CalendarParams.class,
                // Gamification Entities
                Gamification.class,
                Badge.class,
                GamificationBadgeCrossRef.class,
                Challenge.class,
                ChallengeRule.class,
                GamificationChallengeProgress.class,
                Reward.class,
                StreakRewardDefinition.class,
                SurpriseTask.class,
                VirtualGarden.class,
                StoreItem.class,
                GamificationStorePurchase.class,
                // Statistics Entities
                TaskStatistics.class,
                WorkspaceStatistics.class,
                GlobalStatistics.class,
                PomodoroSession.class,
                TaskHistory.class,
                GamificationHistory.class
        },
        version = 16, // Убедитесь, что версия актуальна
        exportSchema = false
)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {
    // Core DAO
    public abstract UserAuthDao userAuthDao();
    public abstract WorkspaceDao workspaceDao();
    public abstract ApproachDao approachDao();
    public abstract TaskDao taskDao();
    public abstract TagDao tagDao();
    public abstract TaskTagCrossRefDao taskTagCrossRefDao();
    public abstract SubtaskRelationDao subtaskRelationDao();
    public abstract PublicCommitmentDao publicCommitmentDao();
    public abstract WitnessDao witnessDao();
    // Params DAO
    public abstract CalendarTaskDao calendarTaskDao();
    public abstract GTDParamsDao gtdParamsDao();
    public abstract EisenhowerParamsDao eisenhowerParamsDao();
    public abstract FrogParamsDao frogParamsDao();
    // Gamification DAO
    public abstract GamificationDao gamificationDao();
    public abstract BadgeDao badgeDao();
    public abstract GamificationBadgeCrossRefDao gamificationBadgeCrossRefDao();
    public abstract ChallengeDao challengeDao();
    public abstract RewardDao rewardDao();
    public abstract StreakRewardDefinitionDao streakRewardDefinitionDao();
    public abstract SurpriseTaskDao surpriseTaskDao();
    public abstract VirtualGardenDao virtualGardenDao();
    public abstract StoreItemDao storeItemDao();
    public abstract GamificationStorePurchaseDao gamificationStorePurchaseDao();
    // Statistics DAO
    public abstract TaskStatisticsDao taskStatisticsDao();
    public abstract WorkspaceStatisticsDao workspaceStatisticsDao();
    public abstract GlobalStatisticsDao globalStatisticsDao();
    public abstract PomodoroSessionDao pomodoroSessionDao();
    public abstract TaskHistoryDao taskHistoryDao();
    public abstract GamificationHistoryDao gamificationHistoryDao();

}