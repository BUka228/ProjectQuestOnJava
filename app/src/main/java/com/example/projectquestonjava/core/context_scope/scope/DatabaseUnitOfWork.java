package com.example.projectquestonjava.core.context_scope.scope;

import com.example.projectquestonjava.core.data.database.AppDatabase;
import java.util.concurrent.Callable;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DatabaseUnitOfWork implements UnitOfWork {
    private final AppDatabase database;

    @Inject
    public DatabaseUnitOfWork(AppDatabase database) {
        this.database = database;
    }

    @Override
    public <T> T withTransaction(Callable<T> block) throws Exception {
        return database.runInTransaction(block);
    }

    @Override
    public void withTransaction(Runnable block) throws Exception {
        database.runInTransaction(() -> {
            block.run();
            return null;
        });
    }
}