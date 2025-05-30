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
import com.example.projectquestonjava.core.utils.Logger; // Предполагаем, что Logger доступен
import com.example.projectquestonjava.feature.gamification.data.model.Badge;
import com.example.projectquestonjava.feature.gamification.data.model.GamificationBadgeCrossRef; // Импорт
import com.example.projectquestonjava.feature.gamification.presentation.adapters.BadgesAdapter;
import com.example.projectquestonjava.feature.gamification.presentation.dialogs.BadgeDetailsDialog;
import com.example.projectquestonjava.feature.gamification.presentation.viewmodels.GamificationViewModel;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.Collections;
import java.util.List; // Импорт

@AndroidEntryPoint
public class BadgesTabFragment extends Fragment implements BadgesAdapter.OnBadgeClickListener {

    private GamificationViewModel sharedViewModel;
    private RecyclerView recyclerViewBadges;
    private BadgesAdapter adapter;
    private TextView textViewNoBadges;
    private ProgressBar progressBarLoading;
    private Logger logger; // Для логгирования

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireParentFragment()).get(GamificationViewModel.class);
        logger = sharedViewModel.getLogger(); // Получаем логгер
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
        int spanCount = Math.max(2, screenWidthDp / 170);
        recyclerViewBadges.setLayoutManager(new GridLayoutManager(getContext(), spanCount));
        recyclerViewBadges.setAdapter(adapter);
    }

    private void observeViewModel() {
        // Наблюдаем за isLoading из GamificationViewModel
        sharedViewModel.isLoadingLiveData.observe(getViewLifecycleOwner(), isLoading -> {
            if (logger != null) logger.debug("BadgesTabFragment", "isLoadingLiveData observed: " + isLoading);
            progressBarLoading.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE);
        });

        // Комбинируем allBadgesState и earnedBadgesState для обновления UI
        // Этот подход предполагает, что оба LiveData эмиттят значения примерно одновременно
        // или что UI может обновиться несколько раз.
        sharedViewModel.allBadgesState.observe(getViewLifecycleOwner(), allBadges -> {
            List<GamificationBadgeCrossRef> earnedBadgesRefs = sharedViewModel.earnedBadgesState.getValue();
            updateBadgesUi(allBadges, earnedBadgesRefs);
        });

        sharedViewModel.earnedBadgesState.observe(getViewLifecycleOwner(), earnedBadgesRefs -> {
            List<Badge> allBadges = sharedViewModel.allBadgesState.getValue();
            updateBadgesUi(allBadges, earnedBadgesRefs);
        });
    }

    private void updateBadgesUi(List<Badge> allBadges, List<GamificationBadgeCrossRef> earnedBadgesRefs) {
        if (logger != null) {
            logger.debug("BadgesTabFragment", "updateBadgesUi called. allBadges: " + (allBadges == null ? "null" : allBadges.size()) +
                    ", earnedBadgesRefs: " + (earnedBadgesRefs == null ? "null" : earnedBadgesRefs.size()));
        }

        // Скрываем ProgressBar, так как данные (или их отсутствие) уже здесь
        if (progressBarLoading != null) progressBarLoading.setVisibility(View.GONE);

        if (allBadges == null || allBadges.isEmpty()) {
            if (textViewNoBadges != null) {
                textViewNoBadges.setText("Нет доступных значков");
                textViewNoBadges.setVisibility(View.VISIBLE);
            }
            if (recyclerViewBadges != null) recyclerViewBadges.setVisibility(View.GONE);
            if (adapter != null) adapter.submitList(Collections.emptyList(), Collections.emptyList()); // Очищаем адаптер
            if (logger != null) logger.debug("BadgesTabFragment", "No badges available in the system.");
        } else {
            if (recyclerViewBadges != null) recyclerViewBadges.setVisibility(View.VISIBLE);
            if (textViewNoBadges != null) textViewNoBadges.setVisibility(View.GONE); // Скрываем плейсхолдер, так как список значков будет показан
            if (adapter != null) adapter.submitList(allBadges, earnedBadgesRefs != null ? earnedBadgesRefs : Collections.emptyList());
            if (logger != null) logger.debug("BadgesTabFragment", "Displaying all badges. Earned count: " + (earnedBadgesRefs != null ? earnedBadgesRefs.size() : 0));
        }
    }


    @Override
    public void onBadgeClicked(Badge badge) {
        BadgeDetailsDialog.newInstance(badge).show(getChildFragmentManager(), "BadgeDetails");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Очистка ссылок, если это необходимо для предотвращения утечек с RecyclerView
        if (recyclerViewBadges != null) {
            recyclerViewBadges.setAdapter(null);
        }
        adapter = null;
    }
}