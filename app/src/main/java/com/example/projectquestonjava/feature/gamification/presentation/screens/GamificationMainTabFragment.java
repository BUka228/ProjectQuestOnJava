package com.example.projectquestonjava.feature.gamification.presentation.screens;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.core.utils.Logger; // Убедитесь, что логгер импортирован
import com.example.projectquestonjava.feature.gamification.data.model.Reward;
import com.example.projectquestonjava.feature.gamification.data.model.SurpriseTask;
import com.example.projectquestonjava.feature.gamification.domain.model.PlantHealthState; // Импорт для PlantHealthState
import com.example.projectquestonjava.feature.gamification.presentation.adapters.GamificationMainAdapter;
import com.example.projectquestonjava.feature.gamification.presentation.ui_parts.ChallengeDetailsBottomSheetFragment;
import com.example.projectquestonjava.feature.gamification.presentation.viewmodels.ChallengeCardInfo;
import com.example.projectquestonjava.feature.gamification.presentation.viewmodels.GamificationViewModel;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject; // Для логгера, если он внедряется

@AndroidEntryPoint
public class GamificationMainTabFragment extends Fragment implements GamificationMainAdapter.GamificationItemClickListener {

    private GamificationViewModel sharedViewModel;
    private RecyclerView recyclerViewMainContent;
    private GamificationMainAdapter adapter;
    private ProgressBar progressBarLoading;
    private TextView textViewError;

    private boolean isChallengeDetailsSheetShown = false;

    // Внедряем логгер, если он не доступен через sharedViewModel (или если хотим отдельный для фрагмента)
    // @Inject Logger logger; // Если логгер внедряется напрямую в фрагмент

    public GamificationMainTabFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireParentFragment()).get(GamificationViewModel.class);
        // Если логгер не внедряется, используем логгер из ViewModel:
        // this.logger = sharedViewModel.getLogger(); // Убедитесь, что в ViewModel есть getLogger()
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        sharedViewModel.getLogger().debug("GamificationMainTabFragment", "onCreateView");
        return inflater.inflate(R.layout.fragment_gamification_main_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sharedViewModel.getLogger().debug("GamificationMainTabFragment", "onViewCreated");
        recyclerViewMainContent = view.findViewById(R.id.recyclerView_gamification_main_content);
        progressBarLoading = view.findViewById(R.id.progressBar_gamification_main_loading);
        textViewError = view.findViewById(R.id.textView_gamification_main_error);

        setupRecyclerView();
        observeViewModel();
    }

    private void setupRecyclerView() {
        sharedViewModel.getLogger().debug("GamificationMainTabFragment", "setupRecyclerView");
        adapter = new GamificationMainAdapter(this, requireContext(), sharedViewModel.getLogger());
        recyclerViewMainContent.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewMainContent.setAdapter(adapter);
    }

    private void observeViewModel() {
        final Logger logger = sharedViewModel.getLogger(); // Получаем логгер для использования в лямбдах
        logger.debug("GamificationMainTabFragment", "setupObservers called");

        MediatorLiveData<Object> updateTrigger = new MediatorLiveData<>();
        Observer<Object> observer = ignored -> {
            logger.debug("GamificationMainTabFragment", "UpdateTrigger FIRED. Updating adapter and UI elements.");

            Boolean isLoading = sharedViewModel.isLoadingLiveData.getValue();
            String errorMsg = sharedViewModel.errorMessageEvent.getValue(); // errorMessageEvent - SingleLiveEvent, getValue может вернуть null после обработки

            logger.debug("GamificationMainTabFragment", "isLoading: " + isLoading);
            logger.debug("GamificationMainTabFragment", "errorMsg (from event): " + errorMsg);


            if (Boolean.TRUE.equals(isLoading)) {
                progressBarLoading.setVisibility(View.VISIBLE);
                recyclerViewMainContent.setVisibility(View.GONE);
                textViewError.setVisibility(View.GONE);
                logger.debug("GamificationMainTabFragment", "UI State: LOADING");
            } else {
                progressBarLoading.setVisibility(View.GONE);
                if (errorMsg != null && !errorMsg.isEmpty()) {
                    textViewError.setVisibility(View.VISIBLE);
                    textViewError.setText(errorMsg);
                    recyclerViewMainContent.setVisibility(View.GONE);
                    logger.debug("GamificationMainTabFragment", "UI State: ERROR - " + errorMsg);
                    // Очищаем ошибку после отображения, если это SingleLiveEvent
                    // sharedViewModel.clearErrorMessage(); // Если это нужно делать здесь
                } else {
                    textViewError.setVisibility(View.GONE);
                    recyclerViewMainContent.setVisibility(View.VISIBLE);
                    logger.debug("GamificationMainTabFragment", "UI State: SUCCESS/DATA");
                }
            }

            // Данные для адаптера
            adapter.setGamificationData(sharedViewModel.gamificationState.getValue());
            adapter.setPlantData(sharedViewModel.selectedPlantState.getValue());
            adapter.setPlantHealthState(sharedViewModel.plantHealthState.getValue() != null ? sharedViewModel.plantHealthState.getValue() : PlantHealthState.HEALTHY);
            adapter.setCanWater(Boolean.TRUE.equals(sharedViewModel.canWaterToday.getValue()));
            adapter.setSurpriseTaskData(sharedViewModel.surpriseTaskDetailsState.getValue());
            adapter.setDailyRewardsData(sharedViewModel.dailyRewardsInfo.getValue());
            adapter.setActiveChallengesData(sharedViewModel.challengesSectionState.getValue());
            adapter.notifyDataSetChangedHack();

            logger.debug("GamificationMainTabFragment", "Adapter data set: " +
                    "gamificationState: " + (sharedViewModel.gamificationState.getValue() != null) +
                    ", selectedPlantState: " + (sharedViewModel.selectedPlantState.getValue() != null) +
                    ", plantHealthState: " + sharedViewModel.plantHealthState.getValue() +
                    ", canWaterToday: " + sharedViewModel.canWaterToday.getValue() +
                    ", surpriseTaskDetailsState: " + (sharedViewModel.surpriseTaskDetailsState.getValue() != null) +
                    ", dailyRewardsInfo: " + (sharedViewModel.dailyRewardsInfo.getValue() != null) +
                    ", challengesSectionState: " + (sharedViewModel.challengesSectionState.getValue() != null ? "Total: " + sharedViewModel.challengesSectionState.getValue().getTotalActiveCount() : "null")
            );
        };

        // Добавляем источники в MediatorLiveData
        updateTrigger.addSource(sharedViewModel.isLoadingLiveData, observer);
        updateTrigger.addSource(sharedViewModel.errorMessageEvent, observer); // errorMessageEvent - SingleLiveEvent
        updateTrigger.addSource(sharedViewModel.gamificationState, observer);
        updateTrigger.addSource(sharedViewModel.selectedPlantState, observer);
        updateTrigger.addSource(sharedViewModel.plantHealthState, observer);
        updateTrigger.addSource(sharedViewModel.canWaterToday, observer);
        updateTrigger.addSource(sharedViewModel.surpriseTaskDetailsState, observer);
        updateTrigger.addSource(sharedViewModel.dailyRewardsInfo, observer);
        updateTrigger.addSource(sharedViewModel.challengesSectionState, observer);

        updateTrigger.observe(getViewLifecycleOwner(), ignored -> { /* Тело лямбды уже в observer */ });
        // Вызываем начальное обновление, если данные уже есть
        if (sharedViewModel.isLoadingLiveData.getValue() != null) { // Проверка, что LiveData уже имеет значение
            observer.onChanged(null);
        }


        sharedViewModel.challengeToShowDetails.observe(getViewLifecycleOwner(), info -> {
            logger.debug("GamificationMainTabFragment", "challengeToShowDetails observed: " + (info != null ? "Data" : "null"));
            if (info != null && !isChallengeDetailsSheetShown) {
                logger.debug("GamificationMainTabFragment", "Showing ChallengeDetailsBottomSheetFragment");
                ChallengeDetailsBottomSheetFragment.newInstance()
                        .show(getChildFragmentManager(), "ChallengeDetailsSheetGamificationMain");
                isChallengeDetailsSheetShown = true;
            } else if (info == null && isChallengeDetailsSheetShown) {
                logger.debug("GamificationMainTabFragment", "Dismissing ChallengeDetailsBottomSheetFragment");
                Fragment existingSheet = getChildFragmentManager().findFragmentByTag("ChallengeDetailsSheetGamificationMain");
                if (existingSheet instanceof ChallengeDetailsBottomSheetFragment && existingSheet.isVisible()) {
                    ((ChallengeDetailsBottomSheetFragment) existingSheet).dismissAllowingStateLoss();
                }
                isChallengeDetailsSheetShown = false;
            }
        });
    }

    @Override public void onPlantWidgetClick() {
        sharedViewModel.getLogger().debug("GamificationMainTabFragment", "onPlantWidgetClick");
        ((GamificationFragment) requireParentFragment()).navigateToGardenScreen();
    }
    @Override public void onWaterPlantClick() {
        sharedViewModel.getLogger().debug("GamificationMainTabFragment", "onWaterPlantClick");
        sharedViewModel.waterPlant();
    }
    @Override public void onAcceptSurpriseTaskClick(SurpriseTask task) {
        sharedViewModel.getLogger().debug("GamificationMainTabFragment", "onAcceptSurpriseTaskClick: " + task.getId());
        sharedViewModel.acceptSurpriseTask(task);
    }
    @Override public void onHideSurpriseTaskClick(long taskId) {
        sharedViewModel.getLogger().debug("GamificationMainTabFragment", "onHideSurpriseTaskClick: " + taskId);
        sharedViewModel.hideExpiredSurpriseTask(taskId);
    }
    @Override public void onClaimDailyRewardClick() {
        sharedViewModel.getLogger().debug("GamificationMainTabFragment", "onClaimDailyRewardClick");
        sharedViewModel.claimTodayReward();
    }
    @Override public void onViewAllChallengesClick() {
        sharedViewModel.getLogger().debug("GamificationMainTabFragment", "onViewAllChallengesClick");
        ((GamificationFragment) requireParentFragment()).navigateToAllChallengesScreen();
    }
    @Override public void onChallengeCardClick(ChallengeCardInfo challengeInfo) {
        sharedViewModel.getLogger().debug("GamificationMainTabFragment", "onChallengeCardClick: " + challengeInfo.id());
        sharedViewModel.showChallengeDetails(challengeInfo);
    }
    @Override public void onDailyRewardItemClick(Reward reward, int rewardStreakDay, boolean isToday) {
        sharedViewModel.getLogger().debug("GamificationMainTabFragment", "Daily Reward Item Clicked: " + reward.getName() + ", Day: " + rewardStreakDay + ", isTodayAndClaimable: " + isToday);
        // Можно показать RewardInfoDialogFragment, если он реализован
        // RewardInfoDialogFragment.newInstance(reward).show(getChildFragmentManager(), "RewardInfoDialog");
    }

    public void onChallengeDetailsSheetDismissed() {
        sharedViewModel.getLogger().debug("GamificationMainTabFragment", "onChallengeDetailsSheetDismissed callback");
        isChallengeDetailsSheetShown = false;
        sharedViewModel.clearChallengeDetails();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        sharedViewModel.getLogger().debug("GamificationMainTabFragment", "onDestroyView");
        if (recyclerViewMainContent != null) {
            recyclerViewMainContent.setAdapter(null);
        }
        adapter = null;
        recyclerViewMainContent = null;
        progressBarLoading = null;
        textViewError = null;
    }
}