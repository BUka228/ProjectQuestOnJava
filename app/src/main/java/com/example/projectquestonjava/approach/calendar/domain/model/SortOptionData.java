package com.example.projectquestonjava.approach.calendar.domain.model;

import androidx.annotation.DrawableRes;

import lombok.Getter;

public class SortOptionData {
    @Getter
    private final String label;
    @DrawableRes
    private final int iconResId;
    @Getter
    private final TaskSortOption option;

    public SortOptionData(String label, @DrawableRes int iconResId, TaskSortOption option) {
        this.label = label;
        this.iconResId = iconResId;
        this.option = option;
    }

    @DrawableRes
    public int getIconResId() {
        return iconResId;
    }
}