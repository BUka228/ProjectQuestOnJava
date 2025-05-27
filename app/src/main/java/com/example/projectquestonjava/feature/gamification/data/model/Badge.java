package com.example.projectquestonjava.feature.gamification.data.model;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import java.util.Objects;

import lombok.Data;

@Entity(tableName = "badge")
@Data
public class Badge {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;
    public String description;

    @ColumnInfo(name = "image_url")
    @DrawableRes // Указываем, что это ID ресурса drawable
    public int imageUrl;

    public String criteria;

    // Основной конструктор для Room
    public Badge(long id, String name, String description, @DrawableRes int imageUrl, String criteria) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.criteria = criteria;
    }

    @Ignore
    public Badge(String name, String description, @DrawableRes int imageUrl, String criteria) {
        this(0, name, description, imageUrl, criteria);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Badge badge = (Badge) o;
        return id == badge.id && imageUrl == badge.imageUrl && Objects.equals(name, badge.name) && Objects.equals(description, badge.description) && Objects.equals(criteria, badge.criteria);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, imageUrl, criteria);
    }

}