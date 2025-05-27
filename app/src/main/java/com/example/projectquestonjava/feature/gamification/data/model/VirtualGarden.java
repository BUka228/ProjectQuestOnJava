package com.example.projectquestonjava.feature.gamification.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.example.projectquestonjava.feature.gamification.domain.model.PlantType;
import java.time.LocalDateTime;
import java.util.Objects;

import lombok.Data;

@Entity(
        tableName = "virtual_garden",
        foreignKeys = {
                @ForeignKey(
                        entity = Gamification.class,
                        parentColumns = {"id"},
                        childColumns = {"gamification_id"},
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {@Index("gamification_id")}
)
@Data
public class VirtualGarden {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "gamification_id")
    public final long gamificationId;

    @ColumnInfo(name = "plant_type")
    public final PlantType plantType;

    @ColumnInfo(name = "growth_stage")
    public int growthStage;

    @ColumnInfo(name = "growth_points", defaultValue = "0")
    public int growthPoints;

    @ColumnInfo(name = "last_watered")
    public LocalDateTime lastWatered;

    // Основной конструктор для Room
    public VirtualGarden(long id, long gamificationId, PlantType plantType, int growthStage, int growthPoints, LocalDateTime lastWatered) {
        this.id = id;
        this.gamificationId = gamificationId;
        this.plantType = plantType;
        this.growthStage = growthStage;
        this.growthPoints = growthPoints;
        this.lastWatered = lastWatered;
    }

    @Ignore
    public VirtualGarden(long gamificationId, PlantType plantType, int growthStage, int growthPoints, LocalDateTime lastWatered) {
        this(0, gamificationId, plantType, growthStage, growthPoints, lastWatered);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VirtualGarden that = (VirtualGarden) o;
        return id == that.id && gamificationId == that.gamificationId && growthStage == that.growthStage && growthPoints == that.growthPoints && plantType == that.plantType && Objects.equals(lastWatered, that.lastWatered);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, gamificationId, plantType, growthStage, growthPoints, lastWatered);
    }


}