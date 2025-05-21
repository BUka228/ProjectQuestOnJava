package com.example.projectquestonjava.core.data.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

// Импорты всех ваших Java Entities (убедитесь, что они созданы)
import com.example.projectquestonjava.approach.calendar.data.model.CalendarParams;
import com.example.projectquestonjava.approach.eatTheFrog.data.model.FrogParams;
import com.example.projectquestonjava.approach.eisenhower.data.model.EisenhowerParams;
import com.example.projectquestonjava.approach.gtd.data.model.GTDParams;
import com.example.projectquestonjava.core.data.converters.Converters;
import com.example.projectquestonjava.core.data.model.commitment.PublicCommitment;
import com.example.projectquestonjava.core.data.model.commitment.Witness;
import com.example.projectquestonjava.core.data.model.core.Approach;
import com.example.projectquestonjava.core.data.model.core.SubtaskRelation;
import com.example.projectquestonjava.core.data.model.core.Tag;
import com.example.projectquestonjava.core.data.model.core.Task;
import com.example.projectquestonjava.core.data.model.core.TaskTagCrossRef;
import com.example.projectquestonjava.core.data.model.core.UserAuth;
import com.example.projectquestonjava.core.data.model.core.Workspace;
import com.example.projectquestonjava.feature.gamification.data.model.Badge;
import com.example.projectquestonjava.feature.gamification.data.model.Challenge;
import com.example.projectquestonjava.feature.gamification.data.model.ChallengeRule;
import com.example.projectquestonjava.feature.gamification.data.model.Gamification;
import com.example.projectquestonjava.feature.gamification.data.model.GamificationBadgeCrossRef;
import com.example.projectquestonjava.feature.gamification.data.model.GamificationChallengeProgress;
import com.example.projectquestonjava.feature.gamification.data.model.GamificationStorePurchase;
import com.example.projectquestonjava.feature.gamification.data.model.Reward;
import com.example.projectquestonjava.feature.gamification.data.model.StoreItem;
import com.example.projectquestonjava.feature.gamification.data.model.StreakRewardDefinition;
import com.example.projectquestonjava.feature.gamification.data.model.SurpriseTask;
import com.example.projectquestonjava.feature.gamification.data.model.VirtualGarden;
import com.example.projectquestonjava.feature.pomodoro.data.model.PomodoroSession;
import com.example.projectquestonjava.feature.statistics.data.model.GamificationHistory;
import com.example.projectquestonjava.feature.statistics.data.model.GlobalStatistics;
import com.example.projectquestonjava.feature.statistics.data.model.TaskHistory;
import com.example.projectquestonjava.feature.statistics.data.model.TaskStatistics;
import com.example.projectquestonjava.feature.statistics.data.model.WorkspaceStatistics;

// Импорты всех ваших Java DAO
import com.example.projectquestonjava.approach.calendar.data.dao.CalendarTaskDao;
import com.example.projectquestonjava.approach.eatTheFrog.data.dao.FrogParamsDao;
import com.example.projectquestonjava.approach.eisenhower.data.dao.EisenhowerParamsDao;
import com.example.projectquestonjava.approach.gtd.data.dao.GTDParamsDao;
import com.example.projectquestonjava.core.data.dao.ApproachDao;
import com.example.projectquestonjava.core.data.dao.SubtaskRelationDao;
import com.example.projectquestonjava.core.data.dao.TagDao;
import com.example.projectquestonjava.core.data.dao.TaskDao;
import com.example.projectquestonjava.core.data.dao.TaskTagCrossRefDao;
import com.example.projectquestonjava.core.data.dao.UserAuthDao;
import com.example.projectquestonjava.core.data.dao.WorkspaceDao;
import com.example.projectquestonjava.core.data.dao.commitment.PublicCommitmentDao;
import com.example.projectquestonjava.core.data.dao.commitment.WitnessDao;
import com.example.projectquestonjava.feature.gamification.data.dao.BadgeDao;
import com.example.projectquestonjava.feature.gamification.data.dao.ChallengeDao;
import com.example.projectquestonjava.feature.gamification.data.dao.GamificationBadgeCrossRefDao;
import com.example.projectquestonjava.feature.gamification.data.dao.GamificationDao;
import com.example.projectquestonjava.feature.gamification.data.dao.GamificationStorePurchaseDao;
import com.example.projectquestonjava.feature.gamification.data.dao.RewardDao;
import com.example.projectquestonjava.feature.gamification.data.dao.StoreItemDao;
import com.example.projectquestonjava.feature.gamification.data.dao.StreakRewardDefinitionDao;
import com.example.projectquestonjava.feature.gamification.data.dao.SurpriseTaskDao;
import com.example.projectquestonjava.feature.gamification.data.dao.VirtualGardenDao;
import com.example.projectquestonjava.feature.pomodoro.data.dao.PomodoroSessionDao;
import com.example.projectquestonjava.feature.statistics.data.dao.GamificationHistoryDao;
import com.example.projectquestonjava.feature.statistics.data.dao.GlobalStatisticsDao;
import com.example.projectquestonjava.feature.statistics.data.dao.TaskHistoryDao;
import com.example.projectquestonjava.feature.statistics.data.dao.TaskStatisticsDao;
import com.example.projectquestonjava.feature.statistics.data.dao.WorkspaceStatisticsDao;


@Database(
        entities = {
                // Core Entities
                UserAuth.class, Workspace.class, Approach.class, Task.class, Tag.class,
                TaskTagCrossRef.class, SubtaskRelation.class, PublicCommitment.class, Witness.class,
                // Params Entities
                GTDParams.class, EisenhowerParams.class, FrogParams.class, CalendarParams.class,
                // Gamification Entities
                Gamification.class, Badge.class, GamificationBadgeCrossRef.class, Challenge.class,
                ChallengeRule.class, GamificationChallengeProgress.class, Reward.class,
                StreakRewardDefinition.class, SurpriseTask.class, VirtualGarden.class,
                StoreItem.class, GamificationStorePurchase.class,
                // Statistics Entities
                TaskStatistics.class, WorkspaceStatistics.class, GlobalStatistics.class,
                PomodoroSession.class, TaskHistory.class, GamificationHistory.class
        },
        version = 1,
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