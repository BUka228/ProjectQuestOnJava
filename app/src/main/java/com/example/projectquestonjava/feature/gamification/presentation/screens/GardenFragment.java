package com.example.projectquestonjava.feature.gamification.presentation.screens;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.core.ui.BaseFragment;
import com.example.projectquestonjava.feature.gamification.presentation.adapters.GardenPlantsAdapter;
import com.example.projectquestonjava.feature.gamification.presentation.viewmodels.GardenScreenUiState;
import com.example.projectquestonjava.feature.gamification.presentation.viewmodels.GardenViewModel;
import com.example.projectquestonjava.feature.gamification.presentation.ui_elements.WateringEffectView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import java.util.Collections;
import java.util.Objects;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class GardenFragment extends BaseFragment {

    private GardenViewModel viewModel;
    private RecyclerView recyclerViewGardenPlants;
    private GardenPlantsAdapter plantsAdapter;
    private LinearLayout emptyGardenLayout;
    private ProgressBar progressBarLoading;
    private WateringEffectView wateringEffectView;
    private ExtendedFloatingActionButton fabWaterPlant; // Получаем из MainActivity

    public GardenFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_garden, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(GardenViewModel.class);

        bindViews(view);
        setupRecyclerView();
        // setupFab() вызовется из BaseFragment после onViewCreated
        setupObservers();
    }

    private void bindViews(View view) {
        recyclerViewGardenPlants = view.findViewById(R.id.recyclerView_garden_plants);
        emptyGardenLayout = view.findViewById(R.id.layout_empty_garden);
        progressBarLoading = view.findViewById(R.id.progressBar_garden_loading);
        wateringEffectView = view.findViewById(R.id.watering_effect_overlay_garden);
    }

    @Override
    protected void setupToolbar() {
        MaterialToolbar toolbar = getToolbar();
        if (toolbar != null) {
            toolbar.setTitle("Мой Сад");
            // Используем стандартную иконку "назад" из NavigationUI
            // Если ее нет, или нужно кастомное поведение:
            toolbar.setNavigationIcon(R.drawable.arrow_back_ios_new); // Убедись, что drawable существует
            toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
            toolbar.getMenu().clear(); // Очищаем меню от предыдущих фрагментов
        }
    }

    @Override
    protected void setupFab() {
        fabWaterPlant = getExtendedFab(); // Используем Extended FAB
        if (fabWaterPlant == null) {
            if (getStandardFab() != null) getStandardFab().hide(); // Скрываем и стандартный на всякий случай
            return;
        }
        if (getStandardFab() != null) getStandardFab().hide(); // Скрываем стандартный

        // Начальная настройка FAB
        GardenScreenUiState initialState = viewModel.uiStateLiveData.getValue();
        if (initialState != null) {
            configureFabUi(initialState);
        } else {
            // Если состояние еще не пришло, скрываем FAB или ставим дефолтное состояние
            fabWaterPlant.hide();
        }

        // Слушатель клика устанавливается один раз
        fabWaterPlant.setOnClickListener(v -> {
            GardenScreenUiState currentState = viewModel.uiStateLiveData.getValue();
            if (currentState != null && currentState.isCanWaterToday() &&
                    !currentState.isLoading() && !currentState.isShowWateringConfirmation()) {
                viewModel.waterPlant();
            }
        });
    }

    private void configureFabUi(@NonNull GardenScreenUiState uiState) {
        if (fabWaterPlant == null) return;

        boolean isEnabled = uiState.isCanWaterToday() && !uiState.isLoading() && !uiState.isShowWateringConfirmation();
        fabWaterPlant.setEnabled(isEnabled);

        if (uiState.isShowWateringConfirmation()) {
            fabWaterPlant.setIconResource(R.drawable.check); // Иконка галочки
            fabWaterPlant.setText("Полито!");
            fabWaterPlant.extend(); // Показываем текст
        } else {
            fabWaterPlant.setIconResource(R.drawable.water_drop);
            fabWaterPlant.setText(uiState.isCanWaterToday() ? "Полить все" : "Уже полито");
            if (uiState.isCanWaterToday()) {
                fabWaterPlant.extend();
            } else {
                fabWaterPlant.shrink(); // Скрываем текст, если поливать нельзя (и не идет подтверждение)
            }
        }

        // Управление цветом фона и контента
        int fabBgColorRes = isEnabled ? R.color.primaryLight : R.color.surfaceVariantDark; // Замени на свои цвета
        int fabContentColorRes = isEnabled ? R.color.onPrimaryLight : R.color.onSurfaceVariantDark; // Замени

        fabWaterPlant.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), fabBgColorRes)));
        fabWaterPlant.setTextColor(ContextCompat.getColor(requireContext(), fabContentColorRes));
        fabWaterPlant.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), fabContentColorRes)));

        if (uiState.getPlants().isEmpty() && !uiState.isLoading()) {
            fabWaterPlant.hide(); // Скрываем FAB если сад пуст
        } else {
            fabWaterPlant.show();
        }
    }

    private void setupRecyclerView() {
        plantsAdapter = new GardenPlantsAdapter(
                requireContext(),
                Collections.emptyList(),
                -1L,
                Collections.emptyMap(),
                plant -> viewModel.selectActivePlant(plant.getId())
        );
        recyclerViewGardenPlants.setAdapter(plantsAdapter);
        int screenWidthDp = getResources().getConfiguration().screenWidthDp;
        int spanCount = Math.max(2, screenWidthDp / 120);
        recyclerViewGardenPlants.setLayoutManager(new GridLayoutManager(getContext(), spanCount));
        // Добавляем отступы для RecyclerView, чтобы FAB его не перекрывал
        // recyclerViewGardenPlants.setPadding(
        //         recyclerViewGardenPlants.getPaddingLeft(),
        //         recyclerViewGardenPlants.getPaddingTop(),
        //         recyclerViewGardenPlants.getPaddingRight(),
        //         getResources().getDimensionPixelSize(R.dimen.fab_bottom_margin_for_list) // Создать этот dimen
        // );
    }

    private void setupObservers() {
        viewModel.uiStateLiveData.observe(getViewLifecycleOwner(), uiState -> {
            if (uiState == null) return;

            progressBarLoading.setVisibility(uiState.isLoading() && !uiState.isShowWateringPlantEffect() ? View.VISIBLE : View.GONE);
            boolean showEmpty = uiState.getPlants().isEmpty() && !uiState.isLoading();
            emptyGardenLayout.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
            recyclerViewGardenPlants.setVisibility(!showEmpty ? View.VISIBLE : View.GONE);

            plantsAdapter.updateData(uiState.getPlants(), uiState.getSelectedPlantId(), uiState.getHealthStates());
            configureFabUi(uiState); // Обновляем состояние FAB

            if (uiState.getSuccessMessage() != null) {
                Snackbar.make(requireView(), uiState.getSuccessMessage(), Snackbar.LENGTH_SHORT).show();
                viewModel.clearSuccessMessage();
            }
            if (uiState.getErrorMessage() != null) {
                Snackbar.make(requireView(), uiState.getErrorMessage(), Snackbar.LENGTH_LONG).show();
                viewModel.clearErrorMessage();
            }

            if (uiState.isShowWateringPlantEffect()) {
                if (wateringEffectView.getVisibility() == View.GONE) {
                    wateringEffectView.setVisibility(View.VISIBLE);
                    wateringEffectView.startEffect();
                }
            } else {
                // WateringEffectView сам скроется после завершения анимации
                // wateringEffectView.setVisibility(View.GONE); // Не нужно, если он сам себя скрывает
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerViewGardenPlants.setAdapter(null); // Отсоединяем адаптер
        plantsAdapter = null;
        recyclerViewGardenPlants = null;
        emptyGardenLayout = null;
        progressBarLoading = null;
        wateringEffectView = null;
        fabWaterPlant = null;
    }
}