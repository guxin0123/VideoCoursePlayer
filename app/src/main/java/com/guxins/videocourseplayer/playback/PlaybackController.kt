package com.guxins.videocourseplayer.playback

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.guxins.videocourseplayer.data.model.PlayMode
import com.guxins.videocourseplayer.data.model.VideoItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PlaybackSnapshot(
    val currentVideoKey: String? = null,
    val isPlaying: Boolean = false,
    val isEnded: Boolean = false,
)

class PlaybackController(context: Context) {
    val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            true,
        )
        setHandleAudioBecomingNoisy(true)
    }

    private val _snapshot = MutableStateFlow(PlaybackSnapshot())
    val snapshot: StateFlow<PlaybackSnapshot> = _snapshot.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _snapshot.update {
                    it.copy(
                        currentVideoKey = mediaItem?.mediaId,
                        isEnded = false,
                    )
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _snapshot.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    _snapshot.update { it.copy(isPlaying = false, isEnded = true) }
                } else if (playbackState == Player.STATE_READY) {
                    _snapshot.update { it.copy(isEnded = false) }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _snapshot.update { it.copy(isPlaying = false) }
                _errors.tryEmit(error.toUserMessage())
            }
        })
    }

    fun play(
        queue: List<VideoItem>,
        startIndex: Int,
        mode: PlayMode,
    ) {
        if (queue.isEmpty()) return
        val safeIndex = startIndex.coerceIn(queue.indices)
        val mediaItems = queue.map { video ->
            MediaItem.Builder()
                .setUri(video.uri)
                .setMediaId(video.key)
                .setTag(video.displayName)
                .build()
        }
        player.setMediaItems(mediaItems, safeIndex, 0L)
        player.repeatMode = mode.repeatMode
        player.prepare()
        player.play()
    }

    fun setMode(mode: PlayMode) {
        player.repeatMode = mode.repeatMode
    }

    fun pause() {
        if (player.isPlaying) {
            player.pause()
        }
    }

    fun stop() {
        player.stop()
        player.clearMediaItems()
        _snapshot.value = PlaybackSnapshot()
    }

    fun release() {
        player.release()
    }

    private val PlayMode.repeatMode: Int
        get() = when (this) {
            PlayMode.SEQUENTIAL -> Player.REPEAT_MODE_OFF
            PlayMode.SINGLE_LOOP -> Player.REPEAT_MODE_ONE
        }

    private fun PlaybackException.toUserMessage(): String = when (errorCode) {
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "视频文件不存在或已被移动"
        PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> "没有权限读取该视频"
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        -> "当前设备无法解码该视频格式"
        else -> "视频播放失败，请尝试其他视频"
    }
}
