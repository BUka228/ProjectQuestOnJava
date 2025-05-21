package com.example.projectquestonjava.feature.statistics.di;

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
public abstract class StatisticsRepositoryModule {

    @Binds
    @Singleton
    public abstract TaskStatisticsRepository bindTaskStatisticsRepository(TaskStatisticsRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract GamificationHistoryRepository bindGamificationHistoryRepository(GamificationHistoryRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract GlobalStatisticsRepository bindGlobalStatisticsRepository(GlobalStatisticsRepositoryImpl impl);
}