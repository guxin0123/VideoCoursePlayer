package com.guxins.videocourseplayer.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore

private const val DATA_STORE_NAME = "video_course_player_settings"

val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(
    name = DATA_STORE_NAME,
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)
