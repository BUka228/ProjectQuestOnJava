package com.example.projectquestonjava.feature.statistics.presentation.screens;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair; // Используем androidx.core.util.Pair
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.core.ui.BaseFragment;
import com.example.projectquestonjava.feature.statistics.data.model.GlobalStatistics;
import com.example.projectquestonjava.feature.statistics.presentation.viewmodel.DatePoint;
import com.example.projectquestonjava.feature.statistics.presentation.viewmodel.DayOfWeekPoint;
import com.example.projectquestonjava.feature.statistics.presentation.viewmodel.StatisticsScreenUiState;
import com.example.projectquestonjava.feature.statistics.presentation.viewmodel.StatisticsViewModel;
import com.example.projectquestonjava.feature.statistics.presentation.viewmodel.StatsPeriod;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StatisticsFragment extends BaseFragment {

    private StatisticsViewModel viewModel;

    // Period Selector Views
    private MaterialButtonToggleGroup toggleButtonGroupPeriod;
    private MaterialButton buttonPeriodWeek, buttonPeriodMonth, buttonPeriodAllTime, buttonCustomDateRange; // Изменил на MaterialButton

    // Overview Card Views
    private View statsOverviewCardViewRoot;

    // Chart Card Views (контейнеры)
    private View chartCardTaskCompletionView, chartCardPomodoroFocusView, chartCardDayOfWeekView,
            chartCardXpGainView, chartCardCoinGainView;

    // Skeleton and Error Views
    private View skeletonLoadingView;
    private View errorStateViewRoot;
    private Button buttonRetryError;
    private LinearLayout mainContentLayout;

    private final DateTimeFormatter chartDateFormatter = DateTimeFormatter.ofPattern("d MMM", new Locale("ru"));


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(StatisticsViewModel.class);

        bindViews(view);
        setupPeriodSelector();
        setupObservers();
    }

    private void bindViews(View view) {
        toggleButtonGroupPeriod = view.findViewById(R.id.toggle_button_group_period_stats);
        buttonPeriodWeek = view.findViewById(R.id.button_period_week_stats);
        buttonPeriodMonth = view.findViewById(R.id.button_period_month_stats);
        buttonPeriodAllTime = view.findViewById(R.id.button_period_all_time_stats);
        buttonCustomDateRange = view.findViewById(R.id.button_custom_date_range_stats);

        mainContentLayout = view.findViewById(R.id.layout_statistics_root_content_visibility_toggle);
        statsOverviewCardViewRoot = view.findViewById(R.id.stats_overview_card_stats_include);

        chartCardTaskCompletionView = view.findViewById(R.id.chart_card_task_completion_stats);
        chartCardPomodoroFocusView = view.findViewById(R.id.chart_card_pomodoro_focus_stats);
        chartCardDayOfWeekView = view.findViewById(R.id.chart_card_day_of_week_stats);
        chartCardXpGainView = view.findViewById(R.id.chart_card_xp_gain_stats);
        chartCardCoinGainView = view.findViewById(R.id.chart_card_coin_gain_stats);

        skeletonLoadingView = view.findViewById(R.id.skeleton_statistics_loading_include);
        errorStateViewRoot = view.findViewById(R.id.error_state_statistics_include);
        if (errorStateViewRoot != null) {
            buttonRetryError = errorStateViewRoot.findViewById(R.id.button_retry_stats);
            buttonRetryError.setOnClickListener(v -> {
                StatisticsScreenUiState state = viewModel.uiStateLiveData.getValue();
                if (state != null) {
                    viewModel.selectCustomDateRange(state.getSelectedStartDate(), state.getSelectedEndDate());
                }
            });
        }
    }

    private void setupPeriodSelector() {
        toggleButtonGroupPeriod.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.button_period_week_stats) viewModel.selectPeriod(StatsPeriod.WEEK);
                else if (checkedId == R.id.button_period_month_stats) viewModel.selectPeriod(StatsPeriod.MONTH);
                else if (checkedId == R.id.button_period_all_time_stats) viewModel.selectPeriod(StatsPeriod.ALL_TIME);
            }
        });
        buttonCustomDateRange.setOnClickListener(v -> showDateRangePicker());
    }

    private void setupBaseLineChartStyle(LineChart chart) {
        if (chart == null) return;
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.getLegend().setEnabled(false);
        chart.setNoDataText("Нет данных для отображения");
        chart.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.onSurfaceVariantLight));

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.onSurfaceVariantLight));
        xAxis.setGranularity(1f);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.onSurfaceVariantLight));
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(ContextCompat.getColor(requireContext(), R.color.outlineVariantLight));
        leftAxis.setAxisMinimum(0f);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    private void setupBaseBarChartStyle(BarChart chart) {
        if (chart == null) return;
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setDrawBarShadow(false);
        chart.setDrawValueAboveBar(true);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setHighlightFullBarEnabled(false);
        chart.setNoDataText("Нет данных для отображения");
        chart.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.onSurfaceVariantLight));

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.onSurfaceVariantLight));

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.onSurfaceVariantLight));
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(ContextCompat.getColor(requireContext(), R.color.outlineVariantLight));
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1f);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    private void setupObservers() {
        viewModel.uiStateLiveData.observe(getViewLifecycleOwner(), uiState -> {
            if (uiState == null) return;

            skeletonLoadingView.setVisibility(uiState.isLoading() ? View.VISIBLE : View.GONE);
            errorStateViewRoot.setVisibility(uiState.getError() != null && !uiState.isLoading() ? View.VISIBLE : View.GONE);
            mainContentLayout.setVisibility(!uiState.isLoading() && uiState.getError() == null ? View.VISIBLE : View.GONE);

            if (uiState.getError() != null && errorStateViewRoot.getVisibility() == View.VISIBLE) {
                TextView errorMsgTv = errorStateViewRoot.findViewById(R.id.textView_error_message_stats);
                if (errorMsgTv != null) errorMsgTv.setText(uiState.getError());
            }

            if (!uiState.isLoading() && uiState.getError() == null) {
                updatePeriodSelectorUi(uiState.getSelectedPeriod(), uiState.getSelectedStartDate(), uiState.getSelectedEndDate());
                updateOverviewCard(
                        statsOverviewCardViewRoot,
                        uiState.getGlobalStats(),
                        uiState.getTaskCompletionRateOverall(),
                        uiState.getAverageTasksPerDayOverall(),
                        uiState.getMostProductiveDayOfWeekOverall()
                );

                updateLineChartCard(chartCardTaskCompletionView, "Выполнение задач",
                        "Выполнено: " + uiState.getTotalTasksCompletedInPeriod(), R.drawable.check_box, null,
                        uiState.getTaskCompletionTrend(), uiState.getSelectedStartDate(), "Задачи");

                updateLineChartCard(chartCardPomodoroFocusView, "Время фокуса (Pomodoro)",
                        "Всего минут: " + uiState.getTotalPomodoroMinutesInPeriod(), R.drawable.timer,
                        String.format(Locale.getDefault(),"Среднее в день: %.1f мин", uiState.getAverageDailyPomodoroMinutes()),
                        uiState.getPomodoroFocusTrend(), uiState.getSelectedStartDate(), "Минуты");

                updateBarChartCard(chartCardDayOfWeekView, "Активность по дням недели",
                        uiState.getMostProductiveDayInPeriod() != null ?
                                "Самый продуктивный: " + capitalizeFirstLetter(uiState.getMostProductiveDayInPeriod().format(DateTimeFormatter.ofPattern("EEEE", new Locale("ru"))))
                                : null,
                        R.drawable.calendar_view_week, null,
                        uiState.getTasksCompletedByDayOfWeek(), "Задачи");

                updateLineChartCard(chartCardXpGainView, "Получено опыта (XP)",
                        "Всего XP: " + uiState.getTotalXpGainedInPeriod(), R.drawable.star, null,
                        uiState.getXpGainTrend(), uiState.getSelectedStartDate(), "XP");

                updateLineChartCard(chartCardCoinGainView, "Получено монет",
                        "Всего монет: " + uiState.getTotalCoinsGainedInPeriod(), R.drawable.paid, null,
                        uiState.getCoinGainTrend(), uiState.getSelectedStartDate(), "Монеты");
            }
        });
    }

    private void updatePeriodSelectorUi(StatsPeriod period, LocalDate startDate, LocalDate endDate) {
        if (period == null || toggleButtonGroupPeriod == null || buttonCustomDateRange == null) return;

        if (period != StatsPeriod.CUSTOM) {
            int buttonIdToCheck = getButtonIdForPeriod(period);
            if (toggleButtonGroupPeriod.getCheckedButtonId() != buttonIdToCheck) {
                toggleButtonGroupPeriod.check(buttonIdToCheck);
            }
        } else {
            if (toggleButtonGroupPeriod.getCheckedButtonId() != View.NO_ID) {
                toggleButtonGroupPeriod.clearChecked();
            }
        }

        if (period == StatsPeriod.CUSTOM && startDate != null && endDate != null) {
            buttonCustomDateRange.setText(String.format(Locale.getDefault(), "%s - %s",
                    startDate.format(chartDateFormatter), endDate.format(chartDateFormatter)));
            buttonCustomDateRange.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primaryLight)));
            buttonCustomDateRange.setTextColor(ContextCompat.getColor(requireContext(), R.color.primaryLight));
        } else {
            buttonCustomDateRange.setText("Диапазон");
            buttonCustomDateRange.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.onSurfaceVariantLight)));
            buttonCustomDateRange.setTextColor(ContextCompat.getColor(requireContext(), R.color.onSurfaceVariantLight));
        }
    }

    private int getButtonIdForPeriod(StatsPeriod period) {
        if (period == null) return View.NO_ID;
        return switch (period) {
            case WEEK -> R.id.button_period_week_stats;
            case MONTH -> R.id.button_period_month_stats;
            case ALL_TIME -> R.id.button_period_all_time_stats;
            default -> View.NO_ID;
        };
    }

    private void updateOverviewCard(View cardRootView, GlobalStatistics stats, Float completionRate, float avgTasks, DayOfWeek mostProductiveDay) {
        if (cardRootView == null) return;
        View completedView = cardRootView.findViewById(R.id.stat_item_completed_overview);
        View avgView = cardRootView.findViewById(R.id.stat_item_avg_tasks_overview);
        View productiveDayView = cardRootView.findViewById(R.id.stat_item_productive_day_overview);

        if (stats != null) {
            ((TextView) completedView.findViewById(R.id.textView_info_value)).setText(String.valueOf(stats.getCompletedTasks()));
            TextView rateTv = completedView.findViewById(R.id.textView_info_subvalue);
            if (completionRate != null) {
                rateTv.setText(String.format(Locale.getDefault(), "%.0f%%", completionRate * 100));
                rateTv.setVisibility(View.VISIBLE);
            } else { rateTv.setVisibility(View.GONE); }
            ((ImageView) completedView.findViewById(R.id.imageView_info_icon)).setImageResource(R.drawable.check_box);
            ((TextView) completedView.findViewById(R.id.textView_info_label)).setText("Выполнено");


            ((TextView) avgView.findViewById(R.id.textView_info_value)).setText(String.format(Locale.getDefault(), "%.1f", avgTasks));
            ((TextView) avgView.findViewById(R.id.textView_info_subvalue)).setText("задач");
            ((ImageView) avgView.findViewById(R.id.imageView_info_icon)).setImageResource(R.drawable.avg_pace);
            ((TextView) avgView.findViewById(R.id.textView_info_label)).setText("Ср. в день");


            ((TextView) productiveDayView.findViewById(R.id.textView_info_value)).setText(
                    mostProductiveDay != null ?
                            capitalizeFirstLetter(mostProductiveDay.getDisplayName(TextStyle.FULL_STANDALONE, new Locale("ru")))
                            : "-"
            );
            ((ImageView) productiveDayView.findViewById(R.id.imageView_info_icon)).setImageResource(R.drawable.calendar_today);
            productiveDayView.findViewById(R.id.textView_info_subvalue).setVisibility(View.GONE);
            ((TextView) productiveDayView.findViewById(R.id.textView_info_label)).setText("Лучший день");

        } else {
            ((TextView) completedView.findViewById(R.id.textView_info_value)).setText("-");
            completedView.findViewById(R.id.textView_info_subvalue).setVisibility(View.GONE);
            ((TextView) avgView.findViewById(R.id.textView_info_value)).setText("-");
            ((TextView) avgView.findViewById(R.id.textView_info_subvalue)).setText("задач");
            ((TextView) productiveDayView.findViewById(R.id.textView_info_value)).setText("-");
        }
    }

    private void updateLineChartCard(View cardView, String title,
                                     @Nullable String summary, @DrawableRes int summaryIconRes, @Nullable String additionalInfo,
                                     List<DatePoint> dataPoints, LocalDate startDateForFormatter, String dataSetLabel) {
        LineChart chart = cardView.findViewById(R.id.line_chart_generic);
        BarChart barChartInCard = cardView.findViewById(R.id.bar_chart_generic);
        if (barChartInCard != null) barChartInCard.setVisibility(View.GONE);
        if (chart == null) {
            updateChartCardCommonInfo(cardView, title, summary, summaryIconRes, additionalInfo, true);
            return;
        }
        chart.setVisibility(View.VISIBLE);

        setupBaseLineChartStyle(chart);
        boolean isEmpty = dataPoints == null || dataPoints.isEmpty() || dataPoints.stream().allMatch(p -> p.value() == 0f);
        updateChartCardCommonInfo(cardView, title, summary, summaryIconRes, additionalInfo, isEmpty);

        if (!isEmpty) {
            ArrayList<Entry> entries = new ArrayList<>();
            for (int i = 0; i < dataPoints.size(); i++) {
                entries.add(new Entry(i, dataPoints.get(i).value()));
            }
            LineDataSet dataSet = new LineDataSet(entries, dataSetLabel);
            dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.primaryLight));
            dataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.primaryLight));
            dataSet.setLineWidth(2f);
            dataSet.setCircleRadius(4f);
            dataSet.setDrawCircleHole(false);
            dataSet.setDrawValues(false);
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            Drawable fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.chart_fill_gradient_primary);
            if (fillDrawable != null) dataSet.setFillDrawable(fillDrawable);
            dataSet.setDrawFilled(true);

            chart.setData(new LineData(dataSet));
            chart.getXAxis().setValueFormatter(new DateAxisValueFormatterMP(startDateForFormatter));
            chart.animateX(500);
            chart.invalidate();
        } else {
            chart.clear();
        }
    }

    private void updateBarChartCard(View cardView, String title,
                                    @Nullable String summary, @DrawableRes int summaryIconRes, @Nullable String additionalInfo,
                                    List<DayOfWeekPoint> dataPoints, String dataSetLabel) {
        BarChart chart = cardView.findViewById(R.id.bar_chart_generic);
        LineChart lineChartInCard = cardView.findViewById(R.id.line_chart_generic);
        if (lineChartInCard != null) lineChartInCard.setVisibility(View.GONE);
        if (chart == null) {
            updateChartCardCommonInfo(cardView, title, summary, summaryIconRes, additionalInfo, true);
            return;
        }
        chart.setVisibility(View.VISIBLE);

        setupBaseBarChartStyle(chart);
        boolean isEmpty = dataPoints == null || dataPoints.isEmpty() || dataPoints.stream().allMatch(p -> p.value() == 0f);
        updateChartCardCommonInfo(cardView, title, summary, summaryIconRes, additionalInfo, isEmpty);

        if (!isEmpty) {
            ArrayList<BarEntry> entries = new ArrayList<>();
            // Убедимся, что есть данные для всех дней недели, даже если 0
            Map<Integer, Float> valueMap = dataPoints.stream()
                    .collect(Collectors.toMap(p -> p.dayOfWeek().getValue(), DayOfWeekPoint::value));
            for (int i = 1; i <= 7; i++) { // От Пн (1) до Вс (7)
                entries.add(new BarEntry(i, valueMap.getOrDefault(i, 0f)));
            }

            BarDataSet dataSet = new BarDataSet(entries, dataSetLabel);
            dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.secondaryLight));
            dataSet.setValueTextSize(10f);
            dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.onSurfaceVariantLight));
            BarData barData = new BarData(dataSet);
            barData.setBarWidth(0.6f);
            chart.setData(barData);
            chart.getXAxis().setLabelCount(7, false);
            chart.getXAxis().setValueFormatter(new DayOfWeekAxisValueFormatterMP());
            chart.setFitBars(true);
            chart.animateY(500);
            chart.invalidate();
        } else {
            chart.clear();
        }
    }

    private void updateChartCardCommonInfo(View cardView, String title, @Nullable String summary,
                                           @DrawableRes int summaryIconRes, @Nullable String additionalInfo, boolean isEmpty) {
        if (cardView == null) return;
        TextView titleTv = cardView.findViewById(R.id.textView_chart_title_card);
        TextView emptyDataTv = cardView.findViewById(R.id.textView_chart_empty_data_card);
        LinearLayout summaryLayout = cardView.findViewById(R.id.layout_chart_summary_card);
        ImageView summaryIconIv = cardView.findViewById(R.id.imageView_chart_summary_icon_card);
        TextView summaryTextTv = cardView.findViewById(R.id.textView_chart_summary_text_card);
        TextView additionalInfoTv = cardView.findViewById(R.id.textView_chart_additional_info_card);
        ProgressBar chartLoadingPb = cardView.findViewById(R.id.progressBar_chart_loading_card);

        if (titleTv != null) titleTv.setText(title);
        if (emptyDataTv != null) emptyDataTv.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (summaryLayout != null) summaryLayout.setVisibility(!isEmpty ? View.VISIBLE : View.GONE);
        if (chartLoadingPb != null) chartLoadingPb.setVisibility(View.GONE);

        if (!isEmpty && summaryLayout != null) {
            if (summaryTextTv != null && summaryIconIv != null) {
                if (summary != null && !summary.isEmpty()) {
                    summaryTextTv.setText(summary);
                    summaryTextTv.setVisibility(View.VISIBLE);
                    if (summaryIconRes != 0) {
                        summaryIconIv.setImageResource(summaryIconRes);
                        summaryIconIv.setVisibility(View.VISIBLE);
                    } else {
                        summaryIconIv.setVisibility(View.GONE);
                    }
                } else {
                    summaryTextTv.setVisibility(View.GONE);
                    summaryIconIv.setVisibility(View.GONE);
                }
            }
            if (additionalInfoTv != null) {
                if (additionalInfo != null && !additionalInfo.isEmpty()) {
                    additionalInfoTv.setText(additionalInfo);
                    additionalInfoTv.setVisibility(View.VISIBLE);
                } else {
                    additionalInfoTv.setVisibility(View.GONE);
                }
            }
        }
    }

    private void showDateRangePicker() {
        StatisticsScreenUiState currentState = viewModel.uiStateLiveData.getValue();
        if (currentState == null) return;

        MaterialDatePicker.Builder<androidx.core.util.Pair<Long, Long>> builder =
                MaterialDatePicker.Builder.dateRangePicker();
        builder.setTitleText("Выберите диапазон");
        builder.setSelection(new androidx.core.util.Pair<>(
                currentState.getSelectedStartDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                currentState.getSelectedEndDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        ));
        final MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection.first != null && selection.second != null) {
                LocalDate startDate = Instant.ofEpochMilli(selection.first).atZone(ZoneOffset.UTC).toLocalDate();
                LocalDate endDate = Instant.ofEpochMilli(selection.second).atZone(ZoneOffset.UTC).toLocalDate();
                viewModel.selectCustomDateRange(startDate, endDate.isBefore(startDate) ? startDate : endDate);
            }
        });
        picker.show(getChildFragmentManager(), picker.toString());
    }

    @Override
    protected void setupToolbar() {
        MaterialToolbar toolbar = getToolbar();
        if (toolbar != null) {
            toolbar.setTitle("Статистика");
            toolbar.setNavigationIcon(R.drawable.arrow_back_ios_new);
            toolbar.setNavigationOnClickListener(v -> {
                if (!NavHostFragment.findNavController(this).popBackStack()) {
                    // Если не удалось вернуться назад, возможно, это корневой экран
                    // requireActivity().finish(); // Закрыть Activity
                }
            });
            toolbar.getMenu().clear();
        }
    }

    @Override
    protected void setupFab() {
        if (getExtendedFab() != null) getExtendedFab().hide();
        if (getStandardFab() != null) getStandardFab().hide();
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase(new Locale("ru")) + str.substring(1);
    }

    private static class DateAxisValueFormatterMP extends ValueFormatter {
        private final LocalDate startDate;
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM", new Locale("ru"));
        public DateAxisValueFormatterMP(LocalDate startDate) { this.startDate = startDate; }
        @Override public String getFormattedValue(float value) {
            return startDate.plusDays((long) value).format(formatter);
        }
    }
    private static class DayOfWeekAxisValueFormatterMP extends ValueFormatter {
        private final List<String> days = List.of("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс");
        @Override public String getFormattedValue(float value) {
            int index = Math.round(value) - 1; // Округляем и вычитаем 1 для индекса
            return (index >= 0 && index < days.size()) ? days.get(index) : "";
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        toggleButtonGroupPeriod = null; buttonPeriodWeek = null; buttonPeriodMonth = null;
        buttonPeriodAllTime = null; buttonCustomDateRange = null;
        statsOverviewCardViewRoot = null; chartCardTaskCompletionView = null;
        chartCardPomodoroFocusView = null; chartCardDayOfWeekView = null;
        chartCardXpGainView = null; chartCardCoinGainView = null;
        skeletonLoadingView = null; errorStateViewRoot = null; buttonRetryError = null;
        mainContentLayout = null;
    }
}