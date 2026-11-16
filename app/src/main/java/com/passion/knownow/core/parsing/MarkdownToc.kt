package com.passion.knownow.core.parsing

data class DocSection(
    val title: String?,
    val level: Int,
    val markdown: String,
)

/**
 * Splits a markdown body into top-level reading sections at headings so the reader can
 * render lazily and offer a real table of contents with working jumps.
 */
object MarkdownToc {
    private val headingRegex = Regex("""^(#{1,4})\s+(.+?)\s*#*\s*$""")

    fun sections(body: String): List<DocSection> {
        val sections = mutableListOf<DocSection>()
        val current = StringBuilder()
        var title: String? = null
        var level = 0

        fun flush() {
            val text = current.toString().trim()
            if (text.isNotEmpty()) sections += DocSection(title, level, text)
            current.clear()
        }

        var inFence = false
        for (line in body.lineSequence()) {
            val trimmed = line.trimStart()
            if (trimmed.startsWith("```")) inFence = !inFence
            val match = if (!inFence) headingRegex.find(trimmed) else null
            if (match != null && (match.groupValues[1].length <= 2 || sections.isNotEmpty())) {
                flush()
                title = match.groupValues[2].trim()
                level = match.groupValues[1].length
                current.appendLine(line)
            } else {
                current.appendLine(line)
            }
        }
        flush()
        return sections.ifEmpty { listOf(DocSection(null, 0, body)) }
    }
}
