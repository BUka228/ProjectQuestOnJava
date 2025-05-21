package com.example.projectquestonjava.feature.pomodoro.di;

import com.example.projectquestonjava.feature.pomodoro.data.repository.PomodoroSessionRepositoryImpl;
import com.example.projectquestonjava.feature.pomodoro.data.repository.PomodoroSettingsRepositoryImpl;
import com.example.projectquestonjava.feature.pomodoro.domain.repository.PomodoroSessionRepository;
import com.example.projectquestonjava.feature.pomodoro.domain.repository.PomodoroSettingsRepository;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public abstract class PomodoroRepositoryModule {

    @Binds
    @Singleton
    public abstract PomodoroSessionRepository bindPomodoroSessionRepository(PomodoroSessionRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract PomodoroSettingsRepository bindPomodoroSettingsRepository(PomodoroSettingsRepositoryImpl impl);
}