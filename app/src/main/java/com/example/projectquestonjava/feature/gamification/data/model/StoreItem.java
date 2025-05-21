package com.example.projectquestonjava.feature.gamification.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.example.projectquestonjava.feature.gamification.domain.model.StoreItemCategory;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(tableName = "store_item")
public class StoreItem {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String name;
    private String description;
    private int cost;
    private StoreItemCategory category;

    @ColumnInfo(name = "item_value")
    private String itemValue; // Например, "Rose" для растения или "DarkTheme" для темы

    @ColumnInfo(name = "image_url") // В Kotlin был String, предполагаю URL или путь к файлу
    private String imageUrl;

    public StoreItem(long id, String name, String description, int cost, StoreItemCategory category, String itemValue, String imageUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.cost = cost;
        this.category = category;
        this.itemValue = itemValue;
        this.imageUrl = imageUrl;
    }
    // Конструктор для Room
    public StoreItem(String name, String description, int cost, StoreItemCategory category, String itemValue, String imageUrl) {
        this(0, name, description, cost, category, itemValue, imageUrl);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoreItem storeItem = (StoreItem) o;
        return id == storeItem.id && cost == storeItem.cost && Objects.equals(name, storeItem.name) && Objects.equals(description, storeItem.description) && category == storeItem.category && Objects.equals(itemValue, storeItem.itemValue) && Objects.equals(imageUrl, storeItem.imageUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, cost, category, itemValue, imageUrl);
    }
}