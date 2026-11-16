package com.passion.knownow.ui.screens

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.passion.knownow.core.parsing.DocNormalizer
import com.passion.knownow.core.parsing.FrontmatterParser
import com.passion.knownow.core.parsing.MarkdownToc
import com.passion.knownow.core.rendering.HtmlTableParser
import com.passion.knownow.core.rendering.Segment
import com.passion.knownow.data.ServiceLocator
import com.passion.knownow.data.db.BookmarkEntity
import com.passion.knownow.data.db.HighlightEntity
import com.passion.knownow.data.db.NoteEntity
import com.passion.knownow.data.db.ReadingProgressEntity
import com.passion.knownow.ui.components.AdaptiveTable
import com.passion.knownow.ui.theme.ReaderTheme
import com.passion.knownow.ui.theme.ReaderThemeMode
import com.passion.knownow.ui.theme.readerPalette
import kotlinx.coroutines.launch
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(documentId: Long, onBackClick: () -> Unit, onOpenDocument: (Long) -> Unit = {}) {
    val vm: ReaderViewModel = viewModel()
    val settings: SettingsViewModel = viewModel()
    LaunchedEffect(documentId) {
        vm.load(documentId)
        settings.recordRead()
    }

    val doc by vm.document.collectAsState()
    val content by vm.content.collectAsState()
    val globalReaderTheme by settings.readerTheme.collectAsState()
    val docOverride by settings.readerThemeOverrideFlow(documentId).collectAsState(initial = null)
    val readerTheme = docOverride ?: globalReaderTheme
    val fontScale by settings.fontScale.collectAsState()

    val body = remember(content, doc?.title) {
        if (content.isBlank()) "" else {
            val raw = FrontmatterParser.parse(content).second
            DocNormalizer.normalize(raw, doc?.title)
        }
    }
    val sections = remember(body) { if (body.isBlank()) emptyList() else MarkdownToc.sections(body) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val db = ServiceLocator.database
    val density = LocalDensity.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val palette = com.passion.knownow.ui.theme.readerPalette(readerTheme, false)
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var speaking by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
            }
        }
        tts = engine
        onDispose {
            engine.stop()
            engine.shutdown()
        }
    }

    var bookmarked by remember { mutableStateOf(false) }
    LaunchedEffect(documentId) {
        db.bookmarkDao().isBookmarked(documentId).collect { bookmarked = it }
    }

    var hideChrome by remember { mutableStateOf(false) }
    LaunchedEffect(listState) {
        var lastIndex = 0
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                if (index > lastIndex) hideChrome = true
                if (index < lastIndex || index <= 1) hideChrome = false
                lastIndex = index
            }
    }

    var restored by remember(documentId) { mutableStateOf(false) }
    LaunchedEffect(documentId, sections.size) {
        if (!restored && sections.isNotEmpty()) {
            val saved = db.readingProgressDao().get(documentId)?.progressPercent ?: 0f
            val target = (saved * sections.size).toInt().coerceIn(0, sections.size - 1)
            if (target > 0) listState.scrollToItem(target)
            restored = true
        }
    }

    LaunchedEffect(listState, sections.size, documentId, restored) {
        if (!restored) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex to sections.size }
            .collect { (first, total) ->
                if (total > 0) {
                    val progress = (first.toFloat() / total).coerceIn(0f, 1f)
                    db.readingProgressDao().upsert(ReadingProgressEntity(documentId, progress))
                }
            }
    }

    var showToc by remember { mutableStateOf(false) }
    var showType by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(false) }
    var showHighlight by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }
    var noteTags by remember { mutableStateOf("") }
    var highlightText by remember { mutableStateOf("") }

    var overrideEnabled by remember(docOverride) { mutableStateOf(docOverride != null) }

    val uriHandler = remember(onOpenDocument) {
        object : androidx.compose.ui.platform.UriHandler {
            override fun openUri(uri: String) {
                scope.launch {
                    val id = resolveInternalLink(uri)
                    if (id != null) {
                        onOpenDocument(id)
                    } else {
                        runCatching {
                            context.startActivity(
                                android.content.Intent(android.content.Intent.ACTION_VIEW, uri.toUri())
                            )
                        }
                    }
                }
            }
        }
    }

    ReaderTheme(mode = readerTheme, fontScale = fontScale) {
        val mdTypography = markdownTypography(
            h1 = MaterialTheme.typography.headlineMedium,
            h2 = MaterialTheme.typography.titleLarge,
            h3 = MaterialTheme.typography.titleMedium,
            h4 = MaterialTheme.typography.titleSmall,
            text = MaterialTheme.typography.bodyLarge,
            paragraph = MaterialTheme.typography.bodyLarge,
            code = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            inlineCode = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        )
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalUriHandler provides uriHandler) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 760.dp)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(
                    start = 22.dp,
                    end = 22.dp,
                    top = statusBarTop + 96.dp,
                    bottom = navBarBottom + 128.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Column {
                        Text(
                            doc?.topicType?.replace('-', ' ')?.uppercase()
                                ?: "SERVICENOW DOCS",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.accent,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            doc?.title ?: "Loading…",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        val minutes = doc?.readingTimeMinutes
                        Text(
                            buildString {
                                if (minutes != null) append("$minutes min read")
                                doc?.lastUpdated?.let {
                                    if (isNotEmpty()) append(" · ")
                                    append("updated $it")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.muted,
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { if (sections.isEmpty()) 0f else listState.firstVisibleItemIndex.toFloat() / sections.size },
                            modifier = Modifier.fillMaxWidth().height(2.dp).clip(CircleShape),
                            color = palette.accent,
                            trackColor = palette.muted.copy(alpha = 0.15f),
                        )
                    }
                }

                itemsIndexed(sections) { index, section ->
                    val parts = HtmlTableParser.splitSegments(section.markdown)
                    Column(Modifier.fillMaxWidth()) {
                        if (index > 0) Spacer(Modifier.height(12.dp))
                        parts.forEach { seg ->
                            when (seg) {
                                is Segment.Text -> if (seg.content.isNotBlank()) {
                                    Markdown(content = seg.content, typography = mdTypography)
                                }
                                is Segment.Table -> AdaptiveTable(seg.model)
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = !hideChrome,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                Surface(
                    color = palette.background.copy(alpha = 0.94f),
                    contentColor = palette.text,
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(palette.background)
                            .statusBarsPadding()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Text(
                            doc?.title ?: "",
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = {
                            scope.launch {
                                if (bookmarked) db.bookmarkDao().delete(documentId)
                                else db.bookmarkDao().upsert(BookmarkEntity(documentId))
                            }
                        }) {
                            Icon(
                                if (bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = "Bookmark",
                            )
                        }
                        IconButton(onClick = {
                            if (speaking) {
                                tts?.stop()
                                speaking = false
                            } else {
                                tts?.speak(body, TextToSpeech.QUEUE_FLUSH, null, "doc")
                                speaking = true
                            }
                        }) {
                            Icon(
                                if (speaking) Icons.Filled.PauseCircle else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (speaking) "Stop reading" else "Read aloud",
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = !hideChrome,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                Surface(
                    color = palette.background,
                    contentColor = palette.text,
                    shadowElevation = 12.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ReaderAction(Icons.AutoMirrored.Filled.List, "Contents") { showToc = true }
                        ReaderAction(Icons.Filled.FormatSize, "Type") { showType = true }
                        ReaderAction(Icons.AutoMirrored.Filled.NoteAdd, "Note") { showNote = true }
                        ReaderAction(Icons.Filled.Highlight, "Highlight") { showHighlight = true }
                        doc?.canonicalUrl?.let { url ->
                            ReaderAction(Icons.AutoMirrored.Filled.OpenInNew, "Open online") {
                                context.startActivity(
                                    android.content.Intent(android.content.Intent.ACTION_VIEW, url.toUri())
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }

    if (showToc) {
        ModalBottomSheet(
            onDismissRequest = { showToc = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
                Text("Contents", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 480.dp)) {
                    itemsIndexed(sections) { index, section ->
                        val title = section.title
                        if (title != null) {
                            Surface(
                                onClick = {
                                    showToc = false
                                    scope.launch { listState.animateScrollToItem(index) }
                                },
                                color = Color.Transparent,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    title,
                                    style = if (section.level <= 2) MaterialTheme.typography.bodyLarge
                                    else MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (section.level <= 2) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (section.level <= 2) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(
                                        start = ((section.level - 1) * 12).dp,
                                        top = 10.dp,
                                        bottom = 10.dp,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showType) {
        ModalBottomSheet(
            onDismissRequest = { showType = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
                Text("Reading style", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReaderThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = readerTheme == mode,
                            onClick = {
                                if (overrideEnabled) {
                                    settings.setReaderThemeOverride(documentId, mode)
                                } else {
                                    settings.setReaderTheme(mode)
                                }
                            },
                            label = {
                                Text(
                                    when (mode) {
                                        ReaderThemeMode.PAPER -> "Paper"
                                        ReaderThemeMode.SEPIA -> "Sepia"
                                        ReaderThemeMode.NIGHT -> "Night"
                                        ReaderThemeMode.EXPRESSIVE -> "Vivid"
                                    }
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Remember for this document", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (overrideEnabled) "This document keeps its own palette."
                            else "Follows your global reading theme.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = overrideEnabled,
                        onCheckedChange = { enabled ->
                            overrideEnabled = enabled
                            if (enabled) {
                                settings.setReaderThemeOverride(documentId, readerTheme)
                            } else {
                                settings.setReaderThemeOverride(documentId, null)
                            }
                        },
                    )
                }
                if (docOverride != null) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        overrideEnabled = false
                        settings.setReaderThemeOverride(documentId, null)
                    }) { Text("Reset to global theme") }
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    "Text size  ·  ${(fontScale * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                )
                Slider(
                    value = fontScale,
                    onValueChange = { settings.setFontScale(it) },
                    valueRange = 0.85f..1.35f,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "The reader palette is independent of the app shell. Your choice is remembered.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showNote) {
        AlertDialog(
            onDismissRequest = { showNote = false },
            title = { Text("Add note") },
            text = {
                Column {
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Note") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = noteTags,
                        onValueChange = { noteTags = it },
                        label = { Text("Tags (comma separated)") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val text = noteText
                    if (text.isNotBlank()) {
                        scope.launch {
                            val current = doc
                            val pub = current?.publicationId?.let { db.publicationDao().getById(it) }
                            db.noteDao().upsert(
                                NoteEntity(
                                    documentId = documentId,
                                    publicationId = current?.publicationId ?: 0L,
                                    releaseName = pub?.releaseName ?: "",
                                    documentTitleSnapshot = current?.title ?: "Untitled",
                                    publicationTitleSnapshot = pub?.title ?: "",
                                    canonicalUrl = current?.canonicalUrl,
                                    content = text,
                                    tags = noteTags,
                                )
                            )
                        }
                    }
                    noteText = ""
                    noteTags = ""
                    showNote = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showNote = false }) { Text("Cancel") } },
        )
    }

    if (showHighlight) {
        AlertDialog(
            onDismissRequest = { showHighlight = false },
            title = { Text("Add highlight") },
            text = {
                OutlinedTextField(
                    value = highlightText,
                    onValueChange = { highlightText = it },
                    label = { Text("Highlighted text") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val text = highlightText
                    if (text.isNotBlank()) {
                        scope.launch { db.highlightDao().upsert(HighlightEntity(documentId = documentId, text = text)) }
                    }
                    highlightText = ""
                    showHighlight = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showHighlight = false }) { Text("Cancel") } },
        )
    }
}

private suspend fun resolveInternalLink(url: String): Long? {
    val match = Regex("raw\\.githubusercontent\\.com/ServiceNow/ServiceNowDocs/([^/]+)/markdown/(.+)").find(url)
        ?: return null
    val release = match.groupValues[1]
    val relative = match.groupValues[2].substringBefore('?').substringBefore('#')
    val slug = relative.substringBefore('/')
    val rest = relative.substringAfter('/', missingDelimiterValue = "")
    if (rest.isBlank()) return null
    val db = com.passion.knownow.data.ServiceLocator.database
    val pub = db.publicationDao().getByReleaseAndSlug(release, slug) ?: return null
    return db.documentDao().getByPath(pub.id, rest)?.id
}

@Composable
private fun ReaderAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
