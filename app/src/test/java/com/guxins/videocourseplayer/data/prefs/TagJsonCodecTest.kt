package com.guxins.videocourseplayer.data.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TagJsonCodecTest {
    @Test
    fun encodesAndDecodesMultipleTags() {
        val source = mapOf(
            "external_primary:1" to listOf("重点", "待复习"),
            "external_primary:2" to listOf("数学"),
        )
        val decoded = TagJsonCodec.decode(TagJsonCodec.encode(source))
        assertEquals(source, decoded)
    }

    @Test
    fun decodesLegacySingleTagString() {
        val decoded = TagJsonCodec.decode("""{"external_primary:1":"重点"}""")
        assertEquals(mapOf("external_primary:1" to listOf("重点")), decoded)
    }

    @Test
    fun invalidJsonFallsBackToEmptyMap() {
        assertTrue(TagJsonCodec.decode("not-json").isEmpty())
        assertTrue(TagJsonCodec.decode(null).isEmpty())
        assertTrue(TagJsonCodec.decode("").isEmpty())
    }

    @Test
    fun reservedAndBlankValuesAreDroppedOnDecode() {
        val decoded = TagJsonCodec.decode(
            """{"external_primary:1":["重点","全部","","  "]}""",
        )
        assertEquals(mapOf("external_primary:1" to listOf("重点")), decoded)
    }
}
