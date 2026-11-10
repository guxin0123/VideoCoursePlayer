package com.guxins.videocourseplayer.ui.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.guxins.videocourseplayer.data.media.ThumbnailLoader
import com.guxins.videocourseplayer.data.model.Album
import com.guxins.videocourseplayer.ui.AppUiState
import com.guxins.videocourseplayer.ui.components.LimitedAccessBanner
import com.guxins.videocourseplayer.ui.components.UriThumbnail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumPickerScreen(
    state: AppUiState,
    thumbnailLoader: ThumbnailLoader,
    onSelectAlbum: (Album) -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("选择视频相册", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "只显示本机视频",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "刷新相册")
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
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoadingAlbums && state.albums.isEmpty() -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    state.albums.isEmpty() -> {
                        EmptyAlbums(onRefresh = onRefresh, onOpenSettings = onOpenSettings)
                    }

                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 240.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(state.albums, key = { it.key }) { album ->
                                AlbumCard(
                                    album = album,
                                    thumbnailLoader = thumbnailLoader,
                                    onClick = { onSelectAlbum(album) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumCard(
    album: Album,
    thumbnailLoader: ThumbnailLoader,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        UriThumbnail(
            loader = thumbnailLoader,
            uri = album.coverUri,
            cacheKey = "album:${album.key}",
            updatedAt = album.coverModified,
            widthPx = 480,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        )
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${album.videoCount} 个视频",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val path = album.relativePath?.trim()?.trimEnd('/')
            if (!path.isNullOrBlank() && path.substringAfterLast('/') != album.name) {
                Text(
                    text = path,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EmptyAlbums(
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            modifier = Modifier.padding(12.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "没有找到视频相册",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "请确认设备中已有视频，并允许应用读取视频权限。",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onRefresh, modifier = Modifier.padding(top = 8.dp)) {
            Text("重新扫描")
        }
        TextButton(onClick = onOpenSettings) {
            Text("打开系统设置")
        }
    }
}
