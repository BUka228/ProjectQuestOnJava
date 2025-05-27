package com.example.projectquestonjava.core.data.model.commitment;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.util.Objects;

@Entity(
        tableName = "witness",
        foreignKeys = {
                @ForeignKey(
                        entity = PublicCommitment.class,
                        parentColumns = {"id"},
                        childColumns = {"commitment_id"},
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {@Index(value = {"commitment_id"})}
)
public class Witness {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "commitment_id")
    public final int commitmentId;

    @ColumnInfo(name = "witness_name")
    public final String witnessName;

    // Основной конструктор для Room
    public Witness(int id, int commitmentId, String witnessName) {
        this.id = id;
        this.commitmentId = commitmentId;
        this.witnessName = witnessName;
    }

    @Ignore
    public Witness(int commitmentId, String witnessName) {
        this(0, commitmentId, witnessName);
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Witness witness = (Witness) o;
        return id == witness.id && commitmentId == witness.commitmentId && Objects.equals(witnessName, witness.witnessName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, commitmentId, witnessName);
    }

    @NonNull
    @Override
    public String toString() {
        return "Witness{" +
                "id=" + id +
                ", commitmentId=" + commitmentId +
                ", witnessName='" + witnessName + '\'' +
                '}';
    }
}