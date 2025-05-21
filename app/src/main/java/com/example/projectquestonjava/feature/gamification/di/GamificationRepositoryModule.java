package com.example.projectquestonjava.feature.gamification.di;

import com.example.projectquestonjava.feature.gamification.data.repository.BadgeRepositoryImpl;
import com.example.projectquestonjava.feature.gamification.data.repository.ChallengeRepositoryImpl;
import com.example.projectquestonjava.feature.gamification.data.repository.DailyRewardRepositoryImpl;
import com.example.projectquestonjava.feature.gamification.data.repository.GamificationRepositoryImpl;
import com.example.projectquestonjava.feature.gamification.data.repository.RewardRepositoryImpl;
import com.example.projectquestonjava.feature.gamification.data.repository.StreakRewardDefinitionRepositoryImpl;
import com.example.projectquestonjava.feature.gamification.data.repository.SurpriseTaskRepositoryImpl;
import com.example.projectquestonjava.feature.gamification.data.repository.VirtualGardenRepositoryImpl;
import com.example.projectquestonjava.feature.gamification.domain.repository.BadgeRepository;
import com.example.projectquestonjava.feature.gamification.domain.repository.ChallengeRepository;
import com.example.projectquestonjava.feature.gamification.domain.repository.DailyRewardRepository;
import com.example.projectquestonjava.feature.gamification.domain.repository.GamificationRepository;
import com.example.projectquestonjava.feature.gamification.domain.repository.RewardRepository;
import com.example.projectquestonjava.feature.gamification.domain.repository.StreakRewardDefinitionRepository;
import com.example.projectquestonjava.feature.gamification.domain.repository.SurpriseTaskRepository;
import com.example.projectquestonjava.feature.gamification.domain.repository.VirtualGardenRepository;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public abstract class GamificationRepositoryModule {

    @Binds
    @Singleton
    public abstract GamificationRepository bindGamificationRepository(GamificationRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract BadgeRepository bindBadgeRepository(BadgeRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract ChallengeRepository bindChallengeRepository(ChallengeRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract DailyRewardRepository bindDailyRewardRepository(DailyRewardRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract SurpriseTaskRepository bindSurpriseTaskRepository(SurpriseTaskRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract VirtualGardenRepository bindVirtualGardenRepository(VirtualGardenRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract StreakRewardDefinitionRepository bindStreakRewardDefinitionRepository(StreakRewardDefinitionRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract RewardRepository bindRewardRepository(RewardRepositoryImpl impl);
}