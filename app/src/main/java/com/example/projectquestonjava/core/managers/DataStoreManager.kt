// File: A:\Progects\ProjectQuestOnJava\app\src\main\kotlin\com\example\projectquestonjava\core\managers\DataStoreManager.kt
package com.example.projectquestonjava.core.managers

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.example.projectquestonjava.core.di.IODispatcher
import com.example.projectquestonjava.core.utils.Logger
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreManager @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val logger: Logger,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val applicationScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    fun <T : Any> getPreferenceFlow(key: Preferences.Key<T>, defaultValue: T): Flow<T> =
        dataStore.data
            .catch { exception ->
                logger.error("DataStoreManager", "Error reading preferences for key: ${key.name}", exception)
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[key] ?: defaultValue
            }

    fun <T : Any> getPreferenceLiveData(key: Preferences.Key<T>, defaultValue: T): LiveData<T> =
        getPreferenceFlow(key, defaultValue).asLiveData(ioDispatcher)

    suspend fun <T : Any> getValue(key: Preferences.Key<T>, defaultValue: T): T =
        withContext(ioDispatcher) {
            getPreferenceFlow(key, defaultValue).first()
        }

    fun <T : Any> getValueFuture(key: Preferences.Key<T>, defaultValue: T): ListenableFuture<T> {
        return applicationScope.future(ioDispatcher) {
            getPreferenceFlow(key, defaultValue).first()
        }
    }

    suspend fun <T> saveValue(key: Preferences.Key<T>, value: T): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                dataStore.edit { preferences ->
                    preferences[key] = value
                }
                logger.debug("DataStoreManager", "Value updated: ${key.name} -> $value")
            }.onFailure { e ->
                logger.error("DataStoreManager", "Failed to save value for key: ${key.name}", e)
            }
        }

    suspend fun <T> clearValue(key: Preferences.Key<T>): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                dataStore.edit { preferences ->
                    preferences.remove(key)
                }
                logger.debug("DataStoreManager", "Value cleared: ${key.name}")
            }.onFailure { e ->
                logger.error("DataStoreManager", "Failed to clear value for key: ${key.name}", e)
            }
        }

    // --- Java-friendly обертки ---
    @Suppress("UNCHECKED_CAST")
    fun saveValueFuture(key: Preferences.Key<Boolean>, value: Boolean): ListenableFuture<Void> {
        return applicationScope.future(ioDispatcher) {
            saveValue(key, value).getOrThrow()
            null // Явно возвращаем null для Void?
        } as ListenableFuture<Void>
    }

    @Suppress("UNCHECKED_CAST")
    fun saveValueFuture(key: Preferences.Key<Int>, value: Int): ListenableFuture<Void> {
        return applicationScope.future(ioDispatcher) {
            saveValue(key, value).getOrThrow()
            null
        } as ListenableFuture<Void>
    }

    @Suppress("UNCHECKED_CAST")
    fun saveValueFuture(key: Preferences.Key<Long>, value: Long): ListenableFuture<Void> {
        return applicationScope.future(ioDispatcher) {
            saveValue(key, value).getOrThrow()
            null
        } as ListenableFuture<Void>
    }

    @Suppress("UNCHECKED_CAST")
    fun saveValueFuture(key: Preferences.Key<Float>, value: Float): ListenableFuture<Void> {
        return applicationScope.future(ioDispatcher) {
            saveValue(key, value).getOrThrow()
            null
        } as ListenableFuture<Void>
    }

    @Suppress("UNCHECKED_CAST")
    fun saveValueFuture(key: Preferences.Key<String>, value: String): ListenableFuture<Void> {
        return applicationScope.future(ioDispatcher) {
            saveValue(key, value).getOrThrow()
            null
        } as ListenableFuture<Void>
    }

    @Suppress("UNCHECKED_CAST")
    fun saveValueFuture(key: Preferences.Key<Set<String>>, value: Set<String>): ListenableFuture<Void> {
        return applicationScope.future(ioDispatcher) {
            saveValue(key, value).getOrThrow()
            null
        } as ListenableFuture<Void>
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> clearValueFuture(key: Preferences.Key<T>): ListenableFuture<Void> {
        return applicationScope.future(ioDispatcher) {
            clearValue(key).getOrThrow()
            null
        } as ListenableFuture<Void>
    }
}