package com.guxins.videocourseplayer.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.guxins.videocourseplayer.data.model.TagSummary
import com.guxins.videocourseplayer.data.model.VideoFilter

@Composable
fun TagFilterBar(
    totalCount: Int,
    untaggedCount: Int,
    tags: List<TagSummary>,
    filter: VideoFilter,
    onFilterChange: (VideoFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "all") {
            FilterChip(
                selected = filter == VideoFilter.All,
                onClick = { onFilterChange(VideoFilter.All) },
                label = { Text("全部 · $totalCount") },
            )
        }
        item(key = "untagged") {
            FilterChip(
                selected = filter == VideoFilter.Untagged,
                onClick = { onFilterChange(VideoFilter.Untagged) },
                label = { Text("未标记 · $untaggedCount") },
            )
        }
        items(tags, key = { it.name }) { tag ->
            FilterChip(
                selected = filter is VideoFilter.ByTag && filter.tag == tag.name,
                onClick = { onFilterChange(VideoFilter.ByTag(tag.name)) },
                label = { Text("${tag.name} · ${tag.count}") },
            )
        }
    }
}
