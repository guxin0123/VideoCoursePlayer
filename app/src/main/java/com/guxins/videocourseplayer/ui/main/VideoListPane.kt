package com.guxins.videocourseplayer.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.guxins.videocourseplayer.data.media.ThumbnailLoader
import com.guxins.videocourseplayer.data.model.VideoFilter
import com.guxins.videocourseplayer.data.model.VideoItem
import com.guxins.videocourseplayer.ui.AppUiState
import com.guxins.videocourseplayer.ui.components.UriThumbnail
import com.guxins.videocourseplayer.util.formatDuration
import com.guxins.videocourseplayer.util.formatSize

@Composable
fun VideoListPane(
    state: AppUiState,
    thumbnailLoader: ThumbnailLoader,
    onPlay: (VideoItem) -> Unit,
    onTagClick: (VideoItem) -> Unit,
    onSetFilter: (VideoFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (state.videos.isNotEmpty()) {
            TagFilterBar(
                totalCount = state.videos.size,
                untaggedCount = state.videos.count { it.tags.isEmpty() },
                tags = state.tagSummaries,
                filter = state.filter,
                onFilterChange = onSetFilter,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
        if (state.isLoadingVideos && state.videos.isNotEmpty()) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoadingVideos && state.videos.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.videos.isEmpty() -> {
                    EmptyListState(
                        title = "这个相册里没有视频",
                        subtitle = "可以返回上一个页面重新选择相册。",
                    )
                }

                state.visibleVideos.isEmpty() -> {
                    EmptyListState(
                        title = "没有符合筛选条件的视频",
                        subtitle = "换一个标签，或清除当前筛选。",
                        action = {
                            TextButton(onClick = { onSetFilter(VideoFilter.All) }) {
                                Text("清除筛选")
                            }
                        },
                    )
                }

                else -> {
                    val listState = rememberLazyListState()
                    LaunchedEffect(state.currentVideoKey, state.visibleVideos) {
                        val key = state.currentVideoKey ?: return@LaunchedEffect
                        val index = state.visibleVideos.indexOfFirst { it.key == key }
                        if (index >= 0) {
                            listState.animateScrollToItem(index)
                        }
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        items(state.visibleVideos, key = { it.key }) { video ->
                            VideoRow(
                                video = video,
                                selected = state.currentVideoKey == video.key,
                                isPlaying = state.isPlaying && state.currentVideoKey == video.key,
                                thumbnailLoader = thumbnailLoader,
                                onClick = { onPlay(video) },
                                onTagClick = { onTagClick(video) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoRow(
    video: VideoItem,
    selected: Boolean,
    isPlaying: Boolean,
    thumbnailLoader: ThumbnailLoader,
    onClick: () -> Unit,
    onTagClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        } else {
            MaterialTheme.colorScheme.surface
        },
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            UriThumbnail(
                loader = thumbnailLoader,
                uri = video.uri,
                cacheKey = "video:${video.key}",
                updatedAt = video.lastModified,
                modifier = Modifier
                    .width(106.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp)),
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = video.displayName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    IconButton(
                        onClick = onTagClick,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "编辑标签",
                            modifier = Modifier.size(17.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                val metadata = remember(video.durationMs, video.sizeBytes) {
                    listOf(formatDuration(video.durationMs), formatSize(video.sizeBytes))
                        .filter { it.isNotBlank() }
                        .joinToString(" · ")
                }
                Text(
                    text = if (isPlaying) "播放中 · $metadata" else metadata,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isPlaying) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.size(2.dp))
                val tagLabel = remember(video.tags) {
                    if (video.tags.isEmpty()) {
                        "+ 添加标签"
                    } else {
                        video.tags.take(2).joinToString(" · ") +
                            if (video.tags.size > 2) "  +${video.tags.size - 2}" else ""
                    }
                }
                Text(
                    text = tagLabel,
                    modifier = Modifier
                        .clickable(onClick = onTagClick)
                        .padding(vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (video.tags.isEmpty()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EmptyListState(
    title: String,
    subtitle: String,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = title,
            modifier = Modifier.padding(top = 10.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = subtitle,
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        action?.invoke()
    }
}
