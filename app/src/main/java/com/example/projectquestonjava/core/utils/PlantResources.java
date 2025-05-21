package com.example.projectquestonjava.core.utils;

import com.example.projectquestonjava.R;
import com.example.projectquestonjava.feature.gamification.domain.model.PlantHealthState;
import com.example.projectquestonjava.feature.gamification.domain.model.PlantType;
import java.util.HashMap;
import java.util.Map;

public final class PlantResources {
    private PlantResources() {}

    private static final Map<String, Integer> plantImageMap = new HashMap<>();
    private static final int defaultImage = R.drawable.sunflower_0_healthy; // Или другое дефолтное

    static {
        // Sunflower
        plantImageMap.put("sunflower_0_healthy", R.drawable.sunflower_0_healthy);
        plantImageMap.put("sunflower_1_healthy", R.drawable.sunflower_1_healthy);
        plantImageMap.put("sunflower_1_needswater", R.drawable.sunflower_1_healthy); // Заглушка
        plantImageMap.put("sunflower_1_wilted", R.drawable.sunflower);          // Заглушка
        plantImageMap.put("sunflower_2_healthy", R.drawable.sunflower_2_healthy);
        plantImageMap.put("sunflower_2_needswater", R.drawable.sunflower);          // Заглушка
        plantImageMap.put("sunflower_2_wilted", R.drawable.sunflower);          // Заглушка
        plantImageMap.put("sunflower_9_healthy", R.drawable.sunflower);           // Заглушка
        plantImageMap.put("sunflower_9_needswater", R.drawable.sunflower);       // Заглушка
        plantImageMap.put("sunflower_9_wilted", R.drawable.sunflower);           // Заглушка

        // Rose Example
        plantImageMap.put("rose_5_healthy", R.drawable.sunflower); // Заменить на R.drawable.plant_rose_5_healthy
        plantImageMap.put("rose_5_needswater", R.drawable.sunflower); // Заменить на R.drawable.plant_rose_5_needswater
        plantImageMap.put("rose_5_wilted", R.drawable.sunflower); // Заменить на R.drawable.plant_rose_5_wilted
        // ... Добавьте остальные ресурсы
    }

    public static int getPlantImageResId(
            PlantType plantType,
            int growthStage,
            PlantHealthState healthState
    ) {
        if (plantType == PlantType.NONE) return defaultImage;

        String typeString = plantType.name().toLowerCase();
        String healthString = healthState.name().toLowerCase();
        String key = typeString + "_" + growthStage + "_" + healthString;

        Integer resId = plantImageMap.get(key);
        return resId != null ? resId : defaultImage;
    }
}