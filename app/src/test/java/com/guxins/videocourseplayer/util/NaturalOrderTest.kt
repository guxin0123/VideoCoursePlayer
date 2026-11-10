package com.guxins.videocourseplayer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NaturalOrderTest {
    @Test
    fun numericRunsAreComparedByValue() {
        assertTrue(NaturalOrder.compare("第2课.mp4", "第10课.mp4") < 0)
        assertTrue(NaturalOrder.compare("lesson2.mp4", "lesson10.mp4") < 0)
        assertTrue(NaturalOrder.compare("第1章第2节.mp4", "第1章第10节.mp4") < 0)
    }

    @Test
    fun comparisonIsCaseInsensitiveFirst() {
        assertTrue(NaturalOrder.compare("A2.mp4", "a10.mp4") < 0)
        assertTrue(NaturalOrder.compare("lesson.mp4", "Lesson.mp4") > 0)
    }

    @Test
    fun identicalNamesAreEqual() {
        assertEquals(0, NaturalOrder.compare("第01课.mp4", "第01课.mp4"))
    }

    @Test
    fun ordinaryChineseNamesAreOrderedDeterministically() {
        val sorted = listOf("第10章.mp4", "第2章.mp4", "第1章.mp4")
            .sortedWith(NaturalOrder)
        assertEquals(listOf("第1章.mp4", "第2章.mp4", "第10章.mp4"), sorted)
    }
}
