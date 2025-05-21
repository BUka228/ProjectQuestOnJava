package com.example.projectquestonjava.core.context_scope.scope;

import java.util.concurrent.Callable;

public interface UnitOfWork {
    <T> T withTransaction(Callable<T> block) throws Exception;

    void withTransaction(Runnable block) throws Exception;
}