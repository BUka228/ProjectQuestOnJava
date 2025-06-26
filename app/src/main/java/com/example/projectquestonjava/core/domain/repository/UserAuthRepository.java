package com.example.projectquestonjava.core.domain.repository;

import com.example.projectquestonjava.core.data.model.core.UserAuth;
import com.google.common.util.concurrent.ListenableFuture;


public interface UserAuthRepository {
    ListenableFuture<UserAuth> getUserById(int userId);
    ListenableFuture<Void> updateUsername(int userId, String newName);
    ListenableFuture<Void> updateUser(UserAuth user);
    ListenableFuture<Void> updateAvatarUrl(int userId, String avatarPath);
    ListenableFuture<Void> updatePasswordHash(int userId, String newHash);
}