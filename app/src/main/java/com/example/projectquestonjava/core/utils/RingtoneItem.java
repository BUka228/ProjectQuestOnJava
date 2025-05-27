package com.example.projectquestonjava.core.utils; // Убедитесь, что пакет соответствует вашей структуре

import androidx.annotation.NonNull;
import androidx.annotation.Nullable; // Для nullable URI
import java.util.Objects;

import lombok.Getter;

/**
 * @param uri URI может быть null для опции "Без звука"
 */
public record RingtoneItem(@Nullable String uri, @Getter String title, boolean isCustom) {

    // equals и hashCode важны для корректной работы в списках и сравнениях
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RingtoneItem that = (RingtoneItem) o;
        return isCustom == that.isCustom &&
                Objects.equals(uri, that.uri) && // Сравнение nullable URI
                title.equals(that.title);
    }

    // toString() для отладки и для ArrayAdapter, если не переопределен getView
    @NonNull
    @Override
    public String toString() {
        return title; // ArrayAdapter по умолчанию использует toString() для отображения элемента
    }
}