package com.example.projectquestonjava.feature.profile.presentation.screens; // Уточни пакет

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.text.method.HideReturnsTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import coil.Coil;
import coil.request.ImageRequest;
import coil.transform.CircleCropTransformation;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.core.ui.BaseFragment;
import com.example.projectquestonjava.feature.profile.presentation.viewmodels.ProfileEditUiState;
import com.example.projectquestonjava.feature.profile.presentation.viewmodels.ProfileEditViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Objects;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ProfileEditFragment extends BaseFragment {

    private ProfileEditViewModel viewModel;

    // Avatar Section
    private ImageView imageViewAvatarPreview;
    private FrameLayout overlayAvatarLoading;
    private MaterialButton buttonEditAvatar, buttonRemoveAvatar;

    // Basic Info
    private TextInputLayout textInputLayoutUsername, textInputLayoutEmail;
    private TextInputEditText editTextUsername, editTextEmail;

    // Password Section
    private MaterialButton buttonTogglePasswordFields;
    private LinearLayout layoutPasswordFields;
    private TextView textViewPasswordUpdateError;
    private TextInputLayout textInputLayoutCurrentPassword, textInputLayoutNewPassword, textInputLayoutConfirmPassword;
    private TextInputEditText editTextCurrentPassword, editTextNewPassword, editTextConfirmPassword;

    private ProgressBar progressBarMainLoading; // Для общей загрузки данных
    private ExtendedFloatingActionButton fabSaveChanges; // Ссылка на FAB

    private ActivityResultLauncher<String> imagePickerLauncher;
    private boolean usernameChangedByUser = true; // Флаг для предотвращения рекурсии TextWatcher

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfileEditViewModel.class);
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        viewModel.onAvatarSelected(uri);
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_edit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState); // Вызов setupToolbar и setupFab
        bindViews(view);
        setupListeners();
        observeViewModel();
    }

    private void bindViews(View view) {
        View avatarSectionInclude = view.findViewById(R.id.avatar_section_edit_include);
        imageViewAvatarPreview = avatarSectionInclude.findViewById(R.id.imageView_avatar_preview);
        overlayAvatarLoading = avatarSectionInclude.findViewById(R.id.overlay_avatar_loading);
        buttonEditAvatar = avatarSectionInclude.findViewById(R.id.button_edit_avatar_action);
        buttonRemoveAvatar = avatarSectionInclude.findViewById(R.id.button_remove_avatar_action);

        textInputLayoutUsername = view.findViewById(R.id.textInputLayout_edit_username_profile);
        editTextUsername = view.findViewById(R.id.editText_edit_username_profile);
        textInputLayoutEmail = view.findViewById(R.id.textInputLayout_edit_email_profile);
        editTextEmail = view.findViewById(R.id.editText_edit_email_profile);

        buttonTogglePasswordFields = view.findViewById(R.id.button_toggle_password_fields_edit);
        layoutPasswordFields = view.findViewById(R.id.layout_password_fields_edit);
        textViewPasswordUpdateError = view.findViewById(R.id.textView_password_update_error_edit);
        textInputLayoutCurrentPassword = view.findViewById(R.id.textInputLayout_current_password_edit);
        editTextCurrentPassword = view.findViewById(R.id.editText_current_password_edit);
        textInputLayoutNewPassword = view.findViewById(R.id.textInputLayout_new_password_edit);
        editTextNewPassword = view.findViewById(R.id.editText_new_password_edit);
        textInputLayoutConfirmPassword = view.findViewById(R.id.textInputLayout_confirm_password_edit);
        editTextConfirmPassword = view.findViewById(R.id.editText_confirm_password_edit);

        progressBarMainLoading = view.findViewById(R.id.progressBar_profile_edit_main_loading);
    }

    private void setupListeners() {
        buttonEditAvatar.setOnClickListener(v -> {
            ProfileEditUiState state = viewModel.uiStateLiveData.getValue();
            if (state != null && !state.isAvatarLoading() && !state.isRemovingAvatar() &&
                    !state.isSavingUsername() && !state.isSavingPassword()) {
                imagePickerLauncher.launch("image/*");
            }
        });
        buttonRemoveAvatar.setOnClickListener(v -> viewModel.requestAvatarRemoval());

        editTextUsername.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (usernameChangedByUser) {
                    viewModel.onUsernameChange(s.toString());
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        buttonTogglePasswordFields.setOnClickListener(v -> viewModel.toggleShowPasswordFields());

        // Добавляем TextWatcher для полей пароля
        editTextCurrentPassword.addTextChangedListener(new SimpleTextWatcher(viewModel::onCurrentPasswordChange));
        editTextNewPassword.addTextChangedListener(new SimpleTextWatcher(viewModel::onNewPasswordChange));
        editTextConfirmPassword.addTextChangedListener(new SimpleTextWatcher(viewModel::onConfirmPasswordChange));

        editTextConfirmPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard(v);
                editTextConfirmPassword.clearFocus();
                return true;
            }
            return false;
        });
    }

    private void observeViewModel() {
        viewModel.uiStateLiveData.observe(getViewLifecycleOwner(), uiState -> {
            if (uiState == null) return;

            progressBarMainLoading.setVisibility(uiState.isLoading() ? View.VISIBLE : View.GONE);

            // Аватар
            loadAvatar(uiState.getSelectedAvatarUri(), uiState.getUser() != null ? uiState.getUser().getAvatarUrl() : null);
            overlayAvatarLoading.setVisibility(uiState.isAvatarLoading() || uiState.isRemovingAvatar() ? View.VISIBLE : View.GONE);
            boolean anySaveInProgress = uiState.isSavingUsername() || uiState.isSavingPassword() || uiState.isAvatarLoading() || uiState.isRemovingAvatar();
            buttonEditAvatar.setEnabled(!anySaveInProgress);
            buttonRemoveAvatar.setEnabled(!anySaveInProgress);
            buttonRemoveAvatar.setVisibility((uiState.getUser() != null && uiState.getUser().getAvatarUrl() != null) || uiState.getSelectedAvatarUri() != null ? View.VISIBLE : View.GONE);

            // Имя пользователя
            usernameChangedByUser = false; // Временно отключаем слушатель
            if (!Objects.equals(editTextUsername.getText().toString(), uiState.getEditedUsername())) {
                editTextUsername.setText(uiState.getEditedUsername());
                editTextUsername.setSelection(uiState.getEditedUsername().length());
            }
            usernameChangedByUser = true; // Включаем слушатель обратно
            textInputLayoutUsername.setError(uiState.getUsernameError());
            textInputLayoutUsername.setEnabled(!anySaveInProgress);

            // Email
            editTextEmail.setText(uiState.getUser() != null ? uiState.getUser().getEmail() : "");

            // Пароли
            buttonTogglePasswordFields.setText(uiState.isShowPasswordFields() ? "Отмена смены пароля" : "Изменить пароль");
            buttonTogglePasswordFields.setIconResource(uiState.isShowPasswordFields() ? R.drawable.keyboard_arrow_up : R.drawable.keyboard_arrow_down);
            layoutPasswordFields.setVisibility(uiState.isShowPasswordFields() ? View.VISIBLE : View.GONE);

            if (uiState.isShowPasswordFields()) {
                if (!Objects.equals(editTextCurrentPassword.getText().toString(), uiState.getCurrentPassword()))
                    editTextCurrentPassword.setText(uiState.getCurrentPassword());
                if (!Objects.equals(editTextNewPassword.getText().toString(), uiState.getNewPassword()))
                    editTextNewPassword.setText(uiState.getNewPassword());
                if (!Objects.equals(editTextConfirmPassword.getText().toString(), uiState.getConfirmPassword()))
                    editTextConfirmPassword.setText(uiState.getConfirmPassword());

                textInputLayoutCurrentPassword.setError(uiState.getCurrentPasswordError());
                textInputLayoutNewPassword.setError(uiState.getNewPasswordError());
                textInputLayoutConfirmPassword.setError(uiState.getConfirmPasswordError());
                textViewPasswordUpdateError.setText(uiState.getPasswordUpdateError());
                textViewPasswordUpdateError.setVisibility(uiState.getPasswordUpdateError() != null ? View.VISIBLE : View.GONE);
            }
            textInputLayoutCurrentPassword.setEnabled(!anySaveInProgress);
            textInputLayoutNewPassword.setEnabled(!anySaveInProgress);
            textInputLayoutConfirmPassword.setEnabled(!anySaveInProgress);
            buttonTogglePasswordFields.setEnabled(!anySaveInProgress);

            // Ошибки и успех
            if (uiState.getGeneralError() != null) {
                showSnackbar(uiState.getGeneralError());
                viewModel.clearGeneralError();
            }
            if (uiState.getAvatarUpdateError() != null) {
                showSnackbar(uiState.getAvatarUpdateError());
                viewModel.clearAvatarError();
            }
            // passwordUpdateError отображается как textViewPasswordUpdateError, но можно и в Snackbar

            if (uiState.isSaveSuccess()) {
                showSnackbar("Профиль успешно сохранен!"); // Показываем Snackbar из ViewModel
                NavHostFragment.findNavController(this).popBackStack();
                viewModel.onNavigatedBack();
            }

            // Диалог удаления аватара
            if (uiState.isShowAvatarDeleteConfirm()) {
                showAvatarDeleteConfirmationDialog();
                viewModel.onAvatarDeleteDialogShown();
            }

            // Обновление состояния FAB
            if (fabSaveChanges != null) {
                configureFab(uiState);
            }
        });
    }

    private void loadAvatar(Uri previewUri, String currentUrl) {
        ImageRequest.Builder requestBuilder = new ImageRequest.Builder(requireContext())
                .placeholder(R.drawable.person) // Создать этот drawable
                .error(R.drawable.person)
                .transformations(new CircleCropTransformation());

        if (previewUri != null) {
            requestBuilder.data(previewUri);
        } else if (currentUrl != null && !currentUrl.isEmpty()) {
            requestBuilder.data(currentUrl);
        } else {
            requestBuilder.data(R.drawable.person);
        }
        Coil.imageLoader(requireContext()).enqueue(requestBuilder.target(imageViewAvatarPreview).build());
    }

    private void showAvatarDeleteConfirmationDialog() {
        new AlertDialog.Builder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
                .setTitle("Удалить аватар?")
                .setMessage("Вы уверены, что хотите удалить текущий аватар?")
                .setPositiveButton("Удалить", (dialog, which) -> viewModel.confirmAvatarRemoval())
                .setNegativeButton("Отмена", (dialog, which) -> viewModel.dismissAvatarRemovalDialog())
                .setOnDismissListener(dialog -> {
                    // Если диалог закрыт без выбора (например, кнопкой Назад), также сбрасываем флаг
                    if (viewModel.uiStateLiveData.getValue() != null && viewModel.uiStateLiveData.getValue().isShowAvatarDeleteConfirm()) {
                        viewModel.dismissAvatarRemovalDialog();
                    }
                })
                .show();
    }

    @Override
    protected void setupToolbar() {
        MaterialToolbar toolbar = getToolbar();
        if (toolbar != null) {
            toolbar.setTitle("Редактирование");
            toolbar.setNavigationIcon(R.drawable.arrow_back_ios_new);
            toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
            toolbar.getMenu().clear();
        }
    }

    @Override
    protected void setupFab() {
        fabSaveChanges = getExtendedFab(); // Получаем Extended FAB из MainActivity
        if (fabSaveChanges != null) {
            ProfileEditUiState initialState = viewModel.uiStateLiveData.getValue();
            if (initialState != null) {
                configureFab(initialState);
            }
            fabSaveChanges.setOnClickListener(v -> {
                ProfileEditUiState state = viewModel.uiStateLiveData.getValue();
                if (state != null && !state.isSavingUsername() && !state.isSavingPassword() && !state.isAvatarLoading() && !state.isRemovingAvatar()) {
                    viewModel.saveChanges();
                }
            });
        }
        if (getStandardFab() != null) getStandardFab().hide(); // Скрываем стандартный, если есть
    }

    private void configureFab(ProfileEditUiState uiState) {
        if (fabSaveChanges == null) return;

        boolean isAnySavingInProgress = uiState.isSavingUsername() || uiState.isSavingPassword() || uiState.isAvatarLoading() || uiState.isRemovingAvatar();

        if (isAnySavingInProgress) {
            fabSaveChanges.setText("Сохранение...");
            fabSaveChanges.setIconResource(0); // Убираем иконку, если хотим показать ProgressBar
            // Здесь можно добавить ProgressBar на FAB, если он кастомный,
            // или изменить стиль кнопки, чтобы она выглядела как "загружается".
            // Например, можно использовать setIcon(null) и показывать ProgressBar рядом или вместо.
        } else {
            fabSaveChanges.setText("Сохранить");
            fabSaveChanges.setIconResource(R.drawable.save); // Убедись, что drawable save есть
        }
        fabSaveChanges.setEnabled(!isAnySavingInProgress && uiState.isHasUnsavedChanges()); // Активен, если есть изменения и нет загрузки
        fabSaveChanges.show(); // Показываем FAB
    }

    private void showSnackbar(String message) {
        if (getView() != null) {
            Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    // Простой TextWatcher для уменьшения бойлерплейта
    private static class SimpleTextWatcher implements TextWatcher {
        private final java.util.function.Consumer<String> onTextChangedConsumer;
        SimpleTextWatcher(java.util.function.Consumer<String> consumer) { this.onTextChangedConsumer = consumer; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { onTextChangedConsumer.accept(s.toString()); }
        @Override public void afterTextChanged(Editable s) {}
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Очистка ссылок на View
        imageViewAvatarPreview = null; overlayAvatarLoading = null; buttonEditAvatar = null; buttonRemoveAvatar = null;
        textInputLayoutUsername = null; editTextUsername = null; textInputLayoutEmail = null; editTextEmail = null;
        buttonTogglePasswordFields = null; layoutPasswordFields = null; textViewPasswordUpdateError = null;
        textInputLayoutCurrentPassword = null; editTextCurrentPassword = null; textInputLayoutNewPassword = null;
        editTextNewPassword = null; textInputLayoutConfirmPassword = null; editTextConfirmPassword = null;
        progressBarMainLoading = null; fabSaveChanges = null;
    }
}