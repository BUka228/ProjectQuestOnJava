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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.feature.gamification.data.model.Badge;
import com.example.projectquestonjava.feature.gamification.presentation.adapters.BadgesAdapter;
import com.example.projectquestonjava.feature.gamification.presentation.dialogs.BadgeDetailsDialog;
import com.example.projectquestonjava.feature.gamification.presentation.viewmodels.GamificationViewModel;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.Collections;

@AndroidEntryPoint
public class BadgesTabFragment extends Fragment implements BadgesAdapter.OnBadgeClickListener {

    private GamificationViewModel sharedViewModel;
    private RecyclerView recyclerViewBadges;
    private BadgesAdapter adapter;
    private TextView textViewNoBadges;
    private ProgressBar progressBarLoading;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireParentFragment()).get(GamificationViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_badges_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerViewBadges = view.findViewById(R.id.recyclerView_badges);
        textViewNoBadges = view.findViewById(R.id.textView_no_badges_placeholder);
        progressBarLoading = view.findViewById(R.id.progressBar_badges_loading);

        setupRecyclerView();
        observeViewModel();
    }

    private void setupRecyclerView() {
        adapter = new BadgesAdapter(this);
        int screenWidthDp = getResources().getConfiguration().screenWidthDp;
        int spanCount = Math.max(2, screenWidthDp / 170); // Примерно 170dp на элемент
        recyclerViewBadges.setLayoutManager(new GridLayoutManager(getContext(), spanCount));
        recyclerViewBadges.setAdapter(adapter);
    }

    private void observeViewModel() {
        // Наблюдаем за isLoading из GamificationViewModel (если он есть для значков)
        // sharedViewModel.isLoadingBadges.observe(getViewLifecycleOwner(), isLoading -> {
        //     progressBarLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        // });

        sharedViewModel.allBadgesState.observe(getViewLifecycleOwner(), allBadges -> {
            sharedViewModel.earnedBadgesState.observe(getViewLifecycleOwner(), earnedBadgesRefs -> {
                progressBarLoading.setVisibility(View.GONE); // Скрываем загрузку после получения данных
                if (allBadges == null || allBadges.isEmpty()) {
                    textViewNoBadges.setText("Нет доступных значков");
                    textViewNoBadges.setVisibility(View.VISIBLE);
                    recyclerViewBadges.setVisibility(View.GONE);
                } else {
                    adapter.submitList(allBadges, earnedBadgesRefs);
                    boolean hasAnyEarned = earnedBadgesRefs != null && !earnedBadgesRefs.isEmpty();
                    // Показываем "Значки еще не заработаны", если список всех значков не пуст, но заработанных нет
                    textViewNoBadges.setText(hasAnyEarned || allBadges.isEmpty() ? "Нет доступных значков" : "Значки еще не заработаны");
                    textViewNoBadges.setVisibility(allBadges.isEmpty() || hasAnyEarned ? View.GONE : View.VISIBLE);
                    recyclerViewBadges.setVisibility(allBadges.isEmpty() ? View.GONE : View.VISIBLE);

                }
            });
        });
    }

    @Override
    public void onBadgeClicked(Badge badge) {
        BadgeDetailsDialog.newInstance(badge).show(getChildFragmentManager(), "BadgeDetails");
    }
}