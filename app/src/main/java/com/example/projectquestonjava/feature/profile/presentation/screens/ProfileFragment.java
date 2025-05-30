package com.example.projectquestonjava.feature.profile.presentation.screens;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import coil.ImageLoader;
import coil.request.ImageRequest;
import coil.transform.CircleCropTransformation;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.core.data.model.core.UserAuth;
import com.example.projectquestonjava.core.ui.BaseFragment;
import com.example.projectquestonjava.feature.gamification.data.model.Badge;
import com.example.projectquestonjava.feature.gamification.data.model.Gamification;
import com.example.projectquestonjava.feature.gamification.presentation.dialogs.BadgeDetailsDialog;
import com.example.projectquestonjava.feature.profile.presentation.adapters.ActivityFeedAdapter;
import com.example.projectquestonjava.feature.profile.presentation.adapters.RecentBadgesAdapter;
import com.example.projectquestonjava.feature.profile.presentation.viewmodels.ProfileViewModel;
import com.example.projectquestonjava.utils.dialogs.LogoutConfirmationDialog;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import javax.inject.Inject;
import coil.Coil;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ProfileFragment extends BaseFragment implements RecentBadgesAdapter.OnProfileBadgeClickListener {

    private ProfileViewModel viewModel;

    private ImageView imageViewAvatar;
    private TextView textViewUsername, textViewEmail, textViewLevelValue, textViewXpCurrentMax, textViewXpToNext;
    private ProgressBar progressBarXp, progressBarLoading;

    private TextView textViewCoinsValue, textViewCoinsLabel;
    private TextView textViewStreakValue, textViewStreakDays, textViewStreakSubValue;
    private ImageView imageViewCoinsIcon, imageViewStreakIcon;
    private LinearLayout infoColumnCoins, infoColumnStreak;


    private RecyclerView recyclerViewRecentBadges;
    private RecentBadgesAdapter recentBadgesAdapter;
    private TextView textViewNoRecentBadges;
    private Button buttonAllBadges;

    private RecyclerView recyclerViewActivityFeed;
    private ActivityFeedAdapter activityFeedAdapter;
    private TextView textViewNoActivity;

    private MaterialCardView quickAccessGardenCard, quickAccessStatisticsCard, quickAccessSettingsCard;
    private TextView gardenTitle, statisticsTitle, settingsTitle;
    private ImageView gardenIcon, statisticsIcon, settingsIcon;

    private Button buttonLogout;

    @Inject
    ImageLoader imageLoader;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.profile_toolbar_menu, menu);
            }
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int itemId = menuItem.getItemId();
                if (itemId == R.id.action_edit_profile) {
                    NavHostFragment.findNavController(ProfileFragment.this).navigate(R.id.action_navigation_profile_to_profileEditFragment);
                    return true;
                } else if (itemId == R.id.action_statistics_profile) {
                    NavHostFragment.findNavController(ProfileFragment.this).navigate(R.id.action_navigation_profile_to_statisticsFragment);
                    return true;
                } else if (itemId == R.id.action_settings_profile) {
                    NavHostFragment.findNavController(ProfileFragment.this).navigate(R.id.action_navigation_profile_to_settingsFragment);
                    return true;
                } else if (itemId == R.id.action_logout_profile) {
                    new LogoutConfirmationDialog(() -> {
                        if (viewModel != null) viewModel.logout();
                    }).show(getChildFragmentManager(), "LogoutConfirmDialogTag_Profile"); // Добавил уникальный тег
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        bindViews(view);
        setupRecyclerViews();
        setupClickListeners();
        observeViewModel();
    }

    private void bindViews(View view) {
        imageViewAvatar = view.findViewById(R.id.imageView_profile_avatar_main);
        textViewUsername = view.findViewById(R.id.textView_profile_username_main);
        textViewEmail = view.findViewById(R.id.textView_profile_email_main);
        textViewLevelValue = view.findViewById(R.id.textView_profile_level_value_main);
        progressBarXp = view.findViewById(R.id.progressBar_profile_xp_main);
        textViewXpCurrentMax = view.findViewById(R.id.textView_profile_xp_current_max_main);
        textViewXpToNext = view.findViewById(R.id.textView_profile_xp_to_next_main);
        progressBarLoading = view.findViewById(R.id.progressBar_profile_main_loading);

        infoColumnCoins = view.findViewById(R.id.info_column_coins_profile);
        imageViewCoinsIcon = infoColumnCoins.findViewById(R.id.imageView_info_icon);
        textViewCoinsValue = infoColumnCoins.findViewById(R.id.textView_info_value);
        textViewCoinsLabel = infoColumnCoins.findViewById(R.id.textView_info_label);

        infoColumnStreak = view.findViewById(R.id.info_column_streak_profile);
        imageViewStreakIcon = infoColumnStreak.findViewById(R.id.imageView_info_icon);
        textViewStreakValue = infoColumnStreak.findViewById(R.id.textView_info_value);
        textViewStreakSubValue = infoColumnStreak.findViewById(R.id.textView_info_subvalue);
        textViewStreakDays = infoColumnStreak.findViewById(R.id.textView_info_label);

        recyclerViewRecentBadges = view.findViewById(R.id.recyclerView_recent_badges_profile);
        textViewNoRecentBadges = view.findViewById(R.id.textView_no_recent_badges_profile);
        buttonAllBadges = view.findViewById(R.id.button_all_badges_profile);

        recyclerViewActivityFeed = view.findViewById(R.id.recyclerView_activity_feed);
        textViewNoActivity = view.findViewById(R.id.textView_no_activity_profile);

        quickAccessGardenCard = view.findViewById(R.id.quick_access_garden_profile);
        quickAccessStatisticsCard = view.findViewById(R.id.quick_access_statistics_profile);
        quickAccessSettingsCard = view.findViewById(R.id.quick_access_settings_profile);
        buttonLogout = view.findViewById(R.id.button_logout_profile);

        // Настраиваем контент для каждого элемента быстрого доступа
        if (quickAccessGardenCard != null) {
            gardenIcon = quickAccessGardenCard.findViewById(R.id.imageView_quick_access_icon);
            gardenTitle = quickAccessGardenCard.findViewById(R.id.textView_quick_access_title);
            if(gardenIcon != null) gardenIcon.setImageResource(R.drawable.grass);
            if(gardenTitle != null) gardenTitle.setText("Мой Сад");
        }
        if (quickAccessStatisticsCard != null) {
            statisticsIcon = quickAccessStatisticsCard.findViewById(R.id.imageView_quick_access_icon);
            statisticsTitle = quickAccessStatisticsCard.findViewById(R.id.textView_quick_access_title);
            if(statisticsIcon != null) statisticsIcon.setImageResource(R.drawable.trending_up);
            if(statisticsTitle != null) statisticsTitle.setText("Статистика");
        }
        if (quickAccessSettingsCard != null) {
            settingsIcon = quickAccessSettingsCard.findViewById(R.id.imageView_quick_access_icon);
            settingsTitle = quickAccessSettingsCard.findViewById(R.id.textView_quick_access_title);
            if(settingsIcon != null) settingsIcon.setImageResource(R.drawable.settings);
            if(settingsTitle != null) settingsTitle.setText("Настройки");
        }
    }

    private void setupRecyclerViews() {
        recentBadgesAdapter = new RecentBadgesAdapter(this);
        recyclerViewRecentBadges.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerViewRecentBadges.setAdapter(recentBadgesAdapter);

        activityFeedAdapter = new ActivityFeedAdapter();
        recyclerViewActivityFeed.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewActivityFeed.setAdapter(activityFeedAdapter);
        recyclerViewActivityFeed.setNestedScrollingEnabled(false);
    }

    private void setupClickListeners() {
        View avatarClickableArea = getView().findViewById(R.id.frame_avatar_clickable_profile);
        if (avatarClickableArea != null) {
            avatarClickableArea.setOnClickListener(v ->
                    NavHostFragment.findNavController(this).navigate(R.id.action_navigation_profile_to_profileEditFragment)
            );
        }

        if (quickAccessGardenCard != null) {
            quickAccessGardenCard.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.action_navigation_profile_to_gardenFragment));
        }
        if (quickAccessStatisticsCard != null) {
            quickAccessStatisticsCard.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.action_navigation_profile_to_statisticsFragment));
        }
        if (quickAccessSettingsCard != null) {
            quickAccessSettingsCard.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.action_navigation_profile_to_settingsFragment));
        }

        buttonLogout.setOnClickListener(v -> {
            new LogoutConfirmationDialog(() -> {
                if (viewModel != null) viewModel.logout();
            }).show(getChildFragmentManager(), "LogoutConfirmationDialogTag_Profile");
        });

        buttonAllBadges.setOnClickListener(v -> {
            Snackbar.make(requireView(), "Экран всех значков в разработке", Snackbar.LENGTH_SHORT).show();
        });
    }

    private void observeViewModel() {
        viewModel.uiStateLiveData.observe(getViewLifecycleOwner(), uiState -> {
            if (uiState == null) return;

            progressBarLoading.setVisibility(uiState.isLoading() ? View.VISIBLE : View.GONE);

            if (uiState.getError() != null) {
                Snackbar.make(requireView(), uiState.getError(), Snackbar.LENGTH_LONG).show();
                viewModel.clearError();
            }

            UserAuth user = uiState.getUser();
            if (user != null) {
                textViewUsername.setText(user.getUsername());
                textViewEmail.setText(user.getEmail());
                ImageRequest.Builder requestBuilder = new ImageRequest.Builder(requireContext())
                        .placeholder(R.drawable.person)
                        .error(R.drawable.person);
                if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                    requestBuilder.data(user.getAvatarUrl());
                } else {
                    requestBuilder.data(R.drawable.person); // Явный drawable, если URL пуст
                }
                imageLoader.enqueue(requestBuilder.target(imageViewAvatar).build());
            } else {
                textViewUsername.setText("Пользователь");
                textViewEmail.setText("");
                imageViewAvatar.setImageResource(R.drawable.person);
            }

            Gamification gamification = uiState.getGamification();
            if (gamification != null) {
                textViewLevelValue.setText(String.valueOf(gamification.getLevel()));
                if (gamification.getMaxExperienceForLevel() > 0) {
                    progressBarXp.setProgress((int) ((float) gamification.getExperience() / gamification.getMaxExperienceForLevel() * 100));
                    textViewXpCurrentMax.setText(String.format(Locale.getDefault(), "%d / %d XP", gamification.getExperience(), gamification.getMaxExperienceForLevel()));
                    int xpToNext = Math.max(0, gamification.getMaxExperienceForLevel() - gamification.getExperience());
                    textViewXpToNext.setText(xpToNext > 0 ? "Еще " + xpToNext + " XP" : "Макс. уровень");
                    textViewXpToNext.setVisibility(xpToNext > 0 ? View.VISIBLE : View.INVISIBLE);
                } else {
                    progressBarXp.setProgress(0);
                    textViewXpCurrentMax.setText(String.format(Locale.getDefault(), "%d / %d XP", gamification.getExperience(), 0));
                    textViewXpToNext.setText("");
                    textViewXpToNext.setVisibility(View.VISIBLE); // Показываем пустую строку, чтобы не было скачка
                }
                imageViewCoinsIcon.setImageResource(R.drawable.paid);
                ImageViewCompat.setImageTintList(imageViewCoinsIcon, ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.coins_color)));
                textViewCoinsValue.setText(String.valueOf(gamification.getCoins()));
                textViewCoinsLabel.setText("Монеты");
                infoColumnCoins.findViewById(R.id.textView_info_subvalue).setVisibility(View.GONE);

                imageViewStreakIcon.setImageResource(R.drawable.local_fire_department);
                ImageViewCompat.setImageTintList(imageViewStreakIcon, ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.streak_color)));
                textViewStreakValue.setText(String.valueOf(gamification.getCurrentStreak()));
                textViewStreakDays.setText(viewModel.getDaysString(gamification.getCurrentStreak())); // Используем метод из ViewModel
                TextView subValueStreak = infoColumnStreak.findViewById(R.id.textView_info_subvalue);
                if (gamification.getMaxStreak() > 0) {
                    subValueStreak.setText(String.format(Locale.getDefault(), "(макс. %d)", gamification.getMaxStreak()));
                    subValueStreak.setVisibility(View.VISIBLE);
                } else {
                    subValueStreak.setVisibility(View.GONE);
                }

            } else {
                textViewLevelValue.setText("-");
                progressBarXp.setProgress(0);
                textViewXpCurrentMax.setText("- / - XP");
                textViewXpToNext.setText("");
                textViewCoinsValue.setText("-");
                textViewStreakValue.setText("-");
                textViewStreakDays.setText("дней");
                infoColumnCoins.findViewById(R.id.textView_info_subvalue).setVisibility(View.GONE);
                infoColumnStreak.findViewById(R.id.textView_info_subvalue).setVisibility(View.GONE);
            }

            if (uiState.getRecentBadges().isEmpty() && uiState.getEarnedBadgesCount() == 0) {
                textViewNoRecentBadges.setVisibility(View.VISIBLE);
                recyclerViewRecentBadges.setVisibility(View.GONE);
                buttonAllBadges.setVisibility(View.GONE);
            } else {
                textViewNoRecentBadges.setVisibility(View.GONE);
                recyclerViewRecentBadges.setVisibility(View.VISIBLE);
                recentBadgesAdapter.submitList(uiState.getRecentBadges());
                if (uiState.getEarnedBadgesCount() > 0) {
                    buttonAllBadges.setText(String.format(Locale.getDefault(), "Все (%d)", uiState.getEarnedBadgesCount()));
                    buttonAllBadges.setVisibility(View.VISIBLE);
                } else {
                    buttonAllBadges.setVisibility(View.GONE);
                }
            }

            if (uiState.getRecentHistory().isEmpty()) {
                textViewNoActivity.setVisibility(View.VISIBLE);
                recyclerViewActivityFeed.setVisibility(View.GONE);
            } else {
                textViewNoActivity.setVisibility(View.GONE);
                recyclerViewActivityFeed.setVisibility(View.VISIBLE);
                activityFeedAdapter.submitList(uiState.getRecentHistory());
            }
        });
    }

    @Override
    protected void setupToolbar() {
        MaterialToolbar toolbar = getToolbar();
        if (toolbar != null) {
            toolbar.setTitle("Профиль");
            // MenuProvider уже добавлен в onCreateView
        }
    }

    @Override
    protected void setupFab() {
        if (getExtendedFab() != null) getExtendedFab().hide();
        if (getStandardFab() != null) getStandardFab().hide();
    }

    @Override
    public void onProfileBadgeClicked(Badge badge) {
        BadgeDetailsDialog.newInstance(badge).show(getChildFragmentManager(), "ProfileBadgeDetailsDialogTag");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Очистка ссылок на View, чтобы избежать утечек памяти
        imageViewAvatar = null;
        textViewUsername = null;
        textViewEmail = null;
        textViewLevelValue = null;
        progressBarXp = null;
        textViewXpCurrentMax = null;
        textViewXpToNext = null;
        progressBarLoading = null;
        textViewCoinsValue = null;
        textViewCoinsLabel = null;
        textViewStreakValue = null;
        textViewStreakDays = null;
        textViewStreakSubValue = null;
        imageViewCoinsIcon = null;
        imageViewStreakIcon = null;
        infoColumnCoins = null;
        infoColumnStreak = null;
        if (recyclerViewRecentBadges != null) {
            recyclerViewRecentBadges.setAdapter(null);
        }
        recyclerViewRecentBadges = null;
        recentBadgesAdapter = null;
        textViewNoRecentBadges = null;
        buttonAllBadges = null;
        if (recyclerViewActivityFeed != null) {
            recyclerViewActivityFeed.setAdapter(null);
        }
        recyclerViewActivityFeed = null;
        activityFeedAdapter = null;
        textViewNoActivity = null;
        quickAccessGardenCard = null;
        quickAccessStatisticsCard = null;
        quickAccessSettingsCard = null;
        gardenTitle = null; statisticsTitle = null; settingsTitle = null;
        gardenIcon = null; statisticsIcon = null; settingsIcon = null;
        buttonLogout = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refreshData(); // Нужен такой метод в ProfileViewModel
        }
    }
}