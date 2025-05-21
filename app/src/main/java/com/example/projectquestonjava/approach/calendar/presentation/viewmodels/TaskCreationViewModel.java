package com.example.projectquestonjava.approach.calendar.presentation.viewmodels;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import com.example.projectquestonjava.core.data.model.core.Tag;
import com.example.projectquestonjava.core.di.DefaultDispatcher;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

// Loadable и CombinedState уже были определены как Java классы.

@HiltViewModel
public class TaskCreationViewModel extends ViewModel {
    private static final String TAG = "TaskCreationViewModel";

    private final CreateCalendarTaskUseCase createCalendarTaskUseCase;
    private final UpdateCalendarTaskUseCase updateCalendarTaskUseCase;
    private final GetTaskInputForEditUseCase getTaskInputForEditUseCase;
    private final AddTagUseCase addTagUseCase;
    private final DeleteTagsUseCase deleteTagsUseCase;
    private final Executor defaultExecutor; // Для фоновых задач
    private final Logger logger;

    private final Long taskId;
    private final boolean isEditMode;

    private final MutableLiveData<TaskCreationState> _uiStateLiveData = new MutableLiveData<>();
    public LiveData<TaskCreationState> uiStateLiveData = _uiStateLiveData;

    // Используем LiveData для тегов, полученных из GetAllTagsUseCase
    private final LiveData<Loadable<List<Tag>>> tagsLoadableLiveData;


    // CombinedState LiveData
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
            @DefaultDispatcher Executor defaultExecutor,
            SavedStateHandle savedStateHandle,
            Logger logger) throws ExecutionException, InterruptedException {
        this.createCalendarTaskUseCase = createCalendarTaskUseCase;
        this.updateCalendarTaskUseCase = updateCalendarTaskUseCase;
        this.getTaskInputForEditUseCase = getTaskInputForEditUseCase;
        this.addTagUseCase = addTagUseCase;
        this.deleteTagsUseCase = deleteTagsUseCase;
        this.defaultExecutor = defaultExecutor;
        this.logger = logger;

        String taskIdString = savedStateHandle.get("taskId");
        this.taskId = (taskIdString != null) ? Long.parseLong(taskIdString) : null;
        this.isEditMode = (this.taskId != null);

        // Инициализация начального состояния UI
        TaskInput initialInput = new TaskInput(
                null, "", "",
                dateTimeUtils.currentLocalDateTime().plusMinutes(15).withSecond(0).withNano(0),
                null, Collections.emptySet()
        );
        _uiStateLiveData.setValue(new TaskCreationState(initialInput, false, false, false, false, null, null, isEditMode));

        // GetAllTagsUseCase.execute() теперь возвращает LiveData<List<Tag>>
        // Преобразуем его в LiveData<Loadable<List<Tag>>>
        tagsLoadableLiveData = Transformations.map(getAllTagsUseCase.execute(), tagsList -> {
            if (tagsList != null) {
                logger.debug(TAG, "Loaded " + tagsList.size() + " tags from LiveData.");
                // Попытка обновить выбранные теги, если задача уже загружена
                TaskCreationState currentUi = _uiStateLiveData.getValue();
                if (isEditMode && currentUi != null && !currentUi.isLoading()) {
                    updateSelectedTagsFromLoadedList(tagsList, currentUi); // Передаем флаг для postValue
                }
                return new Loadable.Success<>(tagsList);
            } else {
                logger.error(TAG, "Failed to load tags, LiveData from use case returned null.");
                return new Loadable.Error<List<Tag>>(new IOException("Failed to load tags"));
            }
        });


        // Настраиваем MediatorLiveData для combinedStateLiveData
        _combinedStateLiveData.addSource(_uiStateLiveData, uiState -> {
            Loadable<List<Tag>> currentTagsLoadable = tagsLoadableLiveData.getValue();
            if (uiState != null && currentTagsLoadable != null) {
                _combinedStateLiveData.setValue(new CombinedState(uiState, currentTagsLoadable));
            }
        });
        _combinedStateLiveData.addSource(tagsLoadableLiveData, tagsLoadable -> {
            TaskCreationState currentUiState = _uiStateLiveData.getValue();
            if (tagsLoadable != null && currentUiState != null) {
                _combinedStateLiveData.setValue(new CombinedState(currentUiState, tagsLoadable));
            }
        });

        loadInitialTaskDataIfNeeded();
    }

    private void loadInitialTaskDataIfNeeded() throws ExecutionException, InterruptedException {
        logger.debug(TAG, "Initializing Task Data. Edit mode: " + isEditMode + ", Task ID: " + taskId);
        if (isEditMode && taskId != null) {
            loadExistingTaskInternal(taskId);
        } else {
            updateUiState(state -> state.copy(null, null, null, null, false, null, null, null), false);
        }
    }

    private void loadExistingTaskInternal(long id) throws ExecutionException, InterruptedException {
        logger.debug(TAG, "Loading existing task with id=" + id);
        updateUiState(state -> state.copy(null, null, null, null, true, null, null, null), false);

        // GetTaskInputForEditUseCase.executeAsync возвращает ListenableFuture<ListenableFuture<TaskInput>>
        // это нужно упростить до ListenableFuture<TaskInput>
        // Предположим, executeAsync изменен на execute() и возвращает ListenableFuture<TaskInput>
        ListenableFuture<TaskInput> future = getTaskInputForEditUseCase.executeAsync(id).get();
        // Если executeAsync(id) возвращает вложенный Future, его нужно "развернуть":
        // ListenableFuture<TaskInput> flattenedFuture = Futures.transformAsync(future, f -> f, MoreExecutors.directExecutor());

        Futures.addCallback(future, new FutureCallback<TaskInput>() {
            @Override
            public void onSuccess(TaskInput taskInput) {
                updateUiState(state -> state.copy(taskInput, null, null, null, false, null, null, null), true);
                logger.info(TAG, "Task " + id + " loaded successfully for editing.");
                Loadable<List<Tag>> tagsLoadable = tagsLoadableLiveData.getValue(); // Используем tagsLoadableLiveData
                if (tagsLoadable instanceof Loadable.Success) {
                    updateSelectedTagsFromLoadedList(((Loadable.Success<List<Tag>>) tagsLoadable).getData(), _uiStateLiveData.getValue());
                }
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                updateUiState(state -> state.copy(null, null, null, null, false, toUserFriendlyMessage(t), null, null), true);
                logger.error(TAG, "Failed to load task " + id + " for editing.", t);
            }
        }, defaultExecutor); // Используем defaultExecutor (который должен быть ioExecutor) для коллбэка
    }

    private void updateSelectedTagsFromLoadedList(List<Tag> allTags, TaskCreationState currentUiState) {
        if (currentUiState == null || currentUiState.getTaskInput().getId() == null || !Objects.equals(currentUiState.getTaskInput().getId(), taskId)) return;

        TaskInput currentInput = currentUiState.getTaskInput();
        Set<Long> selectedTagIds = currentInput.getSelectedTags().stream().map(Tag::getId).collect(Collectors.toSet());
        Set<Tag> fullSelectedTags = allTags.stream().filter(tag -> selectedTagIds.contains(tag.getId())).collect(Collectors.toSet());

        if (!fullSelectedTags.equals(currentInput.getSelectedTags())) {
            logger.debug(TAG, "Updating selected tags with full objects.");
            TaskInput updatedTaskInput = new TaskInput(
                    currentInput.getId(), currentInput.getTitle(), currentInput.getDescription(),
                    currentInput.getDueDate(), currentInput.getRecurrenceRule(), fullSelectedTags
            );
            updateUiState(state -> state.copy(updatedTaskInput, null, null, null, null, null, null, null), true);
        }
    }


    private void updateUiState(UiStateUpdater updater, boolean usePostValue) {
        TaskCreationState currentState = _uiStateLiveData.getValue();
        if (currentState != null) {
            TaskCreationState newState = updater.update(currentState);
            if (usePostValue) {
                _uiStateLiveData.postValue(newState);
            } else {
                _uiStateLiveData.setValue(newState);
            }
        }
    }

    @FunctionalInterface
    interface UiStateUpdater {
        TaskCreationState update(TaskCreationState currentState);
    }

    // --- Обновление полей ---
    private void updateTaskInput(TaskInputUpdater updater) {
        updateUiState(state -> {
            TaskInput currentTaskInput = state.getTaskInput();
            TaskInput updatedTaskInput = updater.update(new TaskInput( // Создаем копию для изменения
                    currentTaskInput.getId(), currentTaskInput.getTitle(), currentTaskInput.getDescription(),
                    currentTaskInput.getDueDate(), currentTaskInput.getRecurrenceRule(), new HashSet<>(currentTaskInput.getSelectedTags())
            ));
            return state.copy(updatedTaskInput, null, null, null, null, null, null, null);
        }, false);
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

    // --- Диалоги ---
    public void openDateDialog() { updateUiState(state -> state.copy(null, true, null, null, null, null, null, null), false); }
    public void closeDateDialog() { updateUiState(state -> state.copy(null, false, null, null, null, null, null, null), false); }
    public void openTimeDialog() { updateUiState(state -> state.copy(null, null, true, null, null, null, null, null), false); }
    public void closeTimeDialog() { updateUiState(state -> state.copy(null, null, false, null, null, null, null, null), false); }
    public void openRecurrenceDialog() { updateUiState(state -> state.copy(null, null, null, true, null, null, null, null), false); }
    public void closeRecurrenceDialog() { updateUiState(state -> state.copy(null, null, null, false, null, null, null, null), false); }


    public void toggleTagSelection(Tag tag) {
        updateTaskInput(taskInput -> {
            Set<Tag> currentTags = new HashSet<>(taskInput.getSelectedTags());
            boolean removed = currentTags.removeIf(t -> t.getId() == tag.getId());
            if (!removed) {
                Loadable<List<Tag>> tagsLoadable = tagsLoadableLiveData.getValue(); // Используем tagsLoadableLiveData
                if (tagsLoadable instanceof Loadable.Success) {
                    Tag fullTag = ((Loadable.Success<List<Tag>>) tagsLoadable).getData().stream()
                            .filter(t -> t.getId() == tag.getId()).findFirst().orElse(tag);
                    currentTags.add(fullTag);
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
            updateUiState(state -> state.copy(null, null, null, null, null, "Имя тега не может быть пустым", null, null), false);
            return;
        }
        Loadable<List<Tag>> tagsLoadable = tagsLoadableLiveData.getValue();
        if (tagsLoadable instanceof Loadable.Success) {
            boolean exists = ((Loadable.Success<List<Tag>>) tagsLoadable).getData().stream()
                    .anyMatch(t -> t.getName().equalsIgnoreCase(tagName.trim()));
            if (exists) {
                updateUiState(state -> state.copy(null, null, null, null, null, "Тег с таким именем уже существует", null, null), false);
                return;
            }
        }

        logger.debug(TAG, "Adding tag: " + tagName);
        ListenableFuture<Long> future = addTagUseCase.execute(tagName);
        Futures.addCallback(future, new FutureCallback<Long>() {
            @Override
            public void onSuccess(Long result) { /* LiveData тегов обновится автоматически */ }
            @Override
            public void onFailure(@NonNull Throwable t) {
                updateUiState(state -> state.copy(null, null, null, null, null, toUserFriendlyMessage(t), null, null), true);
                logger.error(TAG, "Failed to add tag '" + tagName + "'", t);
            }
        }, defaultExecutor);
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
            public void onSuccess(Void result) { /* LiveData тегов обновится */ }
            @Override
            public void onFailure(@NonNull Throwable t) {
                updateUiState(state -> state.copy(null, null, null, null, null, toUserFriendlyMessage(t), null, null), true);
                logger.error(TAG, "Failed to delete selected tags", t);
                // Перезагрузка тегов здесь может быть излишней, если LiveData из getAllTagsUseCase обновляется
            }
        }, defaultExecutor);
    }

    public void saveTask() {
        if (!validateInput()) return;
        TaskCreationState currentState = Objects.requireNonNull(_uiStateLiveData.getValue());
        updateUiState(state -> state.copy(null, null, null, null, true, null, null, null), false);
        TaskInput taskInputToSave = currentState.getTaskInput();

        logger.debug(TAG, "Saving task (Edit mode: " + isEditMode + "): " + taskInputToSave.getTitle());

        ListenableFuture<?> operationFuture; // Тип зависит от UseCase
        final TaskCreationEvent successEvent;

        if (isEditMode) {
            if (taskId == null) { // Должно быть проверено ранее, но для безопасности
                logger.error(TAG, "Task ID is null in edit mode!");
                updateUiState(state -> state.copy(null, null, null, null, false, "Ошибка: ID задачи отсутствует.", null, null), false);
                return;
            }
            // Убедимся, что ID установлен в taskInputToSave
            if (taskInputToSave.getId() == null) {
                taskInputToSave.setId(taskId);
            }
            operationFuture = updateCalendarTaskUseCase.execute(taskInputToSave);
            successEvent = TaskCreationEvent.TASK_UPDATED;
        } else {
            operationFuture = createCalendarTaskUseCase.execute(taskInputToSave);
            successEvent = TaskCreationEvent.TASK_CREATED;
        }

        Futures.addCallback(operationFuture, new FutureCallback<Object>() { // Object, т.к. Future<?>
            @Override
            public void onSuccess(Object result) {
                logger.info(TAG, "Task save/update successful. Event: " + successEvent);
                updateUiState(state -> state.copy(null, null, null, null, false, null, successEvent, null), true);
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Failed to save/update task", t);
                updateUiState(state -> state.copy(null, null, null, null, false, toUserFriendlyMessage(t), null, null), true);
            }
        }, defaultExecutor);
    }

    public void clearError() { updateUiState(state -> state.copy(null, null, null, null, null, null, null, null), false); }
    public void clearEvent() { updateUiState(state -> state.copy(null, null, null, null, null, null, null, null), false); }

    private boolean validateInput() {
        TaskCreationState currentState = _uiStateLiveData.getValue();
        if (currentState == null || currentState.getTaskInput().getTitle() == null || currentState.getTaskInput().getTitle().trim().isEmpty()) {
            updateUiState(state -> state.copy(null, null, null, null, null, "Заголовок не может быть пустым", null, null), false);
            return false;
        }
        return true;
    }

    public String getScreenTitle() { return isEditMode ? "Редактирование задачи" : "Новая задача"; }
    public String getSaveButtonText() { return isEditMode ? "Обновить задачу" : "Создать задачу"; }

    private static String toUserFriendlyMessage(Throwable t) { // Сделал static
        if (t instanceof IOException) return "Ошибка сети. Проверьте подключение.";
        if (t instanceof IllegalArgumentException) return "Некорректные данные: " + t.getMessage();
        if (t instanceof NoSuchElementException) return "Запрашиваемый элемент не найден.";
        if (t instanceof IllegalStateException) return "Ошибка состояния: " + t.getMessage();
        return "Произошла неизвестная ошибка. Попробуйте снова.";
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Если была подписка observeForever на tagsLoadableLiveData (которая была LiveData от UseCase),
        // ее нужно было бы здесь удалить. Но так как tagsLoadableLiveData теперь сама LiveData,
        // Android обработает ее жизненный цикл.
        logger.debug(TAG, "ViewModel cleared.");
    }
}