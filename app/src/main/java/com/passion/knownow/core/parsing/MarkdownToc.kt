package com.passion.knownow.core.parsing

data class DocSection(
    val title: String?,
    val level: Int,
    val markdown: String,
)

/**
 * Splits a markdown body into top-level reading sections at headings so the reader can
 * render lazily and offer a real table of contents with working jumps.
 *
 * Sections are also capped in size. A handful of ServiceNow pages have no headings at all
 * (or one giant table), and handing a single multi-megabyte string to the markdown renderer
 * both blocks the UI thread and exhausts memory. Oversized sections are split on blank lines
 * into render-sized chunks.
 */
object MarkdownToc {
    private val headingRegex = Regex("""^(#{1,3})\s+(.+?)\s*#*\s*$""")

    /** Soft cap for a single rendered chunk, in characters. */
    private const val MAX_CHUNK = 4_000

    fun sections(body: String): List<DocSection> {
        val raw = splitAtHeadings(body)
        return raw.flatMap { section ->
            if (section.markdown.length <= MAX_CHUNK) listOf(section) else chunk(section)
        }
    }

    private fun splitAtHeadings(body: String): List<DocSection> {
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
            if (match != null) {
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

    /** Splits one oversized section on paragraph boundaries, keeping fences balanced. */
    private fun chunk(section: DocSection): List<DocSection> {
        val paragraphs = section.markdown.split(Regex("""\n\s*\n"""))
        val out = mutableListOf<DocSection>()
        val buffer = StringBuilder()
        var first = true
        var openFence = false

        fun flushChunk() {
            val text = buffer.toString().trim()
            buffer.clear()
            if (text.isEmpty()) return
            out += DocSection(if (first) section.title else null, if (first) section.level else 0, text)
            first = false
        }

        for (paragraph in paragraphs) {
            if (paragraph.isBlank()) continue
            val fenceCount = paragraph.lineSequence().count { it.trimStart().startsWith("```") }
            if (fenceCount % 2 == 1) openFence = !openFence

            if (buffer.length + paragraph.length > MAX_CHUNK && buffer.isNotEmpty() && !openFence) {
                flushChunk()
            }
            if (buffer.isNotEmpty()) buffer.append("\n\n")
            buffer.append(paragraph)
            // Never let a single huge paragraph escape the cap: split on sentence ends.
            while (buffer.length > MAX_CHUNK * 2) {
                val cut = buffer.indexOfAny(charArrayOf('\n', '.'), MAX_CHUNK).takeIf { it > 0 } ?: MAX_CHUNK
                val head = buffer.substring(0, cut + 1)
                val tail = buffer.substring(cut + 1)
                buffer.clear()
                buffer.append(head.trim())
                flushChunk()
                buffer.append(tail.trim())
            }
        }
        flushChunk()
        return out.ifEmpty { listOf(section) }
    }
}
