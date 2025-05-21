package com.example.projectquestonjava.core.data.initializers;


public final class InitialDataConstants {
    private InitialDataConstants() {}

    public static final String DEFAULT_WORKSPACE_NAME = "Default Workspace";
    public static final String DEFAULT_WORKSPACE_DESCRIPTION = "Default";
    public static final String DEFAULT_APPROACH_NAME = "CALENDAR";
    // Если ApproachName.java готов:
    // import com.example.projectquestonjava.core.data.model.enums.ApproachName;
    // public static final ApproachName DEFAULT_APPROACH_NAME_ENUM = ApproachName.CALENDAR;
    public static final String DEFAULT_PLANT_TYPE = "SUNFLOWER"; // Имя enum как строка, если PlantType.java не готов
    // Если PlantType.java готов:
    // import com.example.projectquestonjava.feature.gamification.domain.model.PlantType;
    // public static final PlantType DEFAULT_PLANT_TYPE_ENUM = PlantType.SUNFLOWER;
    public static final int INITIAL_LEVEL = 1;
    public static final int INITIAL_XP = 0;
    public static final int MAX_XP_FOR_LEVEL = 100;
    public static final int INITIAL_COINS = 0;
    public static final int INITIAL_GROWTH_STAGE = 0;
}