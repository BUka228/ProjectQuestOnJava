package com.example.projectquestonjava.feature.pomodoro.presentation.ui_elements;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.core.data.model.core.Task;
import com.example.projectquestonjava.feature.pomodoro.presentation.adapters.TaskSelectionAdapter;
import com.example.projectquestonjava.feature.pomodoro.presentation.viewmodels.PomodoroViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.Collections;
import java.util.List;

public class TaskSelectorBottomSheetFragment extends BottomSheetDialogFragment implements TaskSelectionAdapter.OnTaskSelectedListener {

    private PomodoroViewModel viewModel;
    private RecyclerView recyclerViewTasks;
    private TaskSelectionAdapter adapter;
    private TextView noTasksTextView;

    public static TaskSelectorBottomSheetFragment newInstance() {
        return new TaskSelectorBottomSheetFragment();
    }

    public TaskSelectorBottomSheetFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireParentFragment()).get(PomodoroViewModel.class);
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
        return inflater.inflate(R.layout.bottom_sheet_task_selector_pomodoro, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerViewTasks = view.findViewById(R.id.recyclerView_pomodoro_task_selection);
        noTasksTextView = view.findViewById(R.id.textView_no_upcoming_tasks);
        view.findViewById(R.id.button_close_task_selector).setOnClickListener(v -> dismiss());

        adapter = new TaskSelectionAdapter(this);
        recyclerViewTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewTasks.setAdapter(adapter);

        viewModel.upcomingTasksLiveData.observe(getViewLifecycleOwner(), tasks -> {
            List<Task> taskList = tasks != null ? tasks : Collections.emptyList();
            adapter.submitList(taskList);
            noTasksTextView.setVisibility(taskList.isEmpty() ? View.VISIBLE : View.GONE);
            recyclerViewTasks.setVisibility(taskList.isEmpty() ? View.GONE : View.VISIBLE);
        });

        viewModel.currentTaskLiveData.observe(getViewLifecycleOwner(), currentTask -> {
            adapter.setCurrentTaskId(currentTask != null ? currentTask.getId() : -1L);
        });
    }

    @Override
    public void onTaskSelected(Task task) {
        viewModel.setCurrentTask(task);
        dismiss();
    }
}