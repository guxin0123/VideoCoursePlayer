package com.guxins.videocourseplayer.data.prefs

import com.guxins.videocourseplayer.data.model.TagSummary
import com.guxins.videocourseplayer.data.model.VideoFilter
import com.guxins.videocourseplayer.data.model.VideoItem
import com.guxins.videocourseplayer.util.NaturalOrder

object TagRules {
    const val MAX_TAG_LENGTH = 20
    val RESERVED_NAMES = setOf("全部", "未标记")
    private val whitespace = Regex("\\s+")

    fun normalize(raw: String): String = raw.trim().replace(whitespace, " ")

    fun validationError(raw: String): String? {
        val normalized = normalize(raw)
        return when {
            normalized.isEmpty() -> "标签不能为空"
            normalized.length > MAX_TAG_LENGTH -> "标签最多 $MAX_TAG_LENGTH 个字符"
            normalized in RESERVED_NAMES -> "“$normalized”是系统保留名称"
            else -> null
        }
    }

    fun sanitize(tags: Iterable<String>): List<String> = tags
        .map(::normalize)
        .filter { it.isNotEmpty() && it.length <= MAX_TAG_LENGTH && it !in RESERVED_NAMES }
        .distinct()
}

object VideoFilters {
    fun apply(videos: List<VideoItem>, filter: VideoFilter): List<VideoItem> =
        filterAndSort(
            items = videos,
            filter = filter,
            tagsOf = VideoItem::tags,
            nameOf = VideoItem::displayName,
            keyOf = VideoItem::key,
        )

    fun <T> filterAndSort(
        items: List<T>,
        filter: VideoFilter,
        tagsOf: (T) -> List<String>,
        nameOf: (T) -> String,
        keyOf: (T) -> String,
    ): List<T> {
        val filtered = when (filter) {
            VideoFilter.All -> items
            VideoFilter.Untagged -> items.filter { tagsOf(it).isEmpty() }
            is VideoFilter.ByTag -> items.filter { filter.tag in tagsOf(it) }
        }
        return filtered.sortedWith { left, right ->
            val byName = NaturalOrder.compare(nameOf(left), nameOf(right))
            if (byName != 0) byName else keyOf(left).compareTo(keyOf(right))
        }
    }

    fun summarizeTags(videos: List<VideoItem>): List<TagSummary> =
        summarizeTagsBy(videos, VideoItem::tags)

    fun <T> summarizeTagsBy(items: List<T>, tagsOf: (T) -> List<String>): List<TagSummary> {
        val counts = LinkedHashMap<String, Int>()
        for (item in items) {
            for (tag in tagsOf(item)) {
                counts[tag] = (counts[tag] ?: 0) + 1
            }
        }
        return counts.map { TagSummary(it.key, it.value) }
            .sortedWith(
                compareByDescending<TagSummary> { it.count }
                    .thenComparator { left, right -> NaturalOrder.compare(left.name, right.name) },
            )
    }

    fun findNextIndex(videos: List<VideoItem>, currentKey: String): Int? =
        findNextIndexBy(videos, currentKey, VideoItem::key)

    fun <T> findNextIndexBy(items: List<T>, currentKey: String, keyOf: (T) -> String): Int? {
        val index = items.indexOfFirst { keyOf(it) == currentKey }
        if (index < 0 || index == items.lastIndex) return null
        return index + 1
    }
}
