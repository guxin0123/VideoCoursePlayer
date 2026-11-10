package com.guxins.videocourseplayer.data.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TagRulesTest {
    @Test
    fun normalizeTrimsAndCollapsesWhitespace() {
        assertEquals("重点 章节", TagRules.normalize("  重点   章节  "))
    }

    @Test
    fun validationRejectsBlankReservedAndTooLongTags() {
        assertEquals("标签不能为空", TagRules.validationError("   "))
        assertEquals("“全部”是系统保留名称", TagRules.validationError("全部"))
        assertEquals("“未标记”是系统保留名称", TagRules.validationError("未标记"))
        assertEquals(
            "标签最多 20 个字符",
            TagRules.validationError("123456789012345678901"),
        )
        assertNull(TagRules.validationError("重点"))
    }

    @Test
    fun sanitizeDeduplicatesAndDropsInvalidValues() {
        val result = TagRules.sanitize(
            listOf(" 重点 ", "重点", "", "全部", "123456789012345678901", "待复习"),
        )
        assertEquals(listOf("重点", "待复习"), result)
    }
}
