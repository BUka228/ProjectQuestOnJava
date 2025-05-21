package com.example.projectquestonjava.core.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.projectquestonjava.core.data.model.core.Tag;
import com.example.projectquestonjava.core.data.model.core.TaskTagCrossRef;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;

public interface TaskTagRepository {
    LiveData<List<Tag>> getAllTags();
    ListenableFuture<Long> insertTag(Tag tag);
    ListenableFuture<Void> deleteTags(List<Tag> tags);
    ListenableFuture<Void> deleteTaskTagsByTaskId(long taskId);
    ListenableFuture<Void> insertAllTaskTag(List<TaskTagCrossRef> crossRefs);
}