package com.example.projectquestonjava.core.di;

import com.example.projectquestonjava.core.context_scope.scope.DatabaseUnitOfWork;
import com.example.projectquestonjava.core.context_scope.scope.UnitOfWork;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public abstract class ContextAndScopeModule {
    @Binds
    public abstract WorkspaceContext bindWorkspaceContext(WorkspaceContextImpl impl);

    @Binds
    public abstract UnitOfWork bindUnitOfWork(DatabaseUnitOfWork impl);

    @Binds
    public abstract WorkspaceUnitOfWork bindWorkspaceUnitOfWork(WorkspaceUnitOfWorkImpl impl);
}