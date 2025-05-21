package com.example.projectquestonjava.approach.calendar.di;

import com.example.projectquestonjava.core.data.database.AppDatabase;
import com.example.projectquestonjava.approach.calendar.data.dao.CalendarTaskDao;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class CalendarRoomModule {
    @Provides
    public static CalendarTaskDao provideCalendarTaskDao(AppDatabase appDatabase) {
        return appDatabase.calendarTaskDao();
    }
}