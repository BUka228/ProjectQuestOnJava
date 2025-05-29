package com.example.projectquestonjava.approach.calendar.presentation.ui_parts;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment; // Импорт для Fragment
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment; // Для навигации
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.approach.calendar.domain.model.CalendarTaskSummary;
import com.example.projectquestonjava.approach.calendar.presentation.screens.CalendarDashboardFragment; // Для instanceof
import com.example.projectquestonjava.approach.calendar.presentation.screens.CalendarPlanningFragment; // Для instanceof
import com.example.projectquestonjava.approach.calendar.presentation.screens.TaskPlanningListAdapter; // Для интерфейса
import com.example.projectquestonjava.approach.calendar.presentation.viewmodels.CalendarDashboardViewModel;
import com.example.projectquestonjava.approach.calendar.presentation.viewmodels.CalendarPlanningViewModel; // Импорт
import com.example.projectquestonjava.core.data.model.core.Tag;
import com.example.projectquestonjava.core.data.model.enums.Priority;
import com.example.projectquestonjava.core.data.model.enums.TaskStatus;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import dagger.hilt.android.AndroidEntryPoint; // Добавил, если используется Hilt

@AndroidEntryPoint // Добавил, если используется Hilt
public class TaskDetailsBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String ARG_TASK_ID_FOR_DETAILS = "arg_task_id_for_details";
    private static final String ARG_IS_DASHBOARD_VIEW = "arg_is_dashboard_view";

    private CalendarDashboardViewModel dashboardViewModel;
    private CalendarPlanningViewModel planningViewModel; // Добавлено поле

    private long currentTaskId = -1L;
    private boolean isDashboardSource = true;

    // Views
    private TextView titleView, dateView, timeView, descLabel, descView, priorityLabelText, pomodoroCountText, tagsLabel;
    private CheckBox doneCheckbox;
    private LinearLayout priorityBadgeLayout, pomodoroContainerLayout;
    private ImageView priorityIconView;
    private ChipGroup tagsChipGroup;
    private Button pomodoroActionButton;
    private ImageButton deleteButton, editButton;
    private View spacerAfterDesc, spacerAfterPriority, spacerAfterTags;

    // Listener для колбэков в родительский фрагмент (для Planning)
    private TaskPlanningListAdapter.OnPlanningTaskItemClickListener planningTaskClickListener;


    public static TaskDetailsBottomSheetFragment newInstance(long taskId, boolean isFromDashboard) {
        TaskDetailsBottomSheetFragment fragment = new TaskDetailsBottomSheetFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_TASK_ID_FOR_DETAILS, taskId);
        args.putBoolean(ARG_IS_DASHBOARD_VIEW, isFromDashboard);
        fragment.setArguments(args);
        return fragment;
    }

    public TaskDetailsBottomSheetFragment() {}

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Fragment parent = getParentFragment();
        if (!isDashboardSource && parent instanceof TaskPlanningListAdapter.OnPlanningTaskItemClickListener) {
            planningTaskClickListener = (TaskPlanningListAdapter.OnPlanningTaskItemClickListener) parent;
        }
        // Для dashboardClickListener аналогично, если бы он был нужен для прямых вызовов
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentTaskId = getArguments().getLong(ARG_TASK_ID_FOR_DETAILS, -1L);
            isDashboardSource = getArguments().getBoolean(ARG_IS_DASHBOARD_VIEW, true);
        }

        if (isDashboardSource) {
            dashboardViewModel = new ViewModelProvider(requireParentFragment()).get(CalendarDashboardViewModel.class);
        } else {
            planningViewModel = new ViewModelProvider(requireParentFragment()).get(CalendarPlanningViewModel.class);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            BottomSheetDialog dInternal = (BottomSheetDialog) d;
            View bottomSheetInternal = dInternal.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheetInternal != null) {
                BottomSheetBehavior.from(bottomSheetInternal).setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_task_details_dashboard, container, false);
        bindViewsFromLayout(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (currentTaskId == -1L) {
            dismissAllowingStateLoss();
            return;
        }

        if (isDashboardSource && dashboardViewModel != null) {
            dashboardViewModel.taskDetailsForBottomSheetLiveData.observe(getViewLifecycleOwner(), summary -> {
                if (summary != null && summary.getId() == currentTaskId) {
                    populateViews(summary);
                } else if (summary == null && isVisible()) {
                    dismissAllowingStateLoss();
                }
            });
        } else if (!isDashboardSource && planningViewModel != null) {
            planningViewModel.taskDetailsForBottomSheetLiveData.observe(getViewLifecycleOwner(), summary -> {
                if (summary != null && summary.getId() == currentTaskId) {
                    populateViews(summary);
                } else if (summary == null && isVisible()) {
                    dismissAllowingStateLoss();
                }
            });
        }
    }

    private void bindViewsFromLayout(View view) {
        titleView = view.findViewById(R.id.textView_details_title);
        doneCheckbox = view.findViewById(R.id.checkbox_details_done);
        dateView = view.findViewById(R.id.textView_details_date);
        timeView = view.findViewById(R.id.textView_details_time);
        descLabel = view.findViewById(R.id.textView_details_description_label);
        descView = view.findViewById(R.id.textView_details_description);
        spacerAfterDesc = view.findViewById(R.id.spacer_after_description);
        priorityBadgeLayout = view.findViewById(R.id.layout_details_priority_badge);
        priorityIconView = view.findViewById(R.id.imageView_details_priority_icon);
        priorityLabelText = view.findViewById(R.id.textView_details_priority_label);
        spacerAfterPriority = view.findViewById(R.id.spacer_after_priority);
        pomodoroContainerLayout = view.findViewById(R.id.pomodoro_counter_container_details);
        pomodoroCountText = view.findViewById(R.id.text_pomodoro_count_details);
        tagsLabel = view.findViewById(R.id.textView_details_tags_label);
        tagsChipGroup = view.findViewById(R.id.chipGroup_details_tags);
        spacerAfterTags = view.findViewById(R.id.spacer_after_tags);
        pomodoroActionButton = view.findViewById(R.id.button_details_pomodoro_action);
        deleteButton = view.findViewById(R.id.button_details_delete);
        editButton = view.findViewById(R.id.button_details_edit);
    }

    private void populateViews(CalendarTaskSummary summary) {
        titleView.setText(summary.getTitle());
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", new Locale("ru"));
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        dateView.setText(summary.getDueDate().format(dateFormatter));
        timeView.setText(summary.getDueDate().format(timeFormatter));

        if (summary.getDescription() != null && !summary.getDescription().isEmpty()) {
            descLabel.setVisibility(View.VISIBLE);
            descView.setVisibility(View.VISIBLE);
            spacerAfterDesc.setVisibility(View.VISIBLE);
            descView.setText(summary.getDescription());
        } else {
            descLabel.setVisibility(View.GONE);
            descView.setVisibility(View.GONE);
            spacerAfterDesc.setVisibility(View.GONE);
        }

        Priority priority = summary.getPriority();
        if (priority != null) {
            priorityBadgeLayout.setVisibility(View.VISIBLE);
            spacerAfterPriority.setVisibility(View.VISIBLE);
            int colorRes = R.color.priority_low_container_bg;
            int iconRes = R.drawable.low_priority;
            String priorityText = "Низкий";

            switch (priority) {
                case CRITICAL:
                    colorRes = R.color.priority_critical_container_bg;
                    iconRes = R.drawable.priority_high;
                    priorityText = "Критический";
                    break;
                case HIGH:
                    colorRes = R.color.priority_high_container_bg;
                    iconRes = R.drawable.priority_high;
                    priorityText = "Высокий";
                    break;
                case MEDIUM:
                    colorRes = R.color.priority_medium_container_bg;
                    iconRes = R.drawable.priority_medium_icon;
                    priorityText = "Средний";
                    break;
                case LOW: // Уже установлено по умолчанию
                    break;
            }
            priorityIconView.setImageResource(iconRes);
            Drawable bg = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_background_surface_variant_alpha);
            if (bg instanceof GradientDrawable) {
                ((GradientDrawable) bg.mutate()).setColor(ContextCompat.getColor(requireContext(), colorRes));
                priorityBadgeLayout.setBackground(bg);
            } else {
                priorityBadgeLayout.setBackgroundColor(ContextCompat.getColor(requireContext(), colorRes));
            }
            priorityLabelText.setText(priorityText);
            // TODO: Установить цвет текста и иконки в badge в зависимости от colorRes для контраста
            priorityLabelText.setTextColor(Color.BLACK); // Пример
            ImageViewCompat.setImageTintList(priorityIconView, ColorStateList.valueOf(Color.BLACK));

        } else {
            priorityBadgeLayout.setVisibility(View.GONE);
            spacerAfterPriority.setVisibility(View.GONE);
        }

        pomodoroCountText.setText(String.valueOf(summary.getPomodoroCount()));
        pomodoroContainerLayout.setOnClickListener(v -> {
            if (isDashboardSource && dashboardViewModel != null) {
                dashboardViewModel.startPomodoroForTask(summary.getId());
            } else if (!isDashboardSource && planningTaskClickListener != null) {
                planningTaskClickListener.onPomodoroStartRequest(summary);
            }
            dismiss();
        });

        tagsChipGroup.removeAllViews();
        if (summary.getTags() != null && !summary.getTags().isEmpty()) {
            tagsLabel.setVisibility(View.VISIBLE);
            tagsChipGroup.setVisibility(View.VISIBLE);
            spacerAfterTags.setVisibility(View.VISIBLE);
            for (Tag tag : summary.getTags()) {
                Chip chip = (Chip) LayoutInflater.from(getContext()).inflate(R.layout.chip_tag_item_details, tagsChipGroup, false);
                chip.setText(tag.getName());
                chip.setOnClickListener(v -> {
                    if (isDashboardSource && dashboardViewModel != null) {
                        dashboardViewModel.addTagToFilter(tag);
                    }
                    // Для планирования клик по тегу в деталях может не делать ничего или закрывать
                    dismiss();
                });
                tagsChipGroup.addView(chip);
            }
        } else {
            tagsLabel.setVisibility(View.GONE);
            tagsChipGroup.setVisibility(View.GONE);
            spacerAfterTags.setVisibility(View.GONE);
        }

        if (isDashboardSource && dashboardViewModel != null) {
            doneCheckbox.setVisibility(View.VISIBLE);
            doneCheckbox.setOnCheckedChangeListener(null);
            doneCheckbox.setChecked(summary.getStatus() == TaskStatus.DONE);
            doneCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) {
                    dashboardViewModel.handleSwipeAction(summary.getId(), CalendarDashboardViewModel.SwipeDirection.RIGHT);
                }
            });
        } else {
            doneCheckbox.setVisibility(View.GONE);
        }

        pomodoroActionButton.setOnClickListener(v -> {
            if (isDashboardSource && dashboardViewModel != null) {
                dashboardViewModel.startPomodoroForTask(summary.getId());
            } else if (!isDashboardSource && planningTaskClickListener != null) {
                planningTaskClickListener.onPomodoroStartRequest(summary);
            }
            dismiss();
        });

        deleteButton.setOnClickListener(v -> {
            if (isDashboardSource && dashboardViewModel != null) {
                dashboardViewModel.handleSwipeAction(summary.getId(), CalendarDashboardViewModel.SwipeDirection.LEFT);
            } else if (!isDashboardSource && planningViewModel != null) {
                // Для планировщика, возможно, нужно показать диалог подтверждения перед удалением
                // Или вызвать метод ViewModel, который это сделает.
                // Для простоты пока прямой вызов:
                planningViewModel.deleteTask(summary.getId());
            }
            dismiss();
        });

        editButton.setOnClickListener(v -> {
            if (isDashboardSource && dashboardViewModel != null) {
                dashboardViewModel.editTask(summary.getId());
            } else if (!isDashboardSource && planningTaskClickListener != null) {
                planningTaskClickListener.onTaskEditRequest(summary);
            }
            dismiss();
        });
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (isDashboardSource && dashboardViewModel != null) {
            dashboardViewModel.clearRequestedTaskDetails();
        } else if (!isDashboardSource && planningViewModel != null) {
            planningViewModel.clearTaskDetails();
        }

        Fragment parent = getParentFragment();
        if (parent instanceof CalendarPlanningFragment) {
            ((CalendarPlanningFragment) parent).onDetailsSheetDismissed();
        } else if (parent instanceof CalendarDashboardFragment) {
            ((CalendarDashboardFragment) parent).onDetailsSheetDismissed();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Очистка ссылок на View
        titleView = null; dateView = null; timeView = null; descLabel = null; descView = null;
        priorityLabelText = null; pomodoroCountText = null; tagsLabel = null; doneCheckbox = null;
        priorityBadgeLayout = null; pomodoroContainerLayout = null; priorityIconView = null;
        tagsChipGroup = null; pomodoroActionButton = null; deleteButton = null; editButton = null;
        spacerAfterDesc = null; spacerAfterPriority = null; spacerAfterTags = null;
    }
}