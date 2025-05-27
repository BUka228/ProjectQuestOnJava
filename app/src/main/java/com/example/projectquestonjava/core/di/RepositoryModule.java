package com.example.projectquestonjava.core.di;

import com.example.projectquestonjava.approach.calendar.data.repositories.CalendarParamsRepositoryImpl;
import com.example.projectquestonjava.approach.calendar.data.repositories.CalendarRepositoryImpl;
import com.example.projectquestonjava.approach.calendar.domain.repository.CalendarParamsRepository;
import com.example.projectquestonjava.approach.calendar.domain.repository.CalendarRepository;
import com.example.projectquestonjava.core.data.repositories.PriorityResolverImpl;
import com.example.projectquestonjava.core.data.repositories.TaskRepositoryImpl;
import com.example.projectquestonjava.core.data.repositories.TaskTagRepositoryImpl;
import com.example.projectquestonjava.core.data.repositories.UserAuthRepositoryImpl;
import com.example.projectquestonjava.core.domain.repository.PriorityResolver;
import com.example.projectquestonjava.core.domain.repository.TaskRepository;
import com.example.projectquestonjava.core.domain.repository.TaskTagRepository;
import com.example.projectquestonjava.core.domain.repository.UserAuthRepository;
import com.example.projectquestonjava.feature.gamification.data.repository.*;
import com.example.projectquestonjava.feature.gamification.domain.repository.*;
import com.example.projectquestonjava.feature.pomodoro.data.repository.PomodoroSessionRepositoryImpl;
import com.example.projectquestonjava.feature.pomodoro.data.repository.PomodoroSettingsRepositoryImpl;
import com.example.projectquestonjava.feature.pomodoro.domain.repository.PomodoroSessionRepository;
import com.example.projectquestonjava.feature.pomodoro.domain.repository.PomodoroSettingsRepository;
import com.example.projectquestonjava.feature.statistics.data.repository.GamificationHistoryRepositoryImpl;
import com.example.projectquestonjava.feature.statistics.data.repository.GlobalStatisticsRepositoryImpl;
import com.example.projectquestonjava.feature.statistics.data.repository.TaskStatisticsRepositoryImpl;
import com.example.projectquestonjava.feature.statistics.domain.repository.GamificationHistoryRepository;
import com.example.projectquestonjava.feature.statistics.domain.repository.GlobalStatisticsRepository;
import com.example.projectquestonjava.feature.statistics.domain.repository.TaskStatisticsRepository;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public abstract class RepositoryModule {

    // Core Repositories
    @Binds @Singleton public abstract TaskRepository bindTaskRepository(TaskRepositoryImpl impl);
    @Binds @Singleton public abstract TaskTagRepository bindTaskTagRepository(TaskTagRepositoryImpl impl);
    @Binds @Singleton public abstract UserAuthRepository bindUserAuthRepository(UserAuthRepositoryImpl impl);


}