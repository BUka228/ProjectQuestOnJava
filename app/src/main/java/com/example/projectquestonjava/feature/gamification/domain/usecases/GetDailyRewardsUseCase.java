package com.example.projectquestonjava.feature.gamification.domain.usecases;

import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.gamification.data.model.Gamification;
import com.example.projectquestonjava.feature.gamification.data.model.Reward;
import com.example.projectquestonjava.feature.gamification.data.model.StreakRewardDefinition;
import com.example.projectquestonjava.feature.gamification.domain.model.DailyRewardsInfo;
import com.example.projectquestonjava.feature.gamification.domain.repository.GamificationRepository;
import com.example.projectquestonjava.feature.gamification.domain.repository.RewardRepository;
import com.example.projectquestonjava.feature.gamification.domain.repository.StreakRewardDefinitionRepository;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.inject.Inject;

public class GetDailyRewardsUseCase {
    private static final String TAG = "GetDailyRewardsUseCase";
    private static final int DAYS_IN_WEEK = 7;

    private final GamificationRepository gamificationRepository;
    private final StreakRewardDefinitionRepository streakRewardDefinitionRepository;
    private final RewardRepository rewardRepository;
    private final DateTimeUtils dateTimeUtils;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public GetDailyRewardsUseCase(
            GamificationRepository gamificationRepository,
            StreakRewardDefinitionRepository streakRewardDefinitionRepository,
            RewardRepository rewardRepository,
            DateTimeUtils dateTimeUtils,
            @IODispatcher Executor ioExecutor,
            Logger logger) {
        this.gamificationRepository = gamificationRepository;
        this.streakRewardDefinitionRepository = streakRewardDefinitionRepository;
        this.rewardRepository = rewardRepository;
        this.dateTimeUtils = dateTimeUtils;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    public ListenableFuture<DailyRewardsInfo> execute() {
        logger.debug(TAG, "Fetching daily rewards info for current user (non-cyclic logic)");

        // Получаем Gamification асинхронно
        ListenableFuture<Gamification> gamificationFuture = gamificationRepository.getCurrentUserGamificationFuture();

        return Futures.transformAsync(gamificationFuture, gamification -> {
            LocalDate today = dateTimeUtils.currentLocalDate();

            if (gamification == null) {
                logger.debug(TAG, "No gamification profile found, returning defaults.");
                // Если профиля нет, можно получить награду за 1-й день, но нужны определения.
                // Для простоты возвращаем пустое состояние.
                return Futures.immediateFuture(new DailyRewardsInfo(Collections.emptyList(), 0, true, 1, 0L));
            }

            int currentStreak = gamification.getCurrentStreak();
            LocalDate lastClaimedDate = gamification.getLastClaimedDate();
            boolean canClaimToday = lastClaimedDate.isBefore(today);
            long daysSinceLastClaim = ChronoUnit.DAYS.between(lastClaimedDate, today);
            daysSinceLastClaim = Math.max(0, daysSinceLastClaim);

            int todayStreakDay = canClaimToday ? (daysSinceLastClaim == 1L ? currentStreak + 1 : 1) : currentStreak;

            int currentDisplayWeekStartStreak = ((todayStreakDay - 1) / DAYS_IN_WEEK) * DAYS_IN_WEEK + 1;
            int currentDisplayWeekEndStreak = currentDisplayWeekStartStreak + DAYS_IN_WEEK - 1;

            logger.debug(TAG, "Current streak: " + currentStreak + ", Last claimed: " + lastClaimedDate + ", Can claim today: " + canClaimToday + ", Today's streak day: " + todayStreakDay + ", Display range: " + currentDisplayWeekStartStreak + ".." + currentDisplayWeekEndStreak);

            ListenableFuture<List<StreakRewardDefinition>> definitionsFuture =
                    streakRewardDefinitionRepository.getRewardDefinitionsForStreakRange(currentDisplayWeekStartStreak, currentDisplayWeekEndStreak);

            return Futures.transformAsync(definitionsFuture, definitions -> {
                if (definitions == null || definitions.isEmpty()) {
                    return Futures.immediateFuture(new DailyRewardsInfo(Collections.emptyList(), currentStreak, canClaimToday, todayStreakDay, daysSinceLastClaim));
                }

                List<ListenableFuture<Reward>> rewardFutures = new ArrayList<>();
                for (StreakRewardDefinition def : definitions) {
                    rewardFutures.add(rewardRepository.getRewardById(def.getRewardId()));
                }

                return Futures.transform(Futures.allAsList(rewardFutures), rewardListFromFutures -> {
                    List<Reward> validRewards = rewardListFromFutures.stream()
                            .filter(Objects::nonNull) // Отфильтровываем null награды
                            .collect(Collectors.toList());

                    Map<Long, StreakRewardDefinition> definitionMap = definitions.stream()
                            .collect(Collectors.toMap(StreakRewardDefinition::getRewardId, d -> d));

                    validRewards.sort(Comparator.comparingInt(r -> {
                        StreakRewardDefinition def = definitionMap.get(r.getId());
                        return def != null ? def.getStreakDay() : Integer.MAX_VALUE;
                    }));
                    return new DailyRewardsInfo(validRewards, currentStreak, canClaimToday, todayStreakDay, daysSinceLastClaim);
                }, MoreExecutors.directExecutor()); // Преобразование списка наград
            }, ioExecutor); // Преобразование определений наград
        }, ioExecutor); // Преобразование профиля геймификации
    }
}