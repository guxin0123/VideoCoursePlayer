package com.guxins.videocourseplayer.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class TagRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val tags: Flow<Map<String, List<String>>> = dataStore.safeData()
        .map { preferences -> TagJsonCodec.decode(preferences[Keys.TAGS_JSON]) }
        .distinctUntilChanged()

    suspend fun setTags(videoKey: String, tags: List<String>) {
        val sanitized = TagRules.sanitize(tags)
        dataStore.edit { preferences ->
            val current = TagJsonCodec.decode(preferences[Keys.TAGS_JSON]).toMutableMap()
            if (sanitized.isEmpty()) {
                current.remove(videoKey)
            } else {
                current[videoKey] = sanitized
            }
            preferences[Keys.TAGS_JSON] = TagJsonCodec.encode(current)
            preferences[Keys.TAGS_SCHEMA_VERSION] = 1
        }
    }

    private object Keys {
        val TAGS_JSON = stringPreferencesKey("tags_json")
        val TAGS_SCHEMA_VERSION = intPreferencesKey("tags_schema_version")
    }
}
