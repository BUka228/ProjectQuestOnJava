package com.example.projectquestonjava.core.data.repositories;

import androidx.lifecycle.LiveData;

import com.example.projectquestonjava.core.data.dao.TagDao;
import com.example.projectquestonjava.core.data.dao.TaskTagCrossRefDao;
import com.example.projectquestonjava.core.data.model.core.Tag;
import com.example.projectquestonjava.core.data.model.core.TaskTagCrossRef;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.domain.repository.TaskTagRepository;
import com.example.projectquestonjava.core.utils.Logger;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TaskTagRepositoryImpl  implements TaskTagRepository {

    private final TaskTagCrossRefDao taskTagCrossRefDao;
    private final TagDao tagDao;
    private final Executor ioExecutor;
    private final Logger logger;

    @Inject
    public TaskTagRepositoryImpl(
            TaskTagCrossRefDao taskTagCrossRefDao,
            TagDao tagDao,
            Logger logger,
            @IODispatcher Executor ioExecutor) {
        this.taskTagCrossRefDao = taskTagCrossRefDao;
        this.tagDao = tagDao;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
    }

    @Override
    public LiveData<List<Tag>> getAllTags() {
        return tagDao.getAllTags();
    }

    @Override
    public ListenableFuture<Long> insertTag(Tag tag) {
        return tagDao.insertTag(tag);
    }

    @Override
    public ListenableFuture<Void> deleteTags(List<Tag> tags) {
        return tagDao.deleteTags(tags);
    }

    @Override
    public ListenableFuture<Void> deleteTaskTagsByTaskId(long taskId) {
        return taskTagCrossRefDao.deleteTaskTagsByTaskId(taskId);
    }

    @Override
    public ListenableFuture<Void> insertAllTaskTag(List<TaskTagCrossRef> crossRefs) {
        return taskTagCrossRefDao.insertAllTaskTag(crossRefs);
    }
}