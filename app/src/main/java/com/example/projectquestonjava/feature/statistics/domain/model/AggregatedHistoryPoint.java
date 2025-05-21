package com.example.projectquestonjava.feature.statistics.domain.model;

import androidx.room.ColumnInfo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AggregatedHistoryPoint {
    private LocalDateTime timestamp;
    @ColumnInfo(name = "xp_change")
    private int xpChange;
    @ColumnInfo(name = "coins_change")
    private int coinsChange;
}