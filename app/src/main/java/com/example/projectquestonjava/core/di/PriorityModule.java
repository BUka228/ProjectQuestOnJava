package com.example.projectquestonjava.core.di;

import com.example.projectquestonjava.core.data.repositories.PriorityResolverImpl;
import com.example.projectquestonjava.core.domain.model.PriorityThresholds;
import com.example.projectquestonjava.core.domain.repository.PriorityResolver;
import com.example.projectquestonjava.core.priority_strategy.*;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import com.example.projectquestonjava.core.utils.Logger;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import dagger.multibindings.IntoSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class PriorityModule {

    @Provides
    @Singleton
    public PriorityThresholds providePriorityThresholds() {
        return new PriorityThresholds();
    }

    @Provides
    @IntoSet
    public PriorityStrategy provideOverdueCriticalStrategy(DateTimeUtils dateTimeUtils) {
        return new OverdueCriticalStrategy(dateTimeUtils);
    }

    @Provides
    @IntoSet
    public PriorityStrategy provideCompletedTaskStrategy() {
        return new CompletedTaskStrategy();
    }

    @Provides
    @IntoSet
    public PriorityStrategy provideCriticalPriorityStrategy(DateTimeUtils dateTimeUtils, PriorityThresholds thresholds) {
        return new CriticalPriorityStrategy(dateTimeUtils, thresholds);
    }

    @Provides
    @IntoSet
    public PriorityStrategy provideHighPriorityStrategy(DateTimeUtils dateTimeUtils, PriorityThresholds thresholds) {
        return new HighPriorityStrategy(dateTimeUtils, thresholds);
    }

    @Provides
    @IntoSet
    public PriorityStrategy provideMediumPriorityStrategy(DateTimeUtils dateTimeUtils, PriorityThresholds thresholds) {
        return new MediumPriorityStrategy(dateTimeUtils, thresholds);
    }

    @Provides
    @IntoSet
    public PriorityStrategy provideLowPriorityStrategy() {
        return new LowPriorityStrategy();
    }

    @Provides
    @Singleton
    public PriorityResolver providePriorityResolver(
            Set<PriorityStrategy> strategiesSet,
            Logger logger
    ) {
        List<PriorityStrategy> sortedStrategies = new ArrayList<>(strategiesSet);

        // Определяем порядок приоритета стратегий
        // Чем меньше значение, тем выше приоритет (раньше будет проверена)
        Comparator<PriorityStrategy> strategyComparator = Comparator.comparingInt(s -> {
            if (s instanceof OverdueCriticalStrategy) return 0;
            if (s instanceof CompletedTaskStrategy) return 1; // Выполненные задачи имеют приоритет над временными порогами
            if (s instanceof CriticalPriorityStrategy) return 2;
            if (s instanceof HighPriorityStrategy) return 3;
            if (s instanceof MediumPriorityStrategy) return 4;
            if (s instanceof LowPriorityStrategy) return 5; // Fallback - самый низкий приоритет
            return Integer.MAX_VALUE; // Неизвестные стратегии в конец
        });

        sortedStrategies.sort(strategyComparator);

        return new PriorityResolverImpl(sortedStrategies, logger);
    }
}