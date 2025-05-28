package com.example.projectquestonjava.core.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.projectquestonjava.core.data.model.core.UserAuth;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;

@Dao
public interface UserAuthDao {
    // --- ASYNC ---
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    ListenableFuture<UserAuth> getUserByEmail(String email);

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    ListenableFuture<UserAuth> getUserById(int userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Long> insertUser(UserAuth user);

    @Query("SELECT * FROM users")
    ListenableFuture<List<UserAuth>> getAllUsers();

    @Query("DELETE FROM users WHERE id = :userId")
    ListenableFuture<Integer> deleteUserById(int userId);

    @Query("UPDATE users SET username = :newName WHERE id = :userId")
    ListenableFuture<Integer> updateUsername(int userId, String newName);

    @Update
    ListenableFuture<Integer> updateUser(UserAuth user);

    @Query("UPDATE users SET avatar_url = :avatarPath WHERE id = :userId")
    ListenableFuture<Integer> updateAvatarUrl(int userId, String avatarPath);

    @Query("UPDATE users SET password_hash = :newHash WHERE id = :userId")
    ListenableFuture<Integer> updatePasswordHash(int userId, String newHash);

    // --- SYNC ---
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    UserAuth getUserByEmailSync(String email);

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    UserAuth getUserByIdSync(int userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertUserSync(UserAuth user);

    @Query("DELETE FROM users WHERE id = :userId")
    int deleteUserByIdSync(int userId);

    @Query("UPDATE users SET username = :newName WHERE id = :userId")
    int updateUsernameSync(int userId, String newName);

    @Update
    int updateUserSync(UserAuth user);

    @Query("UPDATE users SET avatar_url = :avatarPath WHERE id = :userId")
    int updateAvatarUrlSync(int userId, String avatarPath);

    @Query("UPDATE users SET password_hash = :newHash WHERE id = :userId")
    int updatePasswordHashSync(int userId, String newHash);
}