package com.example.projectquestonjava.approach.calendar.presentation.ui_parts;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
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

@AndroidEntryPoint
public class MonthPageFragment extends Fragment {

    private static final String ARG_YEAR_MONTH = "arg_year_month";
    private YearMonth currentYearMonth;
    private RecyclerView recyclerViewDays;
    private DayAdapter adapter;
    private CalendarPlanningViewModel planningViewModel;

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
        planningViewModel = new ViewModelProvider(requireParentFragment()).get(CalendarPlanningViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        recyclerViewDays = new RecyclerView(requireContext());
        recyclerViewDays.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        recyclerViewDays.setLayoutManager(new GridLayoutManager(getContext(), 7));
        int paddingInPx = dpToPx(4, requireContext());
        recyclerViewDays.setPadding(paddingInPx, dpToPx(8, requireContext()), paddingInPx, dpToPx(8, requireContext()));
        return recyclerViewDays;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new DayAdapter(requireContext(), getDaysForMonth(currentYearMonth), planningViewModel,
                date -> planningViewModel.selectDate(date));
        recyclerViewDays.setAdapter(adapter);

        planningViewModel.selectedDateLiveData.observe(getViewLifecycleOwner(), selectedDate -> adapter.setSelectedDate(selectedDate));
        planningViewModel.dailyTaskCountsLiveData.observe(getViewLifecycleOwner(), counts -> {
            if (counts != null) {
                adapter.setDailyTaskCounts(counts);
            }
        });
    }

    private List<LocalDate> getDaysForMonth(YearMonth month) {
        List<LocalDate> days = new ArrayList<>();
        if (month == null) return days;

        LocalDate firstDayOfMonth = month.atDay(1);
        int firstDayOfWeekValue = firstDayOfMonth.getDayOfWeek().getValue();
        int daysOffset = firstDayOfWeekValue - 1;

        for (int i = 0; i < daysOffset; i++) {
            days.add(null);
        }
        for (int i = 1; i <= month.lengthOfMonth(); i++) {
            days.add(month.atDay(i));
        }
        int cellsToFill = (daysOffset + month.lengthOfMonth() <= 35) ? 35 : 42;
        while (days.size() < cellsToFill) {
            days.add(null);
        }
        return days;
    }

    private static int dpToPx(int dp, Context context) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    private static class DayAdapter extends RecyclerView.Adapter<DayAdapter.DayViewHolder> {
        private final Context context;
        private List<LocalDate> days;
        private LocalDate selectedDate;
        private Map<LocalDate, Integer> dailyTaskCounts;
        private final OnDateSelectedListener listener;

        interface OnDateSelectedListener {
            void onDateSelected(LocalDate date);
        }

        DayAdapter(Context context, List<LocalDate> days, CalendarPlanningViewModel viewModel, OnDateSelectedListener listener) {
            this.context = context;
            this.days = days;
            this.listener = listener;
            this.dailyTaskCounts = Collections.emptyMap();
        }

        public void setSelectedDate(LocalDate selectedDate) {
            this.selectedDate = selectedDate;
            notifyDataSetChanged();
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
            holder.bind(day, selectedDate, dailyTaskCounts.getOrDefault(day, 0), listener, context);
        }

        @Override
        public int getItemCount() {
            return days.size();
        }

        static class DayViewHolder extends RecyclerView.ViewHolder {
            FrameLayout layoutDayCellRoot;
            FrameLayout frameDayBackgroundContainer;
            TextView textViewDayNumber;
            LinearLayout layoutTaskIndicatorDots;

            DayViewHolder(@NonNull View itemView) {
                super(itemView);
                layoutDayCellRoot = (FrameLayout) itemView;
                frameDayBackgroundContainer = itemView.findViewById(R.id.frame_day_background_container);
                textViewDayNumber = itemView.findViewById(R.id.textView_day_number);
                layoutTaskIndicatorDots = itemView.findViewById(R.id.layout_task_indicator_dots);
            }

            void bind(@Nullable LocalDate day, @Nullable LocalDate selectedDate, int taskCount, OnDateSelectedListener listener, Context context) {
                if (day == null) {
                    itemView.setVisibility(View.INVISIBLE);
                    frameDayBackgroundContainer.setBackground(null); // Убираем фон у невидимых
                    return;
                }
                itemView.setVisibility(View.VISIBLE);
                textViewDayNumber.setText(String.valueOf(day.getDayOfMonth()));

                boolean isSelected = day.equals(selectedDate);
                boolean isToday = day.equals(LocalDate.now());

                GradientDrawable backgroundOval = new GradientDrawable();
                backgroundOval.setShape(GradientDrawable.OVAL);

                if (isSelected) {
                    // Цвет фона выбранного дня (светло-зеленый как на скриншоте)
                    backgroundOval.setColor(ContextCompat.getColor(context, R.color.calendar_selected_day_bg_dark));
                    frameDayBackgroundContainer.setBackground(backgroundOval);
                    // Цвет текста на выбранном фоне (темный)
                    textViewDayNumber.setTextColor(ContextCompat.getColor(context, R.color.calendar_selected_day_text_dark));
                } else if (isToday) {
                    // Для "сегодня" - прозрачный фон с рамкой
                    backgroundOval.setColor(Color.TRANSPARENT); // Прозрачный фон
                    // Используем атрибут цвета из темы для рамки
                    TypedValue typedValue = new TypedValue();
                    context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSecondary, typedValue, true);
                    backgroundOval.setStroke(dpToPx(1, context), typedValue.data);
                    frameDayBackgroundContainer.setBackground(backgroundOval);
                    // Цвет текста для "сегодня" (цвет рамки)
                    textViewDayNumber.setTextColor(typedValue.data);
                } else {
                    frameDayBackgroundContainer.setBackground(null); // Нет фона
                    // Цвет текста для обычных дней (тусклый серый на темной теме)
                    textViewDayNumber.setTextColor(ContextCompat.getColor(context, R.color.calendar_inactive_day_text_dark));
                }

                layoutTaskIndicatorDots.removeAllViews();
                if (taskCount > 0) {
                    int dotsToShow = Math.min(taskCount, 3);
                    for (int i = 0; i < dotsToShow; i++) {
                        View dot = new View(context);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(4, context), dpToPx(4, context));
                        params.setMarginEnd(dpToPx(1, context));
                        dot.setLayoutParams(params);
                        GradientDrawable dotBg = new GradientDrawable();
                        dotBg.setShape(GradientDrawable.OVAL);
                        // Цвет точек - основной цвет приложения
                        TypedValue primaryColorTypedValue = new TypedValue();
                        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, primaryColorTypedValue, true);
                        dotBg.setColor(primaryColorTypedValue.data);
                        dotBg.setAlpha((int) (255 * 0.6f)); // полупрозрачный
                        dot.setBackground(dotBg);
                        layoutTaskIndicatorDots.addView(dot);
                    }
                    layoutTaskIndicatorDots.setVisibility(View.VISIBLE);
                } else {
                    layoutTaskIndicatorDots.setVisibility(View.INVISIBLE); // Занимает место, но не виден
                }

                layoutDayCellRoot.setOnClickListener(v -> listener.onDateSelected(day));
            }
        }
    }
}