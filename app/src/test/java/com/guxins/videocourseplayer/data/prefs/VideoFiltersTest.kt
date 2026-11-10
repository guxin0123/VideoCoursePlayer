package com.guxins.videocourseplayer.data.prefs

import com.guxins.videocourseplayer.data.model.VideoFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoFiltersTest {
    private data class Entry(
        val key: String,
        val name: String,
        val tags: List<String> = emptyList(),
    )

    private fun entries(): List<Entry> = listOf(
        Entry("3", "第1课.mp4"),
        Entry("1", "第2课.mp4", listOf("重点", "数学")),
        Entry("2", "第10课.mp4", listOf("待复习")),
    )

    private fun List<Entry>.applyFilter(filter: VideoFilter): List<String> {
        return VideoFilters.filterAndSort(
            items = this,
            filter = filter,
            tagsOf = Entry::tags,
            nameOf = Entry::name,
            keyOf = Entry::key,
        ).map { it.name }
    }

    @Test
    fun multiTagVideoMatchesAnyOfItsTags() {
        val videos = entries()
        assertEquals(
            listOf("第2课.mp4"),
            videos.applyFilter(VideoFilter.ByTag("重点")),
        )
        assertEquals(
            listOf("第2课.mp4"),
            videos.applyFilter(VideoFilter.ByTag("数学")),
        )
        assertEquals(
            listOf("第1课.mp4"),
            videos.applyFilter(VideoFilter.Untagged),
        )
    }

    @Test
    fun allFilterUsesNaturalOrder() {
        assertEquals(
            listOf("第1课.mp4", "第2课.mp4", "第10课.mp4"),
            entries().applyFilter(VideoFilter.All),
        )
    }

    @Test
    fun tagSummaryCountsDistinctVideosAndSortsByCount() {
        val tagged = listOf(
            Entry("1", "a.mp4", listOf("重点", "数学")),
            Entry("2", "b.mp4", listOf("重点")),
        )
        val summaries = VideoFilters.summarizeTagsBy(tagged, Entry::tags)
        assertEquals(2, summaries.size)
        assertEquals("重点", summaries[0].name)
        assertEquals(2, summaries[0].count)
        assertEquals("数学", summaries[1].name)
        assertEquals(1, summaries[1].count)
    }

    @Test
    fun nextIndexStopsAtEnd() {
        val videos = entries().sortedBy { it.key }
        assertEquals(1, VideoFilters.findNextIndexBy(videos, videos.first().key, Entry::key))
        assertNull(VideoFilters.findNextIndexBy(videos, videos.last().key, Entry::key))
    }
}
