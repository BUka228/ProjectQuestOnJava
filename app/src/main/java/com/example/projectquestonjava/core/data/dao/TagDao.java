package com.example.projectquestonjava.core.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.projectquestonjava.core.data.model.core.Tag;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;

@Dao
public interface TagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    ListenableFuture<Long> insertTag(Tag tag);

    @Delete
    ListenableFuture<Void> deleteTag(Tag tag);

    @Delete
    ListenableFuture<Void> deleteTags(List<Tag> tags);

    @Query("SELECT * FROM tags ORDER BY name ASC")
    LiveData<List<Tag>> getAllTags();

    @Query("SELECT * FROM tags WHERE id = :tagId")
    ListenableFuture<Tag> getTagById(long tagId);

    @Query("SELECT * FROM tags WHERE name LIKE :name")
    ListenableFuture<List<Tag>> findTagsByName(String name);

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    ListenableFuture<Tag> getTagByName(String name);

    // --- SYNC ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertTagSync(Tag tag);

    @Delete
    void deleteTagSync(Tag tag); // или int

    @Delete
    void deleteTagsSync(List<Tag> tags); // или int
}