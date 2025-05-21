package com.example.projectquestonjava.core.data.model.core;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(tableName = "tags", indices = {@Index(value = {"name"}, unique = true)})
public class Tag {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private String name;
    private String color = "#FFFFFF";

    public Tag(String name, String color) {
        this.name = name;
        this.color = color;
    }
}