package com.guxins.videocourseplayer.ui.main

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color as AndroidColor
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.guxins.videocourseplayer.data.model.PlayMode

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerPane(
    player: Player,
    currentVideoName: String?,
    playMode: PlayMode,
    onSetPlayMode: (PlayMode) -> Unit,
    fillHeight: Boolean,
    modifier: Modifier = Modifier,
) {
    KeepScreenOnWhilePlaying(player)

    Column(
        modifier = modifier.background(Color.Black),
    ) {
        val playerModifier = if (fillHeight) {
            Modifier
                .fillMaxWidth()
                .weight(1f)
        } else {
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        }

        Box(
            modifier = playerModifier
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        useController = true
                        controllerAutoShow = true
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setBackgroundColor(AndroidColor.BLACK)
                        this.player = player
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    view.player = player
                    view.useController = currentVideoName != null
                },
                onRelease = { view ->
                    view.player = null
                    view.keepScreenOn = false
                },
            )

            if (currentVideoName == null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        tint = Color.White.copy(alpha = 0.7f),
                    )
                    Text(
                        text = "从列表中选择视频开始播放",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = currentVideoName ?: "未选择视频",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (currentVideoName == null) FontWeight.Normal else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (currentVideoName == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                FilterChip(
                    selected = playMode == PlayMode.SINGLE_LOOP,
                    onClick = { onSetPlayMode(PlayMode.SINGLE_LOOP) },
                    label = { Text("单曲循环") },
                )
                FilterChip(
                    selected = playMode == PlayMode.SEQUENTIAL,
                    onClick = { onSetPlayMode(PlayMode.SEQUENTIAL) },
                    label = { Text("顺序播放") },
                )
            }
        }
    }
}

/**
 * 播放时让屏幕保持常亮，暂停、播放结束、出错或离开播放页时立即解除常亮。
 *
 * 常亮标志直接写在宿主窗口上，并由播放器自身的事件驱动，不再依赖 UI 状态是否及时重组，
 * 避免暂停或播完后标志没有被清掉、屏幕被一直点亮。
 */
@Composable
private fun KeepScreenOnWhilePlaying(player: Player) {
    val context = LocalContext.current
    val window = remember(context) { context.findActivity()?.window }

    DisposableEffect(player, window) {
        if (window == null) {
            return@DisposableEffect onDispose { }
        }
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                window.updateKeepScreenOn(player.isPlayingOrBuffering)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                window.updateKeepScreenOn(player.isPlayingOrBuffering)
            }
        }
        player.addListener(listener)
        // 重建界面（例如旋转屏幕）时播放器可能已经在播放，先同步一次当前状态。
        window.updateKeepScreenOn(player.isPlayingOrBuffering)
        onDispose {
            player.removeListener(listener)
            window.updateKeepScreenOn(false)
        }
    }
}

/** 正在播放，或用户希望播放且仍在缓冲。播放结束、暂停、IDLE 都返回 false。 */
private val Player.isPlayingOrBuffering: Boolean
    get() = isPlaying || (playWhenReady && playbackState == Player.STATE_BUFFERING)

private fun Window.updateKeepScreenOn(keepScreenOn: Boolean) {
    if (keepScreenOn) {
        addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
        clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
