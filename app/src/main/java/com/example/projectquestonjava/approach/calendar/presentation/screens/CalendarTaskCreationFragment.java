package com.example.projectquestonjava.approach.calendar.presentation.screens;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.approach.calendar.domain.model.TaskCreationEvent;
import com.example.projectquestonjava.approach.calendar.domain.model.TaskCreationState;

import com.example.projectquestonjava.approach.calendar.presentation.viewmodels.Loadable;
import com.example.projectquestonjava.approach.calendar.presentation.viewmodels.TaskCreationViewModel;
import com.example.projectquestonjava.core.data.model.core.Tag;
import com.example.projectquestonjava.core.ui.BaseFragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    private TextInputLayout textInputLayoutTitle, textInputLayoutDescription;
    private TextInputEditText editTextTitle, editTextDescription;
    private Button buttonSelectDate, buttonSelectTime, buttonSelectRecurrence;
    private ChipGroup chipGroupTags;
    private ImageButton buttonDeleteSelectedTags, buttonAddTag;
    private TextView textViewNoTags;
    private ProgressBar progressBarTagsLoading;
    private View mainContentContainer; // Для управления enabled состоянием контейнера
    private ProgressBar fabProgressBar; // Прогресс-бар для FAB, если нужен


    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", new Locale("ru"));
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
        mainContentContainer = view.findViewById(R.id.nestedScrollView_task_creation);
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
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { viewModel.updateTitle(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        editTextDescription.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { viewModel.updateDescription(s.toString()); }
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

            // Update input fields only if text differs
            if (!Objects.equals(editTextTitle.getText().toString(), state.getTaskInput().getTitle())) {
                editTextTitle.setText(state.getTaskInput().getTitle());
            }
            if (!Objects.equals(editTextDescription.getText().toString(), state.getTaskInput().getDescription())) {
                editTextDescription.setText(state.getTaskInput().getDescription());
            }
            LocalDateTime dueDate = state.getTaskInput().getDueDate();
            buttonSelectDate.setText(dueDate.format(dateFormatter));
            buttonSelectTime.setText(dueDate.format(timeFormatter));

            String recurrenceText = "Не повторяется";
            if (state.getTaskInput().getRecurrenceRule() != null) {
                recurrenceText = switch (state.getTaskInput().getRecurrenceRule()) {
                    case "DAILY" -> "Ежедневно";
                    case "WEEKLY" -> "Еженедельно";
                    case "MONTHLY" -> "Ежемесячно";
                    case "YEARLY" -> "Ежегодно";
                    default -> state.getTaskInput().getRecurrenceRule();
                };
            }
            buttonSelectRecurrence.setText(recurrenceText);


            boolean isEnabled = !state.isLoading();
            // Управляем enabled состоянием через родительский контейнер или каждый элемент
            setViewGroupEnabled(mainContentContainer, isEnabled);
            // Некоторые элементы могут иметь свою логику enabled, например buttonDeleteSelectedTags
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

            // Dialogs (ViewModel управляет флагами, мы здесь реагируем)
            if (state.isDateDialogOpen()) showDatePicker(state.getTaskInput().getDueDate().toLocalDate());
            if (state.isTimeDialogOpen()) showTimePicker(state.getTaskInput().getDueDate().toLocalTime());
            if (state.isRecurrenceDialogOpen()) showRecurrenceDialog(state.getTaskInput().getRecurrenceRule());

            // Events
            if (state.getEvent() != null) {
                if (state.getEvent() == TaskCreationEvent.TASK_CREATED || state.getEvent() == TaskCreationEvent.TASK_UPDATED) {
                    NavHostFragment.findNavController(this).popBackStack();
                    viewModel.clearEvent(); // Очищаем событие после обработки
                }
            }

            // Error Snackbar (будет управляться MainActivity через SnackbarManager)
            if (state.getError() != null) {
                // viewModel.showSnackbar(state.getError()); // Убираем, т.к. SnackbarManager
                viewModel.clearError();
            }

            // Обновление FAB
            ExtendedFloatingActionButton fab = getExtendedFab();
            if (fab != null) {
                fab.setText(viewModel.getSaveButtonText());
                fab.setEnabled(isEnabled); // FAB активен, если не идет загрузка
                // Если в state.isLoading() также учитывается сохранение, то это ок.
                // Если нет, то нужен отдельный флаг isSaving.
                // Пока предполагаем, что state.isLoading() достаточно.
            }
        });
    }

    private void setViewGroupEnabled(View view, boolean enabled) {
        if (view == null) return;
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                setViewGroupEnabled(viewGroup.getChildAt(i), enabled);
            }
        }
    }


    private void updateTagsChips(List<Tag> allTags, Set<Tag> selectedTags) {
        chipGroupTags.removeAllViews();
        if (allTags == null) return;

        Set<Long> selectedTagIds = (selectedTags != null)
                ? selectedTags.stream().map(Tag::getId).collect(Collectors.toSet())
                : Collections.emptySet();

        for (Tag tag : allTags) {
            Chip chip = (Chip) LayoutInflater.from(getContext()).inflate(R.layout.chip_filter_tag_item_creation, chipGroupTags, false);
            chip.setText(tag.getName());
            chip.setCheckable(true);
            chip.setChecked(selectedTagIds.contains(tag.getId()));
            chip.setOnClickListener(v -> viewModel.toggleTagSelection(tag));
            // Скрываем иконку закрытия, так как удаление через отдельную кнопку
            chip.setCloseIconVisible(false);
            chipGroupTags.addView(chip);
        }
    }

    private void showDatePicker(LocalDate initialDate) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    viewModel.setDueDate(LocalDate.of(year, month + 1, dayOfMonth));
                    viewModel.closeDateDialog(); // Закрываем после выбора
                },
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
                (view, hourOfDay, minute) -> {
                    viewModel.setDueTime(LocalTime.of(hourOfDay, minute));
                    viewModel.closeTimeDialog(); // Закрываем после выбора
                },
                initialTime.getHour(),
                initialTime.getMinute(),
                true // is24HourView
        );
        timePickerDialog.setOnDismissListener(dialog -> viewModel.closeTimeDialog());
        timePickerDialog.show();
    }

    private void showRecurrenceDialog(@Nullable String currentRule) {
        final String[] rules = {"DAILY", "WEEKLY", "MONTHLY", "YEARLY"}; // Значения для отправки в ViewModel
        final String[] ruleLabels = {"Ежедневно", "Еженедельно", "Ежемесячно", "Ежегодно", "Не повторять"}; // Для отображения

        int checkedItem = ruleLabels.length - 1; // По умолчанию "Не повторять"
        if (currentRule != null) {
            for (int i = 0; i < rules.length; i++) {
                if (rules[i].equals(currentRule)) {
                    checkedItem = i;
                    break;
                }
            }
        }

        new AlertDialog.Builder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
                .setTitle("Выберите повторение")
                .setSingleChoiceItems(ruleLabels, checkedItem, (dialog, which) -> {
                    String selectedRule = (which == ruleLabels.length - 1) ? null : rules[which];
                    viewModel.setRecurrenceRule(selectedRule); // ViewModel закроет диалог
                    dialog.dismiss();
                })
                .setNegativeButton("Отмена", (dialog, which) -> viewModel.closeRecurrenceDialog())
                .setOnDismissListener(dialog -> viewModel.closeRecurrenceDialog()) // Закрываем также при свайпе
                .show();
    }

    private void showAddTagDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog);
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_tag, null);
        final TextInputEditText inputTagName = dialogView.findViewById(R.id.editText_new_tag_name);

        builder.setView(dialogView)
                .setTitle("Добавить тег")
                .setPositiveButton("Добавить", (dialog, which) -> {
                    String tagName = inputTagName.getText() != null ? inputTagName.getText().toString().trim() : "";
                    if (!tagName.isEmpty()) {
                        viewModel.addTag(tagName); // ViewModel обработает результат
                    } else {
                        // Можно показать Toast или ошибку в самом диалоге
                        Snackbar.make(requireView(), "Имя тега не может быть пустым", Snackbar.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", (dialog, which) -> dialog.cancel())
                .show();
    }

    private void showDeleteSelectedTagsDialog(int quantity) {
        new AlertDialog.Builder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
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
            toolbar.setTitle(viewModel.getScreenTitle()); // Заголовок из ViewModel
            toolbar.setNavigationIcon(R.drawable.arrow_back_ios_new);
            toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
            toolbar.getMenu().clear(); // Очищаем меню от предыдущего фрагмента
        }
    }

    @Override
    protected void setupFab() {
        ExtendedFloatingActionButton fab = getExtendedFab();
        if (fab != null) {
            fab.setText(viewModel.getSaveButtonText());
            fab.setIconResource(R.drawable.save);
            fab.setOnClickListener(v -> {
                hideKeyboard(getView()); // Скрываем клавиатуру перед сохранением
                viewModel.saveTask();
            });
            fab.show();
        }
        if (getStandardFab() != null) getStandardFab().hide();
    }

    private void hideKeyboard(View view) {
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    @Override
    public void onDestroyView() {
        mainContentContainer = null;
        textInputLayoutTitle = null; editTextTitle = null; textInputLayoutDescription = null; editTextDescription = null;
        buttonSelectDate = null; buttonSelectTime = null; buttonSelectRecurrence = null;
        chipGroupTags = null; buttonDeleteSelectedTags = null; buttonAddTag = null;
        textViewNoTags = null; progressBarTagsLoading = null;
        super.onDestroyView();
    }
}