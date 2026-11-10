package com.guxins.videocourseplayer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.guxins.videocourseplayer.AppContainer
import com.guxins.videocourseplayer.data.model.Album
import com.guxins.videocourseplayer.data.model.PlayMode
import com.guxins.videocourseplayer.data.model.TagSummary
import com.guxins.videocourseplayer.data.model.VideoFilter
import com.guxins.videocourseplayer.data.model.VideoItem
import com.guxins.videocourseplayer.data.model.VideoPermissionState
import com.guxins.videocourseplayer.data.prefs.TagRules
import com.guxins.videocourseplayer.data.prefs.VideoFilters
import com.guxins.videocourseplayer.playback.PlaybackController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUiState(
    val permission: VideoPermissionState = VideoPermissionState.UNKNOWN,
    val isBootstrapping: Boolean = true,
    val albums: List<Album> = emptyList(),
    val selectedAlbum: Album? = null,
    val videos: List<VideoItem> = emptyList(),
    val visibleVideos: List<VideoItem> = emptyList(),
    val tagSummaries: List<TagSummary> = emptyList(),
    val filter: VideoFilter = VideoFilter.All,
    val playMode: PlayMode = PlayMode.SEQUENTIAL,
    val currentVideoKey: String? = null,
    val isPlaying: Boolean = false,
    val isLoadingAlbums: Boolean = false,
    val isLoadingVideos: Boolean = false,
) {
    val currentVideo: VideoItem?
        get() = videos.firstOrNull { it.key == currentVideoKey }

    val isLimitedAccess: Boolean
        get() = permission == VideoPermissionState.LIMITED
}

class AppViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val messageChannel = Channel<String>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()

    private val playback = PlaybackController(container.applicationContext)

    val player get() = playback.player

    private var rawVideos: List<VideoItem> = emptyList()
    private var tagsByVideo: Map<String, List<String>> = emptyMap()
    private var loadedPermission: VideoPermissionState? = null
    private var bootstrapJob: Job? = null
    private var videosJob: Job? = null

    init {
        viewModelScope.launch {
            container.preferences.playMode.collect { mode ->
                _uiState.update { it.copy(playMode = mode) }
                playback.setMode(mode)
            }
        }
        viewModelScope.launch {
            container.tagRepository.tags.collect { tags ->
                tagsByVideo = tags
                refreshTaggedVideos()
            }
        }
        viewModelScope.launch {
            playback.snapshot.collect { snapshot ->
                _uiState.update {
                    it.copy(
                        currentVideoKey = snapshot.currentVideoKey,
                        isPlaying = snapshot.isPlaying,
                    )
                }
            }
        }
        viewModelScope.launch {
            playback.errors.collect(::sendMessage)
        }
    }

    fun onPermissionState(permission: VideoPermissionState) {
        val previous = _uiState.value.permission
        if (permission == previous) {
            if (!permission.isGranted || loadedPermission == permission) return
        }

        _uiState.update { it.copy(permission = permission) }
        if (!permission.isGranted) {
            loadedPermission = null
            bootstrapJob?.cancel()
            videosJob?.cancel()
            playback.stop()
            rawVideos = emptyList()
            tagsByVideo = emptyMap()
            _uiState.update {
                it.copy(
                    isBootstrapping = false,
                    albums = emptyList(),
                    selectedAlbum = null,
                    videos = emptyList(),
                    visibleVideos = emptyList(),
                    tagSummaries = emptyList(),
                    filter = VideoFilter.All,
                    currentVideoKey = null,
                    isPlaying = false,
                    isLoadingAlbums = false,
                    isLoadingVideos = false,
                )
            }
            return
        }

        loadedPermission = permission
        bootstrap()
    }

    fun refresh() {
        val selected = _uiState.value.selectedAlbum
        if (selected != null) {
            loadVideos(selected)
        } else {
            reloadAlbums()
        }
    }

    fun selectAlbum(album: Album) {
        viewModelScope.launch {
            runCatching { container.preferences.setSelectedAlbumKey(album.key) }
                .onFailure { sendMessage("无法保存相册选择") }
        }
        loadVideos(album)
    }

    fun changeAlbum() {
        playback.stop()
        rawVideos = emptyList()
        videosJob?.cancel()
        _uiState.update {
            it.copy(
                selectedAlbum = null,
                videos = emptyList(),
                visibleVideos = emptyList(),
                tagSummaries = emptyList(),
                filter = VideoFilter.All,
                currentVideoKey = null,
                isPlaying = false,
                isLoadingVideos = false,
            )
        }
        viewModelScope.launch {
            runCatching { container.preferences.clearSelectedAlbumKey() }
                .onFailure { sendMessage("无法清除相册选择") }
        }
        reloadAlbums()
    }

    fun setFilter(filter: VideoFilter) {
        if (filter is VideoFilter.ByTag && _uiState.value.tagSummaries.none { it.name == filter.tag }) {
            return
        }
        val visible = VideoFilters.apply(_uiState.value.videos, filter)
        _uiState.update { it.copy(filter = filter, visibleVideos = visible) }
    }

    fun play(video: VideoItem) {
        val queue = _uiState.value.visibleVideos
        val index = queue.indexOfFirst { it.key == video.key }
        if (index < 0) return
        playback.play(queue, index, _uiState.value.playMode)
    }

    fun setPlayMode(mode: PlayMode) {
        if (mode == _uiState.value.playMode) return
        _uiState.update { it.copy(playMode = mode) }
        playback.setMode(mode)
        viewModelScope.launch {
            runCatching { container.preferences.setPlayMode(mode) }
                .onFailure { sendMessage("无法保存播放模式") }
        }
    }

    fun setVideoTags(video: VideoItem, tags: List<String>) {
        val sanitized = TagRules.sanitize(tags)
        viewModelScope.launch {
            runCatching { container.tagRepository.setTags(video.key, sanitized) }
                .onSuccess {
                    sendMessage(if (sanitized.isEmpty()) "已清除标签" else "标签已更新")
                }
                .onFailure { sendMessage("保存标签失败") }
        }
    }

    fun pauseForBackground() {
        playback.pause()
    }

    override fun onCleared() {
        playback.release()
        super.onCleared()
    }

    private fun bootstrap() {
        bootstrapJob?.cancel()
        videosJob?.cancel()
        bootstrapJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBootstrapping = true,
                    isLoadingAlbums = true,
                    isLoadingVideos = false,
                )
            }
            val albumsResult = runCatching { container.mediaRepository.getAlbums() }
            if (albumsResult.isFailure) {
                sendMessage("读取相册失败，请检查权限后重试")
                _uiState.update {
                    it.copy(
                        isBootstrapping = false,
                        isLoadingAlbums = false,
                        albums = emptyList(),
                        selectedAlbum = null,
                    )
                }
                return@launch
            }

            val albums = albumsResult.getOrThrow()
            val selectedKey = runCatching {
                container.preferences.selectedAlbumKey.first()
            }.getOrNull()
            val selected = albums.firstOrNull { it.key == selectedKey }

            if (selected == null) {
                rawVideos = emptyList()
                playback.stop()
                if (selectedKey != null) {
                    if (_uiState.value.permission == VideoPermissionState.LIMITED) {
                        sendMessage("当前仅能访问部分视频，暂时找不到上次的相册")
                    } else {
                        runCatching { container.preferences.clearSelectedAlbumKey() }
                        sendMessage("上次选择的相册不可用，请重新选择")
                    }
                }
                _uiState.update {
                    it.copy(
                        albums = albums,
                        selectedAlbum = null,
                        videos = emptyList(),
                        visibleVideos = emptyList(),
                        tagSummaries = emptyList(),
                        filter = VideoFilter.All,
                        currentVideoKey = null,
                        isPlaying = false,
                        isBootstrapping = false,
                        isLoadingAlbums = false,
                        isLoadingVideos = false,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        albums = albums,
                        selectedAlbum = selected,
                        isBootstrapping = false,
                        isLoadingAlbums = false,
                    )
                }
                loadVideos(selected)
            }
        }
    }

    private fun reloadAlbums() {
        bootstrapJob?.cancel()
        bootstrapJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAlbums = true) }
            try {
                val albums = container.mediaRepository.getAlbums()
                _uiState.update { it.copy(albums = albums, isLoadingAlbums = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingAlbums = false) }
                sendMessage("读取相册失败，请稍后重试")
            }
        }
    }

    private fun loadVideos(album: Album) {
        videosJob?.cancel()
        val albumChanged = _uiState.value.selectedAlbum?.key != album.key
        if (albumChanged) {
            playback.stop()
            rawVideos = emptyList()
        }
        _uiState.update {
            it.copy(
                selectedAlbum = album,
                isLoadingVideos = true,
                filter = if (albumChanged) VideoFilter.All else it.filter,
                videos = if (albumChanged) emptyList() else it.videos,
                visibleVideos = if (albumChanged) emptyList() else it.visibleVideos,
                tagSummaries = if (albumChanged) emptyList() else it.tagSummaries,
                currentVideoKey = if (albumChanged) null else it.currentVideoKey,
                isPlaying = if (albumChanged) false else it.isPlaying,
            )
        }

        videosJob = viewModelScope.launch {
            try {
                val videos = container.mediaRepository.getVideos(album)
                if (_uiState.value.selectedAlbum?.key != album.key) return@launch
                rawVideos = videos
                refreshTaggedVideos()
                _uiState.update { it.copy(isLoadingVideos = false) }
                val currentKey = _uiState.value.currentVideoKey
                if (currentKey != null && videos.none { it.key == currentKey }) {
                    playback.stop()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                rawVideos = emptyList()
                refreshTaggedVideos()
                _uiState.update { it.copy(isLoadingVideos = false) }
                sendMessage("读取视频失败，请检查权限后重试")
            }
        }
    }

    private fun refreshTaggedVideos() {
        val tagged = rawVideos.map { video ->
            video.copy(tags = tagsByVideo[video.key].orEmpty())
        }
        val summaries = VideoFilters.summarizeTags(tagged)
        val oldFilter = _uiState.value.filter
        val filter = if (oldFilter is VideoFilter.ByTag &&
            summaries.none { it.name == oldFilter.tag }
        ) {
            if (rawVideos.isNotEmpty()) {
                sendMessage("标签“${oldFilter.tag}”已不存在，已显示全部视频")
            }
            VideoFilter.All
        } else {
            oldFilter
        }
        _uiState.update {
            it.copy(
                videos = tagged,
                visibleVideos = VideoFilters.apply(tagged, filter),
                tagSummaries = summaries,
                filter = filter,
            )
        }
    }

    private fun sendMessage(message: String) {
        messageChannel.trySend(message)
    }

    class Factory(
        private val container: AppContainer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
                return AppViewModel(container) as T
            }
            error("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
