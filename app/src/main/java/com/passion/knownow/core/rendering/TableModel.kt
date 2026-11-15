package com.passion.knownow.core.rendering

data class TableModel(
    val header: List<String>,
    val rows: List<List<String>>,
    val kind: TableKind,
)

enum class TableKind { PROPERTIES, PARAMETERS, NAV_CARDS, GENERIC }

object HtmlTableParser {
    private val tableRegex = Regex("<table[^>]*>(.*?)</table>", RegexOption.DOT_MATCHES_ALL)
    private val rowRegex = Regex("<tr[^>]*>(.*?)</tr>", RegexOption.DOT_MATCHES_ALL)
    private val cellRegex = Regex("<t[hd][^>]*>(.*?)</t[hd]>", RegexOption.DOT_MATCHES_ALL)
    private val tagRegex = Regex("<[^>]+>")

    fun parse(html: String): TableModel {
        val tableMatch = tableRegex.find(html) ?: return TableModel(emptyList(), emptyList(), TableKind.GENERIC)
        val body = tableMatch.groupValues[1]
        val rows = rowRegex.findAll(body).map { r ->
            cellRegex.findAll(r.groupValues[1]).map { c ->
                stripTags(c.groupValues[1]).trim()
            }.toList()
        }.toList()
        if (rows.isEmpty()) return TableModel(emptyList(), emptyList(), TableKind.GENERIC)
        val header = rows.firstOrNull()?.takeIf { body.contains("<th") } ?: emptyList()
        val dataRows = if (header.isNotEmpty()) rows.drop(1) else rows
        val colCount = (header.ifEmpty { dataRows.firstOrNull() } ?: emptyList()).size
        val kind = inferKind(header, colCount)
        return TableModel(header, dataRows, kind)
    }

    fun splitSegments(markdown: String): List<Segment> {
        val result = mutableListOf<Segment>()
        var last = 0
        for (m in tableRegex.findAll(markdown)) {
            if (m.range.first > last) {
                val text = markdown.substring(last, m.range.first).trim()
                if (text.isNotBlank()) result += Segment.Text(text)
            }
            result += Segment.Table(parse(m.value))
            last = m.range.last + 1
        }
        if (last < markdown.length) {
            val text = markdown.substring(last).trim()
            if (text.isNotBlank()) result += Segment.Text(text)
        }
        if (result.isEmpty()) result += Segment.Text(markdown)
        return result
    }

    private fun stripTags(s: String): String = tagRegex.replace(s, "").trim()

    private fun inferKind(header: List<String>, colCount: Int): TableKind {
        val h = header.map { it.lowercase() }
        return when {
            h.any { it.contains("name") } && h.any { it.contains("description") } && h.any { it.contains("type") } -> TableKind.PARAMETERS
            h.any { it.contains("name") } && h.any { it.contains("description") } && colCount == 2 -> TableKind.PROPERTIES
            colCount >= 2 && header.isEmpty() -> TableKind.NAV_CARDS
            else -> TableKind.GENERIC
        }
    }
}

sealed class Segment {
    data class Text(val content: String) : Segment()
    data class Table(val model: TableModel) : Segment()
}