package com.example.projectquestonjava.approach.calendar.presentation.screens;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class DashboardPagerAdapter extends FragmentStateAdapter {

    private final List<LocalDateTime> dates;

    /**
     * Конструктор для использования ViewPager2 внутри другого Fragment.
     * @param fragment Родительский фрагмент.
     * @param dates Список дат.
     */
    public DashboardPagerAdapter(@NonNull Fragment fragment, @NonNull List<LocalDateTime> dates) {
        super(fragment); // Используем конструктор для Fragment
        Objects.requireNonNull(dates, "Dates list cannot be null");
        this.dates = dates;
    }

    /**
     * Конструктор для использования ViewPager2 внутри Activity.
     * @param fragmentActivity Родительская Activity.
     * @param dates Список дат.
     */
    public DashboardPagerAdapter(@NonNull FragmentActivity fragmentActivity, @NonNull List<LocalDateTime> dates) {
        super(fragmentActivity); // Используем конструктор для FragmentActivity
        Objects.requireNonNull(dates, "Dates list cannot be null");
        this.dates = dates;
    }


    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Получаем LocalDateTime для текущей позиции
        LocalDateTime dateTimeForPage = dates.get(position);
        // DayTasksFragment ожидает LocalDate
        LocalDate localDateForPage = dateTimeForPage.toLocalDate();

        // Создаем новый экземпляр DayTasksFragment для каждой страницы,
        // передавая дату в аргументы фрагмента.
        return DayTasksFragment.newInstance(localDateForPage);
    }

    @Override
    public int getItemCount() {
        return dates.size();
    }

    /**
     * Возвращает дату для указанной позиции в адаптере.
     * Может быть полезно для TabLayoutMediator или для получения даты текущей страницы.
     * @param position Позиция страницы.
     * @return LocalDateTime для этой страницы или null, если позиция некорректна.
     */
    @Nullable
    public LocalDateTime getDateForPosition(int position) {
        if (position >= 0 && position < dates.size()) {
            return dates.get(position);
        }
        return null;
    }

    /**
     * Метод для обновления списка дат в адаптере.
     * ВНИМАНИЕ: Прямое изменение списка и вызов notifyDataSetChanged()
     * для FragmentStateAdapter может привести к проблемам с состоянием фрагментов.
     * Обычно, если список дат должен меняться, более надежным подходом
     * является создание НОВОГО экземпляра адаптера с новым списком дат и
     * установка его в ViewPager2.
     * Этот метод оставлен для примера, но его использование требует осторожности.
     *
     * @param newDates Новый список дат.
     */
    public void updateDates(@NonNull List<LocalDateTime> newDates) {
        Objects.requireNonNull(newDates, "New dates list cannot be null");

        if (this.dates != newDates) {
            System.err.println("DashboardPagerAdapter.updateDates() called. This is generally not recommended for FragmentStateAdapter. Consider re-creating the adapter.");
        }
    }
}