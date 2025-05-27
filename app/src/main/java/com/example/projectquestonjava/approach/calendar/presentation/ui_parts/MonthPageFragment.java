package com.example.projectquestonjava.approach.calendar.presentation.ui_parts; // Пример пакета

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider; // Для доступа к общей ViewModel
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.approach.calendar.presentation.viewmodels.CalendarPlanningViewModel;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint // Если используются Hilt зависимости
public class MonthPageFragment extends Fragment {

    private static final String ARG_YEAR_MONTH = "arg_year_month";
    private YearMonth currentYearMonth;
    private RecyclerView recyclerViewDays;
    private DayAdapter adapter;
    private CalendarPlanningViewModel planningViewModel; // Общая ViewModel

    public static MonthPageFragment newInstance(YearMonth yearMonth) {
        MonthPageFragment fragment = new MonthPageFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_YEAR_MONTH, yearMonth);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentYearMonth = (YearMonth) getArguments().getSerializable(ARG_YEAR_MONTH);
        }
        // Получаем ViewModel от родительского CalendarPlanningFragment
        planningViewModel = new ViewModelProvider(requireParentFragment()).get(CalendarPlanningViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Макет для страницы месяца - просто RecyclerView
        // или можно создать XML с RecyclerView внутри, если нужна доп. структура
        recyclerViewDays = new RecyclerView(requireContext());
        recyclerViewDays.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT // Высота будет зависеть от количества недель
        ));
        recyclerViewDays.setLayoutManager(new GridLayoutManager(getContext(), 7));
        recyclerViewDays.setPadding(dpToPx(4), dpToPx(8), dpToPx(4), dpToPx(8));
        // recyclerViewDays.setHasFixedSize(true); // Если размер ячеек не меняется
        return recyclerViewDays;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new DayAdapter(getContext(), getDaysForMonth(currentYearMonth), planningViewModel,
                date -> planningViewModel.selectDate(date)); // Передаем обработчик клика
        recyclerViewDays.setAdapter(adapter);

        // Наблюдаем за изменениями в selectedDateLiveData и dailyTaskCountsLiveData из общей ViewModel
        planningViewModel.selectedDateLiveData.observe(getViewLifecycleOwner(), selectedDate -> adapter.setSelectedDate(selectedDate));
        //TODO planningViewModel.dailyTaskCountsLiveData.observe(getViewLifecycleOwner(), counts -> adapter.setDailyTaskCounts(counts));
    }

    private List<LocalDate> getDaysForMonth(YearMonth month) {
        List<LocalDate> days = new ArrayList<>();
        if (month == null) return days;

        LocalDate firstDayOfMonth = month.atDay(1);
        int firstDayOfWeekValue = firstDayOfMonth.getDayOfWeek().getValue(); // Пн=1, Вс=7
        int daysOffset = firstDayOfWeekValue - 1; // Смещение для начала с понедельника

        for (int i = 0; i < daysOffset; i++) {
            days.add(null); // Пустые ячейки для дней предыдущего месяца
        }
        for (int i = 1; i <= month.lengthOfMonth(); i++) {
            days.add(month.atDay(i));
        }
        // Дополняем пустыми ячейками до конца сетки (6 недель * 7 дней = 42 ячейки)
        while (days.size() % 7 != 0 || days.size() < 42 && days.size() < 35 && month.lengthOfMonth()+daysOffset > 28) { // чтобы было 5 или 6 строк
            if(days.size() >=35 && month.lengthOfMonth()+daysOffset <= 35) break; // если 5 строк достаточно
            if(days.size() >=42) break; // если 6 строк достаточно
            days.add(null);
        }
        return days;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // --- Адаптер для дней месяца ---
    private static class DayAdapter extends RecyclerView.Adapter<DayAdapter.DayViewHolder> {
        private final Context context;
        private List<LocalDate> days;
        private LocalDate selectedDate;
        private Map<LocalDate, Integer> dailyTaskCounts;
        private final OnDateSelectedListener listener;
        private final CalendarPlanningViewModel viewModel; // Для getPriorityColor

        interface OnDateSelectedListener {
            void onDateSelected(LocalDate date);
        }

        DayAdapter(Context context, List<LocalDate> days, CalendarPlanningViewModel viewModel, OnDateSelectedListener listener) {
            this.context = context;
            this.days = days;
            this.viewModel = viewModel;
            this.listener = listener;
            this.dailyTaskCounts = Collections.emptyMap();
        }

        public void setSelectedDate(LocalDate selectedDate) {
            this.selectedDate = selectedDate;
            notifyDataSetChanged(); // Простое обновление, для оптимизации можно использовать DiffUtil
        }

        public void setDailyTaskCounts(Map<LocalDate, Integer> counts) {
            this.dailyTaskCounts = counts != null ? counts : Collections.emptyMap();
            notifyDataSetChanged();
        }


        @NonNull
        @Override
        public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_day_cell_planning, parent, false);
            return new DayViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
            LocalDate day = days.get(position);
            holder.bind(day, selectedDate, dailyTaskCounts.getOrDefault(day, 0), listener);
        }

        @Override
        public int getItemCount() {
            return days.size();
        }

        static class DayViewHolder extends RecyclerView.ViewHolder {
            LinearLayout layoutDayCell;
            TextView textViewDayNumber;
            LinearLayout layoutTaskIndicatorDots;

            DayViewHolder(@NonNull View itemView) {
                super(itemView);
                layoutDayCell = itemView.findViewById(R.id.layout_day_cell);
                textViewDayNumber = itemView.findViewById(R.id.textView_day_number);
                layoutTaskIndicatorDots = itemView.findViewById(R.id.layout_task_indicator_dots);
            }

            void bind(@Nullable LocalDate day, @Nullable LocalDate selectedDate, int taskCount, OnDateSelectedListener listener) {
                if (day == null) {
                    itemView.setVisibility(View.INVISIBLE); // Скрываем пустые ячейки
                    return;
                }
                itemView.setVisibility(View.VISIBLE);
                textViewDayNumber.setText(String.valueOf(day.getDayOfMonth()));

                boolean isSelected = day.equals(selectedDate);
                boolean isToday = day.equals(LocalDate.now());
                // boolean isCurrentDisplayMonth = day.getMonth() == currentDisplayMonth.getMonth(); // Если нужно затемнять дни других месяцев

                // --- Стилизация ---
                int backgroundColorRes;
                int textColorRes;

                if (isSelected) {
                    backgroundColorRes = R.color.primaryLight; // Из colors.xml
                    textColorRes = R.color.onPrimaryLight;
                } else if (isToday) {
                    backgroundColorRes = R.color.secondaryContainerLight; // Создать цвет с альфой
                    textColorRes = R.color.onSecondaryContainerLight;
                } else {
                    backgroundColorRes = android.R.color.transparent;
                    textColorRes = R.color.onSurfaceLight;
                }
                // itemView.setAlpha(isCurrentDisplayMonth ? 1.0f : 0.5f); // Затемнение дней не текущего месяца
                layoutDayCell.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), backgroundColorRes));
                textViewDayNumber.setTextColor(ContextCompat.getColor(itemView.getContext(), textColorRes));

                // --- Индикаторы задач ---
                layoutTaskIndicatorDots.removeAllViews();
                if (taskCount > 0) {
                    int dotsToShow = Math.min(taskCount, 3); // Максимум 3 точки
                    for (int i = 0; i < dotsToShow; i++) {
                        View dot = new View(itemView.getContext());
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(4), dpToPx(4));
                        params.setMarginEnd(dpToPx(1));
                        dot.setLayoutParams(params);
                        GradientDrawable dotBackground = new GradientDrawable();
                        dotBackground.setShape(GradientDrawable.OVAL);
                        dotBackground.setColor(ContextCompat.getColor(itemView.getContext(), R.color.primaryLight)); // Цвет точек
                        dot.setBackground(dotBackground);
                        layoutTaskIndicatorDots.addView(dot);
                    }
                }

                itemView.setOnClickListener(v -> listener.onDateSelected(day));
            }
            private int dpToPx(int dp) {
                return (int) (dp * itemView.getContext().getResources().getDisplayMetrics().density);
            }
        }
    }
}