package com.example.projectquestonjava.approach.calendar.domain.usecases;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import com.example.projectquestonjava.core.data.model.core.Tag;
import com.example.projectquestonjava.core.di.IODispatcher;
import com.example.projectquestonjava.core.domain.repository.TaskTagRepository;
import com.example.projectquestonjava.core.utils.Logger;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import javax.inject.Inject;

public class GetAllTagsUseCase {
    private static final String TAG = "GetAllTagsUseCase";

    private final TaskTagRepository taskTagRepository;
    private final Logger logger;

    @Inject
    public GetAllTagsUseCase(
            TaskTagRepository taskTagRepository,
            Logger logger) {
        this.taskTagRepository = taskTagRepository;
        this.logger = logger;
    }

    public LiveData<List<Tag>> execute() {
        logger.debug(TAG, "Requesting all tags LiveData.");
        return taskTagRepository.getAllTags();
    }
}