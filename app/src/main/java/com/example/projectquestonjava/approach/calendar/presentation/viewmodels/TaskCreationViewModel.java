package com.example.projectquestonjava.approach.calendar.presentation.viewmodels;

import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.projectquestonjava.core.data.model.core.Tag;
import com.example.projectquestonjava.core.di.DefaultDispatcher; // Предполагаем, что это Executor
import com.example.projectquestonjava.core.di.IODispatcher; // И это тоже Executor
import com.example.projectquestonjava.core.di.MainExecutor;
import com.example.projectquestonjava.core.managers.SnackbarManager;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.approach.calendar.domain.model.TaskCreationEvent;
import com.example.projectquestonjava.approach.calendar.domain.model.TaskCreationState;
import com.example.projectquestonjava.approach.calendar.domain.model.TaskInput;
import com.example.projectquestonjava.approach.calendar.domain.usecases.CreateCalendarTaskUseCase;
import com.example.projectquestonjava.approach.calendar.domain.usecases.GetAllTagsUseCase;
import com.example.projectquestonjava.approach.calendar.domain.usecases.GetTaskInputForEditUseCase;
import com.example.projectquestonjava.approach.calendar.domain.usecases.UpdateCalendarTaskUseCase;
import com.example.projectquestonjava.core.domain.usecases.tag.AddTagUseCase;
import com.example.projectquestonjava.core.domain.usecases.tag.DeleteTagsUseCase;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TaskCreationViewModel extends ViewModel {
    private static final String TAG = "TaskCreationViewModel";

    private final CreateCalendarTaskUseCase createCalendarTaskUseCase;
    private final UpdateCalendarTaskUseCase updateCalendarTaskUseCase;
    private final GetTaskInputForEditUseCase getTaskInputForEditUseCase;
    private final GetAllTagsUseCase getAllTagsUseCase;
    private final AddTagUseCase addTagUseCase;
    private final DeleteTagsUseCase deleteTagsUseCase;
    private final DateTimeUtils dateTimeUtils;
    @IODispatcher private final Executor ioExecutor;
    private final Executor mainExecutor;
    private final Logger logger;
    private final SnackbarManager snackbarManager;

    private final Long taskId; // Должно быть final
    private final boolean isEditMode;

    private final MutableLiveData<TaskCreationState> _uiStateLiveData;
    public final LiveData<TaskCreationState> uiStateLiveData;

    public final LiveData<Loadable<List<Tag>>> allTagsLoadableLiveData;

    private final MediatorLiveData<CombinedState> _combinedStateLiveData = new MediatorLiveData<>();
    public LiveData<CombinedState> combinedStateLiveData = _combinedStateLiveData;


    @Inject
    public TaskCreationViewModel(
            CreateCalendarTaskUseCase createCalendarTaskUseCase,
            UpdateCalendarTaskUseCase updateCalendarTaskUseCase,
            GetTaskInputForEditUseCase getTaskInputForEditUseCase,
            GetAllTagsUseCase getAllTagsUseCase,
            AddTagUseCase addTagUseCase,
            DeleteTagsUseCase deleteTagsUseCase,
            DateTimeUtils dateTimeUtils,
            @IODispatcher Executor ioExecutor,
            @MainExecutor Executor mainExecutorInjected,
            SavedStateHandle savedStateHandle,
            SnackbarManager snackbarManager,
            Logger logger) {
        this.createCalendarTaskUseCase = createCalendarTaskUseCase;
        this.updateCalendarTaskUseCase = updateCalendarTaskUseCase;
        this.getTaskInputForEditUseCase = getTaskInputForEditUseCase;
        this.getAllTagsUseCase = getAllTagsUseCase;
        this.addTagUseCase = addTagUseCase;
        this.deleteTagsUseCase = deleteTagsUseCase;
        this.dateTimeUtils = dateTimeUtils;
        this.ioExecutor = ioExecutor;
        this.mainExecutor = mainExecutorInjected;
        this.logger = logger;
        this.snackbarManager = snackbarManager;

        Long parsedTaskId = null;
        Object taskIdObject = savedStateHandle.get("taskId");
        if (taskIdObject instanceof Long) {
            parsedTaskId = (Long) taskIdObject;
        } else if (taskIdObject instanceof String) {
            try {
                parsedTaskId = Long.parseLong((String) taskIdObject);
            } catch (NumberFormatException e) {
                logger.warn(TAG, "Could not parse taskId from String in SavedStateHandle: " + taskIdObject);
                // parsedTaskId остается null
            }
        } else if (taskIdObject != null) { // Если не Long, не String, но и не null
            logger.warn(TAG, "Unexpected type for taskId in SavedStateHandle: " + taskIdObject.getClass().getName());
            // parsedTaskId остается null
        }

        this.taskId = parsedTaskId; // Однократное присвоение final переменной

        this.isEditMode = (this.taskId != null && this.taskId != -1L);
        logger.debug(TAG, "ViewModel initialized. Parsed TaskId: " + this.taskId + ", isEditMode: " + this.isEditMode);

        TaskInput initialInput = TaskInput.EMPTY.copy(
                null, "", "",
                dateTimeUtils.currentLocalDateTime().plusMinutes(15).withSecond(0).withNano(0),
                null, Collections.emptySet()
        );
        _uiStateLiveData = new MutableLiveData<>(
                new TaskCreationState(initialInput, false, false, false, true, null, null, isEditMode)
        );
        uiStateLiveData = _uiStateLiveData;

        allTagsLoadableLiveData = Transformations.map(getAllTagsUseCase.execute(), tagsList -> {
            if (tagsList != null) {
                logger.debug(TAG, "Tags loaded via LiveData: " + tagsList.size());
                TaskCreationState currentUi = _uiStateLiveData.getValue();
                if (isEditMode && currentUi != null && !currentUi.isLoading() && currentUi.getTaskInput().getId() != null) {
                    updateSelectedTagsFromLoadedList(tagsList, currentUi.getTaskInput());
                }
                return new Loadable.Success<>(tagsList);
            } else {
                logger.warn(TAG, "Tags list from LiveData is null.");
                return new Loadable.Success<>(Collections.emptyList());
            }
        });

        _combinedStateLiveData.addSource(_uiStateLiveData, uiState -> {
            Loadable<List<Tag>> currentTags = allTagsLoadableLiveData.getValue();
            if (uiState != null && currentTags != null) {
                _combinedStateLiveData.setValue(new CombinedState(uiState, currentTags));
            }
        });
        _combinedStateLiveData.addSource(allTagsLoadableLiveData, tagsLoadable -> {
            TaskCreationState currentUiState = _uiStateLiveData.getValue();
            if (tagsLoadable != null && currentUiState != null) {
                _combinedStateLiveData.setValue(new CombinedState(currentUiState, tagsLoadable));
            }
        });
        if (_uiStateLiveData.getValue() != null && allTagsLoadableLiveData.getValue() != null) {
            _combinedStateLiveData.setValue(new CombinedState(_uiStateLiveData.getValue(), allTagsLoadableLiveData.getValue()));
        }
        loadInitialData();
    }

    private void loadInitialData() {
        logger.debug(TAG, "loadInitialData called. Edit mode: " + isEditMode + ", Current Task ID in ViewModel: " + this.taskId);
        if (isEditMode && this.taskId != null) { // this.taskId теперь корректно инициализирован
            loadExistingTaskInternal(this.taskId);
        } else {
            updateUiState(state -> state.copy(null,null,null,null, false, null, null, null));
        }
    }

    private void loadExistingTaskInternal(long id) {
        logger.debug(TAG, "Loading existing task with id=" + id);
        ListenableFuture<ListenableFuture<TaskInput>> futureOfFuture = getTaskInputForEditUseCase.executeAsync(id);

        Futures.addCallback(futureOfFuture, new FutureCallback<ListenableFuture<TaskInput>>() {
            @Override
            public void onSuccess(ListenableFuture<TaskInput> taskInputFuture) {
                Futures.addCallback(taskInputFuture, new FutureCallback<TaskInput>() {
                    @Override
                    public void onSuccess(TaskInput taskInput) {
                        updateUiState(state -> state.copy(taskInput,null,null,null, false, null, null, null));
                        logger.info(TAG, "Task " + id + " loaded successfully for editing.");
                        Loadable<List<Tag>> tagsLoadable = allTagsLoadableLiveData.getValue();
                        if (tagsLoadable instanceof Loadable.Success) {
                            updateSelectedTagsFromLoadedList(((Loadable.Success<List<Tag>>) tagsLoadable).getData(), taskInput);
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        handleLoadTaskFailure(t, id);
                    }
                }, ioExecutor);
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                handleLoadTaskFailure(t, id);
            }
        }, ioExecutor);
    }

    private void handleLoadTaskFailure(Throwable t, long id) {
        logger.error(TAG, "Failed to load task " + id + " for editing.", t);
        updateUiState(state -> state.copy(null,null,null,null, false, toUserFriendlyMessage(t), null, null));
    }


    private void updateSelectedTagsFromLoadedList(List<Tag> allTags, TaskInput currentLoadedInput) {
        TaskCreationState currentUiState = _uiStateLiveData.getValue();
        if (currentUiState == null || !Objects.equals(currentLoadedInput.getId(), this.taskId)) {
            logger.warn(TAG, "Skipping updateSelectedTags: currentLoadedInput.id ("+currentLoadedInput.getId()+") != this.taskId ("+this.taskId+")");
            return;
        }

        Set<Long> selectedTagIds = currentLoadedInput.getSelectedTags().stream().map(Tag::getId).collect(Collectors.toSet());
        Set<Tag> fullSelectedTags = allTags.stream().filter(tag -> selectedTagIds.contains(tag.getId())).collect(Collectors.toSet());

        // Сравниваем по содержимому, а не по ссылкам, т.к. currentLoadedInput.getSelectedTags() могли быть неполными
        if (!fullSelectedTags.equals(currentLoadedInput.getSelectedTags())) {
            logger.debug(TAG, "Updating selected tags with full objects for loaded task ID: " + currentLoadedInput.getId());
            TaskInput updatedTaskInputWithFullTags = currentLoadedInput.copy(
                    null, null, null, null, null, fullSelectedTags
            );
            updateUiState(state -> state.copy(updatedTaskInputWithFullTags, null,null,null,null,null,null, null));
        } else {
            logger.debug(TAG, "Selected tags are already full objects for task ID: " + currentLoadedInput.getId());
        }
    }


    private void updateUiState(UiStateUpdater updater) {
        TaskCreationState currentState = _uiStateLiveData.getValue();
        if (currentState != null) {
            _uiStateLiveData.postValue(updater.update(currentState));
        }
    }

    @FunctionalInterface
    interface UiStateUpdater {
        TaskCreationState update(TaskCreationState currentState);
    }

    private void updateTaskInput(TaskInputUpdater updater) {
        updateUiState(state -> {
            TaskInput currentTaskInput = state.getTaskInput();
            TaskInput mutableCopy = new TaskInput(
                    currentTaskInput.getId(), currentTaskInput.getTitle(), currentTaskInput.getDescription(),
                    currentTaskInput.getDueDate(), currentTaskInput.getRecurrenceRule(), new HashSet<>(currentTaskInput.getSelectedTags())
            );
            TaskInput updatedTaskInput = updater.update(mutableCopy);
            return state.copy(updatedTaskInput, null, null, null, null, null, null, null);
        });
    }

    @FunctionalInterface
    interface TaskInputUpdater {
        TaskInput update(TaskInput currentInput);
    }

    public void updateTitle(String newTitle) { updateTaskInput(input -> { input.setTitle(newTitle); return input; }); }
    public void updateDescription(String newDescription) { updateTaskInput(input -> { input.setDescription(newDescription); return input; }); }

    public void setDueDate(LocalDate newDate) {
        TaskCreationState state = _uiStateLiveData.getValue();
        if (state == null) return;
        LocalTime currentTime = state.getTaskInput().getDueDate().toLocalTime();
        updateDateTime(LocalDateTime.of(newDate, currentTime));
        closeDateDialog();
    }

    public void setDueTime(LocalTime newTime) {
        TaskCreationState state = _uiStateLiveData.getValue();
        if (state == null) return;
        LocalDate currentDate = state.getTaskInput().getDueDate().toLocalDate();
        updateDateTime(LocalDateTime.of(currentDate, newTime));
        closeTimeDialog();
    }

    private void updateDateTime(LocalDateTime newDateTime) {
        updateTaskInput(input -> { input.setDueDate(newDateTime.withSecond(0).withNano(0)); return input; });
    }

    public void setRecurrenceRule(String rule) {
        updateTaskInput(input -> { input.setRecurrenceRule(rule); return input; });
        closeRecurrenceDialog();
    }

    public void openDateDialog() { updateUiState(state -> state.copy(null, true, null, null, null, null, null, null)); }
    public void closeDateDialog() { updateUiState(state -> state.copy(null, false, null, null, null, null, null, null)); }
    public void openTimeDialog() { updateUiState(state -> state.copy(null, null, true, null, null, null, null, null)); }
    public void closeTimeDialog() { updateUiState(state -> state.copy(null, null, false, null, null, null, null, null)); }
    public void openRecurrenceDialog() { updateUiState(state -> state.copy(null, null, null, true, null, null, null, null)); }
    public void closeRecurrenceDialog() { updateUiState(state -> state.copy(null, null, null, false, null, null, null, null)); }

    public void toggleTagSelection(Tag tag) {
        updateTaskInput(taskInput -> {
            Set<Tag> currentTags = new HashSet<>(taskInput.getSelectedTags());
            // Пытаемся найти тег по ID, чтобы корректно удалить, даже если объект другой
            Tag tagToRemove = null;
            for (Tag selectedTag : currentTags) {
                if (selectedTag.getId() == tag.getId()) {
                    tagToRemove = selectedTag;
                    break;
                }
            }

            if (tagToRemove != null) {
                currentTags.remove(tagToRemove);
            } else {
                Loadable<List<Tag>> tagsLoadable = allTagsLoadableLiveData.getValue();
                if (tagsLoadable instanceof Loadable.Success) {
                    Tag fullTagFromList = ((Loadable.Success<List<Tag>>) tagsLoadable).getData().stream()
                            .filter(t -> t.getId() == tag.getId())
                            .findFirst()
                            .orElse(tag);
                    currentTags.add(fullTagFromList);
                } else {
                    currentTags.add(tag);
                }
            }
            taskInput.setSelectedTags(currentTags);
            return taskInput;
        });
    }


    public void addTag(String tagName) {
        if (tagName == null || tagName.trim().isEmpty()) {
            snackbarManager.showMessage("Имя тега не может быть пустым");
            return;
        }
        Loadable<List<Tag>> tagsLoadable = allTagsLoadableLiveData.getValue();
        if (tagsLoadable instanceof Loadable.Success) {
            boolean exists = ((Loadable.Success<List<Tag>>) tagsLoadable).getData().stream()
                    .anyMatch(t -> t.getName().equalsIgnoreCase(tagName.trim()));
            if (exists) {
                snackbarManager.showMessage("Тег с таким именем уже существует");
                return;
            }
        }

        logger.debug(TAG, "Adding tag: " + tagName);
        ListenableFuture<Long> future = addTagUseCase.execute(tagName);
        Futures.addCallback(future, new FutureCallback<Long>() {
            @Override
            public void onSuccess(Long result) {
                logger.info(TAG, "Tag '" + tagName + "' added successfully with id " + result);
                snackbarManager.showMessage("Тег '" + tagName + "' добавлен");
                // LiveData тегов обновится автоматически
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Failed to add tag '" + tagName + "'", t);
                snackbarManager.showMessage(toUserFriendlyMessage(t));
            }
        }, mainExecutor);
    }

    public void deleteSelectedTags() {
        TaskCreationState currentState = _uiStateLiveData.getValue();
        if (currentState == null || currentState.getTaskInput().getSelectedTags().isEmpty()) return;

        List<Tag> tagsToDelete = new ArrayList<>(currentState.getTaskInput().getSelectedTags());
        updateTaskInput(input -> { input.setSelectedTags(Collections.emptySet()); return input; });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            logger.debug(TAG, "Deleting selected tags: " + tagsToDelete.stream().map(Tag::getId).toList());
        }
        ListenableFuture<Void> future = deleteTagsUseCase.execute(tagsToDelete);
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                snackbarManager.showMessage(tagsToDelete.size() + " тег(а/ов) удалено");
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Failed to delete selected tags", t);
                snackbarManager.showMessage(toUserFriendlyMessage(t));
                // TODO: Потенциально откатить UI, если удаление не удалось, перезагрузив теги
            }
        }, mainExecutor);
    }

    public void saveTask() {
        if (!validateInput()) return;
        TaskCreationState currentState = Objects.requireNonNull(_uiStateLiveData.getValue());
        updateUiState(state -> state.copy(null, null, null, null, true, null, null, null));
        TaskInput taskInputToSave = currentState.getTaskInput();

        logger.debug(TAG, "Saving task (Edit mode: " + isEditMode + "): " + taskInputToSave.getTitle());

        ListenableFuture<?> finalFuture;
        final TaskCreationEvent successEvent;

        if (isEditMode) {
            if (this.taskId == null) {
                logger.error(TAG, "Task ID is null in edit mode logic inside saveTask!");
                updateUiState(state -> state.copy(null, null, null, null, false, "Критическая ошибка: ID задачи отсутствует для обновления.", null, null));
                snackbarManager.showMessage("Критическая ошибка: ID задачи отсутствует.");
                return;
            }
            if (taskInputToSave.getId() == null) taskInputToSave.setId(this.taskId);

            finalFuture = updateCalendarTaskUseCase.execute(taskInputToSave);
            successEvent = TaskCreationEvent.TASK_UPDATED;
        } else {
            finalFuture = createCalendarTaskUseCase.execute(taskInputToSave);
            successEvent = TaskCreationEvent.TASK_CREATED;
        }

        Futures.addCallback(finalFuture, new FutureCallback<Object>() {
            @Override
            public void onSuccess(Object result) {
                logger.info(TAG, "Task save/update successful. Event: " + successEvent);
                updateUiState(state -> state.copy(null, null, null, null, false, null, successEvent, null));
                snackbarManager.showMessage(isEditMode ? "Задача обновлена" : "Задача создана");
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                Throwable cause = t.getCause() != null ? t.getCause() : t;
                logger.error(TAG, "Failed to save/update task. Original Cause: " + cause.getMessage(), cause);
                updateUiState(state -> state.copy(null, null, null, null, false, toUserFriendlyMessage(cause), null, null));
                snackbarManager.showMessage("Ошибка сохранения: " + toUserFriendlyMessage(cause));
            }
        }, mainExecutor); // <-- ИСПОЛЬЗУЕМ mainExecutor
    }

    public void clearError() { updateUiState(state -> state.copy(null, null, null, null, null, null, null, null)); }
    public void clearEvent() { updateUiState(state -> state.copy(null, null, null, null, null, null, null, null)); }

    private boolean validateInput() {
        TaskCreationState currentState = _uiStateLiveData.getValue();
        if (currentState == null || currentState.getTaskInput().getTitle() == null || currentState.getTaskInput().getTitle().trim().isEmpty()) {
            updateUiState(state -> state.copy(null, null, null, null, null, "Заголовок не может быть пустым", null, null));
            return false;
        }
        return true;
    }

    public String getScreenTitle() { return isEditMode ? "Редактирование задачи" : "Новая задача"; }
    public String getSaveButtonText() { return isEditMode ? "Обновить задачу" : "Создать задачу"; }

    private static String toUserFriendlyMessage(Throwable t) {
        if (t instanceof IOException) return "Ошибка сети. Проверьте подключение.";
        if (t instanceof IllegalArgumentException) return "Некорректные данные: " + t.getMessage();
        if (t instanceof NoSuchElementException) return "Запрашиваемый элемент не найден.";
        if (t instanceof IllegalStateException) return "Ошибка состояния: " + t.getMessage();
        return "Произошла неизвестная ошибка: " + t.getMessage();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        logger.debug(TAG, "TaskCreationViewModel cleared.");
    }
}