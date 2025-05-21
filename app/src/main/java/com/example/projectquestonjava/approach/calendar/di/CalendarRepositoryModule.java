package com.example.projectquestonjava.approach.calendar.di;

import com.example.projectquestonjava.approach.calendar.data.repositories.CalendarParamsRepositoryImpl;
import com.example.projectquestonjava.approach.calendar.data.repositories.CalendarRepositoryImpl;
import com.example.projectquestonjava.approach.calendar.domain.repository.CalendarParamsRepository;
import com.example.projectquestonjava.approach.calendar.domain.repository.CalendarRepository;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public abstract class CalendarRepositoryModule {

    @Binds
    @Singleton
    public abstract CalendarRepository bindCalendarRepository(CalendarRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract CalendarParamsRepository bindCalendarParamsRepository(CalendarParamsRepositoryImpl impl);
}