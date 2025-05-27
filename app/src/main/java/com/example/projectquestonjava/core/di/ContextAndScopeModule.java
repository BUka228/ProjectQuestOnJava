package com.example.projectquestonjava.core.di;

import com.example.projectquestonjava.core.context_scope.context.WorkspaceContext;
import com.example.projectquestonjava.core.context_scope.context.WorkspaceContextImpl;
import com.example.projectquestonjava.core.context_scope.scope.DatabaseUnitOfWork;
import com.example.projectquestonjava.core.context_scope.scope.UnitOfWork;
import com.example.projectquestonjava.core.context_scope.scope.WorkspaceUnitOfWork;
import com.example.projectquestonjava.core.context_scope.scope.WorkspaceUnitOfWorkImpl;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton; // Добавлен импорт, если нужен для @Binds @Singleton

@Module
@InstallIn(SingletonComponent.class)
public abstract class ContextAndScopeModule {
    @Binds @Singleton
    public abstract WorkspaceContext bindWorkspaceContext(WorkspaceContextImpl impl);

    @Binds @Singleton
    public abstract UnitOfWork bindUnitOfWork(DatabaseUnitOfWork impl);

    @Binds @Singleton
    public abstract WorkspaceUnitOfWork bindWorkspaceUnitOfWork(WorkspaceUnitOfWorkImpl impl);
}