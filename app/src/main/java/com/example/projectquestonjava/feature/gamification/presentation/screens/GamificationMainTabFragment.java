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
import com.example.projectquestonjava.feature.gamification.data.model.Reward;
import com.example.projectquestonjava.feature.gamification.data.model.SurpriseTask;
import com.example.projectquestonjava.feature.gamification.presentation.adapters.GamificationMainAdapter;
import com.example.projectquestonjava.feature.gamification.presentation.ui_parts.ChallengeDetailsBottomSheetFragment;
import com.example.projectquestonjava.feature.gamification.presentation.viewmodels.ChallengeCardInfo;
import com.example.projectquestonjava.feature.gamification.presentation.viewmodels.GamificationViewModel;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class GamificationMainTabFragment extends Fragment implements GamificationMainAdapter.GamificationItemClickListener {

    private GamificationViewModel sharedViewModel;
    private RecyclerView recyclerViewMainContent;
    private GamificationMainAdapter adapter;
    private ProgressBar progressBarLoading;
    private TextView textViewError;

    private boolean isChallengeDetailsSheetShown = false;

    public GamificationMainTabFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireParentFragment()).get(GamificationViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gamification_main_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerViewMainContent = view.findViewById(R.id.recyclerView_gamification_main_content);
        progressBarLoading = view.findViewById(R.id.progressBar_gamification_main_loading);
        textViewError = view.findViewById(R.id.textView_gamification_main_error);

        setupRecyclerView();
        observeViewModel();
    }

    private void setupRecyclerView() {
        adapter = new GamificationMainAdapter(this, requireContext(), sharedViewModel.getLogger());
        recyclerViewMainContent.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewMainContent.setAdapter(adapter);
    }

    private void observeViewModel() {
        // Используем MediatorLiveData для отслеживания всех необходимых данных
        // и вызова notifyDataSetChangedHack() только один раз.
        MediatorLiveData<Object> updateTrigger = new MediatorLiveData<>();
        Observer<Object> observer = ignored -> adapter.notifyDataSetChangedHack();

        updateTrigger.addSource(sharedViewModel.isLoadingLiveData, isLoading -> {
            progressBarLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (!isLoading) updateTrigger.setValue(new Object()); // Триггер обновления, если загрузка закончилась
        });
        updateTrigger.addSource(sharedViewModel.errorMessageEvent, error -> { // Был errorMessageEvent
            textViewError.setVisibility(error != null ? View.VISIBLE : View.GONE);
            if (error != null) textViewError.setText(error);
            updateTrigger.setValue(new Object());
        });
        updateTrigger.addSource(sharedViewModel.gamificationState, updateTrigger::setValue);
        updateTrigger.addSource(sharedViewModel.selectedPlantState, updateTrigger::setValue);
        updateTrigger.addSource(sharedViewModel.plantHealthState, updateTrigger::setValue);
        updateTrigger.addSource(sharedViewModel.canWaterToday, updateTrigger::setValue);
        updateTrigger.addSource(sharedViewModel.surpriseTaskDetailsState, updateTrigger::setValue);
        updateTrigger.addSource(sharedViewModel.dailyRewardsInfo, updateTrigger::setValue);
        updateTrigger.addSource(sharedViewModel.challengesSectionState, updateTrigger::setValue);

        updateTrigger.observe(getViewLifecycleOwner(), observer);

        sharedViewModel.challengeToShowDetails.observe(getViewLifecycleOwner(), info -> {
            if (info != null && !isChallengeDetailsSheetShown) {
                ChallengeDetailsBottomSheetFragment.newInstance()
                        .show(getChildFragmentManager(), "ChallengeDetailsSheetGamificationMain");
                isChallengeDetailsSheetShown = true;
            } else if (info == null && isChallengeDetailsSheetShown) {
                Fragment existingSheet = getChildFragmentManager().findFragmentByTag("ChallengeDetailsSheetGamificationMain");
                if (existingSheet instanceof ChallengeDetailsBottomSheetFragment && existingSheet.isVisible()) {
                    ((ChallengeDetailsBottomSheetFragment) existingSheet).dismissAllowingStateLoss();
                }
                isChallengeDetailsSheetShown = false;
            }
        });
    }

    // --- Реализация GamificationItemClickListener ---
    @Override public void onPlantWidgetClick() { ((GamificationFragment) requireParentFragment()).navigateToGardenScreen(); }
    @Override public void onWaterPlantClick() { sharedViewModel.waterPlant(); }
    @Override public void onAcceptSurpriseTaskClick(SurpriseTask task) { sharedViewModel.acceptSurpriseTask(task); }
    @Override public void onHideSurpriseTaskClick(long taskId) { sharedViewModel.hideExpiredSurpriseTask(taskId); }
    @Override public void onClaimDailyRewardClick() { sharedViewModel.claimTodayReward(); }
    @Override public void onViewAllChallengesClick() { ((GamificationFragment) requireParentFragment()).navigateToAllChallengesScreen(); }
    @Override public void onChallengeCardClick(ChallengeCardInfo challengeInfo) { sharedViewModel.showChallengeDetails(challengeInfo); }
    @Override public void onDailyRewardItemClick(Reward reward, int rewardStreakDay, boolean isToday) {
        sharedViewModel.getLogger().debug("GamificationMainTab", "Daily Reward Item Clicked: " + reward.getName() + ", Day: " + rewardStreakDay);
        // Можно показать RewardInfoDialogFragment, если он реализован
    }

    public void onChallengeDetailsSheetDismissed() { // Вызывается из родительского GamificationFragment
        isChallengeDetailsSheetShown = false;
        sharedViewModel.clearChallengeDetails();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (recyclerViewMainContent != null) {
            recyclerViewMainContent.setAdapter(null);
        }
        adapter = null;
        recyclerViewMainContent = null;
        progressBarLoading = null;
        textViewError = null;
    }
}