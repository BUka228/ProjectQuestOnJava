package com.example.projectquestonjava.core.di;

import com.example.projectquestonjava.core.data.security.BCryptPasswordHasher;
import com.example.projectquestonjava.core.domain.security.PasswordHasher;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public abstract class HashingModule {

    @Binds
    @Singleton
    public abstract PasswordHasher bindPasswordHasher(BCryptPasswordHasher impl);
}