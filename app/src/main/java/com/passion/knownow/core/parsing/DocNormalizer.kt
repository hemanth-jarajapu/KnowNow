package com.passion.knownow.core.parsing

/**
 * ServiceNow's published markdown is generated from a DITA pipeline and carries artefacts that
 * only look right in their own site renderer: HTML "nav card" tables, `[Omitted image "..."]`
 * placeholders inside link labels, and "Alt text:" prefixes. Left alone they dump raw markup into
 * the reading column, so we tidy them into plain markdown before rendering.
 */
object DocNormalizer {

    private val navCardTable = Regex(
        """<table[^>]*class="[^"]*nav-card[^"]*"[^>]*>(.*?)</table>""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
    )
    private val cell = Regex("""<td[^>]*>(.*?)</td>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val omittedImage = Regex("""\\?\[Omitted image[^\]]*\\?]""", RegexOption.IGNORE_CASE)
    private val altTextLabel = Regex("""\[([^\]]*?)\s*Alt text:[^\]]*]\((.*?)\)""")
    private val escapedBrackets = Regex("""\\([\[\]])""")
    private val htmlTag = Regex("""<[^>]+>""")
    private val heading1 = Regex("""^\s*#\s+(.+)$""")
    private val extraBlankLines = Regex("""\n{3,}""")

    /** Returns the document body with pipeline artefacts removed and a duplicate H1 dropped. */
    fun normalize(markdown: String, title: String?): String {
        var text = markdown.replace("\r\n", "\n")

        text = dropDuplicateTitle(text, title)
        text = navCardTable.replace(text) { match -> navCards(match.groupValues[1]) }
        text = omittedImage.replace(text, "")
        text = altTextLabel.replace(text) { m ->
            "[${m.groupValues[1].trim()}](${m.groupValues[2]})"
        }
        text = escapedBrackets.replace(text, "$1")
        text = text.replace("</table>", "</table>\n\n")

        return extraBlankLines.replace(text, "\n\n").trim()
    }

    private fun navCards(inner: String): String {
        val links = cell.findAll(inner)
            .map { tidy(htmlTag.replace(it.groupValues[1], " ")) }
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
        return if (links.isBlank()) "" else "\n\n$links\n\n"
    }

    private fun tidy(raw: String): String =
        raw.replace('\n', ' ')
            .replace(Regex("""\s+"""), " ")
            .trim()

    private fun dropDuplicateTitle(text: String, title: String?): String {
        val desired = title?.trim().orEmpty()
        if (desired.isEmpty()) return text
        val lines = text.lines()
        val firstContent = lines.indexOfFirst { it.isNotBlank() }
        if (firstContent < 0) return text
        val match = heading1.find(lines[firstContent]) ?: return text
        val heading = match.groupValues[1].trim()
        if (!heading.equals(desired, ignoreCase = true)) return text
        return lines.filterIndexed { index, _ -> index != firstContent }.joinToString("\n")
    }
}
