package com.guxins.videocourseplayer.data.prefs

import com.guxins.videocourseplayer.data.model.PlayMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException

class AppPreferences(
    private val dataStore: DataStore<Preferences>,
) {
    val selectedAlbumKey: Flow<String?> = dataStore.safeData()
        .map { it[Keys.SELECTED_ALBUM_KEY] }
        .distinctUntilChanged()

    val playMode: Flow<PlayMode> = dataStore.safeData()
        .map { PlayMode.fromStorage(it[Keys.PLAY_MODE]) }
        .distinctUntilChanged()

    suspend fun setSelectedAlbumKey(key: String) {
        dataStore.edit { it[Keys.SELECTED_ALBUM_KEY] = key }
    }

    suspend fun clearSelectedAlbumKey() {
        dataStore.edit { it.remove(Keys.SELECTED_ALBUM_KEY) }
    }

    suspend fun setPlayMode(mode: PlayMode) {
        dataStore.edit { it[Keys.PLAY_MODE] = mode.name }
    }

    private object Keys {
        val SELECTED_ALBUM_KEY = stringPreferencesKey("selected_album_key")
        val PLAY_MODE = stringPreferencesKey("play_mode")
    }
}

internal fun DataStore<Preferences>.safeData(): Flow<Preferences> = data
    .catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }
