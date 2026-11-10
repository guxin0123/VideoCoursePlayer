package com.guxins.videocourseplayer.data.prefs

import org.json.JSONArray
import org.json.JSONObject

object TagJsonCodec {
    fun decode(json: String?): Map<String, List<String>> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching {
            val root = JSONObject(json)
            buildMap {
                val keys = root.keys()
                while (keys.hasNext()) {
                    val videoKey = keys.next()
                    val value = root.opt(videoKey)
                    val tags = when (value) {
                        is JSONArray -> (0 until value.length()).mapNotNull { index ->
                            value.optString(index).takeIf { it.isNotBlank() }
                        }
                        is String -> listOf(value)
                        else -> emptyList()
                    }.let(TagRules::sanitize)
                    if (videoKey.isNotBlank() && tags.isNotEmpty()) {
                        put(videoKey, tags)
                    }
                }
            }
        }.getOrDefault(emptyMap())
    }

    fun encode(tagsByVideo: Map<String, List<String>>): String {
        val root = JSONObject()
        for ((videoKey, tags) in tagsByVideo) {
            val sanitized = TagRules.sanitize(tags)
            if (sanitized.isEmpty()) continue
            root.put(videoKey, JSONArray(sanitized))
        }
        return root.toString()
    }
}
