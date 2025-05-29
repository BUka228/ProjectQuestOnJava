package com.example.projectquestonjava.approach.calendar.presentation.ui_parts;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout; // Добавлен FrameLayout
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.approach.calendar.presentation.viewmodels.CalendarPlanningViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CalendarMoveSheetFragment extends BottomSheetDialogFragment {

    private CalendarPlanningViewModel planningViewModel;
    private TextView textViewCurrentMonth;
    private RecyclerView recyclerViewDays;
    private DayAdapterForMoveSheet adapter;
    private YearMonth currentDisplayMonth;

    public CalendarMoveSheetFragment() {}

    public static CalendarMoveSheetFragment newInstance(YearMonth initialMonth) {
        CalendarMoveSheetFragment fragment = new CalendarMoveSheetFragment();
        Bundle args = new Bundle();
        args.putSerializable("initialMonth", initialMonth);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        planningViewModel = new ViewModelProvider(requireParentFragment()).get(CalendarPlanningViewModel.class);
        if (getArguments() != null && getArguments().containsKey("initialMonth")) {
            currentDisplayMonth = (YearMonth) getArguments().getSerializable("initialMonth");
        } else {
            currentDisplayMonth = Objects.requireNonNull(planningViewModel.currentMonthLiveData.getValue());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_calendar_move, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textViewCurrentMonth = view.findViewById(R.id.textView_current_month_move_sheet);
        recyclerViewDays = view.findViewById(R.id.recyclerView_days_move_sheet);
        ImageButton prevMonthButton = view.findViewById(R.id.button_prev_month_move_sheet);
        ImageButton nextMonthButton = view.findViewById(R.id.button_next_month_move_sheet);
        view.findViewById(R.id.button_cancel_move_sheet).setOnClickListener(v -> dismiss());

        recyclerViewDays.setLayoutManager(new GridLayoutManager(getContext(), 7));
        adapter = new DayAdapterForMoveSheet(requireContext(), getDaysForCurrentDisplayMonth(), date -> {
            planningViewModel.onMoveDateSelected(date);
            dismiss();
        });
        recyclerViewDays.setAdapter(adapter);

        prevMonthButton.setOnClickListener(v -> {
            currentDisplayMonth = currentDisplayMonth.minusMonths(1);
            updateCalendarDisplay();
        });
        nextMonthButton.setOnClickListener(v -> {
            currentDisplayMonth = currentDisplayMonth.plusMonths(1);
            updateCalendarDisplay();
        });

        updateCalendarDisplay();
    }

    private void updateCalendarDisplay() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("LLLL yyyy", new Locale("ru"));
        String formattedMonth = currentDisplayMonth.format(formatter);
        textViewCurrentMonth.setText(formattedMonth.substring(0, 1).toUpperCase() + formattedMonth.substring(1));
        adapter.updateDays(getDaysForCurrentDisplayMonth());
    }

    private List<LocalDate> getDaysForCurrentDisplayMonth() {
        List<LocalDate> days = new ArrayList<>();
        LocalDate firstDayOfMonth = currentDisplayMonth.atDay(1);
        int firstDayOfWeekValue = firstDayOfMonth.getDayOfWeek().getValue();
        int daysOffset = firstDayOfWeekValue - 1;

        for (int i = 0; i < daysOffset; i++) days.add(null);
        for (int i = 1; i <= currentDisplayMonth.lengthOfMonth(); i++) days.add(currentDisplayMonth.atDay(i));
        // Заполняем до 6 недель, если необходимо
        int cellsToFill = (daysOffset + currentDisplayMonth.lengthOfMonth() <= 35) ? 35 : 42;
        while (days.size() < cellsToFill) {
            days.add(null);
        }
        return days;
    }

    private static class DayAdapterForMoveSheet extends RecyclerView.Adapter<DayAdapterForMoveSheet.DayViewHolder> {
        private final Context context;
        private List<LocalDate> days;
        private final OnDateSelectedListener listener;

        interface OnDateSelectedListener { void onDateSelected(LocalDate date); }

        DayAdapterForMoveSheet(Context context, List<LocalDate> days, OnDateSelectedListener listener) {
            this.context = context;
            this.days = days;
            this.listener = listener;
        }

        void updateDays(List<LocalDate> newDays) {
            this.days = newDays;
            notifyDataSetChanged();
        }

        @NonNull @Override
        public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Используем новый макет item_day_cell_move_sheet
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_day_cell_move_sheet, parent, false);
            return new DayViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
            LocalDate day = days.get(position);
            holder.bind(day, listener, context);
        }

        @Override public int getItemCount() { return days.size(); }

        static class DayViewHolder extends RecyclerView.ViewHolder {
            FrameLayout layoutDayCellRoot;
            FrameLayout frameDayBackgroundContainer;
            TextView textViewDayNumber;

            DayViewHolder(@NonNull View itemView) {
                super(itemView);
                layoutDayCellRoot = (FrameLayout) itemView;
                frameDayBackgroundContainer = itemView.findViewById(R.id.frame_day_background_move_container);
                textViewDayNumber = itemView.findViewById(R.id.textView_day_number_move);
            }

            void bind(@Nullable LocalDate day, OnDateSelectedListener listener, Context context) {
                if (day == null) {
                    itemView.setVisibility(View.INVISIBLE);
                    frameDayBackgroundContainer.setBackground(null);
                    return;
                }
                itemView.setVisibility(View.VISIBLE);
                textViewDayNumber.setText(String.valueOf(day.getDayOfMonth()));

                boolean isToday = day.equals(LocalDate.now());

                GradientDrawable backgroundOval = new GradientDrawable();
                backgroundOval.setShape(GradientDrawable.OVAL);

                // Для MoveSheet выбранной даты нет, есть только "сегодня"
                if (isToday) {
                    backgroundOval.setColor(Color.TRANSPARENT);
                    TypedValue typedValue = new TypedValue();
                    context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSecondaryContainer, typedValue, true); // Используем атрибут для фона "сегодня"
                    backgroundOval.setStroke(dpToPx(1, context), typedValue.data);
                    frameDayBackgroundContainer.setBackground(backgroundOval);
                    context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSecondaryContainer, typedValue, true);
                    textViewDayNumber.setTextColor(typedValue.data);
                } else {
                    frameDayBackgroundContainer.setBackground(null);
                    // Цвет текста для обычных дней - цвет по умолчанию для текста на ?attr/colorSurfaceContainer
                    TypedValue typedValue = new TypedValue();
                    context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
                    textViewDayNumber.setTextColor(typedValue.data);
                }
                itemView.setOnClickListener(v -> listener.onDateSelected(day));
            }
            private static int dpToPx(int dp, Context context) {
                return (int) (dp * context.getResources().getDisplayMetrics().density);
            }
        }
    }
}