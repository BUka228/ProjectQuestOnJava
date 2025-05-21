package com.example.projectquestonjava.feature.statistics.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.example.projectquestonjava.feature.gamification.data.model.Gamification;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(
        tableName = "gamification_history",
        foreignKeys = {
                @ForeignKey(
                        entity = Gamification.class,
                        parentColumns = {"id"},
                        childColumns = {"gamification_id"},
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {@Index("gamification_id"), @Index("timestamp")}
)
public class GamificationHistory {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "gamification_id")
    private long gamificationId;

    private LocalDateTime timestamp = LocalDateTime.now();

    @ColumnInfo(name = "xp_change")
    private int xpChange = 0;

    @ColumnInfo(name = "coins_change")
    private int coinsChange = 0;

    @ColumnInfo(name = "reason")
    private String reason;

    @ColumnInfo(name = "related_entity_id")
    private Long relatedEntityId;
}