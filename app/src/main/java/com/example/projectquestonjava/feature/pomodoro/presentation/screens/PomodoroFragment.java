package com.example.projectquestonjava.feature.pomodoro.presentation.screens;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
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
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectquestonjava.R;
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
import com.google.android.material.card.MaterialCardView;
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

    // UI Elements from fragment_pomodoro.xml
    private View errorMessageCardView;
    private TextView textViewErrorMessage;
    private ImageButton buttonDismissError;

    private FrameLayout taskSelectionContainer;
    private View selectTaskButtonInclude; // R.id.button_select_task_pomodoro_include
    private View currentTaskCardInclude;  // R.id.card_current_task_pomodoro_include
    private TextView currentTaskTitlePomodoro; // Inside current_task_card_pomodoro_include

    private FrameLayout timerOrInputContainer;
    private View estimatedTimeInputView; // R.id.estimated_time_input_pomodoro
    private EditText editTextHours, editTextMinutes;
    private TextInputLayout textInputLayoutHours, textInputLayoutMinutes;

    private View timeDisplayProgressView; // R.id.time_display_progress_pomodoro
    private TextView textViewTimerMinutes, textViewTimerSeconds, textViewTimerColon;
    private ProgressBar progressBarTimerProgress;

    private View noTaskSelectedPlaceholderView; // R.id.no_task_selected_placeholder_pomodoro
    private Button buttonSelectTaskFromPlaceholder;

    private RecyclerView recyclerViewPomodoroCycle;
    private PomodoroPhaseAdapter phaseAdapter;

    private LinearLayout layoutSecondaryControls;
    private MaterialButton buttonSkipBreak, buttonCompleteEarly;
    private FrameLayout frameCompleteEarlyProgress; // Контейнер для кнопки с прогрессом
    private MaterialButton buttonCompleteEarlyWithProgress; // Кнопка внутри frame_complete_early_progress (если она одна)
    private ProgressBar progressBarCompleteEarly; // ProgressBar внутри frame_complete_early_progress

    // Timer Controls (from view_timer_controls.xml)
    private FloatingActionButton fabStopTimer, fabMainTimerAction;

    private Handler uiHandler = new Handler(Looper.getMainLooper());
    private boolean StundenTextChangedByUser = false; // Флаги для отслеживания ручного ввода
    private boolean MinutenTextChangedByUser = false;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pomodoro, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PomodoroViewModel.class);

        bindViews(view);
        setupTaskSelection();
        setupTimeInput();
        setupTimerControls();
        setupRecyclerView();
        setupObservers();
    }

    @Override
    protected void setupToolbar() {

    }

    @Override
    protected void setupFab() {

    }

    private void bindViews(View view) {
        errorMessageCardView = view.findViewById(R.id.error_message_card_pomodoro);
        if (errorMessageCardView != null) { // Проверка, т.к. include может не иметь ID самого include
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

        noTaskSelectedPlaceholderView = view.findViewById(R.id.no_task_selected_placeholder_pomodoro);
        if (noTaskSelectedPlaceholderView != null) {
            View buttonInsidePlaceholder = noTaskSelectedPlaceholderView.findViewById(R.id.button_placeholder_select_task); // Предполагаем ID для кнопки внутри плейсхолдера
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
        // Предполагаем, что у кнопки внутри frame_complete_early_progress есть ID
        // buttonCompleteEarlyWithProgress = frameCompleteEarlyProgress.findViewById(R.id.button_complete_early_with_progress_text);
        // progressBarCompleteEarly = frameCompleteEarlyProgress.findViewById(R.id.progress_bar_on_complete_button);


        View timerControlsView = view.findViewById(R.id.timer_controls_pomodoro); // ID от <include>
        if (timerControlsView != null) {
            fabStopTimer = timerControlsView.findViewById(R.id.fab_stop_timer);
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

    private void setupTimeInput() {
        if (editTextHours != null && editTextMinutes != null) {
            editTextHours.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    if (StundenTextChangedByUser) viewModel.onEstimatedHoursChanged(s.toString());
                }
            });
            editTextHours.setOnFocusChangeListener((v, hasFocus) -> StundenTextChangedByUser = hasFocus);


            editTextMinutes.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    if (MinutenTextChangedByUser) viewModel.onEstimatedMinutesChanged(s.toString());
                }
            });
            editTextMinutes.setOnFocusChangeListener((v, hasFocus) -> MinutenTextChangedByUser = hasFocus);


            editTextMinutes.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // Можно скрыть клавиатуру
                    // InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    // imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    editTextMinutes.clearFocus(); // Убираем фокус
                    return true;
                }
                return false;
            });
        }
    }

    private void setupTimerControls() {
        if (fabMainTimerAction != null) fabMainTimerAction.setOnClickListener(v -> viewModel.startOrToggleTimer());
        if (fabStopTimer != null) fabStopTimer.setOnClickListener(v -> viewModel.stopTimer());
        if (buttonSkipBreak != null) buttonSkipBreak.setOnClickListener(v -> viewModel.skipCurrentBreakAndStartNextFocus());
        if (buttonCompleteEarly != null) buttonCompleteEarly.setOnClickListener(v -> viewModel.completeTaskEarly());
    }

    private void setupRecyclerView() {
        phaseAdapter = new PomodoroPhaseAdapter(requireContext());
        recyclerViewPomodoroCycle.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerViewPomodoroCycle.setAdapter(phaseAdapter);
    }

    private void setupObservers() {
        viewModel.uiStateLiveData.observe(getViewLifecycleOwner(), uiState -> {
            if (uiState == null) return;

            // Error Message Card
            if (errorMessageCardView != null) {
                errorMessageCardView.setVisibility(uiState.errorMessage != null ? View.VISIBLE : View.GONE);
                if (uiState.errorMessage != null && textViewErrorMessage != null) {
                    textViewErrorMessage.setText(uiState.errorMessage);
                }
            }

            // Timer Display / Input Fields / No Task Placeholder
            boolean showTimeInput = uiState.isTimeSetupMode && viewModel.currentTaskLiveData.getValue() != null && !Boolean.TRUE.equals(viewModel.showTaskSelectorLiveData.getValue());
            boolean showTimerDisplay = !uiState.isTimeSetupMode && viewModel.currentTaskLiveData.getValue() != null;
            boolean showNoTaskPlaceholder = viewModel.currentTaskLiveData.getValue() == null && !Boolean.TRUE.equals(viewModel.showTaskSelectorLiveData.getValue());

            if(estimatedTimeInputView != null) estimatedTimeInputView.setVisibility(showTimeInput ? View.VISIBLE : View.GONE);
            if(timeDisplayProgressView != null) timeDisplayProgressView.setVisibility(showTimerDisplay ? View.VISIBLE : View.GONE);
            if(noTaskSelectedPlaceholderView != null) noTaskSelectedPlaceholderView.setVisibility(showNoTaskPlaceholder ? View.VISIBLE : View.GONE);


            if (showTimeInput && editTextHours != null && editTextMinutes != null) {
                // Обновляем только если текст отличается, чтобы не вызывать рекурсию TextWatcher
                if (!String.valueOf(uiState.estimatedHours).equals(editTextHours.getText().toString())) {
                    StundenTextChangedByUser = false; // Предотвращаем срабатывание TextWatcher
                    editTextHours.setText(String.format(Locale.getDefault(), "%02d", uiState.estimatedHours));
                }
                if (!String.valueOf(uiState.estimatedMinutes).equals(editTextMinutes.getText().toString())) {
                    MinutenTextChangedByUser = false;
                    editTextMinutes.setText(String.format(Locale.getDefault(), "%02d", uiState.estimatedMinutes));
                }
            }

            if (showTimerDisplay && textViewTimerMinutes != null && textViewTimerSeconds != null && progressBarTimerProgress != null) {
                textViewTimerMinutes.setText(uiState.formattedTime.substring(0, uiState.formattedTime.indexOf(':')));
                textViewTimerSeconds.setText(uiState.formattedTime.substring(uiState.formattedTime.indexOf(':') + 1));
                // Анимация цвета для двоеточия и текста
                animateTextColor(textViewTimerMinutes, uiState.currentPhaseType);
                animateTextColor(textViewTimerSeconds, uiState.currentPhaseType);
                animateTextColor(textViewTimerColon, uiState.currentPhaseType);

                ObjectAnimator.ofInt(progressBarTimerProgress, "progress", (int) (uiState.progress * 100))
                        .setDuration(300).start();
                progressBarTimerProgress.setProgressTintList(ColorStateList.valueOf(getPhaseColor(uiState.currentPhaseType)));
            }

            // Pomodoro Cycle Visualizer Visibility
            boolean shouldShowVisualizer = viewModel.currentTaskLiveData.getValue() != null &&
                    ((uiState.isTimeSetupMode && !Objects.requireNonNull(viewModel.generatedPhasesFlow.getValue()).isEmpty()) ||
                            (uiState.timerState != TimerState.Idle.getInstance()));
            recyclerViewPomodoroCycle.setVisibility(shouldShowVisualizer ? View.VISIBLE : View.GONE);


            // Secondary Controls Visibility
            boolean showSecondaryControls = !uiState.isTimeSetupMode && viewModel.currentTaskLiveData.getValue() != null && uiState.timerState != TimerState.Idle.getInstance();
            if (layoutSecondaryControls != null) layoutSecondaryControls.setVisibility(showSecondaryControls ? View.VISIBLE : View.GONE);

            if (showSecondaryControls) {
                boolean isBreak = uiState.currentPhaseType.isBreak();
                TimerState timerState = uiState.timerState;
                boolean canSkipBreak = isBreak && (timerState instanceof TimerState.Running || timerState instanceof TimerState.Paused || timerState instanceof TimerState.WaitingForConfirmation);
                if(buttonSkipBreak != null) buttonSkipBreak.setVisibility(canSkipBreak ? View.VISIBLE : View.GONE);

                // Управление кнопкой "Готово" и ее состоянием загрузки
                if (buttonCompleteEarly != null && frameCompleteEarlyProgress != null) {
                    if (uiState.isCompletingTaskEarly) {
                        buttonCompleteEarly.setVisibility(View.GONE);
                        frameCompleteEarlyProgress.setVisibility(View.VISIBLE);
                    } else {
                        buttonCompleteEarly.setVisibility(View.VISIBLE);
                        frameCompleteEarlyProgress.setVisibility(View.GONE);
                    }
                    buttonCompleteEarly.setEnabled(!uiState.isCompletingTaskEarly);
                }
            }

            // Timer Controls (FABs)
            updateTimerControls(uiState.timerState, viewModel.currentTaskLiveData.getValue() != null);
        });

        viewModel.currentTaskLiveData.observe(getViewLifecycleOwner(), task -> {
            updateTaskSelectionView(task);
            // Обновляем фазы при смене задачи, если в режиме настройки
            PomodoroUiState currentUi = viewModel.uiStateLiveData.getValue();
            if (task != null && currentUi != null && currentUi.isTimeSetupMode) {
                viewModel.updateGeneratedPhasesForCurrentEstimatedTime(currentUi.estimatedHours, currentUi.estimatedMinutes, task);
            } else if (task == null && currentUi != null && currentUi.isTimeSetupMode) {
                //viewModel.generatedPhasesLiveData.postValue(Collections.emptyList());
            }
            viewModel.combineAndPostUiState(); // Пересчитать UI
        });

        viewModel.showTaskSelectorLiveData.observe(getViewLifecycleOwner(), show -> {
            if (Boolean.TRUE.equals(show)) {
                TaskSelectorBottomSheetFragment.newInstance().show(getChildFragmentManager(), "TaskSelectorPomodoro");
            }
        });

        viewModel.generatedPhasesFlow.observe(getViewLifecycleOwner(), phases -> {
            phaseAdapter.submitList(phases != null ? phases : Collections.emptyList());
            // Обновляем состояние таймера в адаптере, если он инициализирован
            PomodoroUiState ui = viewModel.uiStateLiveData.getValue();
            if (ui != null) {
                phaseAdapter.setCurrentTimerState(ui.timerState);
            }
        });
        viewModel.currentPhaseIndexFlow.observe(getViewLifecycleOwner(), index -> {
            phaseAdapter.setCurrentPhaseIndex(index != null ? index : -1);
            if (index != null && index >= 0 && recyclerViewPomodoroCycle.getLayoutManager() != null) {
                // Плавная прокрутка к текущей фазе
                ((LinearLayoutManager)recyclerViewPomodoroCycle.getLayoutManager()).smoothScrollToPosition(recyclerViewPomodoroCycle, new RecyclerView.State(), index);
            }
        });

        viewModel.uiStateLiveData.observe(getViewLifecycleOwner(), state -> { // Обновляем состояние таймера в адаптере
            if(state != null) phaseAdapter.setCurrentTimerState(state.timerState);
        });


        // Snackbar
        viewModel.snackbarMessageEvent.observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                View coordinatorLayout = requireActivity().findViewById(R.id.coordinatorLayout_main);
                if (coordinatorLayout != null) {
                    Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG).show();
                }
                //viewModel.clearSnackbarMessage();
            }
        });

        // Навигация из ViewModel
        viewModel.navigateToSettingsEvent.observe(getViewLifecycleOwner(), navigate -> {
            if (Boolean.TRUE.equals(navigate)) {
                //NavHostFragment.findNavController(this).navigate(R.id.action_pomodoroFragment_to_timerSettingsFragment);
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

    private void updateTimerControls(TimerState timerState, boolean isTaskSelected) {
        if (fabStopTimer == null || fabMainTimerAction == null) return;

        boolean isIdle = timerState instanceof TimerState.Idle;
        fabStopTimer.setVisibility(!isIdle && isTaskSelected ? View.VISIBLE : View.GONE);

        boolean isMainButtonEnabled = isTaskSelected || !isIdle;
        fabMainTimerAction.setEnabled(isMainButtonEnabled);

        int mainButtonIconRes;
        ColorStateList mainButtonBgTint;
        ColorStateList mainButtonIconTint;
        int mainButtonSizeDp = (isIdle && !isTaskSelected) ? 96 : 80;
        int mainIconSizeDp = (isIdle && !isTaskSelected) ? 48 : 40;


        if (!isMainButtonEnabled && timerState instanceof TimerState.Idle) {
            mainButtonIconRes = R.drawable.playlist_add;
            mainButtonBgTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.surfaceBrightDark));
            mainButtonIconTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.surfaceBrightDark));
        } else if (timerState instanceof TimerState.Running) {
            mainButtonIconRes = R.drawable.pause;
            mainButtonBgTint = ColorStateList.valueOf(getPhaseColor(((TimerState.Running) timerState).getType()));
            mainButtonIconTint = ColorStateList.valueOf(getOnPhaseColor(((TimerState.Running) timerState).getType()));
        } else if (timerState instanceof TimerState.Paused || timerState instanceof TimerState.Idle) {
            mainButtonIconRes = R.drawable.play_arrow;
            mainButtonBgTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primaryLight)); // Default play color
            mainButtonIconTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.onPrimaryLight));
        } else if (timerState instanceof TimerState.WaitingForConfirmation) {
            mainButtonIconRes = R.drawable.check;
            mainButtonBgTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.secondaryLight));
            mainButtonIconTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.onSecondaryLight));
        } else { // Fallback
            mainButtonIconRes = R.drawable.play_arrow;
            mainButtonBgTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primaryLight));
            mainButtonIconTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.onPrimaryLight));
        }

        fabMainTimerAction.setImageResource(mainButtonIconRes);
        fabMainTimerAction.setBackgroundTintList(mainButtonBgTint);
        ImageViewCompat.setImageTintList(fabMainTimerAction, mainButtonIconTint);

        ViewGroup.LayoutParams params = fabMainTimerAction.getLayoutParams();
        int sizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, mainButtonSizeDp, getResources().getDisplayMetrics());
        if (params.width != sizePx || params.height != sizePx) {
            params.width = sizePx;
            params.height = sizePx;
            fabMainTimerAction.setLayoutParams(params);
        }
        // fabMainTimerAction.setCustomSize(sizePx); // Если используем setCustomSize
        // AppcompatImageView не имеет setMaxImageSize, это для FAB
        // Для ImageButton внутри FAB, размер иконки нужно контролировать через padding/scaleType ImageView
    }

    private int getPhaseColor(SessionType type) {
        return ContextCompat.getColor(requireContext(), switch (type) {
            case FOCUS -> R.color.primaryLight;
            case SHORT_BREAK, LONG_BREAK -> R.color.tertiaryLight;
        });
    }
    private int getOnPhaseColor(SessionType type) {
        return ContextCompat.getColor(requireContext(), switch (type) {
            case FOCUS -> R.color.onPrimaryLight;
            case SHORT_BREAK, LONG_BREAK -> R.color.onTertiaryLight;
        });
    }

    private void animateTextColor(TextView textView, SessionType phaseType) {
        if (textView == null) return;
        int targetColor = getPhaseColor(phaseType);
        int currentColor = textView.getCurrentTextColor();
        if (currentColor != targetColor) {
            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), currentColor, targetColor);
            colorAnimation.setDuration(500); // milliseconds
            colorAnimation.addUpdateListener(animator -> textView.setTextColor((int) animator.getAnimatedValue()));
            colorAnimation.start();
        }
    }

    @Override
    public void onDestroyView() {
        // Важно отписаться от LiveData из ViewModel сервиса, если они используют observeForever
        // или если подписка была сделана не через getViewLifecycleOwner()
        // viewModel.unsubscribeFromServiceState(); // Добавить такой метод в ViewModel, если нужно
        super.onDestroyView();
    }
}