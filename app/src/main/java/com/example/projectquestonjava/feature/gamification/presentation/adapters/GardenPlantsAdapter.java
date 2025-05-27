package com.example.projectquestonjava.feature.gamification.presentation.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
import com.example.projectquestonjava.core.utils.PlantResources;
import com.example.projectquestonjava.feature.gamification.data.model.VirtualGarden;
import com.example.projectquestonjava.feature.gamification.domain.model.PlantHealthState;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GardenPlantsAdapter extends ListAdapter<VirtualGarden, GardenPlantsAdapter.PlantViewHolder> {

    private final OnPlantClickListener listener;
    private long selectedPlantId;
    private Map<Long, PlantHealthState> healthStates;
    private final Context context; // Нужен для доступа к ресурсам

    public interface OnPlantClickListener {
        void onPlantClicked(VirtualGarden plant);
    }

    public GardenPlantsAdapter(@NonNull Context context,
                               @NonNull List<VirtualGarden> initialPlants,
                               long initialSelectedPlantId,
                               @NonNull Map<Long, PlantHealthState> initialHealthStates,
                               @NonNull OnPlantClickListener listener) {
        super(PLANT_DIFF_CALLBACK);
        this.context = context;
        this.listener = listener;
        this.selectedPlantId = initialSelectedPlantId;
        this.healthStates = initialHealthStates;
        submitList(initialPlants);
    }

    // Метод для обновления всех данных адаптера
    public void updateData(List<VirtualGarden> newPlants, long newSelectedPlantId, Map<Long, PlantHealthState> newHealthStates) {
        boolean listChanged = !Objects.equals(getCurrentList(), newPlants);
        boolean selectionChanged = this.selectedPlantId != newSelectedPlantId;
        boolean healthChanged = !Objects.equals(this.healthStates, newHealthStates);

        this.selectedPlantId = newSelectedPlantId;
        this.healthStates = newHealthStates != null ? newHealthStates : Collections.emptyMap();

        if (listChanged) {
            submitList(newPlants != null ? new ArrayList<>(newPlants) : Collections.emptyList());
        } else if (selectionChanged || healthChanged) {
            // Если список не изменился, но изменился выбор или состояние,
            // нужно перерисовать видимые элементы.
            // notifyDataSetChanged() - самый простой способ, но не самый эффективный.
            // Для оптимизации можно найти индексы измененных элементов и вызвать notifyItemChanged().
            notifyDataSetChanged();
        }
    }


    @NonNull
    @Override
    public PlantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_garden_plant, parent, false);
        return new PlantViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull PlantViewHolder holder, int position) {
        VirtualGarden plant = getItem(position);
        boolean isSelected = plant.getId() == selectedPlantId;
        PlantHealthState healthState = healthStates.getOrDefault(plant.getId(), PlantHealthState.HEALTHY);
        holder.bind(plant, isSelected, healthState, listener, context);
    }

    static class PlantViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView plantImageView;
        TextView stageTextView;
        View cardContentLayout; // LinearLayout или FrameLayout внутри MaterialCardView

        public PlantViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            // Предполагается, что item_garden_plant.xml имеет такую структуру:
            // MaterialCardView -> FrameLayout (или другой контейнер для aspect ratio) -> ImageView + CardView для значка стадии
            // Если у вас FrameLayout для aspectRatio имеет ID, используйте его.
            // Если нет, берем первый дочерний View у cardView, который является ViewGroup.
            if (cardView.getChildCount() > 0 && cardView.getChildAt(0) instanceof ViewGroup innerContainer) {
                plantImageView = innerContainer.findViewById(R.id.imageView_garden_plant_image);
                View badgeView = innerContainer.findViewById(R.id.card_plant_stage_badge);
                if (badgeView instanceof MaterialCardView) {
                    stageTextView = badgeView.findViewById(R.id.textView_plant_stage);
                }
            }
            // Запасной вариант, если структура другая (может не сработать для aspectRatio)
            if (plantImageView == null) plantImageView = itemView.findViewById(R.id.imageView_garden_plant_image);
            if (stageTextView == null) {
                View badgeContainer = itemView.findViewById(R.id.card_plant_stage_badge);
                if(badgeContainer != null) {
                    stageTextView = badgeContainer.findViewById(R.id.textView_plant_stage);
                }
            }
            cardContentLayout = itemView.findViewById(R.id.frameLayout_garden_plant_content);
        }

        void bind(final VirtualGarden plant, boolean isSelected, PlantHealthState healthState,
                  final OnPlantClickListener listener, Context context) {

            if (plantImageView == null || stageTextView == null || cardContentLayout == null) {
                // Логирование ошибки или пропуск, если View не найдены
                return;
            }

            int imageResId = PlantResources.getPlantImageResId(plant.getPlantType(), plant.getGrowthStage(), healthState);
            plantImageView.setImageResource(imageResId);
            stageTextView.setText(String.valueOf(plant.getGrowthStage()));

            // Анимация выделения (упрощенная)
            cardView.setElevation(isSelected ? dpToPx(10, context) : dpToPx(2, context));

            int healthBorderColorRes = switch (healthState) {
                case HEALTHY -> R.color.plant_healthy_border; // Зеленый
                case NEEDSWATER -> R.color.plant_needs_water_border; // Желтый
                default -> R.color.plant_wilted_border; // Красный
            };
            cardView.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, healthBorderColorRes)));
            cardView.setStrokeWidth(isSelected ? dpToPx(3, context) : dpToPx(2,context)); // Толще рамка у выбранного

            itemView.setOnClickListener(v -> listener.onPlantClicked(plant));

        }

        private int dpToPx(int dp, Context context) {
            return (int) (dp * context.getResources().getDisplayMetrics().density);
        }
    }

    private static final DiffUtil.ItemCallback<VirtualGarden> PLANT_DIFF_CALLBACK =
            new DiffUtil.ItemCallback<VirtualGarden>() {
                @Override
                public boolean areItemsTheSame(@NonNull VirtualGarden oldItem, @NonNull VirtualGarden newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull VirtualGarden oldItem, @NonNull VirtualGarden newItem) {
                    return oldItem.equals(newItem);
                }
            };
}