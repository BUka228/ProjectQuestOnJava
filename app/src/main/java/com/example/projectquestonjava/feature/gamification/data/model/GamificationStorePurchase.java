package com.example.projectquestonjava.feature.gamification.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.time.LocalDateTime;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity(
        tableName = "gamification_store_purchase",
        foreignKeys = {
                @ForeignKey(
                        entity = Gamification.class,
                        parentColumns = {"id"},
                        childColumns = {"gamification_id"},
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = StoreItem.class,
                        parentColumns = {"id"},
                        childColumns = {"store_item_id"},
                        onDelete = ForeignKey.RESTRICT // Покупка не должна удаляться, если удаляется товар из магазина? Или CASCADE?
                )
        },
        indices = {@Index("gamification_id"), @Index("store_item_id")}
)
public class GamificationStorePurchase {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "gamification_id")
    private long gamificationId;

    @ColumnInfo(name = "store_item_id")
    private long storeItemId;

    @ColumnInfo(name = "purchased_at")
    private LocalDateTime purchasedAt;

    public GamificationStorePurchase(long id, long gamificationId, long storeItemId, LocalDateTime purchasedAt) {
        this.id = id;
        this.gamificationId = gamificationId;
        this.storeItemId = storeItemId;
        this.purchasedAt = purchasedAt != null ? purchasedAt : LocalDateTime.now();
    }
    // Конструктор для Room
    public GamificationStorePurchase(long gamificationId, long storeItemId, LocalDateTime purchasedAt) {
        this(0, gamificationId, storeItemId, purchasedAt);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GamificationStorePurchase that = (GamificationStorePurchase) o;
        return id == that.id && gamificationId == that.gamificationId && storeItemId == that.storeItemId && Objects.equals(purchasedAt, that.purchasedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, gamificationId, storeItemId, purchasedAt);
    }
}