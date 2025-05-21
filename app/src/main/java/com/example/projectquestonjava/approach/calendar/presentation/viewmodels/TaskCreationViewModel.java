package com.example.projectquestonjava.approach.calendar.presentation.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.example.projectquestonjava.core.data.model.core.Tag;
import com.example.projectquestonjava.core.di.DefaultDispatcher; // Для Executor
import com.example.projectquestonjava.core.domain.usecases.tag.AddTagUseCase;
import com.example.projectquestonjava.core.domain.usecases.tag.DeleteTagsUseCase;
import com.example.projectquestonjava.core.utils.DateTimeUtils;
import com.example.projectquestonjava.core.utils.Logger;
import com.example.projectquestonjava.approach.calendar.domain.model.TaskCreationEvent;
import com.example.projectquestonjava.approach.calendar.domain.model.TaskCreationState;
import com.example.projectquestonjava.approach.calendar.domain.model.TaskInput;
import com.example.projectquestonjava.approach.calendar.domain.usecases.CreateCalendarTaskUseCase;
import com.example.projectquestonjava.approach.calendar.domain.usecases.GetAllTagsUseCase;
import com.example.projectquestonjava.approach.calendar.domain.usecases.GetTaskInputForEditUseCase;
import com.example.projectquestonjava.approach.calendar.domain.usecases.UpdateCalendarTaskUseCase;
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
    private final Executor defaultExecutor; // Замена CoroutineDispatcher
    private final Logger logger;

    private final Long taskId; // Может быть null
    private final boolean isEditMode;

    private final MutableLiveData<TaskCreationState> _uiState = new MutableLiveData<>();
    private final MutableLiveData<Loadable<List<Tag>>> _tags = new MutableLiveData<>(Loadable.Loading.getInstance());

    private final MediatorLiveData<CombinedState> _combinedState = new MediatorLiveData<>();
    public LiveData<CombinedState> combinedStateLiveData = _combinedState;


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
            Logger logger) {
        this.createCalendarTaskUseCase = createCalendarTaskUseCase;
        this.updateCalendarTaskUseCase = updateCalendarTaskUseCase;
        this.getTaskInputForEditUseCase = getTaskInputForEditUseCase;
        this.getAllTagsUseCase = getAllTagsUseCase;
        this.addTagUseCase = addTagUseCase;
        this.deleteTagsUseCase = deleteTagsUseCase;
        this.dateTimeUtils = dateTimeUtils;
        this.defaultExecutor = defaultExecutor;
        this.logger = logger;

        String taskIdString = savedStateHandle.get("taskId");
        this.taskId = (taskIdString != null) ? Long.parseLong(taskIdString) : null;
        this.isEditMode = (this.taskId != null);

        _uiState.setValue(new TaskCreationState(
                new TaskInput(null, "", "", dateTimeUtils.currentLocalDateTime().plusMinutes(15).withSecond(0).withNano(0), null, Collections.emptySet()),
                false, false, false, false, null, null, isEditMode
        ));

        _combinedState.addSource(_uiState, taskState -> {
            Loadable<List<Tag>> currentTags = _tags.getValue();
            if (taskState != null && currentTags != null) {
                _combinedState.setValue(new CombinedState(taskState, currentTags));
            }
        });
        _combinedState.addSource(_tags, tagsLoadable -> {
            TaskCreationState currentTaskState = _uiState.getValue();
            if (tagsLoadable != null && currentTaskState != null) {
                _combinedState.setValue(new CombinedState(currentTaskState, tagsLoadable));
            }
        });

        loadInitialData();
    }

    private void loadInitialData() {
        logger.debug(TAG, "Initializing ViewModel. Edit mode: " + isEditMode + ", Task ID: " + taskId);
        loadTagsInternal(); // Загрузка тегов

        if (isEditMode && taskId != null) {
            loadExistingTaskInternal(taskId);
        } else {
            updateUiState(state -> state.copy(null, null, null, null, false, null, null, null));
        }
    }

    private void loadTagsInternal() {
        _tags.postValue(Loadable.Loading.getInstance());
        // getAllTagsUseCase().execute() возвращает LiveData, подписываемся на него
        LiveData<List<Tag>> tagsLiveData = getAllTagsUseCase.execute();
        // Используем MediatorLiveData или observeForever (с осторожностью)
        // Здесь для простоты примера сделаем observeForever, но в реальном проекте лучше через MediatorLiveData
        // или трансформировать LiveData в ViewModel
        tagsLiveData.observeForever(tagsList -> { // ВАЖНО: не забыть отписаться в onCleared
            if (tagsList != null) {
                logger.debug(TAG, "Loaded " + tagsList.size() + " tags.");
                _tags.postValue(new Loadable.Success<>(tagsList));
                if (isEditMode && _uiState.getValue() != null && !_uiState.getValue().isLoading()) {
                    updateSelectedTagsFromLoadedList(tagsList);
                }
            } else {
                // Обработка случая, когда LiveData вернул null (хотя getAllTagsUseCase должен вернуть пустой список)
                logger.error(TAG, "Failed to load tags, LiveData returned null.");
                _tags.postValue(new Loadable.Error<>(new IOException("Failed to load tags")));
            }
        });
        // TODO: в onCleared() удалить observer с tagsLiveData.removeObserver(...)
    }


    private void loadExistingTaskInternal(long id) {
        logger.debug(TAG, "Loading existing task with id=" + id);
        updateUiState(state -> state.copy(null, null, null, null, true, null, null, null));

        ListenableFuture<TaskInput> future = getTaskInputForEditUseCase.executeAsync(id);
        Futures.addCallback(future, new FutureCallback<TaskInput>() {
            @Override
            public void onSuccess(TaskInput taskInput) {
                updateUiState(state -> state.copy(taskInput, null, null, null, false, null, null, null));
                logger.info(TAG, "Task " + id + " loaded successfully for editing.");
                Loadable<List<Tag>> tagsLoadable = _tags.getValue();
                if (tagsLoadable instanceof Loadable.Success) {
                    updateSelectedTagsFromLoadedList(((Loadable.Success<List<Tag>>) tagsLoadable).getData());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                updateUiState(state -> state.copy(null, null, null, null, false, toUserFriendlyMessage(t), null, null));
                logger.error(TAG, "Failed to load task " + id + " for editing.", t);
            }
        }, MoreExecutors.directExecutor()); // Выполняем коллбэк в том же потоке, что и future (ioExecutor)
    }

    private void updateSelectedTagsFromLoadedList(List<Tag> allTags) {
        TaskCreationState currentState = _uiState.getValue();
        if (currentState == null || currentState.getTaskInput().getId() == null || !Objects.equals(currentState.getTaskInput().getId(), taskId)) return;

        Set<Long> selectedTagIds = currentState.getTaskInput().getSelectedTags().stream().map(Tag::getId).collect(Collectors.toSet());
        Set<Tag> fullSelectedTags = allTags.stream().filter(tag -> selectedTagIds.contains(tag.getId())).collect(Collectors.toSet());

        if (!fullSelectedTags.equals(currentState.getTaskInput().getSelectedTags())) {
            logger.debug(TAG, "Updating selected tags with full objects.");
            updateUiState(state -> state.copy(state.getTaskInput().copy(
                    null, null, null, null, null, fullSelectedTags
            ), null, null, null, null, null, null, null));
        }
    }

    private void updateUiState(UiStateUpdater updater) {
        TaskCreationState currentState = _uiState.getValue();
        if (currentState != null) {
            _uiState.postValue(updater.update(currentState));
        }
    }

    @FunctionalInterface
    interface UiStateUpdater {
        TaskCreationState update(TaskCreationState currentState);
    }

    // --- Обновление полей ---
    private void updateTaskInput(TaskInputUpdater updater) {
        updateUiState(state -> state.copy(updater.update(state.getTaskInput()), null, null, null, null, null, null, null));
    }

    @FunctionalInterface
    interface TaskInputUpdater {
        TaskInput update(TaskInput currentInput);
    }

    public void updateTitle(String newTitle) { updateTaskInput(input -> { input.setTitle(newTitle); return input; }); }
    public void updateDescription(String newDescription) { updateTaskInput(input -> { input.setDescription(newDescription); return input; }); }

    public void setDueDate(LocalDate newDate) {
        TaskInput currentInput = Objects.requireNonNull(_uiState.getValue()).getTaskInput();
        LocalTime currentTime = currentInput.getDueDate().toLocalTime();
        updateDateTime(LocalDateTime.of(newDate, currentTime));
        closeDateDialog();
    }

    public void setDueTime(LocalTime newTime) {
        TaskInput currentInput = Objects.requireNonNull(_uiState.getValue()).getTaskInput();
        LocalDate currentDate = currentInput.getDueDate().toLocalDate();
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
    private void updateDialogState(Boolean isDateOpen, Boolean isTimeOpen, Boolean isRecurrenceOpen) {
        updateUiState(state -> state.copy(null, isDateOpen, isTimeOpen, isRecurrenceOpen, null, null, null, null));
    }
    public void openDateDialog() { updateDialogState(true, null, null); }
    public void closeDateDialog() { updateDialogState(false, null, null); }
    public void openTimeDialog() { updateDialogState(null, true, null); }
    public void closeTimeDialog() { updateDialogState(null, false, null); }
    public void openRecurrenceDialog() { updateDialogState(null, null, true); }
    public void closeRecurrenceDialog() { updateDialogState(null, null, false); }

    // --- Теги ---
    public void toggleTagSelection(Tag tag) {
        updateTaskInput(taskInput -> {
            Set<Tag> currentTags = new HashSet<>(taskInput.getSelectedTags()); // Копируем для изменения
            boolean removed = currentTags.removeIf(t -> t.getId() == tag.getId());
            if (!removed) {
                Loadable<List<Tag>> tagsLoadable = _tags.getValue();
                if (tagsLoadable instanceof Loadable.Success) {
                    Tag fullTag = ((Loadable.Success<List<Tag>>) tagsLoadable).getData().stream()
                            .filter(t -> t.getId() == tag.getId()).findFirst().orElse(tag);
                    currentTags.add(fullTag);
                } else {
                    currentTags.add(tag); // Если теги не загружены, добавляем что есть
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
        Loadable<List<Tag>> tagsLoadable = _tags.getValue();
        if (tagsLoadable instanceof Loadable.Success) {
            boolean exists = ((Loadable.Success<List<Tag>>) tagsLoadable).getData().stream()
                    .anyMatch(t -> t.getName().equalsIgnoreCase(tagName.trim()));
            if (exists) {
                updateUiState(state -> state.copy(null, null, null, null, null, "Тег с таким именем уже существует", null, null));
                return;
            }
        }

        defaultExecutor.execute(() -> {
            logger.debug(TAG, "Adding tag: " + tagName);
            // AddTagUseCase теперь возвращает ListenableFuture
            ListenableFuture<Long> future = addTagUseCase.execute(tagName);
            Futures.addCallback(future, new FutureCallback<Long>() {
                @Override
                public void onSuccess(Long result) { /* Теги обновятся через LiveData из getAllTagsUseCase */ }
                @Override
                public void onFailure(Throwable t) {
                    updateUiState(state -> state.copy(null, null, null, null, null, toUserFriendlyMessage(t), null, null));
                    logger.error(TAG, "Failed to add tag '" + tagName + "'", t);
                }
            }, MoreExecutors.directExecutor());
        });
    }

    public void deleteSelectedTags() {
        TaskCreationState currentState = _uiState.getValue();
        if (currentState == null || currentState.getTaskInput().getSelectedTags().isEmpty()) return;

        List<Tag> tagsToDelete = new ArrayList<>(currentState.getTaskInput().getSelectedTags());
        updateTaskInput(input -> { input.setSelectedTags(Collections.emptySet()); return input; }); // Оптимистичное обновление UI

        defaultExecutor.execute(() -> {
            logger.debug(TAG, "Deleting selected tags: " + tagsToDelete.stream().map(Tag::getId).collect(Collectors.toList()));
            ListenableFuture<Void> future = deleteTagsUseCase.execute(tagsToDelete);
            Futures.addCallback(future, new FutureCallback<Void>() {
                @Override
                public void onSuccess(Void result) { /* Теги обновятся */ }
                @Override
                public void onFailure(Throwable t) {
                    updateUiState(state -> state.copy(null, null, null, null, null, toUserFriendlyMessage(t), null, null));
                    logger.error(TAG, "Failed to delete selected tags", t);
                    loadTagsInternal(); // Откат UI - перезагрузка тегов
                }
            }, MoreExecutors.directExecutor());
        });
    }

    // --- Сохранение ---
    public void saveTask() {
        if (!validateInput()) return;
        TaskCreationState currentState = Objects.requireNonNull(_uiState.getValue());
        updateUiState(state -> state.copy(null, null, null, null, true, null, null, null));
        TaskInput taskInputToSave = currentState.getTaskInput();

        logger.debug(TAG, "Saving task (Edit mode: " + isEditMode + "): " + taskInputToSave.getTitle());

        ListenableFuture<?> future;
        if (isEditMode) {
            if (taskId == null) {
                logger.error(TAG, "Task ID is null in edit mode!");
                updateUiState(state -> state.copy(null, null, null, null, false, "Ошибка: ID задачи отсутствует.", null, null));
                return;
            }
            // taskInputToSave уже содержит ID, если он был загружен
            future = updateCalendarTaskUseCase.execute(taskInputToSave);
        } else {
            future = createCalendarTaskUseCase.execute(taskInputToSave);
        }

        Futures.addCallback(future, new FutureCallback<Object>() {
            @Override
            public void onSuccess(Object result) {
                TaskCreationEvent event = isEditMode ? TaskCreationEvent.TASK_UPDATED : TaskCreationEvent.TASK_CREATED;
                logger.info(TAG, "Task save/update successful. Event: " + event);
                updateUiState(state -> state.copy(null, null, null, null, false, null, event, null));
            }

            @Override
            public void onFailure(Throwable t) {
                logger.error(TAG, "Failed to save/update task", t);
                updateUiState(state -> state.copy(null, null, null, null, false, toUserFriendlyMessage(t), null, null));
            }
        }, MoreExecutors.directExecutor()); // или defaultExecutor, если коллбэк может быть долгим
    }


    // --- Управление состоянием ---
    public void clearError() { updateUiState(state -> state.copy(null, null, null, null, null, null, null, null)); }
    public void clearEvent() { updateUiState(state -> state.copy(null, null, null, null, null, null, null, null)); }

    private boolean validateInput() {
        TaskCreationState currentState = _uiState.getValue();
        if (currentState == null || currentState.getTaskInput().getTitle() == null || currentState.getTaskInput().getTitle().trim().isEmpty()) {
            updateUiState(state -> state.copy(null, null, null, null, null, "Заголовок не может быть пустым", null, null));
            return false;
        }
        return true;
    }

    public String getScreenTitle() { return isEditMode ? "Редактирование задачи" : "Новая задача"; }
    public String getSaveButtonText() { return isEditMode ? "Обновить задачу" : "Создать задачу"; }

    private String toUserFriendlyMessage(Throwable t) {
        if (t instanceof IOException) return "Ошибка сети. Проверьте подключение.";
        if (t instanceof IllegalArgumentException) return "Некорректные данные: " + t.getMessage();
        if (t instanceof NoSuchElementException) return "Запрашиваемый элемент не найден.";
        if (t instanceof IllegalStateException) return "Ошибка состояния: " + t.getMessage();
        return "Произошла неизвестная ошибка. Попробуйте снова.";
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // TODO: Отписаться от LiveData тегов, если использовался observeForever
        // Например, если tagsLiveData была сохранена как поле:
        // getAllTagsUseCase.execute().removeObserver(tagsObserver);
        logger.debug(TAG, "ViewModel cleared.");
    }
}