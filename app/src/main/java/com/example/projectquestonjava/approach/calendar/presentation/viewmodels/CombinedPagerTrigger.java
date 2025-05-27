package com.example.projectquestonjava.approach.calendar.presentation.viewmodels;

import androidx.annotation.NonNull;
import com.example.projectquestonjava.core.data.model.core.Tag;
import com.example.projectquestonjava.approach.calendar.domain.model.TaskFilterOption;
import com.example.projectquestonjava.approach.calendar.domain.model.TaskSortOption;
import java.util.Objects;
import java.util.Set;

import lombok.Getter;

/**
 * Класс-контейнер для объединения параметров, влияющих на данные пейджера в CalendarDashboard.
 * Используется в MediatorLiveData для отслеживания изменений.
 */
public record CombinedPagerTrigger(@Getter int page, Set<Tag> tags, TaskSortOption sort,
                                   Set<TaskFilterOption> filters) {
    public CombinedPagerTrigger(
            int page,
            @NonNull Set<Tag> tags,
            @NonNull TaskSortOption sort,
            @NonNull Set<TaskFilterOption> filters) {
        this.page = page;
        this.tags = Objects.requireNonNull(tags, "tags cannot be null");
        this.sort = Objects.requireNonNull(sort, "sort cannot be null");
        this.filters = Objects.requireNonNull(filters, "filters cannot be null");
    }

    @Override
    @NonNull
    public Set<Tag> tags() {
        return tags;
    }

    @Override
    @NonNull
    public TaskSortOption sort() {
        return sort;
    }

    @Override
    @NonNull
    public Set<TaskFilterOption> filters() {
        return filters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CombinedPagerTrigger that = (CombinedPagerTrigger) o;
        return page == that.page &&
                tags.equals(that.tags) && // Сравнение Set через equals
                sort == that.sort &&     // Сравнение Enum через ==
                filters.equals(that.filters); // Сравнение Set через equals
    }

    @NonNull
    @Override
    public String toString() {
        return "CombinedPagerTrigger{" +
                "page=" + page +
                ", tags=" + tags.size() + // Выводим размер для краткости
                ", sort=" + sort +
                ", filters=" + filters +
                '}';
    }
}