package com.example.projectquestonjava.core.domain.repository;

import com.example.projectquestonjava.core.data.model.core.UserAuth;
import com.google.common.util.concurrent.ListenableFuture; // Используем ListenableFuture

// Result<T> заменяется на ListenableFuture<T> или прямой тип с обработкой исключений
// в реализации репозитория.
// Если метод может вернуть null, это отражается в типе ListenableFuture<UserAuth>.
// Если метод должен вернуть void, это ListenableFuture<Void>.

public interface UserAuthRepository {
    ListenableFuture<UserAuth> getUserById(int userId); // Может вернуть null, если пользователя нет
    ListenableFuture<Void> updateUsername(int userId, String newName);
    ListenableFuture<Void> updateUser(UserAuth user);
    ListenableFuture<Void> updateAvatarUrl(int userId, String avatarPath);
    ListenableFuture<Void> updatePasswordHash(int userId, String newHash);
}