package com.example.projectquestonjava.core.data.repositories;

import com.example.projectquestonjava.core.data.model.enums.Priority;
import com.example.projectquestonjava.core.data.model.enums.TaskStatus;
import com.example.projectquestonjava.core.domain.repository.PriorityResolver;
import com.example.projectquestonjava.core.priority_strategy.PriorityStrategy;
import com.example.projectquestonjava.core.utils.Logger;
import java.time.LocalDateTime;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PriorityResolverImpl implements PriorityResolver {
    private final List<PriorityStrategy> strategies;
    private final Logger logger;

    @Inject
    public PriorityResolverImpl(List<PriorityStrategy> strategies, Logger logger) {
        this.strategies = strategies;
        this.logger = logger;
    }

    @Override
    public Priority resolve(LocalDateTime dueDate, TaskStatus status) {
        logger.debug("PriorityResolverImpl", "Resolving priority for due date: " + dueDate + ", status: " + status);
        for (PriorityStrategy strategy : strategies) {
            if (strategy.canHandle(dueDate, status)) {
                return strategy.getPriority();
            }
        }
        return Priority.LOW;
    }
}