package com.example.projectquestonjava.feature.gamification.data.model;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

@Entity(tableName = "badge")
public class Badge {

    @Getter
    @Setter
    @PrimaryKey(autoGenerate = true)
    private long id;

    @Getter
    @Setter
    private String name;
    @Setter
    @Getter
    private String description;

    @ColumnInfo(name = "image_url")
    @DrawableRes
    private int imageUrl;

    @Setter
    @Getter
    private String criteria;

    public Badge(long id, String name, String description, @DrawableRes int imageUrl, String criteria) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.criteria = criteria;
    }

    // Конструктор для Room, если id автогенерируется
    public Badge(String name, String description, @DrawableRes int imageUrl, String criteria) {
        this(0, name, description, imageUrl, criteria);
    }


    @DrawableRes
    public int getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(@DrawableRes int imageUrl) {
        this.imageUrl = imageUrl;
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

    @NonNull
    @Override
    public String toString() {
        return "Badge{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", imageUrl=" + imageUrl +
                ", criteria='" + criteria + '\'' +
                '}';
    }
}