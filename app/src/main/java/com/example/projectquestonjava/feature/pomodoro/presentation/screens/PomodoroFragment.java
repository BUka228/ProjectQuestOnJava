package com.example.projectquestonjava.feature.pomodoro.presentation.screens;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.core.widget.ImageViewCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.app.MainActivity;
import com.example.projectquestonjava.core.data.model.core.Task;
import com.example.projectquestonjava.core.ui.BaseFragment;
import com.example.projectquestonjava.feature.pomodoro.domain.model.PomodoroPhase;
import com.example.projectquestonjava.feature.pomodoro.domain.model.SessionType;
import com.example.projectquestonjava.feature.pomodoro.domain.model.TimerState;
import com.example.projectquestonjava.feature.pomodoro.presentation.adapters.PomodoroPhaseAdapter;
import com.example.projectquestonjava.feature.pomodoro.presentation.ui_elements.TaskSelectorBottomSheetFragment;
import com.example.projectquestonjava.feature.pomodoro.presentation.viewmodels.PomodoroUiState;
import com.example.projectquestonjava.feature.pomodoro.presentation.viewmodels.PomodoroViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PomodoroFragment extends BaseFragment {

    private PomodoroViewModel viewModel;

    private View errorMessageCardView;
    private TextView textViewErrorMessage;
    private ImageButton buttonDismissError;

    private FrameLayout taskSelectionContainer;
    private View selectTaskButtonInclude;
    private View currentTaskCardInclude;
    private TextView currentTaskTitlePomodoro;

    private FrameLayout timerOrInputContainer;
    private View estimatedTimeInputView;
    private EditText editTextHours, editTextMinutes;
    private TextInputLayout textInputLayoutHours, textInputLayoutMinutes;

    private View timeDisplayProgressView;
    private TextView textViewTimerMinutes, textViewTimerSeconds, textViewTimerColon;
    private ProgressBar progressBarTimerProgress;
    private TextView textViewSessionPlanLabel;

    private View noTaskSelectedPlaceholderView;
    private Button buttonSelectTaskFromPlaceholder;

    private RecyclerView recyclerViewPomodoroCycle;
    private PomodoroPhaseAdapter phaseAdapter;

    private LinearLayout layoutSecondaryControls;
    private MaterialButton buttonSkipBreak, buttonCompleteEarly;
    private FrameLayout frameCompleteEarlyProgress;
    private MaterialButton buttonCompleteEarlyWithProgress;
    private ProgressBar progressBarCompleteEarly;

    private FloatingActionButton fabMainTimerAction;

    private MenuItem stopTimerMenuItem;

    private View customToolbarTitleView;
    private TextView toolbarPhaseName, toolbarSessionCount, toolbarCompletedCountValue;
    private ImageView toolbarCompletedCountIcon;

    private Handler uiHandler = new Handler(Looper.getMainLooper());
    private boolean isUpdatingHoursProgrammatically = false;
    private boolean isUpdatingMinutesProgrammatically = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.pomodoro_toolbar_menu, menu);
                stopTimerMenuItem = menu.findItem(R.id.action_stop_pomodoro);
                if (stopTimerMenuItem != null) {
                    // Начальная видимость кнопки "Стоп" будет установлена в setupObservers
                    // на основе состояния ViewModel
                    PomodoroUiState currentState = viewModel.uiStateLiveData.getValue();
                    if (currentState != null) {
                        updateTimerControlsState(currentState, viewModel.currentTaskLiveData.getValue() != null);
                    } else {
                        stopTimerMenuItem.setVisible(false);
                    }
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int itemId = menuItem.getItemId();
                if (itemId == R.id.action_timer_settings_pomodoro) {
                    if (viewModel != null) {
                        viewModel.navigateToSettings();
                    }
                    return true;
                } else if (itemId == R.id.action_stop_pomodoro) {
                    if (viewModel != null) {
                        viewModel.stopTimer();
                    }
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        return inflater.inflate(R.layout.fragment_pomodoro, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PomodoroViewModel.class);

        bindViews(view);
        setupTaskSelection();
        setupTimeInputListeners();
        setupTimerControlsListeners();
        setupRecyclerView();
        setupObservers();
    }

    @Override
    protected void setupToolbar() {
        MainActivity mainActivity = getMainActivity();
        if (mainActivity != null && getContext() != null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            customToolbarTitleView = inflater.inflate(R.layout.toolbar_pomodoro_custom_title, mainActivity.getToolbar(), false);

            toolbarPhaseName = customToolbarTitleView.findViewById(R.id.textView_toolbar_pomodoro_phase_name);
            toolbarSessionCount = customToolbarTitleView.findViewById(R.id.textView_toolbar_pomodoro_session_count);
            View completedCountContainer = customToolbarTitleView.findViewById(R.id.frameLayout_toolbar_pomodoro_completed_count_container);
            toolbarCompletedCountValue = completedCountContainer.findViewById(R.id.textView_toolbar_pomodoro_completed_count_value);
            toolbarCompletedCountIcon = completedCountContainer.findViewById(R.id.imageView_pomodoro_timer_icon); // ID иконки таймера

            mainActivity.setCustomToolbarTitleView(customToolbarTitleView);
            viewModel.uiStateLiveData.observe(getViewLifecycleOwner(), this::updateToolbarContent);
        }
    }


    private void updateToolbarContent(PomodoroUiState uiState) {
        if (customToolbarTitleView == null || uiState == null) return;

        SessionType phaseType = uiState.currentPhaseType;
        String phaseName = "";
        if (phaseType != null) {
            phaseName = switch (phaseType) {
                case FOCUS -> "Фокус";
                case SHORT_BREAK -> "Отдых";
                case LONG_BREAK -> "Перерыв";
            };
        }
        toolbarPhaseName.setText(phaseName);

        if (uiState.timerState instanceof TimerState.Idle || uiState.isTimeSetupMode) {
            toolbarSessionCount.setVisibility(View.GONE);
            customToolbarTitleView.findViewById(R.id.frameLayout_toolbar_pomodoro_completed_count_container).setVisibility(View.GONE);
        } else {
            toolbarSessionCount.setVisibility(View.VISIBLE);
            toolbarSessionCount.setText(String.format(Locale.getDefault(), "Подход %d/%d",
                    uiState.currentFocusSessionDisplayIndex, uiState.totalFocusSessionsInTask));

            View completedCountContainer = customToolbarTitleView.findViewById(R.id.frameLayout_toolbar_pomodoro_completed_count_container);
            if (uiState.totalFocusSessionsInTask > 0) { // Показываем, если есть фокус-сессии
                completedCountContainer.setVisibility(View.VISIBLE);
                int completedFocusSessions = uiState.currentFocusSessionDisplayIndex;

                // Корректировка отображаемого количества выполненных сессий
                // Если текущая фаза - фокус и она не завершена (не WaitingForConfirmation),
                // то эта фокус-сессия еще не считается выполненной для счетчика.
                if (phaseType.isFocus() && !(uiState.timerState instanceof TimerState.WaitingForConfirmation)) {
                    completedFocusSessions = Math.max(0, completedFocusSessions - 1);
                }
                toolbarCompletedCountValue.setText(String.valueOf(completedFocusSessions));
            } else {
                completedCountContainer.setVisibility(View.GONE);
            }
        }
    }


    @Override
    protected void setupFab() {
        if (getStandardFab() != null) {
            getStandardFab().hide();
        }
        if (getExtendedFab() != null) {
            getExtendedFab().hide();
        }
    }

    private void bindViews(View view) {
        errorMessageCardView = view.findViewById(R.id.error_message_card_pomodoro);
        if (errorMessageCardView != null) {
            textViewErrorMessage = errorMessageCardView.findViewById(R.id.textView_error_message);
            buttonDismissError = errorMessageCardView.findViewById(R.id.button_dismiss_error);
            if (buttonDismissError != null) buttonDismissError.setOnClickListener(v -> viewModel.clearErrorMessage());
        }

        taskSelectionContainer = view.findViewById(R.id.frameLayout_task_selection_container_pomodoro);
        selectTaskButtonInclude = view.findViewById(R.id.button_select_task_pomodoro_include);
        currentTaskCardInclude = view.findViewById(R.id.card_current_task_pomodoro_include);
        if (currentTaskCardInclude != null) {
            currentTaskTitlePomodoro = currentTaskCardInclude.findViewById(R.id.textView_current_task_title_pomodoro);
        }

        timerOrInputContainer = view.findViewById(R.id.frameLayout_timer_or_input_container_pomodoro);
        estimatedTimeInputView = view.findViewById(R.id.estimated_time_input_pomodoro);
        if (estimatedTimeInputView != null) {
            editTextHours = estimatedTimeInputView.findViewById(R.id.editText_hours);
            editTextMinutes = estimatedTimeInputView.findViewById(R.id.editText_minutes);
            textInputLayoutHours = estimatedTimeInputView.findViewById(R.id.textInputLayout_hours);
            textInputLayoutMinutes = estimatedTimeInputView.findViewById(R.id.textInputLayout_minutes);
        }

        timeDisplayProgressView = view.findViewById(R.id.time_display_progress_pomodoro);
        if (timeDisplayProgressView != null) {
            textViewTimerMinutes = timeDisplayProgressView.findViewById(R.id.textView_timer_minutes);
            textViewTimerSeconds = timeDisplayProgressView.findViewById(R.id.textView_timer_seconds);
            textViewTimerColon = timeDisplayProgressView.findViewById(R.id.textView_timer_colon);
            progressBarTimerProgress = timeDisplayProgressView.findViewById(R.id.progressBar_timer_progress);
        }
        textViewSessionPlanLabel = view.findViewById(R.id.textView_session_plan_label);


        noTaskSelectedPlaceholderView = view.findViewById(R.id.no_task_selected_placeholder_pomodoro);
        if (noTaskSelectedPlaceholderView != null) {
            View buttonInsidePlaceholder = noTaskSelectedPlaceholderView.findViewById(R.id.button_placeholder_select_task);
            if (buttonInsidePlaceholder instanceof Button) {
                buttonSelectTaskFromPlaceholder = (Button) buttonInsidePlaceholder;
                buttonSelectTaskFromPlaceholder.setOnClickListener(v -> viewModel.toggleTaskSelector());
            }
        }

        recyclerViewPomodoroCycle = view.findViewById(R.id.recyclerView_pomodoro_cycle_visualizer);

        layoutSecondaryControls = view.findViewById(R.id.layout_secondary_pomodoro_controls);
        buttonSkipBreak = view.findViewById(R.id.button_skip_break_pomodoro);
        buttonCompleteEarly = view.findViewById(R.id.button_complete_early_pomodoro);
        frameCompleteEarlyProgress = view.findViewById(R.id.frame_complete_early_progress);
        if (frameCompleteEarlyProgress != null) {
            buttonCompleteEarlyWithProgress = frameCompleteEarlyProgress.findViewById(R.id.button_complete_early_with_progress_text);
            progressBarCompleteEarly = frameCompleteEarlyProgress.findViewById(R.id.progress_bar_on_complete_button);
        }

        View timerControlsView = view.findViewById(R.id.timer_controls_pomodoro);
        if (timerControlsView != null) {
            fabMainTimerAction = timerControlsView.findViewById(R.id.fab_main_timer_action);
        }
    }

    private void setupTaskSelection() {
        if (selectTaskButtonInclude != null) {
            selectTaskButtonInclude.setOnClickListener(v -> viewModel.toggleTaskSelector());
        }
        if (currentTaskCardInclude != null) {
            currentTaskCardInclude.setOnClickListener(v -> viewModel.toggleTaskSelector());
        }
    }

    private void setupTimeInputListeners() {
        if (editTextHours == null || editTextMinutes == null) return;

        editTextHours.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && "00".equals(editTextHours.getText().toString())) {
                isUpdatingHoursProgrammatically = true;
                editTextHours.setText("");
                isUpdatingHoursProgrammatically = false;
            } else if (!hasFocus && editTextHours.getText().toString().isEmpty()) {
                isUpdatingHoursProgrammatically = true;
                editTextHours.setText("00");
                isUpdatingHoursProgrammatically = false;
                // После потери фокуса, если поле было очищено, вызываем обновление ViewModel с 0
                viewModel.onEstimatedHoursChanged("0");
            }
        });
        editTextHours.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (!isUpdatingHoursProgrammatically) {
                    viewModel.onEstimatedHoursChanged(s.toString());
                }
            }
        });

        editTextMinutes.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && "00".equals(editTextMinutes.getText().toString())) {
                isUpdatingMinutesProgrammatically = true;
                editTextMinutes.setText("");
                isUpdatingMinutesProgrammatically = false;
            } else if (!hasFocus && editTextMinutes.getText().toString().isEmpty()) {
                isUpdatingMinutesProgrammatically = true;
                editTextMinutes.setText("00");
                isUpdatingMinutesProgrammatically = false;
                viewModel.onEstimatedMinutesChanged("0");
            }
        });
        editTextMinutes.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (!isUpdatingMinutesProgrammatically) {
                    viewModel.onEstimatedMinutesChanged(s.toString());
                }
            }
        });

        editTextMinutes.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                editTextMinutes.clearFocus();
                hideKeyboard(v);
                return true;
            }
            return false;
        });
    }
    private void hideKeyboard(View view) {
        if (view != null && getContext() != null) {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void setupTimerControlsListeners() {
        if (fabMainTimerAction != null) fabMainTimerAction.setOnClickListener(v -> viewModel.startOrToggleTimer());
        if (buttonSkipBreak != null) buttonSkipBreak.setOnClickListener(v -> viewModel.skipCurrentBreakAndStartNextFocus());
        if (buttonCompleteEarly != null) buttonCompleteEarly.setOnClickListener(v -> viewModel.completeTaskEarly());
    }

    private void setupRecyclerView() {
        phaseAdapter = new PomodoroPhaseAdapter(requireContext());
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerViewPomodoroCycle.setLayoutManager(layoutManager);
        recyclerViewPomodoroCycle.setAdapter(phaseAdapter);
    }

    private void setupObservers() {
        viewModel.uiStateLiveData.observe(getViewLifecycleOwner(), uiState -> {
            if (uiState == null) return;

            if (errorMessageCardView != null) {
                errorMessageCardView.setVisibility(uiState.errorMessage != null ? View.VISIBLE : View.GONE);
                if (uiState.errorMessage != null && textViewErrorMessage != null) {
                    textViewErrorMessage.setText(uiState.errorMessage);
                }
            }

            boolean isTaskSelected = viewModel.currentTaskLiveData.getValue() != null;
            boolean showTimeInput = uiState.isTimeSetupMode && isTaskSelected && !Boolean.TRUE.equals(viewModel.showTaskSelectorLiveData.getValue());
            boolean showTimerDisplay = !uiState.isTimeSetupMode && isTaskSelected;
            boolean showNoTaskPlaceholder = !isTaskSelected && !Boolean.TRUE.equals(viewModel.showTaskSelectorLiveData.getValue());

            if(estimatedTimeInputView != null) estimatedTimeInputView.setVisibility(showTimeInput ? View.VISIBLE : View.GONE);
            if(timeDisplayProgressView != null) timeDisplayProgressView.setVisibility(showTimerDisplay ? View.VISIBLE : View.GONE);
            if(noTaskSelectedPlaceholderView != null) noTaskSelectedPlaceholderView.setVisibility(showNoTaskPlaceholder ? View.VISIBLE : View.GONE);

            if (showTimeInput && editTextHours != null && editTextMinutes != null) {
                if (!editTextHours.hasFocus() && !String.valueOf(uiState.estimatedHours).equals(editTextHours.getText().toString().replaceAll("^0+(?!$)", ""))) {
                    isUpdatingHoursProgrammatically = true;
                    editTextHours.setText(String.format(Locale.getDefault(), "%02d", uiState.estimatedHours));
                    isUpdatingHoursProgrammatically = false;
                }
                if (!editTextMinutes.hasFocus() && !String.valueOf(uiState.estimatedMinutes).equals(editTextMinutes.getText().toString().replaceAll("^0+(?!$)", ""))) {
                    isUpdatingMinutesProgrammatically = true;
                    editTextMinutes.setText(String.format(Locale.getDefault(), "%02d", uiState.estimatedMinutes));
                    isUpdatingMinutesProgrammatically = false;
                }
            }


            if (showTimerDisplay && textViewTimerMinutes != null && textViewTimerSeconds != null && progressBarTimerProgress != null) {
                String[] timeParts = uiState.formattedTime.split(":", 2);
                if (timeParts.length == 2) {
                    textViewTimerMinutes.setText(timeParts[0]);
                    textViewTimerSeconds.setText(timeParts[1]);
                }
                animateTextColor(textViewTimerMinutes, uiState.currentPhaseType);
                animateTextColor(textViewTimerSeconds, uiState.currentPhaseType);
                animateTextColor(textViewTimerColon, uiState.currentPhaseType);

                ObjectAnimator.ofInt(progressBarTimerProgress, "progress", (int) (uiState.progress * 100))
                        .setDuration(300).start();
                progressBarTimerProgress.setProgressTintList(ColorStateList.valueOf(getPhaseColor(uiState.currentPhaseType)));
            }

            boolean shouldShowVisualizer = isTaskSelected &&
                    ((uiState.isTimeSetupMode && viewModel.generatedPhasesFlow.getValue() != null && !Objects.requireNonNull(viewModel.generatedPhasesFlow.getValue()).isEmpty()) ||
                            (uiState.timerState != TimerState.Idle.getInstance()));
            recyclerViewPomodoroCycle.setVisibility(shouldShowVisualizer ? View.VISIBLE : View.GONE);
            textViewSessionPlanLabel.setVisibility(shouldShowVisualizer ? View.VISIBLE : View.GONE);


            boolean showSecondaryControls = !uiState.isTimeSetupMode && isTaskSelected && uiState.timerState != TimerState.Idle.getInstance();
            if (layoutSecondaryControls != null) layoutSecondaryControls.setVisibility(showSecondaryControls ? View.VISIBLE : View.GONE);

            if (showSecondaryControls && buttonSkipBreak != null && buttonCompleteEarly != null && frameCompleteEarlyProgress != null) {
                boolean isBreak = uiState.currentPhaseType.isBreak();
                TimerState timerState = uiState.timerState;
                boolean canSkipBreak = isBreak && (timerState instanceof TimerState.Running || timerState instanceof TimerState.Paused || timerState instanceof TimerState.WaitingForConfirmation);
                buttonSkipBreak.setVisibility(canSkipBreak ? View.VISIBLE : View.GONE);

                if (uiState.isCompletingTaskEarly) {
                    buttonCompleteEarly.setVisibility(View.GONE);
                    frameCompleteEarlyProgress.setVisibility(View.VISIBLE);
                } else {
                    buttonCompleteEarly.setVisibility(View.VISIBLE);
                    frameCompleteEarlyProgress.setVisibility(View.GONE);
                }
                buttonCompleteEarly.setEnabled(!uiState.isCompletingTaskEarly && !isBreak);
            }
            updateTimerControlsState(uiState, isTaskSelected);
        });

        viewModel.currentTaskLiveData.observe(getViewLifecycleOwner(), task -> {
            updateTaskSelectionView(task);
            PomodoroUiState currentUi = viewModel.uiStateLiveData.getValue();
            if (task != null && currentUi != null && currentUi.isTimeSetupMode) {
                viewModel.updateGeneratedPhasesForCurrentEstimatedTime(currentUi.estimatedHours, currentUi.estimatedMinutes, task);
            }
            viewModel.combineAndPostUiState();
        });

        viewModel.showTaskSelectorLiveData.observe(getViewLifecycleOwner(), show -> {
            if (Boolean.TRUE.equals(show)) {
                TaskSelectorBottomSheetFragment.newInstance().show(getChildFragmentManager(), "TaskSelectorPomodoro");
            }
        });

        viewModel.generatedPhasesFlow.observe(getViewLifecycleOwner(), phases -> {
            phaseAdapter.submitList(phases != null ? phases : Collections.emptyList());
            PomodoroUiState ui = viewModel.uiStateLiveData.getValue();
            if (ui != null) {
                phaseAdapter.setCurrentTimerState(ui.timerState);
            }
        });
        viewModel.currentPhaseIndexFlow.observe(getViewLifecycleOwner(), index -> {
            phaseAdapter.setCurrentPhaseIndex(index != null ? index : -1);
            if (index != null && index >= 0 && recyclerViewPomodoroCycle.getLayoutManager() != null) {
                ((LinearLayoutManager)recyclerViewPomodoroCycle.getLayoutManager()).smoothScrollToPosition(recyclerViewPomodoroCycle, new RecyclerView.State(), index);
            }
        });

        viewModel.uiStateLiveData.observe(getViewLifecycleOwner(), state -> {
            if(state != null) phaseAdapter.setCurrentTimerState(state.timerState);
        });

        viewModel.snackbarMessageEvent.observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                View coordinatorLayout = requireActivity().findViewById(R.id.coordinatorLayout_main);
                if (coordinatorLayout != null) {
                    Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG).show();
                }
            }
        });

        viewModel.navigateToSettingsEvent.observe(getViewLifecycleOwner(), navigate -> {
            if (Boolean.TRUE.equals(navigate)) {
                NavHostFragment.findNavController(this).navigate(R.id.action_pomodoroFragment_to_timerSettingsFragment);
                viewModel.clearNavigateToSettings();
            }
        });
    }

    private void updateTaskSelectionView(Task task) {
        if (selectTaskButtonInclude != null) {
            selectTaskButtonInclude.setVisibility(task == null ? View.VISIBLE : View.GONE);
        }
        if (currentTaskCardInclude != null) {
            currentTaskCardInclude.setVisibility(task != null ? View.VISIBLE : View.GONE);
            if (task != null && currentTaskTitlePomodoro != null) {
                currentTaskTitlePomodoro.setText(task.getTitle());
            }
        }
    }

    private void updateTimerControlsState(@NonNull PomodoroUiState uiState, boolean isTaskSelected) {
        if (fabMainTimerAction == null) return;

        TimerState timerState = uiState.timerState;
        boolean isTimeSetup = uiState.isTimeSetupMode;

        boolean isMainButtonEnabled;
        int mainButtonIconRes;
        ColorStateList mainButtonBgTint;
        ColorStateList mainButtonIconTint;

        if (timerState instanceof TimerState.Running) {
            isMainButtonEnabled = true;
            mainButtonIconRes = R.drawable.pause;
            mainButtonBgTint = ColorStateList.valueOf(getPhaseColor(((TimerState.Running) timerState).getType()));
            mainButtonIconTint = ColorStateList.valueOf(getOnPhaseColor(((TimerState.Running) timerState).getType()));
        } else if (timerState instanceof TimerState.Paused) {
            isMainButtonEnabled = true;
            mainButtonIconRes = R.drawable.play_arrow;
            mainButtonBgTint = ColorStateList.valueOf(getPhaseColor(((TimerState.Paused) timerState).getType()));
            mainButtonIconTint = ColorStateList.valueOf(getOnPhaseColor(((TimerState.Paused) timerState).getType()));
        } else if (timerState instanceof TimerState.Idle) {
            isMainButtonEnabled = isTaskSelected;
            mainButtonIconRes = R.drawable.play_arrow;
            mainButtonBgTint = ColorStateList.valueOf(getPhaseColor(SessionType.FOCUS));
            mainButtonIconTint = ColorStateList.valueOf(getOnPhaseColor(SessionType.FOCUS));
            if (!isTaskSelected && isTimeSetup) {
                mainButtonIconRes = R.drawable.playlist_add;
                mainButtonBgTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.surfaceVariantDark));
                mainButtonIconTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.onSurfaceVariantDark));
            }
        } else if (timerState instanceof TimerState.WaitingForConfirmation) {
            isMainButtonEnabled = true;
            mainButtonIconRes = R.drawable.check;
            mainButtonBgTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.secondaryLight));
            mainButtonIconTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.onSecondaryLight));
        } else {
            isMainButtonEnabled = false;
            mainButtonIconRes = R.drawable.play_arrow;
            mainButtonBgTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.surfaceVariantDark));
            mainButtonIconTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.onSurfaceVariantDark));
        }

        fabMainTimerAction.setEnabled(isMainButtonEnabled);
        fabMainTimerAction.setImageResource(mainButtonIconRes);
        fabMainTimerAction.setBackgroundTintList(mainButtonBgTint);
        ImageViewCompat.setImageTintList(fabMainTimerAction, mainButtonIconTint);

        if (stopTimerMenuItem != null) {
            boolean showStopButton = !(timerState instanceof TimerState.Idle) && isTaskSelected;
            stopTimerMenuItem.setVisible(showStopButton);
        }
    }

    private int getPhaseColor(SessionType type) {
        return ContextCompat.getColor(requireContext(), switch (type) {
            case FOCUS -> R.color.primaryDark;
            case SHORT_BREAK, LONG_BREAK -> R.color.tertiaryDark;
        });
    }
    private int getOnPhaseColor(SessionType type) {
        return ContextCompat.getColor(requireContext(), switch (type) {
            case FOCUS -> R.color.onPrimaryDark;
            case SHORT_BREAK, LONG_BREAK -> R.color.onTertiaryDark;
        });
    }

    private void animateTextColor(TextView textView, SessionType phaseType) {
        if (textView == null) return;
        int targetColor = getPhaseColor(phaseType);
        int currentColor = textView.getCurrentTextColor();
        if (currentColor != targetColor) {
            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), currentColor, targetColor);
            colorAnimation.setDuration(300);
            colorAnimation.addUpdateListener(animator -> textView.setTextColor((int) animator.getAnimatedValue()));
            colorAnimation.start();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        errorMessageCardView = null; textViewErrorMessage = null; buttonDismissError = null;
        taskSelectionContainer = null; selectTaskButtonInclude = null; currentTaskCardInclude = null; currentTaskTitlePomodoro = null;
        timerOrInputContainer = null; estimatedTimeInputView = null; editTextHours = null; editTextMinutes = null;
        textInputLayoutHours = null; textInputLayoutMinutes = null;
        timeDisplayProgressView = null; textViewTimerMinutes = null; textViewTimerSeconds = null; textViewTimerColon = null;
        progressBarTimerProgress = null; textViewSessionPlanLabel = null;
        noTaskSelectedPlaceholderView = null; buttonSelectTaskFromPlaceholder = null;
        recyclerViewPomodoroCycle = null; phaseAdapter = null;
        layoutSecondaryControls = null; buttonSkipBreak = null; buttonCompleteEarly = null;
        frameCompleteEarlyProgress = null; buttonCompleteEarlyWithProgress = null; progressBarCompleteEarly = null;
        fabMainTimerAction = null; stopTimerMenuItem = null;
        customToolbarTitleView = null; toolbarPhaseName = null; toolbarSessionCount = null;
        toolbarCompletedCountValue = null; toolbarCompletedCountIcon = null;
        uiHandler.removeCallbacksAndMessages(null);
    }
}