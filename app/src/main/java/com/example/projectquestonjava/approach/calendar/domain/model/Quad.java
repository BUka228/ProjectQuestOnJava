package com.example.projectquestonjava.approach.calendar.domain.model;

import androidx.annotation.NonNull;

import java.util.Objects;

public class Quad<A, B, C, D> {
    public final A first;
    public final B second;
    public final C third;
    public final D fourth;

    public Quad(A first, B second, C third, D fourth) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Quad<?, ?, ?, ?> quad = (Quad<?, ?, ?, ?>) o;
        if (!Objects.equals(first, quad.first)) return false;
        if (!Objects.equals(second, quad.second)) return false;
        if (!Objects.equals(third, quad.third)) return false;
        return Objects.equals(fourth, quad.fourth);
    }

    @Override
    public int hashCode() {
        int result = first != null ? first.hashCode() : 0;
        result = 31 * result + (second != null ? second.hashCode() : 0);
        result = 31 * result + (third != null ? third.hashCode() : 0);
        result = 31 * result + (fourth != null ? fourth.hashCode() : 0);
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "Quad{" +
                "first=" + first +
                ", second=" + second +
                ", third=" + third +
                ", fourth=" + fourth +
                '}';
    }
}