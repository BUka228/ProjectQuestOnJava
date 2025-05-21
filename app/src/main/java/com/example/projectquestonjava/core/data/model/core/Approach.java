package com.example.projectquestonjava.core.data.model.core;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.example.projectquestonjava.core.data.model.enums.ApproachName;

@Entity(tableName = "approach")
public class Approach {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private ApproachName name;
    private String description;

    public Approach(long id, ApproachName name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    // Getters
    public long getId() {
        return id;
    }

    public ApproachName getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setName(ApproachName name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}