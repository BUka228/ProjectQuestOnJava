package com.example.projectquestonjava.core.di;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.example.projectquestonjava.core.data.dao.*; // Импорт всех DAO из core
import com.example.projectquestonjava.core.data.dao.commitment.*; // DAO для commitment
import com.example.projectquestonjava.approach.calendar.data.dao.CalendarTaskDao;
import com.example.projectquestonjava.approach.eatTheFrog.data.dao.FrogParamsDao;
import com.example.projectquestonjava.approach.eisenhower.data.dao.EisenhowerParamsDao;
import com.example.projectquestonjava.approach.gtd.data.dao.GTDParamsDao;
import com.example.projectquestonjava.core.data.database.AppDatabase;
import com.example.projectquestonjava.core.data.initializers.DatabaseInitializer;
// Импорты DAO для gamification, pomodoro, statistics
import com.example.projectquestonjava.feature.gamification.data.dao.*;
import com.example.projectquestonjava.feature.pomodoro.data.dao.PomodoroSessionDao;
import com.example.projectquestonjava.feature.statistics.data.dao.*;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import java.util.concurrent.Executor; // Для callbackExecutor
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    @Provides
    @Singleton
    public AppDatabase provideAppDatabase(
            @ApplicationContext Context context,
            DatabaseInitializer initializer,
            @IODispatcher Executor ioExecutor // Для Room callbackExecutor
    ) {
        return Room.databaseBuilder(context, AppDatabase.class, "app_main.db")
                .addCallback(new RoomDatabase.Callback() {
                    @Override
                    public void onCreate(@NonNull SupportSQLiteDatabase db) {
                        super.onCreate(db);
                        // Выполняем инициализацию на IO потоке, чтобы не блокировать основной
                        ioExecutor.execute(() -> initializer.initialize(db));
                    }
                    // Можно добавить onOpen, onDestructiveMigration и т.д. по необходимости
                })
                // .addMigrations(MIGRATION_X_Y, ...) // <-- НЕ ЗАБУДЬ ДОБАВИТЬ МИГРАЦИИ
                .setQueryExecutor(ioExecutor) // Используем наш IO Executor для запросов
                .setTransactionExecutor(ioExecutor) // И для транзакций
                .build();
    }

    @Provides
    @Singleton // DatabaseInitializer не имеет зависимостей, можно сделать его @Singleton
    public DatabaseInitializer provideDatabaseInitializer() {
        return new DatabaseInitializer();
    }

    // --- Провайдеры для DAO ---
    @Provides @Singleton public UserAuthDao provideUserAuthDao(AppDatabase db) { return db.userAuthDao(); }
    @Provides @Singleton public WorkspaceDao provideWorkspaceDao(AppDatabase db) { return db.workspaceDao(); }
    @Provides @Singleton public ApproachDao provideApproachDao(AppDatabase db) { return db.approachDao(); }
    @Provides @Singleton public TaskDao provideTaskDao(AppDatabase db) { return db.taskDao(); }
    @Provides @Singleton public TagDao provideTagDao(AppDatabase db) { return db.tagDao(); }
    @Provides @Singleton public TaskTagCrossRefDao provideTaskTagCrossRefDao(AppDatabase db) { return db.taskTagCrossRefDao(); }
    @Provides @Singleton public SubtaskRelationDao provideSubtaskRelationDao(AppDatabase db) { return db.subtaskRelationDao(); }
    @Provides @Singleton public PublicCommitmentDao providePublicCommitmentDao(AppDatabase db) { return db.publicCommitmentDao(); }
    @Provides @Singleton public WitnessDao provideWitnessDao(AppDatabase db) { return db.witnessDao(); }
    // Params DAO
    @Provides @Singleton public GTDParamsDao provideGtdParamsDao(AppDatabase db) { return db.gtdParamsDao(); }
    @Provides @Singleton public EisenhowerParamsDao provideEisenhowerParamsDao(AppDatabase db) { return db.eisenhowerParamsDao(); }
    @Provides @Singleton public FrogParamsDao provideFrogParamsDao(AppDatabase db) { return db.frogParamsDao(); }
    // Gamification DAO
    @Provides @Singleton public GamificationDao provideGamificationDao(AppDatabase db) { return db.gamificationDao(); }
    @Provides @Singleton public BadgeDao provideBadgeDao(AppDatabase db) { return db.badgeDao(); }
    @Provides @Singleton public GamificationBadgeCrossRefDao provideGamificationBadgeCrossRefDao(AppDatabase db) { return db.gamificationBadgeCrossRefDao(); }
    @Provides @Singleton public ChallengeDao provideChallengeDao(AppDatabase db) { return db.challengeDao(); }
    @Provides @Singleton public RewardDao provideRewardDao(AppDatabase db) { return db.rewardDao(); }
    @Provides @Singleton public StreakRewardDefinitionDao provideStreakRewardDefinitionDao(AppDatabase db) { return db.streakRewardDefinitionDao(); }
    @Provides @Singleton public SurpriseTaskDao provideSurpriseTaskDao(AppDatabase db) { return db.surpriseTaskDao(); }
    @Provides @Singleton public VirtualGardenDao provideVirtualGardenDao(AppDatabase db) { return db.virtualGardenDao(); }
    @Provides @Singleton public StoreItemDao provideStoreItemDao(AppDatabase db) { return db.storeItemDao(); }
    @Provides @Singleton public GamificationStorePurchaseDao provideGamificationStorePurchaseDao(AppDatabase db) { return db.gamificationStorePurchaseDao(); }
    // Statistics DAO
    @Provides @Singleton public TaskStatisticsDao provideTaskStatisticsDao(AppDatabase db) { return db.taskStatisticsDao(); }
    @Provides @Singleton public WorkspaceStatisticsDao provideWorkspaceStatisticsDao(AppDatabase db) { return db.workspaceStatisticsDao(); }
    @Provides @Singleton public GlobalStatisticsDao provideGlobalStatisticsDao(AppDatabase db) { return db.globalStatisticsDao(); }
    @Provides @Singleton public PomodoroSessionDao providePomodoroSessionDao(AppDatabase db) { return db.pomodoroSessionDao(); }
    @Provides @Singleton public TaskHistoryDao provideTaskHistoryDao(AppDatabase db) { return db.taskHistoryDao(); }
    @Provides @Singleton public GamificationHistoryDao provideGamificationHistoryDao(AppDatabase db) { return db.gamificationHistoryDao(); }
}