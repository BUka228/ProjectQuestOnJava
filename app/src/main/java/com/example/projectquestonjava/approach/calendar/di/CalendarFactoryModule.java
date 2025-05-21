package com.example.projectquestonjava.approach.calendar.di;

import com.example.projectquestonjava.approach.calendar.data.factories.CalendarParamsFactoryImpl;
import com.example.projectquestonjava.approach.calendar.domain.factories.CalendarParamsFactory;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public abstract class CalendarFactoryModule {
    @Binds
    public abstract CalendarParamsFactory bindCalendarParamsFactory(CalendarParamsFactoryImpl impl);
}