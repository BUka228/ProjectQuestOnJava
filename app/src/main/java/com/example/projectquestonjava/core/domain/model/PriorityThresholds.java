package com.example.projectquestonjava.core.domain.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PriorityThresholds {
    private int critical = 30; // минуты
    private int high = 60;     // минуты
    private int medium = 120;    // минуты

    public PriorityThresholds(int critical, int high, int medium) {
        this.critical = critical;
        this.high = high;
        this.medium = medium;
    }
}