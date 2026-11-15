package com.passion.knownow.core.parsing

object FrontmatterParser {
    fun parse(markdown: String): Pair<Map<String, String>, String> {
        val lines = markdown.lines()
        if (lines.firstOrNull()?.trim() != "---") return emptyMap<String, String>() to markdown
        val end = lines.drop(1).indexOfFirst { it.trim() == "---" }
        if (end < 0) return emptyMap<String, String>() to markdown
        val fmLines = lines.subList(1, end + 1)
        val body = lines.drop(end + 2).joinToString("\n")
        val map = linkedMapOf<String, String>()
        for (l in fmLines) {
            val idx = l.indexOf(':')
            if (idx <= 0) continue
            val key = l.substring(0, idx).trim()
            val value = l.substring(idx + 1).trim().trim('"')
            map[key] = value
        }
        return map to body
    }
}