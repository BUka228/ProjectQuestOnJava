package com.example.projectquestonjava.feature.gamification.data.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import com.example.projectquestonjava.feature.gamification.domain.model.StoreItemCategory;
import java.util.Objects;

@Entity(tableName = "store_item")
public class StoreItem {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;
    public String description;
    public int cost;
    public StoreItemCategory category;

    @ColumnInfo(name = "item_value")
    public String itemValue;

    @ColumnInfo(name = "image_url")
    public String imageUrl;

    // Основной конструктор для Room
    public StoreItem(long id, String name, String description, int cost, StoreItemCategory category, String itemValue, String imageUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.cost = cost;
        this.category = category;
        this.itemValue = itemValue;
        this.imageUrl = imageUrl;
    }

    @Ignore
    public StoreItem(String name, String description, int cost, StoreItemCategory category, String itemValue, String imageUrl) {
        this(0, name, description, cost, category, itemValue, imageUrl);
    }

    // equals, hashCode, toString
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

    @NonNull
    @Override
    public String toString() {
        return "StoreItem{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", cost=" + cost +
                ", category=" + category +
                ", itemValue='" + itemValue + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}