package com.example.projectquestonjava.core.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import com.example.projectquestonjava.core.managers.SoundManager;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import dagger.hilt.android.qualifiers.ApplicationContext;
import lombok.Getter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;
import android.media.RingtoneManager;




@Singleton
public class RingtoneUtil {
    private final Context context;
    private final SoundManager soundManager;
    private final Logger logger;
    private final File customRingtonesDir;
    private static final String TAG = "RingtoneUtil";
    private final Executor ioExecutor;

    @Inject
    public RingtoneUtil(
            @ApplicationContext Context context,
            SoundManager soundManager,
            Logger logger,
            @com.example.projectquestonjava.core.di.IODispatcher Executor ioExecutor) {
        this.context = context;
        this.soundManager = soundManager;
        this.logger = logger;
        this.ioExecutor = ioExecutor;
        this.customRingtonesDir = new File(context.getFilesDir(), "ringtones");
        if (!customRingtonesDir.exists()) {
            customRingtonesDir.mkdirs();
        }
    }

    public List<RingtoneItem> loadSystemRingtones() {
        logger.debug(TAG, "Загрузка системных рингтонов");
        List<RingtoneItem> list = new ArrayList<>();
        RingtoneManager manager = new RingtoneManager(context);
        manager.setType(RingtoneManager.TYPE_ALARM); // или TYPE_NOTIFICATION, TYPE_RINGTONE
        Cursor cursor = manager.getCursor();
        if (cursor == null) {
            logger.warn(TAG, "Cursor for system ringtones is null.");
            return Collections.emptyList();
        }
        try {
            while (cursor.moveToNext()) {
                String title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
                Uri uri = manager.getRingtoneUri(cursor.getPosition());
                list.add(new RingtoneItem(uri.toString(), title, false));
            }
        } catch (Exception e) {
            logger.error(TAG, "Ошибка загрузки системных рингтонов", e);
        } finally {
            cursor.close();
        }
        logger.info(TAG, "Загружено " + list.size() + " системных рингтонов");
        return list;
    }

    public List<RingtoneItem> loadCustomRingtones() {
        logger.debug(TAG, "Загрузка кастомных рингтонов");
        File[] files = customRingtonesDir.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        List<RingtoneItem> list = new ArrayList<>();
        for (File file : files) {
            list.add(new RingtoneItem(Uri.fromFile(file).toString(), file.getName(), true));
        }
        logger.info(TAG, "Загружено " + list.size() + " кастомных рингтонов");
        return list;
    }

    public ListenableFuture<RingtoneItem> addCustomRingtone(Uri uri) {
        return Futures.submit(() -> {
            String fileName = getFileNameFromUri(uri);
            if (fileName == null) {
                fileName = "custom_" + System.currentTimeMillis() + ".mp3"; // или другое расширение
            }
            File outputFile = new File(customRingtonesDir, fileName);
            logger.debug(TAG, "Добавление рингтона: " + fileName + " в " + outputFile.getAbsolutePath());

            try (InputStream input = context.getContentResolver().openInputStream(uri);
                 OutputStream output = new FileOutputStream(outputFile)) {
                if (input == null) {
                    throw new IOException("Не удалось открыть InputStream для URI: " + uri);
                }
                byte[] buffer = new byte[4 * 1024]; // 4k buffer
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
                output.flush();
                logger.info(TAG, "Рингтон добавлен: " + fileName);
                return new RingtoneItem(Uri.fromFile(outputFile).toString(), fileName, true);
            } catch (Exception e) {
                logger.error(TAG, "Ошибка добавления рингтона из URI: " + uri, e);
                // outputFile.delete(); // Удаляем частично созданный файл, если нужно
                throw e; // Пробрасываем для ListenableFuture
            }
        }, ioExecutor);
    }

    public ListenableFuture<Boolean> removeCustomRingtone(RingtoneItem ringtone) {
        return Futures.submit(() -> {
            if (!ringtone.isCustom() || ringtone.uri() == null) {
                logger.debug(TAG, "Попытка удалить не кастомный или невалидный рингтон.");
                return false;
            }
            File file = new File(Objects.requireNonNull(Uri.parse(ringtone.uri()).getPath()));
            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    logger.info(TAG, "Рингтон удалён: " + ringtone.title());
                } else {
                    logger.error(TAG, "Не удалось удалить рингтон: " + ringtone.title());
                }
                return deleted;
            }
            logger.warn(TAG, "Файл рингтона для удаления не найден: " + ringtone.uri());
            return false;
        }, ioExecutor);
    }

    public void previewRingtone(String uriString) {
        if (uriString == null || uriString.isEmpty()) {
            logger.warn(TAG, "Preview URI is null or empty.");
            return;
        }
        logger.debug(TAG, "Предпросмотр рингтона: " + uriString);
        try {
            soundManager.playSound(Uri.parse(uriString));
        } catch (Exception e) {
            logger.error(TAG, "Error parsing URI for preview: " + uriString, e);
        }
    }

    public void stopPreview() {
        logger.debug(TAG, "Остановка предпросмотра рингтона");
        soundManager.stop();
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (Objects.equals(uri.getScheme(), "content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (columnIndex != -1) {
                        result = cursor.getString(columnIndex);
                    }
                }
            } catch (Exception e) {
                logger.error(TAG, "Ошибка получения имени файла из content URI", e);
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result;
    }
}