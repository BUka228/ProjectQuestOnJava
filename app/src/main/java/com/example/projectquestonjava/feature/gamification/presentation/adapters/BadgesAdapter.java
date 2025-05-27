package com.example.projectquestonjava.feature.gamification.presentation.adapters;

import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.feature.gamification.data.model.Badge;
import com.example.projectquestonjava.feature.gamification.data.model.GamificationBadgeCrossRef;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class BadgesAdapter extends ListAdapter<Badge, BadgesAdapter.BadgeViewHolder> {

    private final OnBadgeClickListener listener;
    private Set<Long> earnedBadgeIds = new HashSet<>(); // Храним ID заработанных значков

    public interface OnBadgeClickListener {
        void onBadgeClicked(Badge badge);
    }

    public BadgesAdapter(@NonNull OnBadgeClickListener listener) {
        super(BADGE_DIFF_CALLBACK);
        this.listener = listener;
    }

    /**
     * Обновляет список всех значков и информацию о заработанных.
     * @param allBadges Список всех доступных определений значков.
     * @param earnedBadgesRefs Список связей, указывающих, какие значки заработаны.
     */
    public void submitList(List<Badge> allBadges, List<GamificationBadgeCrossRef> earnedBadgesRefs) {
        if (earnedBadgesRefs != null) {
            this.earnedBadgeIds = earnedBadgesRefs.stream()
                    .map(GamificationBadgeCrossRef::getBadgeId)
                    .collect(Collectors.toSet());
        } else {
            this.earnedBadgeIds = new HashSet<>();
        }
        // Передаем в ListAdapter только список всех значков.
        // Информация о том, заработан ли значок, будет проверяться в onBindViewHolder.
        super.submitList(allBadges != null ? new ArrayList<>(allBadges) : Collections.emptyList());
    }


    @NonNull
    @Override
    public BadgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_badge_profile, parent, false); // Используем макет из ProfileFragment
        return new BadgeViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull BadgeViewHolder holder, int position) {
        Badge badge = getItem(position);
        boolean isEarned = earnedBadgeIds.contains(badge.id);
        holder.bind(badge, isEarned, listener);
    }

    static class BadgeViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView badgeIconView;
        ImageView checkMarkView;
        TextView badgeNameView;
        TextView badgeDescriptionView;

        public BadgeViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView; // Корневой элемент
            badgeIconView = itemView.findViewById(R.id.imageView_badge_icon_profile);
            checkMarkView = itemView.findViewById(R.id.imageView_badge_check_profile);
            badgeNameView = itemView.findViewById(R.id.textView_badge_name_profile);
            badgeDescriptionView = itemView.findViewById(R.id.textView_badge_description_profile);
        }

        void bind(final Badge badge, boolean isEarned, final OnBadgeClickListener listener) {
            Context context = itemView.getContext();

            badgeNameView.setText(badge.getName());
            badgeDescriptionView.setText(badge.getDescription());

            if (badge.getImageUrl() > 0) { // imageUrl это @DrawableRes
                badgeIconView.setImageResource(badge.getImageUrl());
            } else {
                badgeIconView.setImageResource(R.drawable.star); // Заглушка по умолчанию
            }

            if (isEarned) {
                cardView.setAlpha(1f);
                // Убираем серость с иконки, если она была установлена
                badgeIconView.setColorFilter(null);
                checkMarkView.setVisibility(View.VISIBLE);
                checkMarkView.setColorFilter(ContextCompat.getColor(context, R.color.secondaryLight), PorterDuff.Mode.SRC_IN);

                cardView.setCardElevation(context.getResources().getDimensionPixelSize(R.dimen.card_elevation_highlighted)); // Немного больше тень
                cardView.setStrokeColor(ContextCompat.getColor(context, R.color.secondaryLight));
                cardView.setStrokeWidth(context.getResources().getDimensionPixelSize(R.dimen.badge_earned_stroke_width));
            } else {
                cardView.setAlpha(0.65f); // Делаем не заработанные значки полупрозрачными
                // Делаем иконку серой
                badgeIconView.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(context, R.color.onSurfaceVariantLight), PorterDuff.Mode.SRC_IN));
                checkMarkView.setVisibility(View.GONE);
                cardView.setCardElevation(context.getResources().getDimensionPixelSize(R.dimen.card_elevation_default));
                cardView.setStrokeWidth(0); // Убираем рамку
            }

            itemView.setOnClickListener(v -> listener.onBadgeClicked(badge));
        }
    }

    private static final DiffUtil.ItemCallback<Badge> BADGE_DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Badge>() {
                @Override
                public boolean areItemsTheSame(@NonNull Badge oldItem, @NonNull Badge newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull Badge oldItem, @NonNull Badge newItem) {
                    // Сравниваем также и imageUrl, т.к. он влияет на отображение
                    return oldItem.getId() == newItem.getId() &&
                            Objects.equals(oldItem.getName(), newItem.getName()) &&
                            Objects.equals(oldItem.getDescription(), newItem.getDescription()) &&
                            oldItem.getImageUrl() == newItem.getImageUrl() && // imageUrl теперь int
                            Objects.equals(oldItem.getCriteria(), newItem.getCriteria());
                }
            };
}