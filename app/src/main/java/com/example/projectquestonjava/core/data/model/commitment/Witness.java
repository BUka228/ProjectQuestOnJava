package com.example.projectquestonjava.core.data.model.commitment;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.util.Objects;

import lombok.Setter;

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

    @Setter
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "commitment_id")
    private final int commitmentId;

    @ColumnInfo(name = "witness_name")
    private final String witnessName;

    public Witness(int id, int commitmentId, String witnessName) {
        this.id = id;
        this.commitmentId = commitmentId;
        this.witnessName = witnessName;
    }
    // Конструктор без id для Room
    public Witness(int commitmentId, String witnessName) {
        this(0, commitmentId, witnessName);
    }


    public int getId() {
        return id;
    }

    public int getCommitmentId() {
        return commitmentId;
    }

    public String getWitnessName() {
        return witnessName;
    }

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