// File: A:\Progects\ProjectQuestOnJava\app\src\main\java\com\example\projectquestonjava\approach\calendar\presentation\screens\CalendarTaskCreationFragment.java
package com.example.projectquestonjava.approach.calendar.presentation.screens;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.app.MainActivity;
import com.example.projectquestonjava.approach.calendar.domain.model.TaskCreationEvent;
import com.example.projectquestonjava.approach.calendar.domain.model.TaskCreationState;
import com.example.projectquestonjava.approach.calendar.domain.model.TaskInput;
import com.example.projectquestonjava.approach.calendar.presentation.viewmodels.Loadable;
import com.example.projectquestonjava.approach.calendar.presentation.viewmodels.TaskCreationViewModel;
import com.example.projectquestonjava.core.data.model.core.Tag;
import com.example.projectquestonjava.core.ui.BaseFragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CalendarTaskCreationFragment extends BaseFragment {

    private TaskCreationViewModel viewModel;

    // UI Elements
    private TextInputLayout textInputLayoutTitle, textInputLayoutDescription;
    private TextInputEditText editTextTitle, editTextDescription;
    private Button buttonSelectDate, buttonSelectTime, buttonSelectRecurrence;
    private ChipGroup chipGroupTags;
    private ImageButton buttonDeleteSelectedTags, buttonAddTag;
    private TextView textViewNoTags;
    private ProgressBar progressBarTagsLoading;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault());
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault());

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar_task_creation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TaskCreationViewModel.class);

        bindViews(view);
        setupListeners();
        observeViewModel();
    }

    private void bindViews(View view) {
        textInputLayoutTitle = view.findViewById(R.id.textInputLayout_task_title);
        editTextTitle = view.findViewById(R.id.editText_task_title);
        textInputLayoutDescription = view.findViewById(R.id.textInputLayout_task_description);
        editTextDescription = view.findViewById(R.id.editText_task_description);
        buttonSelectDate = view.findViewById(R.id.button_select_date);
        buttonSelectTime = view.findViewById(R.id.button_select_time);
        buttonSelectRecurrence = view.findViewById(R.id.button_select_recurrence);
        chipGroupTags = view.findViewById(R.id.chipGroup_task_creation_tags);
        buttonDeleteSelectedTags = view.findViewById(R.id.button_delete_selected_tags);
        buttonAddTag = view.findViewById(R.id.button_add_tag);
        textViewNoTags = view.findViewById(R.id.textView_no_tags);
        progressBarTagsLoading = view.findViewById(R.id.progressBar_tags_loading);
    }

    private void setupListeners() {
        editTextTitle.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { viewModel.updateTitle(s.toString());}
            @Override public void afterTextChanged(Editable s) {}
        });
        editTextDescription.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { viewModel.updateDescription(s.toString());}
            @Override public void afterTextChanged(Editable s) {}
        });

        buttonSelectDate.setOnClickListener(v -> viewModel.openDateDialog());
        buttonSelectTime.setOnClickListener(v -> viewModel.openTimeDialog());
        buttonSelectRecurrence.setOnClickListener(v -> viewModel.openRecurrenceDialog());
        buttonAddTag.setOnClickListener(v -> showAddTagDialog());
        buttonDeleteSelectedTags.setOnClickListener(v -> {
            TaskCreationState state = viewModel.uiStateLiveData.getValue();
            if (state != null && state.getTaskInput().getSelectedTags() != null && !state.getTaskInput().getSelectedTags().isEmpty()) {
                showDeleteSelectedTagsDialog(state.getTaskInput().getSelectedTags().size());
            }
        });
    }

    private void observeViewModel() {
        viewModel.combinedStateLiveData.observe(getViewLifecycleOwner(), combinedState -> {
            if (combinedState == null) return;
            TaskCreationState state = combinedState.getTaskState();
            Loadable<List<Tag>> tagsLoadable = combinedState.getTags();

            // Update input fields
            if (!Objects.equals(editTextTitle.getText().toString(), state.getTaskInput().getTitle())) {
                editTextTitle.setText(state.getTaskInput().getTitle());
            }
            if (!Objects.equals(editTextDescription.getText().toString(), state.getTaskInput().getDescription())) {
                editTextDescription.setText(state.getTaskInput().getDescription());
            }
            LocalDateTime dueDate = state.getTaskInput().getDueDate();
            buttonSelectDate.setText(dueDate.format(dateFormatter));
            buttonSelectTime.setText(dueDate.format(timeFormatter));
            buttonSelectRecurrence.setText(state.getTaskInput().getRecurrenceRule() != null ? state.getTaskInput().getRecurrenceRule() : "Не повторяется");

            boolean isEnabled = !state.isLoading();
            editTextTitle.setEnabled(isEnabled);
            editTextDescription.setEnabled(isEnabled);
            buttonSelectDate.setEnabled(isEnabled);
            buttonSelectTime.setEnabled(isEnabled);
            buttonSelectRecurrence.setEnabled(isEnabled);
            buttonAddTag.setEnabled(isEnabled);
            buttonDeleteSelectedTags.setEnabled(isEnabled && state.getTaskInput().getSelectedTags() != null && !state.getTaskInput().getSelectedTags().isEmpty());
            buttonDeleteSelectedTags.setVisibility(state.getTaskInput().getSelectedTags() != null && !state.getTaskInput().getSelectedTags().isEmpty() ? View.VISIBLE : View.GONE);


            // Update tags section
            progressBarTagsLoading.setVisibility(tagsLoadable instanceof Loadable.Loading ? View.VISIBLE : View.GONE);
            if (tagsLoadable instanceof Loadable.Success) {
                List<Tag> allTags = ((Loadable.Success<List<Tag>>) tagsLoadable).getData();
                textViewNoTags.setVisibility(allTags.isEmpty() ? View.VISIBLE : View.GONE);
                chipGroupTags.setVisibility(allTags.isEmpty() ? View.GONE : View.VISIBLE);
                updateTagsChips(allTags, state.getTaskInput().getSelectedTags());
            } else if (tagsLoadable instanceof Loadable.Error) {
                textViewNoTags.setText("Ошибка загрузки тегов");
                textViewNoTags.setVisibility(View.VISIBLE);
                chipGroupTags.setVisibility(View.GONE);
            }

            // Dialogs
            if (state.isDateDialogOpen()) showDatePicker(state.getTaskInput().getDueDate().toLocalDate());
            if (state.isTimeDialogOpen()) showTimePicker(state.getTaskInput().getDueDate().toLocalTime());
            if (state.isRecurrenceDialogOpen()) showRecurrenceDialog(state.getTaskInput().getRecurrenceRule());

            // Events
            if (state.getEvent() != null) {
                if (state.getEvent() == TaskCreationEvent.TASK_CREATED || state.getEvent() == TaskCreationEvent.TASK_UPDATED) {
                    // В XML навигация обычно управляется NavController
                    NavHostFragment.findNavController(this).popBackStack();
                    viewModel.clearEvent();
                }
            }

            // Error Snackbar
            if (state.getError() != null && getView() != null) {
                Snackbar.make(requireView(), state.getError(), Snackbar.LENGTH_LONG).show();
                viewModel.clearError();
            }

            // Обновление FAB и Toolbar (наследники BaseFragment должны это делать)
            if(getExtendedFab() != null) {
                getExtendedFab().setText(viewModel.getSaveButtonText());
                getExtendedFab().setEnabled(isEnabled);
            }
            if(getToolbar() != null) {
                getToolbar().setTitle(viewModel.getScreenTitle());
            }
        });
    }

    private void updateTagsChips(List<Tag> allTags, Set<Tag> selectedTags) {
        chipGroupTags.removeAllViews();
        if (allTags == null) return;

        Set<Long> selectedTagIds = selectedTags != null
                ? selectedTags.stream().map(Tag::getId).collect(Collectors.toSet())
                : Collections.emptySet();

        for (Tag tag : allTags) {
            Chip chip = (Chip) LayoutInflater.from(getContext()).inflate(R.layout.chip_filter_tag_item, chipGroupTags, false);
            // Chip chip = new Chip(requireContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Filter);
            chip.setText(tag.getName());
            chip.setCheckable(true);
            chip.setChecked(selectedTagIds.contains(tag.getId()));
            chip.setOnClickListener(v -> viewModel.toggleTagSelection(tag));
            chip.setCloseIconVisible(false); // Для создания/редактирования не нужен крестик на чипе
            chipGroupTags.addView(chip);
        }
    }

    private void showDatePicker(LocalDate initialDate) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> viewModel.setDueDate(LocalDate.of(year, month + 1, dayOfMonth)),
                initialDate.getYear(),
                initialDate.getMonthValue() - 1,
                initialDate.getDayOfMonth()
        );
        datePickerDialog.setOnDismissListener(dialog -> viewModel.closeDateDialog());
        datePickerDialog.show();
    }

    private void showTimePicker(LocalTime initialTime) {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> viewModel.setDueTime(LocalTime.of(hourOfDay, minute)),
                initialTime.getHour(),
                initialTime.getMinute(),
                true // is24HourView
        );
        timePickerDialog.setOnDismissListener(dialog -> viewModel.closeTimeDialog());
        timePickerDialog.show();
    }

    private void showRecurrenceDialog(String currentRule) {
        final String[] rules = {"DAILY", "WEEKLY", "MONTHLY", "YEARLY"};
        final String[] ruleLabels = {"Ежедневно", "Еженедельно", "Ежемесячно", "Ежегодно", "Не повторять"};
        int checkedItem = -1;
        if (currentRule != null) {
            for (int i = 0; i < rules.length; i++) {
                if (rules[i].equals(currentRule)) {
                    checkedItem = i;
                    break;
                }
            }
        } else {
            checkedItem = ruleLabels.length - 1; // "Не повторять"
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Выберите повторение")
                .setSingleChoiceItems(ruleLabels, checkedItem, (dialog, which) -> {
                    String selectedRule = (which == ruleLabels.length - 1) ? null : rules[which];
                    viewModel.setRecurrenceRule(selectedRule);
                    dialog.dismiss();
                })
                .setNegativeButton("Отмена", (dialog, which) -> viewModel.closeRecurrenceDialog())
                .setOnDismissListener(dialog -> viewModel.closeRecurrenceDialog())
                .show();
    }

    private void showAddTagDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Добавить тег");
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_tag, null);
        final TextInputEditText inputTagName = dialogView.findViewById(R.id.editText_new_tag_name);
        builder.setView(dialogView);
        builder.setPositiveButton("Добавить", (dialog, which) -> {
            String tagName = inputTagName.getText() != null ? inputTagName.getText().toString().trim() : "";
            if (!tagName.isEmpty()) {
                viewModel.addTag(tagName);
            }
        });
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showDeleteSelectedTagsDialog(int quantity) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Удалить выбранные теги")
                .setMessage("Вы действительно хотите удалить все выбранные теги (" + quantity + " шт.)?")
                .setPositiveButton("Удалить все", (dialog, which) -> viewModel.deleteSelectedTags())
                .setNegativeButton("Отмена", null)
                .show();
    }


    @Override
    protected void setupToolbar() {
        MaterialToolbar toolbar = getToolbar();
        if (toolbar != null) {
            // Заголовок будет устанавливаться через LiveData в observeViewModel
            toolbar.setNavigationIcon(R.drawable.arrow_back_ios_new); // Установим иконку "назад"
            toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
            toolbar.getMenu().clear(); // Очищаем меню от предыдущего фрагмента
        }
    }

    @Override
    protected void setupFab() {
        ExtendedFloatingActionButton fab = getExtendedFab();
        if (fab != null) {
            // Текст и иконка будут устанавливаться через LiveData в observeViewModel
            fab.setIconResource(R.drawable.save);
            fab.setOnClickListener(v -> {
                // Скрываем клавиатуру перед сохранением
                View currentFocus = requireActivity().getCurrentFocus();
                if (currentFocus != null) {
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                }
                viewModel.saveTask();
            });
            fab.show();
        }
        if (getStandardFab() != null) getStandardFab().hide();
    }

    @Override
    public void onDestroyView() {
        // Отписка от LiveData, если была использована observeForever (здесь не используется)
        super.onDestroyView();
    }
}