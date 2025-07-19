package com.example.projectquestonjava.feature.pomodoro.presentation.screens;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button; // Стандартная кнопка
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.core.widget.ImageViewCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.projectquestonjava.R;
import com.example.projectquestonjava.core.ui.BaseFragment;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.core.utils.RingtoneItem;
import com.example.projectquestonjava.feature.pomodoro.domain.model.PomodoroSettings;
import com.example.projectquestonjava.feature.pomodoro.presentation.adapters.RingtoneDropdownAdapter;
import com.example.projectquestonjava.feature.pomodoro.presentation.viewmodels.SettingsUiState; // Убедитесь, что импорт правильный
import com.example.projectquestonjava.feature.pomodoro.presentation.viewmodels.SettingsViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TimerSettingsFragment extends BaseFragment {

    private SettingsViewModel viewModel;
    @javax.inject.Inject Logger logger;

    // Views для DurationSetting (работа)
    private View workDurationIncludeView; // View самого <include>
    private ImageView workDurationIcon;
    private TextView workDurationTitle, workDurationValueText;
    private Slider workDurationSlider;
    private MaterialCardView workDurationValueBgCard; // Для фона значения

    // Views для DurationSetting (отдых)
    private View breakDurationIncludeView; // View самого <include>
    private ImageView breakDurationIcon;
    private TextView breakDurationTitle, breakDurationValueText;
    private Slider breakDurationSlider;
    private MaterialCardView breakDurationValueBgCard; // Для фона значения

    private Button buttonAddCustomRingtone;
    private TextInputLayout dropdownFocusSoundLayout, dropdownBreakSoundLayout;
    private AutoCompleteTextView autoCompleteFocusSound, autoCompleteBreakSound;
    private SwitchMaterial switchVibration;
    private TextView textViewError;

    private RingtoneDropdownAdapter focusSoundAdapter, breakSoundAdapter;
    private ActivityResultLauncher<String> pickRingtoneLauncher;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        pickRingtoneLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                viewModel.addCustomRingtone(uri);
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.timer_settings_toolbar_menu, menu);
            }
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_reset_timer_settings) {
                    viewModel.resetToDefaults(); // Вызываем метод ViewModel
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        return inflater.inflate(R.layout.fragment_timer_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupListeners();
        observeViewModel();
    }

    private void bindViews(View view) {
        // Work Duration
        workDurationIncludeView = view.findViewById(R.id.duration_setting_work_timer); // ID тега <include>
        workDurationIcon = workDurationIncludeView.findViewById(R.id.imageView_duration_icon);
        workDurationTitle = workDurationIncludeView.findViewById(R.id.textView_duration_title);
        workDurationValueText = workDurationIncludeView.findViewById(R.id.textView_duration_value);
        workDurationSlider = workDurationIncludeView.findViewById(R.id.slider_duration);
        workDurationValueBgCard = workDurationIncludeView.findViewById(R.id.card_duration_value_bg);


        // Break Duration
        breakDurationIncludeView = view.findViewById(R.id.duration_setting_break_timer); // ID тега <include>
        breakDurationIcon = breakDurationIncludeView.findViewById(R.id.imageView_duration_icon);
        breakDurationTitle = breakDurationIncludeView.findViewById(R.id.textView_duration_title);
        breakDurationValueText = breakDurationIncludeView.findViewById(R.id.textView_duration_value);
        breakDurationSlider = breakDurationIncludeView.findViewById(R.id.slider_duration);
        breakDurationValueBgCard = breakDurationIncludeView.findViewById(R.id.card_duration_value_bg);


        buttonAddCustomRingtone = view.findViewById(R.id.button_add_custom_ringtone_timer);
        dropdownFocusSoundLayout = view.findViewById(R.id.dropdown_focus_sound_timer);
        autoCompleteFocusSound = view.findViewById(R.id.autoComplete_focus_sound_timer);
        dropdownBreakSoundLayout = view.findViewById(R.id.dropdown_break_sound_timer);
        autoCompleteBreakSound = view.findViewById(R.id.autoComplete_break_sound_timer);
        switchVibration = view.findViewById(R.id.switch_vibration_timer);
        textViewError = view.findViewById(R.id.textView_timer_settings_error);
    }

    private void setupListeners() {
        workDurationSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                SettingsUiState state = viewModel.uiStateLiveData.getValue();
                if (state != null) viewModel.updateSettings(state.getCurrentSettings().copyWorkDurationMinutes((int) value));
            }
        });
        breakDurationSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                SettingsUiState state = viewModel.uiStateLiveData.getValue();
                if (state != null) viewModel.updateSettings(state.getCurrentSettings().copyBreakDurationMinutes((int) value));
            }
        });

        buttonAddCustomRingtone.setOnClickListener(v -> pickRingtoneLauncher.launch("audio/*"));

        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Проверяем isPressed, чтобы реагировать только на пользовательский ввод,
            // а не на программное изменение состояния checked.
            if (buttonView.isPressed()) {
                SettingsUiState state = viewModel.uiStateLiveData.getValue();
                if (state != null) viewModel.updateSettings(state.getCurrentSettings().copyVibrationEnabled(isChecked));
            }
        });

        autoCompleteFocusSound.setOnItemClickListener((parent, view, position, id) -> {
            if (focusSoundAdapter != null) {
                RingtoneItem selected = focusSoundAdapter.getItem(position);
                viewModel.updateSettings(Objects.requireNonNull(viewModel.uiStateLiveData.getValue()).getCurrentSettings().copyFocusSoundUri(selected != null ? selected.uri() : null));
                viewModel.stopPreview();
                focusSoundAdapter.setSelectedUri(selected != null ? selected.uri() : null); // Обновляем выбранный URI в адаптере
            }
        });
        autoCompleteBreakSound.setOnItemClickListener((parent, view, position, id) -> {
            if (breakSoundAdapter != null) {
                RingtoneItem selected = breakSoundAdapter.getItem(position);
                viewModel.updateSettings(Objects.requireNonNull(viewModel.uiStateLiveData.getValue()).getCurrentSettings().copyBreakSoundUri(selected != null ? selected.uri() : null));
                viewModel.stopPreview();
                breakSoundAdapter.setSelectedUri(selected != null ? selected.uri() : null); // Обновляем выбранный URI в адаптере
            }
        });
    }

    private void observeViewModel() {
        viewModel.uiStateLiveData.observe(getViewLifecycleOwner(), uiState -> {
            if (uiState == null) return;

            PomodoroSettings settings = uiState.getCurrentSettings();

            // Work Duration UI Update
            workDurationTitle.setText("Время работы");
            workDurationIcon.setImageResource(R.drawable.timer);
            ImageViewCompat.setImageTintList(workDurationIcon, ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primaryLight)));
            workDurationValueBgCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primaryContainerLight));
            workDurationValueText.setTextColor(ContextCompat.getColor(requireContext(), R.color.onPrimaryContainerLight));
            workDurationValueText.setText(String.valueOf(settings.getWorkDurationMinutes()));
            if (workDurationSlider.getValue() != settings.getWorkDurationMinutes()) {
                workDurationSlider.setValue(settings.getWorkDurationMinutes());
            }
            workDurationSlider.setValueFrom(1f);
            workDurationSlider.setValueTo(60f);
            workDurationSlider.setStepSize(1f);
            workDurationSlider.setThumbTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.primaryLight)));
            workDurationSlider.setTrackActiveTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.primaryLight)));
            workDurationSlider.setTrackInactiveTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.surfaceVariantLight)));


            // Break Duration UI Update
            breakDurationTitle.setText("Время отдыха");
            breakDurationIcon.setImageResource(R.drawable.coffee);
            ImageViewCompat.setImageTintList(breakDurationIcon, ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.tertiaryLight)));
            breakDurationValueBgCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.tertiaryContainerLight));
            breakDurationValueText.setTextColor(ContextCompat.getColor(requireContext(), R.color.onTertiaryContainerLight));
            breakDurationValueText.setText(String.valueOf(settings.getBreakDurationMinutes()));
            if (breakDurationSlider.getValue() != settings.getBreakDurationMinutes()) {
                breakDurationSlider.setValue(settings.getBreakDurationMinutes());
            }
            breakDurationSlider.setValueFrom(1f);
            breakDurationSlider.setValueTo(30f);
            breakDurationSlider.setStepSize(1f);
            breakDurationSlider.setThumbTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.tertiaryLight)));
            breakDurationSlider.setTrackActiveTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.tertiaryLight)));
            breakDurationSlider.setTrackInactiveTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.surfaceVariantLight)));


            // Ringtones
            List<RingtoneItem> allRingtones = new ArrayList<>();
            if (uiState.getSystemRingtones() != null) allRingtones.addAll(uiState.getSystemRingtones());
            if (uiState.getCustomRingtones() != null) allRingtones.addAll(uiState.getCustomRingtones());
            Collections.sort(allRingtones, Comparator.comparing(RingtoneItem::title, String.CASE_INSENSITIVE_ORDER));

            List<RingtoneItem> optionsWithNoSound = new ArrayList<>();
            optionsWithNoSound.add(new RingtoneItem(null, "Без звука", false));
            optionsWithNoSound.addAll(allRingtones);

            // Focus Sound Adapter
            if (focusSoundAdapter == null) {
                focusSoundAdapter = new RingtoneDropdownAdapter(requireContext(), optionsWithNoSound,
                        new RingtoneDropdownAdapter.RingtoneInteractionListener() {
                            @Override public void onPreviewClicked(RingtoneItem item, boolean isPlaying) {
                                viewModel.previewRingtone(Objects.requireNonNull(item.uri())); // URI не должен быть null для превью
                            }
                            @Override public void onRemoveClicked(RingtoneItem item) { viewModel.removeCustomRingtone(item); }
                        }, settings.getFocusSoundUri());
                autoCompleteFocusSound.setAdapter(focusSoundAdapter);
            } else {
                focusSoundAdapter.clear(); // Очищаем старые данные
                focusSoundAdapter.addAll(optionsWithNoSound); // Добавляем новые
                focusSoundAdapter.setSelectedUri(settings.getFocusSoundUri()); // Обновляем выбор
                focusSoundAdapter.notifyDataSetChanged();
            }
            RingtoneItem currentFocusRingtone = optionsWithNoSound.stream()
                    .filter(r -> Objects.equals(r.uri(), settings.getFocusSoundUri())).findFirst().orElse(optionsWithNoSound.get(0));
            autoCompleteFocusSound.setText(currentFocusRingtone.title(), false);


            // Break Sound Adapter
            if (breakSoundAdapter == null) {
                breakSoundAdapter = new RingtoneDropdownAdapter(requireContext(), optionsWithNoSound,
                        new RingtoneDropdownAdapter.RingtoneInteractionListener() {
                            @Override public void onPreviewClicked(RingtoneItem item, boolean isPlaying) {
                                viewModel.previewRingtone(Objects.requireNonNull(item.uri()));
                            }
                            @Override public void onRemoveClicked(RingtoneItem item) { viewModel.removeCustomRingtone(item); }
                        }, settings.getBreakSoundUri());
                autoCompleteBreakSound.setAdapter(breakSoundAdapter);
            } else {
                breakSoundAdapter.clear();
                breakSoundAdapter.addAll(optionsWithNoSound);
                breakSoundAdapter.setSelectedUri(settings.getBreakSoundUri());
                breakSoundAdapter.notifyDataSetChanged();
            }
            RingtoneItem currentBreakRingtone = optionsWithNoSound.stream()
                    .filter(r -> Objects.equals(r.uri(), settings.getBreakSoundUri())).findFirst().orElse(optionsWithNoSound.get(0));
            autoCompleteBreakSound.setText(currentBreakRingtone.title(), false);

            if (focusSoundAdapter != null) focusSoundAdapter.setPlayingUri(uiState.getPlayingRingtoneUri());
            if (breakSoundAdapter != null) breakSoundAdapter.setPlayingUri(uiState.getPlayingRingtoneUri());

            // Vibration Switch
            if (switchVibration.isChecked() != settings.isVibrationEnabled()) {
                switchVibration.setChecked(settings.isVibrationEnabled());
            }

            // Error Message
            if (uiState.getErrorMessage() != null && !uiState.getErrorMessage().isEmpty()) {
                textViewError.setText(uiState.getErrorMessage());
                textViewError.setVisibility(View.VISIBLE);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    // Проверяем, не изменилось ли сообщение об ошибке за это время
                    SettingsUiState latestState = viewModel.uiStateLiveData.getValue();
                    if (latestState != null && Objects.equals(uiState.getErrorMessage(), latestState.getErrorMessage())) {
                        viewModel.clearError();
                    }
                }, 3500);
            } else {
                textViewError.setVisibility(View.GONE);
            }

            // Snackbar и навигация
            if (uiState.isShowSuccess()) {
                // SnackbarManager теперь используется в ViewModel
                viewModel.clearSuccessMessage(); // Говорим ViewModel, что сообщение показано (или будет показано MainActivity)
            }
            if (uiState.isShouldNavigateBack()) {
                // Проверяем, что фрагмент все еще присоединен к FragmentManager
                if (isAdded() && getParentFragmentManager() != null) {
                    try {
                        NavHostFragment.findNavController(this).popBackStack();
                    } catch (IllegalStateException e) {
                        logger.error("TimerSettingsFragment", "NavController not available during back navigation", e);
                    }
                }
                viewModel.onNavigatedBack();
            }
            if (uiState.isShouldNavigateToPomodoroScreen()) {
                // Переходим на экран Pomodoro после сохранения настроек
                if (isAdded() && getParentFragmentManager() != null) {
                    try {
                        NavHostFragment.findNavController(this).navigate(R.id.action_global_to_pomodoroFragment);
                    } catch (IllegalStateException e) {
                        logger.error("TimerSettingsFragment", "NavController not available during Pomodoro navigation", e);
                    }
                }
                viewModel.onNavigatedToPomodoro();
            }
        });
    }

    @Override
    protected void setupToolbar() {
        MaterialToolbar toolbar = getToolbar();
        if (toolbar != null) {
            toolbar.setTitle("Настройки таймера");
            // Убедимся, что иконка навигации "назад" есть (если не обрабатывается NavController)
            // toolbar.setNavigationIcon(R.drawable.arrow_back_ios_new); // Пример
            // toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
            // Добавление MenuProvider для специфичных actions (как в предыдущем ответе)
        }
    }

    @Override
    protected void setupFab() {
        FloatingActionButton standardFab = getStandardFab();
        ExtendedFloatingActionButton extendedFab = getExtendedFab();

        if (standardFab != null) {
            standardFab.hide(); // Скрываем стандартный, если не нужен
        }

        if (extendedFab != null) {
            extendedFab.setText("Сохранить");
            extendedFab.setIconResource(R.drawable.save);
            extendedFab.setOnClickListener(v -> {
                if (viewModel != null) { // viewModel должен быть инициализирован
                    viewModel.saveSettings();
                }
            });
            extendedFab.show(); // Показываем расширенный
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        viewModel.stopPreview();
    }

    @Override
    public void onDestroyView() {
        // Отписываемся от LiveData ViewModel, чтобы избежать утечек, если это необходимо
        // viewModel.uiStateLiveData.removeObservers(getViewLifecycleOwner());
        // Однако, для LiveData, наблюдаемых через getViewLifecycleOwner(), это обычно не требуется.
        // Важнее отписаться от тех, на которые была подписка через observeForever в ViewModel.
        super.onDestroyView();
    }
}