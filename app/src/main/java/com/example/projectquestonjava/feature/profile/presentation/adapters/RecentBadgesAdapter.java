package com.example.projectquestonjava.feature.profile.presentation.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projectquestonjava.R;
import com.example.projectquestonjava.feature.gamification.data.model.Badge;

public class RecentBadgesAdapter extends ListAdapter<Badge, RecentBadgesAdapter.BadgeViewHolder> {

    private final OnProfileBadgeClickListener listener;

    public interface OnProfileBadgeClickListener {
        void onProfileBadgeClicked(Badge badge);
    }

    public RecentBadgesAdapter(@NonNull OnProfileBadgeClickListener listener) {
        super(BADGE_DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public BadgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_small_badge, parent, false); // Используем item_small_badge
        return new BadgeViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull BadgeViewHolder holder, int position) {
        Badge badge = getItem(position);
        holder.bind(badge, listener);
    }

    static class BadgeViewHolder extends RecyclerView.ViewHolder {
        FrameLayout itemLayout; // Корневой элемент из item_small_badge.xml
        ImageView badgeIcon;

        public BadgeViewHolder(@NonNull View itemView) {
            super(itemView);
            itemLayout = (FrameLayout) itemView;
            badgeIcon = itemView.findViewById(R.id.imageView_badge_icon); // ID из item_small_badge.xml
        }

        void bind(final Badge badge, final OnProfileBadgeClickListener listener) {
            Context context = itemView.getContext();
            if (badge.getImageUrl() > 0) { // imageUrl теперь int (ресурс)
                badgeIcon.setImageResource(badge.getImageUrl());
            } else {
                badgeIcon.setImageResource(R.drawable.star); // Заглушка
            }
            // Тултип
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                itemView.setTooltipText(badge.getName());
            } else {
                // Для старых версий можно использовать Toast или кастомный Tooltip
                itemView.setOnLongClickListener(v -> {
                    android.widget.Toast.makeText(context, badge.getName(), android.widget.Toast.LENGTH_SHORT).show();
                    return true;
                });
            }

            itemView.setOnClickListener(v -> listener.onProfileBadgeClicked(badge));
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
                    return oldItem.equals(newItem);
                }
            };
}