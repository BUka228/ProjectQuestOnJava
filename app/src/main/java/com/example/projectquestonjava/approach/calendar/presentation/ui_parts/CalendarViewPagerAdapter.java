package com.example.projectquestonjava.approach.calendar.presentation.ui_parts;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.time.YearMonth;

public class CalendarViewPagerAdapter extends FragmentStateAdapter {

    // Используем большое количество страниц для "бесконечной" прокрутки
    public static final int INITIAL_PAGE = 1000 * 12; // Начальная страница, соответствующая текущему месяцу
    private final YearMonth startYearMonth; // Месяц, соответствующий INITIAL_PAGE

    public CalendarViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle, YearMonth initialDisplayMonth) {
        super(fragmentManager, lifecycle);
        // Рассчитываем месяц, который будет на INITIAL_PAGE
        // Если initialDisplayMonth - это текущий месяц, то startYearMonth будет текущий месяц
        this.startYearMonth = initialDisplayMonth;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Рассчитываем YearMonth для данной позиции относительно startYearMonth
        YearMonth yearMonthForPosition = startYearMonth.plusMonths((long) position - INITIAL_PAGE);
        return MonthPageFragment.newInstance(yearMonthForPosition);
    }

    @Override
    public int getItemCount() {
        return INITIAL_PAGE * 2; // Достаточно большой диапазон для прокрутки
    }
}