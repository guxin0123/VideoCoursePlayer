package com.guxins.videocourseplayer.ui.main

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.guxins.videocourseplayer.data.media.ThumbnailLoader
import com.guxins.videocourseplayer.data.model.PlayMode
import com.guxins.videocourseplayer.data.model.VideoFilter
import com.guxins.videocourseplayer.data.model.VideoItem
import com.guxins.videocourseplayer.ui.AppUiState
import com.guxins.videocourseplayer.ui.components.LimitedAccessBanner
import com.guxins.videocourseplayer.ui.components.TagEditSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: AppUiState,
    thumbnailLoader: ThumbnailLoader,
    player: Player,
    onPlay: (VideoItem) -> Unit,
    onSetFilter: (VideoFilter) -> Unit,
    onSetPlayMode: (PlayMode) -> Unit,
    onSetTags: (VideoItem, List<String>) -> Unit,
    onRefresh: () -> Unit,
    onChangeAlbum: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var editingVideoKey by remember { mutableStateOf<String?>(null) }
    val editingVideo = state.videos.firstOrNull { it.key == editingVideoKey }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.selectedAlbum?.name ?: "视频课程",
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${state.videos.size} 个视频",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "刷新视频")
                    }
                    TextButton(onClick = onChangeAlbum) {
                        Text("切换相册")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.isLimitedAccess) {
                LimitedAccessBanner(onManage = onOpenSettings)
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val landscape = maxWidth > maxHeight
                if (landscape) {
                    val listWidth = (maxWidth * 0.38f).coerceIn(300.dp, 420.dp)
                    Row(modifier = Modifier.fillMaxSize()) {
                        PlayerPane(
                            player = player,
                            currentVideoName = state.currentVideo?.displayName,
                            playMode = state.playMode,
                            onSetPlayMode = onSetPlayMode,
                            fillHeight = true,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                        VerticalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        )
                        VideoListPane(
                            state = state,
                            thumbnailLoader = thumbnailLoader,
                            onPlay = onPlay,
                            onTagClick = { editingVideoKey = it.key },
                            onSetFilter = onSetFilter,
                            modifier = Modifier
                                .width(listWidth)
                                .fillMaxHeight(),
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        PlayerPane(
                            player = player,
                            currentVideoName = state.currentVideo?.displayName,
                            playMode = state.playMode,
                            onSetPlayMode = onSetPlayMode,
                            fillHeight = false,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        )
                        VideoListPane(
                            state = state,
                            thumbnailLoader = thumbnailLoader,
                            onPlay = onPlay,
                            onTagClick = { editingVideoKey = it.key },
                            onSetFilter = onSetFilter,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        )
                    }
                }
            }
        }
    }

    if (editingVideo != null) {
        TagEditSheet(
            video = editingVideo,
            availableTags = state.tagSummaries.map { it.name },
            onDismiss = { editingVideoKey = null },
            onSave = { tags ->
                onSetTags(editingVideo, tags)
                editingVideoKey = null
            },
        )
    }
}
