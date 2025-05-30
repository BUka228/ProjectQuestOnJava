// -------- A:\Progects\ProjectQuestOnJava\app\src\main\java\com\example\projectquestonjava\feature\profile\presentation\screens\SettingsFragment.java --------
package com.example.projectquestonjava.feature.profile.presentation.screens;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.core.widget.ImageViewCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.core.ui.BaseFragment;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.feature.profile.presentation.viewmodels.AppTheme;
import com.example.projectquestonjava.feature.profile.presentation.viewmodels.MainSettingsScreenUiState;
import com.example.projectquestonjava.feature.profile.presentation.viewmodels.MainSettingsViewModel;
import com.example.projectquestonjava.utils.dialogs.DeleteConfirmationDialogFragment;
import com.example.projectquestonjava.utils.dialogs.LogoutConfirmationDialog;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.Objects;
import java.util.function.Consumer;
import javax.inject.Inject;

@AndroidEntryPoint
public class SettingsFragment extends BaseFragment {

    private static final String TAG = "SettingsFragment";

    private MainSettingsViewModel viewModel;
    @Inject Logger logger;

    private LinearLayout settingsContentLayout; // Главный контейнер для секций
    private ProgressBar progressBarLoading;

    // Ссылки на View, которые обновляются из observeViewModel, теперь будут инициализированы в buildSettingsScreen
    private TextView themeSummaryText;
    private MaterialSwitch dynamicColorSwitch, notificationsSwitch;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MainSettingsViewModel.class);
        if (logger != null) {
            logger.debug(TAG, "ViewModel initialized in onCreate.");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                // No specific menu for this fragment's toolbar if managed by BaseFragment or NavGraph
            }
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        observeViewModel();
    }

    private void bindViews(View view) {
        settingsContentLayout = view.findViewById(R.id.settings_content_layout);
        progressBarLoading = view.findViewById(R.id.progressBar_settings_loading);
        logger.debug(TAG, "Views bound. settingsContentLayout is " + (settingsContentLayout == null ? "null" : "not null"));
    }

    private void buildSettingsScreen() {
        if (getContext() == null || settingsContentLayout == null) {
            logger.error(TAG, "Cannot buildSettingsScreen - context or layout is null.");
            return;
        }
        logger.debug(TAG, "Building settings screen dynamically.");
        LayoutInflater inflater = LayoutInflater.from(getContext());
        settingsContentLayout.removeAllViews(); // Очищаем перед построением

        LinearLayout currentSectionItemsContainer;

        // --- Секция Аккаунт ---
        currentSectionItemsContainer = addSectionTitle("Аккаунт");
        if (currentSectionItemsContainer != null) {
            currentSectionItemsContainer.addView(createActionItem(inflater, R.drawable.exit_to_app, "Выйти из аккаунта", null, true, viewModel::showLogoutDialog, currentSectionItemsContainer));
        }

        // --- Секция Внешний вид ---
        currentSectionItemsContainer = addSectionTitle("Внешний вид");
        if (currentSectionItemsContainer != null) {
            View themeItemView = createNavigationItem(inflater, R.drawable.palette, "Тема приложения", null, viewModel::showThemeDialog, currentSectionItemsContainer);
            themeSummaryText = themeItemView.findViewById(R.id.textView_settings_item_subtitle);
            currentSectionItemsContainer.addView(themeItemView);

            View dynamicColorItemView = createSwitchItem(inflater, R.drawable.tonality, "Динамические цвета", "Использовать цвета обоев (Material You)", viewModel::updateDynamicColor, currentSectionItemsContainer);
            dynamicColorSwitch = dynamicColorItemView.findViewById(R.id.switch_settings_item_toggle);
            currentSectionItemsContainer.addView(dynamicColorItemView);
        }

        // --- Секция Уведомления ---
        currentSectionItemsContainer = addSectionTitle("Уведомления");
        if (currentSectionItemsContainer != null) {
            View notificationsItemView = createSwitchItem(inflater, R.drawable.notifications, "Разрешить уведомления", "Основные уведомления приложения", viewModel::updateNotifications, currentSectionItemsContainer);
            notificationsSwitch = notificationsItemView.findViewById(R.id.switch_settings_item_toggle);
            currentSectionItemsContainer.addView(notificationsItemView);
            currentSectionItemsContainer.addView(createNavigationItem(inflater, R.drawable.settings_applications, "Системные настройки уведомлений", null, this::openSystemNotificationSettings, currentSectionItemsContainer));
        }

        // --- Секция Pomodoro ---
        currentSectionItemsContainer = addSectionTitle("Pomodoro");
        if (currentSectionItemsContainer != null) {
            currentSectionItemsContainer.addView(createNavigationItem(inflater, R.drawable.timer, "Настроить таймер", null, () -> {
                if (NavHostFragment.findNavController(this).getCurrentDestination() != null &&
                        NavHostFragment.findNavController(this).getCurrentDestination().getId() == R.id.settingsFragment) {
                    NavHostFragment.findNavController(this).navigate(R.id.action_settingsFragment_to_timerSettingsFragment);
                }
            }, currentSectionItemsContainer));
        }

        // --- Управление данными ---
        currentSectionItemsContainer = addSectionTitle("Управление данными");
        if (currentSectionItemsContainer != null) {
            currentSectionItemsContainer.addView(createActionItem(inflater, R.drawable.delete_sweep, "Очистить кэш", null, false, viewModel::clearCache, currentSectionItemsContainer));
            currentSectionItemsContainer.addView(createActionItem(inflater, R.drawable.ios_share, "Экспорт данных", null, false, viewModel::exportData, currentSectionItemsContainer));
            currentSectionItemsContainer.addView(createActionItem(inflater, R.drawable.download, "Импорт данных", null, false, viewModel::importData, currentSectionItemsContainer));
        }

        // --- О приложении ---
        currentSectionItemsContainer = addSectionTitle("О приложении");
        if (currentSectionItemsContainer != null) {
            currentSectionItemsContainer.addView(createInfoItem(inflater, R.drawable.info, "Версия приложения", "1.0.0 (alpha)", currentSectionItemsContainer));
            currentSectionItemsContainer.addView(createNavigationItem(inflater, R.drawable.gavel, "Условия использования", null, () -> showStubSnackbar("Условия использования"), currentSectionItemsContainer));
            currentSectionItemsContainer.addView(createNavigationItem(inflater, R.drawable.shield_lock, "Политика конфиденциальности", null, () -> showStubSnackbar("Политика конфиденциальности"), currentSectionItemsContainer));
            currentSectionItemsContainer.addView(createNavigationItem(inflater, R.drawable.receipt_long, "Лицензии открытого ПО", null, () -> showStubSnackbar("Лицензии ПО"), currentSectionItemsContainer));
        }

        // --- Опасно ---
        currentSectionItemsContainer = addSectionTitle("Опасно", true);
        if (currentSectionItemsContainer != null) {
            currentSectionItemsContainer.addView(createActionItem(inflater, R.drawable.delete_forever, "Удалить аккаунт", null, true, viewModel::showDeleteAccountDialog, currentSectionItemsContainer));
        }
    }

    private LinearLayout addSectionTitle(String title, boolean isDangerZone) {
        if (getContext() == null || settingsContentLayout == null) return null;
        LayoutInflater inflater = LayoutInflater.from(getContext());
        MaterialCardView sectionCardView = (MaterialCardView) inflater.inflate(R.layout.item_settings_section_card, settingsContentLayout, false);
        TextView titleTv = sectionCardView.findViewById(R.id.textView_settings_section_title);
        LinearLayout itemsContainer = sectionCardView.findViewById(R.id.settings_section_content_container);

        if (titleTv != null) {
            titleTv.setText(title);
            if (isDangerZone && getContext() != null) {
                titleTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.errorDark));
            }
        }
        settingsContentLayout.addView(sectionCardView);
        return itemsContainer;
    }

    private LinearLayout addSectionTitle(String title) {
        return addSectionTitle(title, false);
    }

    private View createActionItem(LayoutInflater inflater, @DrawableRes int iconRes, String title, @Nullable String subtitle, boolean isDestructive, Runnable onClickAction, ViewGroup parentForItems) {
        View itemView = inflater.inflate(R.layout.item_settings_action, parentForItems, false);
        configureBaseItem(itemView, iconRes, title, subtitle, isDestructive, onClickAction);
        return itemView;
    }

    private View createNavigationItem(LayoutInflater inflater, @DrawableRes int iconRes, String title, @Nullable String currentSummary, Runnable onClickAction, ViewGroup parentForItems) {
        View itemView = inflater.inflate(R.layout.item_settings_navigation, parentForItems, false);
        configureBaseItem(itemView, iconRes, title, currentSummary, false, onClickAction);
        return itemView;
    }

    private View createSwitchItem(LayoutInflater inflater, @DrawableRes int iconRes, String title, @Nullable String subtitle, Consumer<Boolean> onCheckedChangeCallback, ViewGroup parentForItems) {
        View itemView = inflater.inflate(R.layout.item_settings_switch, parentForItems, false);
        configureBaseItem(itemView, iconRes, title, subtitle, false, null);

        MaterialSwitch switchView = itemView.findViewById(R.id.switch_settings_item_toggle);
        if (switchView != null) {
            // Initial checked state will be set in observeViewModel
            switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) { // Only react to user input
                    onCheckedChangeCallback.accept(isChecked);
                }
            });
            itemView.setOnClickListener(v -> { if (switchView.isEnabled()) switchView.toggle(); });
        }
        return itemView;
    }

    private View createInfoItem(LayoutInflater inflater, @DrawableRes int iconRes, String title, String value, ViewGroup parentForItems) {
        View itemView = inflater.inflate(R.layout.item_settings_info, parentForItems, false);
        configureBaseItem(itemView, iconRes, title, value, false, null);
        itemView.setClickable(false);
        itemView.setFocusable(false);
        return itemView;
    }

    private void configureBaseItem(View itemView, @DrawableRes int iconRes, String title, @Nullable String subtitle, boolean isDestructive, @Nullable Runnable onClickAction) {
        ImageView icon = itemView.findViewById(R.id.imageView_settings_item_icon);
        TextView titleTv = itemView.findViewById(R.id.textView_settings_item_title);
        TextView subtitleTv = itemView.findViewById(R.id.textView_settings_item_subtitle);

        if (icon != null) icon.setImageResource(iconRes);
        if (titleTv != null) titleTv.setText(title);

        if (subtitleTv != null) {
            if (subtitle != null && !subtitle.isEmpty()) {
                subtitleTv.setText(subtitle);
                subtitleTv.setVisibility(View.VISIBLE);
            } else {
                subtitleTv.setVisibility(View.GONE);
            }
        }

        if (isDestructive && titleTv != null && icon != null && getContext() != null) {
            titleTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.errorDark));
            ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.errorDark)));
        }

        if (onClickAction != null) {
            itemView.setOnClickListener(v -> onClickAction.run());
            itemView.setClickable(true);
            itemView.setFocusable(true);
        } else {
            // If it's a switch item, the row itself is clickable to toggle the switch
            // For info items, it's already set to not clickable
            if (!(itemView.findViewById(R.id.switch_settings_item_toggle) instanceof MaterialSwitch)) {
                itemView.setClickable(false);
                itemView.setFocusable(false);
            }
        }
    }

    private void observeViewModel() {
        viewModel.uiStateLiveData.observe(getViewLifecycleOwner(), uiState -> {
            if (uiState == null) {
                logger.warn(TAG, "UI State is null in observeViewModel.");
                return;
            }
            logger.debug(TAG, "UI State observed: isLoading=" + uiState.isLoading());

            if (progressBarLoading != null) {
                progressBarLoading.setVisibility(uiState.isLoading() ? View.VISIBLE : View.GONE);
            }
            if (settingsContentLayout != null) {
                settingsContentLayout.setAlpha(uiState.isLoading() ? 0.5f : 1.0f);
                if (settingsContentLayout.getChildCount() == 0 && !uiState.isLoading()) {
                    buildSettingsScreen(); // Строим экран, если он еще не построен и загрузка завершена
                }
            }

            if (uiState.getError() != null) {
                showSnackbar(uiState.getError());
                viewModel.clearError();
            }
            if (uiState.getSuccessMessage() != null) {
                showSnackbar(uiState.getSuccessMessage());
                viewModel.clearSuccessMessage();
            }

            // Обновляем UI элементы, если они уже созданы (т.е. buildSettingsScreen был вызван)
            if (themeSummaryText != null) {
                themeSummaryText.setText(getThemeName(uiState.getSelectedTheme()));
            }

            if (dynamicColorSwitch != null) {
                if (dynamicColorSwitch.isChecked() != uiState.isUseDynamicColor()) {
                    dynamicColorSwitch.setChecked(uiState.isUseDynamicColor());
                }
                dynamicColorSwitch.setEnabled(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S);
                View parentItemView = (View) dynamicColorSwitch.getParent().getParent().getParent(); // Switch -> FrameLayout -> LinearLayout(item_settings_base)
                if (parentItemView instanceof ViewGroup) {
                    TextView subtitle = parentItemView.findViewById(R.id.textView_settings_item_subtitle);
                    if (subtitle != null) {
                        subtitle.setText(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ?
                                "Использовать цвета обоев (Material You)" : "Доступно с Android 12+");
                    }
                }
            }

            if (notificationsSwitch != null) {
                if (notificationsSwitch.isChecked() != uiState.isNotificationsEnabled()) {
                    notificationsSwitch.setChecked(uiState.isNotificationsEnabled());
                }
            }

            if (uiState.isShowThemeDialog()) {
                showThemeSelectionDialog(uiState.getSelectedTheme());
            }
            if (uiState.isShowLogoutDialog()) {
                new LogoutConfirmationDialog(() -> viewModel.logout())
                        .show(getChildFragmentManager(), "MainSettingsLogoutConfirm");
                viewModel.dismissLogoutDialog();
            }
            if (uiState.isShowDeleteAccountDialog()) {
                DeleteConfirmationDialogFragment.newInstance(
                        "Удалить аккаунт?",
                        "Это действие необратимо. Все ваши данные будут удалены.",
                        "Удалить",
                        R.drawable.delete_forever,
                        () -> viewModel.deleteAccount()
                ).show(getChildFragmentManager(), "MainSettingsDeleteAccountConfirm");
                viewModel.dismissDeleteAccountDialog();
            }
        });
    }

    private String getThemeName(AppTheme theme) {
        return switch (theme) {
            case SYSTEM -> "Как в системе";
            case LIGHT -> "Светлая";
            case DARK -> "Темная";
        };
    }

    private void showThemeSelectionDialog(AppTheme currentTheme) {
        if (getContext() == null) return;
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog);
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_theme_selection, null);
        RadioGroup radioGroup = dialogView.findViewById(R.id.radioGroup_theme_selection);
        final AlertDialog[] dialogHolder = new AlertDialog[1];

        ((MaterialRadioButton)dialogView.findViewById(R.id.radioButton_theme_system)).setChecked(currentTheme == AppTheme.SYSTEM);
        ((MaterialRadioButton)dialogView.findViewById(R.id.radioButton_theme_light)).setChecked(currentTheme == AppTheme.LIGHT);
        ((MaterialRadioButton)dialogView.findViewById(R.id.radioButton_theme_dark)).setChecked(currentTheme == AppTheme.DARK);

        builder.setView(dialogView);
        builder.setOnDismissListener(dialog -> viewModel.dismissThemeDialog());

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            AppTheme selectedTheme = AppTheme.SYSTEM;
            if (checkedId == R.id.radioButton_theme_light) selectedTheme = AppTheme.LIGHT;
            else if (checkedId == R.id.radioButton_theme_dark) selectedTheme = AppTheme.DARK;

            logger.debug(TAG, "Theme selected in dialog: " + selectedTheme.name());
            viewModel.updateTheme(selectedTheme);
            AppCompatDelegate.setDefaultNightMode(switch (selectedTheme) {
                case LIGHT -> AppCompatDelegate.MODE_NIGHT_NO;
                case DARK -> AppCompatDelegate.MODE_NIGHT_YES;
                default -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            });
            if (dialogHolder[0] != null) {
                dialogHolder[0].dismiss();
            }
        });
        dialogHolder[0] = builder.show();
    }

    private void openSystemNotificationSettings() {
        if (getContext() == null) return;
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
        } else {
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("app_package", requireContext().getPackageName());
            intent.putExtra("app_uid", requireContext().getApplicationInfo().uid);
        }
        try {
            startActivity(intent);
        } catch (Exception e) {
            logger.error(TAG, "Failed to open system notification settings", e);
            showSnackbar("Не удалось открыть системные настройки уведомлений.");
        }
    }

    private void showStubSnackbar(String featureName) {
        if (getView() != null) {
            Snackbar.make(requireView(), featureName + ": в разработке", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void showSnackbar(String message) {
        if (getView() != null && message != null && !message.isEmpty()) {
            Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    protected void setupToolbar() {
        MaterialToolbar toolbar = getToolbar();
        if (toolbar != null) {
            toolbar.setTitle("Настройки");
            toolbar.setNavigationIcon(R.drawable.arrow_back_ios_new);
            toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
            toolbar.getMenu().clear();
        }
    }

    @Override
    protected void setupFab() {
        if (getStandardFab() != null) getStandardFab().hide();
        if (getExtendedFab() != null) getExtendedFab().hide();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        settingsContentLayout = null;
        progressBarLoading = null;
        themeSummaryText = null;
        dynamicColorSwitch = null;
        notificationsSwitch = null;
        logger.debug(TAG, "SettingsFragment onDestroyView");
    }
}