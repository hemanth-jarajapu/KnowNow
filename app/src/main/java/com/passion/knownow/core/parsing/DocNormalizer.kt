package com.passion.knownow.core.parsing

/**
 * ServiceNow's published markdown is generated from a DITA pipeline and carries artefacts that
 * only look right in their own site renderer: HTML "nav card" tables, `[Omitted image "..."]`
 * placeholders inside link labels, "Alt text:" prefixes and links that either point back at raw
 * GitHub blobs or at sibling `.md` files by relative path. Left alone they dump raw markup into
 * the reading column or push the reader out to a browser.
 *
 * The normalizer tidies all of that and rewrites internal references to `knn://doc/...` URIs
 * that the reader resolves against the local database, so cross-references stay in the app.
 */
object DocNormalizer {

    const val INTERNAL_SCHEME = "knn://doc/"

    private val navCardTable = Regex(
        """<table[^>]*class="[^"]*nav-card[^"]*"[^>]*>(.*?)</table>""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
    )
    private val cell = Regex("""<td[^>]*>(.*?)</td>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    // The DITA pipeline leaves `[Omitted image "..."]`, `[Omitted video]` and similar
    // placeholders behind. They are not links, so markdown renders them as literal text.
    private val omittedPlaceholder = Regex("""\\?\[Omitted [^\]]*\\?]""", RegexOption.IGNORE_CASE)
    private val mediaPrefix = Regex("""(?m)^\s*(?:Description|Alt text)\s*:\s*""", RegexOption.IGNORE_CASE)
    private val altTextLabel = Regex("""\[([^\]]*?)\s*Alt text:[^\]]*]\((.*?)\)""")
    private val escapedBrackets = Regex("""\\([\[\]])""")
    // DITA emits `**Parent Topic:**[Link](...)` with no space, so markdown renders the
    // label and the link as one glued-together run.
    private val gluedBold = Regex("""(\*\*[^*\n]{1,80}\*\*)(?=[^\s*.,;:!?)\]])""")
    // `\(CI\)` is an escape in the source; CommonMark understands it, but stripping it keeps
    // selection, search and TTS text clean.
    private val escapedParens = Regex("""\\([()])""")
    private val htmlTag = Regex("""<[^>]+>""")
    private val heading1 = Regex("""^\s*#\s+(.+)$""")
    private val extraBlankLines = Regex("""\n{3,}""")
    private val markdownLink = Regex("""(!?)\[([^\]\n]*)]\(([^)\n]+)\)""")

    // Any URL that carries the upstream layout: .../markdown/<slug>/<path>.md
    private val upstreamDoc = Regex("""/markdown/([^/?#]+)/([^?#)\s]+)""")
    private val absoluteUrl = Regex("""^[a-zA-Z][a-zA-Z0-9+.-]*://""")
    private val webDocHost = Regex("""^https?://(?:www\.)?servicenow\.com/docs/""", RegexOption.IGNORE_CASE)

    /** Returns the document body with pipeline artefacts removed and internal links rewritten. */
    fun normalize(
        markdown: String,
        title: String?,
        slug: String? = null,
        currentPath: String? = null,
    ): String {
        var text = markdown.replace("\r\n", "\n")

        text = dropDuplicateTitle(text, title)
        text = navCardTable.replace(text) { match -> navCards(match.groupValues[1]) }
        text = omittedPlaceholder.replace(text, "")
        text = mediaPrefix.replace(text, "")
        text = altTextLabel.replace(text) { m ->
            "[${m.groupValues[1].trim()}](${m.groupValues[2]})"
        }
        text = escapedBrackets.replace(text, "$1")
        text = text.replace("</table>", "</table>\n\n")
        if (slug != null) text = rewriteLinks(text, slug, currentPath)
        text = gluedBold.replace(text, "$1 ")
        text = escapedParens.replace(text, "$1")

        return extraBlankLines.replace(text, "\n\n").trim()
    }

    /**
     * Points in-body links at `knn://doc/<slug>/<page>` so the reader can open the local copy.
     * Fenced code blocks are left untouched, and unresolvable web links stay external.
     */
    private fun rewriteLinks(text: String, slug: String, currentPath: String?): String {
        val out = StringBuilder(text.length)
        var inFence = false
        for (line in text.lineSequence()) {
            val fence = line.trimStart().startsWith("```")
            if (fence) {
                inFence = !inFence
                out.appendLine(line)
                continue
            }
            if (inFence) {
                out.appendLine(line)
                continue
            }
            out.appendLine(rewriteLine(line, slug, currentPath))
        }
        return out.toString().removeSuffix("\n")
    }

    private fun rewriteLine(line: String, slug: String, currentPath: String?): String =
        markdownLink.replace(line) { match ->
            val isImage = match.groupValues[1] == "!"
            val label = match.groupValues[2]
            val rawTarget = match.groupValues[3].trim()
            if (isImage) return@replace match.value

            val target = rawTarget.removePrefix("<").removeSuffix(">")
            val internal = internalTarget(target, slug, currentPath)
            if (internal == null) match.value else "[$label]($INTERNAL_SCHEME$internal)"
        }

    /**
     * Maps a link target to `<slug>/<relativePath>`, or null when it should stay external.
     * Handles the three shapes ServiceNow content uses: raw repository URLs, the published
     * `servicenow.com/docs/r/<topic>/<page>.html` site URLs, and relative `.md` references.
     */
    private fun internalTarget(target: String, slug: String, currentPath: String?): String? {
        if (target.startsWith("#") || target.startsWith("mailto:")) return null

        if (absoluteUrl.containsMatchIn(target)) {
            val reference = parseReference(target) ?: return null
            return "${reference.first}/${reference.second}"
        }

        // Relative link: another page of the same topic, possibly with ../ segments.
        val clean = target.substringBefore('#').substringBefore('?')
        if (clean.isBlank()) return null
        if (!clean.endsWith(".md") && !clean.contains('/')) return null
        val resolved = resolveRelative(currentPath, clean) ?: return null
        return "$slug/$resolved"
    }

    /**
     * `<slug>` + `<path>` for any absolute reference into the docs corpus, or null when the
     * target is something else entirely. Used both when rewriting links and when resolving
     * them against the database at read time.
     */
    fun parseReference(target: String): Pair<String, String>? {
        if (target.startsWith(INTERNAL_SCHEME)) {
            val body = target.removePrefix(INTERNAL_SCHEME)
            val slug = body.substringBefore('/')
            val path = body.substringAfter('/', "")
            return if (slug.isNotBlank() && path.isNotBlank()) slug to path else null
        }
        if (!absoluteUrl.containsMatchIn(target)) return null
        upstreamDoc.find(target)?.let { match ->
            val slug = match.groupValues[1]
            val path = match.groupValues[2].trimStart('/')
            if (slug.isNotBlank() && path.isNotBlank()) return slug to path
        }
        return webDocReference(target)
    }

    /**
     * `https://www.servicenow.com/docs/r/application-portfolio-management/eaw-app-service-form.html`
     * is the same page as `markdown/application-portfolio-management/eaw-app-service-form.md`,
     * so it can be pointed at the local copy without asking the network.
     */
    fun webDocReference(target: String): Pair<String, String>? {
        if (!webDocHost.containsMatchIn(target)) return null
        val path = target.substringAfter("/docs/", "")
            .substringBefore('#')
            .substringBefore('?')
        val segments = path.split('/').filter { it.isNotBlank() }
        // `/docs/r/<topic>/<page>.html` — the leading marker is not part of the topic slug.
        val cleaned = segments.dropWhile { it == "r" }
        if (cleaned.size < 2) return null
        val targetSlug = cleaned.first()
        if (targetSlug.isBlank()) return null
        var page = cleaned.drop(1).joinToString("/")
        page = page.removeSuffix(".html").removeSuffix(".htm")
        if (page.isBlank()) return null
        return targetSlug to if (page.endsWith(".md")) page else "$page.md"
    }

    fun resolveRelative(currentPath: String?, target: String): String? {
        val base = if (target.startsWith("/")) {
            emptyList()
        } else {
            (currentPath?.substringBeforeLast('/', "") ?: "").split('/').filter { it.isNotBlank() }
        }
        val segments = base.toMutableList()
        for (part in target.trimStart('/').split('/')) {
            when (part) {
                "", "." -> Unit
                ".." -> if (segments.isNotEmpty()) segments.removeAt(segments.size - 1)
                else -> segments += part
            }
        }
        val resolved = segments.joinToString("/")
        return resolved.ifBlank { null }
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
