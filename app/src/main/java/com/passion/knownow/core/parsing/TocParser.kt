package com.passion.knownow.core.parsing

data class TocEntry(
    val depth: Int,
    val title: String,
    val url: String,
    val description: String?,
)

object TocParser {
    private val linkRegex = Regex("""\[(.+?)]\((.+?)\)""")
    fun parse(markdown: String): List<TocEntry> {
        val result = mutableListOf<TocEntry>()
        for (rawLine in markdown.lineSequence()) {
            val line = rawLine.trimEnd()
            if (line.isBlank()) continue
            val trimmed = line.trimStart()
            if (!trimmed.startsWith("-")) continue
            val depth = (line.length - trimmed.length) / 2
            val match = linkRegex.find(trimmed) ?: continue
            val title = match.groupValues[1]
            val url = match.groupValues[2]
            val after = trimmed.substring(match.range.last + 1).trim()
            val description = after.removePrefix("--").trim().ifBlank { null }
            result += TocEntry(depth, title, url, description)
        }
        return result
    }
}