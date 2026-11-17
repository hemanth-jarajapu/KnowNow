package com.passion.knownow.core.parsing

/**
 * A publication's `index.md` is a table of contents with a one-line description per page and
 * the hierarchy of sections. Reading it needs a single small request instead of the whole
 * download, which is what lets Discover preview a topic before the user commits to it.
 */
data class PreviewEntry(
    val title: String,
    val path: String?,
    val description: String,
    /** 0 for a top-level section, growing with each nested level. */
    val depth: Int,
    /** Section titles above this entry, closest first. */
    val ancestors: List<String>,
)

data class TopicPreview(
    val slug: String,
    val title: String?,
    val summary: String?,
    val entries: List<PreviewEntry>,
) {
    val pageCount: Int get() = entries.size
    val isEmpty: Boolean get() = entries.isEmpty()

    companion object {
        fun empty(slug: String) = TopicPreview(slug, null, null, emptyList())
    }
}

object TopicIndexParser {
    private val ownPath = Regex("""/markdown/[^/]+/(.+)$""")

    fun parse(slug: String, markdown: String): TopicPreview {
        val (meta, body) = FrontmatterParser.parse(markdown)
        val title = meta["title"]?.trim()?.takeIf { it.isNotBlank() }
        val heading = Regex("""(?m)^#\s+(.+?)\s*$""").find(body)?.groupValues?.get(1)?.trim()

        val stack = mutableListOf<String>()
        val entries = TocParser.parse(markdown).mapNotNull { toc ->
            if (toc.title.isBlank()) return@mapNotNull null
            val depth = toc.depth.coerceAtLeast(0)
            while (stack.size > depth) stack.removeAt(stack.lastIndex)
            while (stack.size < depth) stack += ""
            val ancestors = stack.filter { it.isNotBlank() }.reversed()
            if (depth < stack.size) stack[depth] = toc.title else stack += toc.title

            PreviewEntry(
                title = toc.title.trim(),
                path = ownPath.find(toc.url)?.groupValues?.get(1),
                description = toc.description?.trim().orEmpty(),
                depth = depth,
                ancestors = ancestors,
            )
        }

        // The first page of a publication is its landing page; its description is the best
        // available "what is this topic about" sentence.
        val summary = entries.firstOrNull { it.description.length > 60 }?.description
            ?: entries.firstOrNull()?.description?.takeIf { it.isNotBlank() }
        return TopicPreview(slug, title ?: heading, summary, entries)
    }
}
