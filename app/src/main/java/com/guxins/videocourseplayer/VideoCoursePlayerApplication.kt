package com.guxins.videocourseplayer

import android.app.Application
import android.content.Context
import com.guxins.videocourseplayer.data.media.MediaRepository
import com.guxins.videocourseplayer.data.media.MediaStoreRepository
import com.guxins.videocourseplayer.data.media.ThumbnailLoader
import com.guxins.videocourseplayer.data.prefs.AppPreferences
import com.guxins.videocourseplayer.data.prefs.TagRepository
import com.guxins.videocourseplayer.data.prefs.appDataStore

class VideoCoursePlayerApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(context: Context) {
    val applicationContext: Context = context.applicationContext
    val mediaRepository: MediaRepository = MediaStoreRepository(applicationContext)
    val thumbnailLoader = ThumbnailLoader(applicationContext)
    val preferences = AppPreferences(applicationContext.appDataStore)
    val tagRepository = TagRepository(applicationContext.appDataStore)
}
