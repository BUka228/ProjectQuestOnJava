package com.example.projectquestonjava.core.utils;

import android.content.Context;
import android.net.Uri;
import com.example.projectquestonjava.core.di.IODispatcher; // Убедитесь, что эта аннотация существует
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AvatarStorageHelper {
    private static final String TAG = "AvatarStorageHelper";
    private static final String AVATAR_DIR = "avatars";
    private static final String AVATAR_EXTENSION = ".jpg";

    private final Context context;
    private final Logger logger;
    private final Executor ioExecutor;
    private final File avatarDirectory;

    @Inject
    public AvatarStorageHelper(
            @ApplicationContext Context context,
            Logger logger,
            @IODispatcher Executor ioExecutor) { // Внедряем Executor
        this.context = context;
        this.logger = logger;
        this.ioExecutor = ioExecutor;
        this.avatarDirectory = new File(context.getFilesDir(), AVATAR_DIR);
        if (!this.avatarDirectory.exists()) {
            if (this.avatarDirectory.mkdirs()) {
                logger.debug(TAG, "Avatar directory created: " + this.avatarDirectory.getAbsolutePath());
            } else {
                logger.error(TAG, "Failed to create avatar directory: " + this.avatarDirectory.getAbsolutePath());
                // Можно бросить исключение, если создание директории критично
            }
        }
    }

    /**
     * Сохраняет изображение из Uri во внутреннее хранилище.
     * Перезаписывает существующий аватар для данного пользователя.
     * @param userId ID пользователя, используется для имени файла.
     * @param sourceUri Uri выбранного изображения.
     * @return ListenableFuture с абсолютным путем к сохраненному файлу или ошибкой.
     */
    public ListenableFuture<String> saveAvatar(int userId, Uri sourceUri) {
        return Futures.submit(() -> { // Используем submit, так как внутри блокирующие операции
            String fileName = "avatar_" + userId + AVATAR_EXTENSION;
            File destinationFile = new File(avatarDirectory, fileName);

            logger.debug(TAG, "Saving avatar for userId=" + userId + " from " + sourceUri + " to " + destinationFile.getAbsolutePath());

            try (InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
                 OutputStream outputStream = new FileOutputStream(destinationFile)) { // Используем try-with-resources

                if (inputStream == null) {
                    throw new IOException("Could not open input stream for URI: " + sourceUri);
                }

                byte[] buffer = new byte[4096]; // 4KB buffer
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush(); // Убедимся, что все данные записаны

                logger.info(TAG, "Avatar saved successfully for userId=" + userId + " at " + destinationFile.getAbsolutePath());
                return destinationFile.getAbsolutePath(); // Возвращаем путь к файлу
            } catch (IOException e) {
                logger.error(TAG, "Failed to save avatar for userId=" + userId, e);
                throw e; // Пробрасываем исключение, чтобы ListenableFuture завершился с ошибкой
            }
        }, ioExecutor);
    }

    /**
     * Удаляет файл аватара по указанному пути.
     * @param filePath Абсолютный путь к файлу аватара.
     * @return ListenableFuture<Void> Успех или ошибка (ошибки удаления файла не считаются критичными для Future).
     */
    public ListenableFuture<Void> deleteAvatar(String filePath) {
        return Futures.submit(() -> {
            if (filePath == null || filePath.trim().isEmpty()) {
                logger.debug(TAG, "Delete avatar skipped: file path is null or blank.");
                return null; // Для Callable<Void>
            }
            try {
                File file = new File(filePath);
                if (file.exists()) {
                    if (file.delete()) {
                        logger.info(TAG, "Successfully deleted avatar file: " + filePath);
                    } else {
                        // Не бросаем исключение, просто логируем, так как удаление старого файла не всегда критично
                        logger.warn(TAG, "Failed to delete avatar file: " + filePath + " (delete() returned false)");
                    }
                } else {
                    logger.debug(TAG, "Avatar file to delete does not exist: " + filePath);
                }
            } catch (Exception e) {
                // Логируем, но не пробрасываем, если не хотим, чтобы это прерывало основную операцию
                logger.error(TAG, "Error deleting avatar file: " + filePath, e);
            }
            return null; // Для Callable<Void>
        }, ioExecutor);
    }
}