package com.example.projectquestonjava.approach.calendar.presentation.viewmodels;

import android.net.Uri;
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
    private final GetAllTagsUseCase getAllTagsUseCase; // Теперь возвращает LiveData
    private final AddTagUseCase addTagUseCase;
    private final DeleteTagsUseCase deleteTagsUseCase;
    private final DateTimeUtils dateTimeUtils;
    @IODispatcher private final Executor ioExecutor; // Для операций с БД
    private final Executor mainExecutor; // Для обновлений LiveData из фоновых потоков
    private final Logger logger;
    private final SnackbarManager snackbarManager;

    private final Long taskId; // Может быть null
    private final boolean isEditMode;

    // --- LiveData для UI ---
    private final MutableLiveData<TaskCreationState> _uiStateLiveData;
    public final LiveData<TaskCreationState> uiStateLiveData;

    // LiveData для списка всех тегов
    public final LiveData<Loadable<List<Tag>>> allTagsLoadableLiveData;

    // Объединенное состояние
    private final MediatorLiveData<CombinedState> _combinedStateLiveData = new MediatorLiveData<>();
    public LiveData<CombinedState> combinedStateLiveData = _combinedStateLiveData;


    @Inject
    public TaskCreationViewModel(
            CreateCalendarTaskUseCase createCalendarTaskUseCase,
            UpdateCalendarTaskUseCase updateCalendarTaskUseCase,
            GetTaskInputForEditUseCase getTaskInputForEditUseCase,
            GetAllTagsUseCase getAllTagsUseCase, // UseCase
            AddTagUseCase addTagUseCase,
            DeleteTagsUseCase deleteTagsUseCase,
            DateTimeUtils dateTimeUtils,
            @IODispatcher Executor ioExecutor, // Для фоновых задач
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
        this.mainExecutor = MoreExecutors.directExecutor(); // Или ContextCompat.getMainExecutor(context) если есть context
        this.logger = logger;
        this.snackbarManager = snackbarManager;

        String taskIdString = savedStateHandle.get("taskId");
        this.taskId = (taskIdString != null) ? Long.parseLong(taskIdString) : null;
        this.isEditMode = (this.taskId != null);

        TaskInput initialInput = TaskInput.EMPTY.copy(
                null, // id
                "",   // title
                "",   // description
                dateTimeUtils.currentLocalDateTime().plusMinutes(15).withSecond(0).withNano(0), // dueDate
                null, // recurrenceRule
                Collections.emptySet() // selectedTags
        );
        _uiStateLiveData = new MutableLiveData<>(
                new TaskCreationState(initialInput, false, false, false, true, null, null, isEditMode)
        );
        uiStateLiveData = _uiStateLiveData;

        // GetAllTagsUseCase.execute() возвращает LiveData<List<Tag>>
        // Преобразуем в LiveData<Loadable<List<Tag>>>
        allTagsLoadableLiveData = Transformations.map(getAllTagsUseCase.execute(), tagsList -> {
            if (tagsList != null) {
                logger.debug(TAG, "Tags loaded via LiveData: " + tagsList.size());
                TaskCreationState currentUi = _uiStateLiveData.getValue();
                if (isEditMode && currentUi != null && !currentUi.isLoading() && currentUi.getTaskInput().getId() != null) {
                    // Попытка обновить выбранные теги, если задача уже загружена
                    updateSelectedTagsFromLoadedList(tagsList, currentUi.getTaskInput());
                }
                return new Loadable.Success<>(tagsList);
            } else {
                logger.warn(TAG, "Tags list from LiveData is null.");
                // Можно вернуть Loadable.Error или Loadable.Success с пустым списком,
                // в зависимости от того, как хотим обрабатывать null от DAO.
                return new Loadable.Success<>(Collections.emptyList());
            }
        });

        // Настройка MediatorLiveData
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
        // Инициализация MediatorLiveData начальными значениями, если они уже есть
        if (_uiStateLiveData.getValue() != null && allTagsLoadableLiveData.getValue() != null) {
            _combinedStateLiveData.setValue(new CombinedState(_uiStateLiveData.getValue(), allTagsLoadableLiveData.getValue()));
        }

        loadInitialData();
    }

    private void loadInitialData() {
        logger.debug(TAG, "loadInitialData called. Edit mode: " + isEditMode + ", Task ID: " + taskId);
        // Состояние загрузки уже установлено в конструкторе
        if (isEditMode && taskId != null) {
            loadExistingTaskInternal(taskId);
        } else {
            // Если не режим редактирования, просто убираем флаг загрузки из UI
            updateUiState(state -> state.copy(null,null,null,null, false, null, null, null));
        }
    }

    private void loadExistingTaskInternal(long id) {
        logger.debug(TAG, "Loading existing task with id=" + id);
        // _uiStateLiveData.update уже установлен isLoading=true
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
        if (currentUiState == null || !Objects.equals(currentLoadedInput.getId(), taskId)) return;

        Set<Long> selectedTagIds = currentLoadedInput.getSelectedTags().stream().map(Tag::getId).collect(Collectors.toSet());
        Set<Tag> fullSelectedTags = allTags.stream().filter(tag -> selectedTagIds.contains(tag.getId())).collect(Collectors.toSet());

        if (!fullSelectedTags.equals(currentLoadedInput.getSelectedTags())) {
            logger.debug(TAG, "Updating selected tags with full objects for loaded task.");
            TaskInput updatedTaskInputWithFullTags = new TaskInput(
                    currentLoadedInput.getId(), currentLoadedInput.getTitle(), currentLoadedInput.getDescription(),
                    currentLoadedInput.getDueDate(), currentLoadedInput.getRecurrenceRule(), fullSelectedTags
            );
            // Обновляем только taskInput в текущем состоянии
            updateUiState(state -> state.copy(updatedTaskInputWithFullTags, null,null,null,null,null,null, null));
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
            // Создаем копию для изменения, чтобы не модифицировать существующий объект в LiveData
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
            Set<Tag> currentTags = new HashSet<>(taskInput.getSelectedTags()); // Создаем изменяемую копию
            boolean removed = currentTags.removeIf(t -> t.getId() == tag.getId());
            if (!removed) {
                Loadable<List<Tag>> tagsLoadable = allTagsLoadableLiveData.getValue();
                if (tagsLoadable instanceof Loadable.Success) {
                    // Пытаемся найти полный объект Tag в загруженном списке
                    Tag fullTagFromList = ((Loadable.Success<List<Tag>>) tagsLoadable).getData().stream()
                            .filter(t -> t.getId() == tag.getId())
                            .findFirst()
                            .orElse(tag); // Если не нашли, используем переданный (хотя это странно)
                    currentTags.add(fullTagFromList);
                } else {
                    currentTags.add(tag); // Если теги не загружены, добавляем как есть
                }
            }
            taskInput.setSelectedTags(currentTags);
            return taskInput;
        });
    }

    public void addTag(String tagName) {
        if (tagName == null || tagName.trim().isEmpty()) {
            updateUiState(state -> state.copy(null, null, null, null, null, "Имя тега не может быть пустым", null, null));
            return;
        }
        Loadable<List<Tag>> tagsLoadable = allTagsLoadableLiveData.getValue();
        if (tagsLoadable instanceof Loadable.Success) {
            boolean exists = ((Loadable.Success<List<Tag>>) tagsLoadable).getData().stream()
                    .anyMatch(t -> t.getName().equalsIgnoreCase(tagName.trim()));
            if (exists) {
                updateUiState(state -> state.copy(null, null, null, null, null, "Тег с таким именем уже существует", null, null));
                return;
            }
        }

        logger.debug(TAG, "Adding tag: " + tagName);
        ListenableFuture<Long> future = addTagUseCase.execute(tagName);
        Futures.addCallback(future, new FutureCallback<Long>() {
            @Override
            public void onSuccess(Long result) {
                // LiveData тегов обновится автоматически, что вызовет обновление allTagsLoadableLiveData
                // и, следовательно, _combinedStateLiveData. Snackbar об успехе не нужен тут.
                logger.info(TAG, "Tag '" + tagName + "' added successfully with id " + result);
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Failed to add tag '" + tagName + "'", t);
                updateUiState(state -> state.copy(null, null, null, null, null, toUserFriendlyMessage(t), null, null));
            }
        }, mainExecutor); // Коллбэк на mainExecutor для обновления UI
    }

    public void deleteSelectedTags() {
        TaskCreationState currentState = _uiStateLiveData.getValue();
        if (currentState == null || currentState.getTaskInput().getSelectedTags().isEmpty()) return;

        List<Tag> tagsToDelete = new ArrayList<>(currentState.getTaskInput().getSelectedTags());
        updateTaskInput(input -> { input.setSelectedTags(Collections.emptySet()); return input; });

        logger.debug(TAG, "Deleting selected tags: " + tagsToDelete.stream().map(Tag::getId).collect(Collectors.toList()));
        ListenableFuture<Void> future = deleteTagsUseCase.execute(tagsToDelete);
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) { /* LiveData тегов обновится */ }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Failed to delete selected tags", t);
                updateUiState(state -> state.copy(null, null, null, null, null, toUserFriendlyMessage(t), null, null));
                // TODO: Возможно, стоит откатить удаление из UI, если удаление на бэке не удалось,
                // но это усложнит логику. Проще будет перезагрузить теги.
            }
        }, mainExecutor);
    }

    public void saveTask() {
        if (!validateInput()) return;
        TaskCreationState currentState = Objects.requireNonNull(_uiStateLiveData.getValue());
        updateUiState(state -> state.copy(null, null, null, null, true, null, null, null));
        TaskInput taskInputToSave = currentState.getTaskInput();

        logger.debug(TAG, "Saving task (Edit mode: " + isEditMode + "): " + taskInputToSave.getTitle());

        ListenableFuture<Long> createFuture = null;
        ListenableFuture<Void> updateFuture = null;
        final TaskCreationEvent successEvent;

        if (isEditMode) {
            if (taskId == null) {
                logger.error(TAG, "Task ID is null in edit mode!");
                updateUiState(state -> state.copy(null, null, null, null, false, "Ошибка: ID задачи отсутствует.", null, null));
                return;
            }
            if (taskInputToSave.getId() == null) taskInputToSave.setId(taskId); // Убедимся, что ID установлен
            updateFuture = updateCalendarTaskUseCase.execute(taskInputToSave);
            successEvent = TaskCreationEvent.TASK_UPDATED;
        } else {
            createFuture = createCalendarTaskUseCase.execute(taskInputToSave);
            successEvent = TaskCreationEvent.TASK_CREATED;
        }

        ListenableFuture<?> finalFuture = isEditMode ? updateFuture : createFuture;

        Futures.addCallback(finalFuture, new FutureCallback<Object>() {
            @Override
            public void onSuccess(Object result) {
                logger.info(TAG, "Task save/update successful. Event: " + successEvent);
                updateUiState(state -> state.copy(null, null, null, null, false, null, successEvent, null));
                snackbarManager.showMessage(isEditMode ? "Задача обновлена" : "Задача создана");
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                logger.error(TAG, "Failed to save/update task", t);
                updateUiState(state -> state.copy(null, null, null, null, false, toUserFriendlyMessage(t), null, null));
            }
        }, mainExecutor); // Используем mainExecutor для коллбэков, обновляющих UI
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
        // Отписываемся от LiveData, если использовали observeForever (здесь не используется)
        logger.debug(TAG, "TaskCreationViewModel cleared.");
    }
}