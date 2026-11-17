package com.passion.knownow.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.markdownPadding
import com.passion.knownow.core.parsing.DocNormalizer
import com.passion.knownow.core.rendering.HtmlTableParser
import com.passion.knownow.core.rendering.Segment
import com.passion.knownow.data.ServiceLocator
import com.passion.knownow.data.db.BookmarkEntity
import com.passion.knownow.data.db.HighlightEntity
import com.passion.knownow.data.db.NoteEntity
import com.passion.knownow.data.db.ReadingProgressEntity
import com.passion.knownow.data.repository.DocLinkResolver
import com.passion.knownow.data.repository.DocLinkTarget
import com.passion.knownow.ui.components.AdaptiveTable
import com.passion.knownow.ui.theme.JetBrainsMono
import com.passion.knownow.ui.theme.ReaderTheme
import com.passion.knownow.ui.theme.ReaderThemeMode
import com.passion.knownow.ui.theme.readerPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLDecoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    documentId: Long,
    onBackClick: () -> Unit,
    onOpenDocument: (Long) -> Unit = {},
) {
    val vm: ReaderViewModel = viewModel()
    val settings: SettingsViewModel = viewModel()
    LaunchedEffect(documentId) { vm.load(documentId) }

    val state by vm.state.collectAsState()
    val doc = state.document
    val sections = state.sections
    val globalReaderTheme by settings.readerTheme.collectAsState()
    val docOverride by settings.readerThemeOverrideFlow(documentId).collectAsState(initial = null)
    val readerTheme = docOverride ?: globalReaderTheme
    val fontScale by settings.fontScale.collectAsState()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val db = ServiceLocator.database
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val palette = readerPalette(readerTheme, false)

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var speaking by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val engine = runCatching {
            TextToSpeech(context) { status ->
                if (status != TextToSpeech.SUCCESS) speaking = false
            }
        }.getOrNull()
        tts = engine
        onDispose {
            runCatching {
                engine?.stop()
                engine?.shutdown()
            }
        }
    }

    var bookmarked by remember(documentId) { mutableStateOf(false) }
    LaunchedEffect(documentId) {
        runCatching {
            db.bookmarkDao().isBookmarked(documentId).collect { bookmarked = it }
        }
    }

    var hideChrome by remember { mutableStateOf(false) }
    LaunchedEffect(listState) {
        var lastIndex = 0
        snapshotFlow { listState.firstVisibleItemIndex }.collect { index ->
            if (index > lastIndex) hideChrome = true
            if (index < lastIndex || index <= 1) hideChrome = false
            lastIndex = index
        }
    }

    var restored by remember(documentId) { mutableStateOf(false) }
    // Progress tracking only starts once the restore scroll has landed. Otherwise the reader
    // would immediately persist item 0 and wipe the very position it just restored.
    var trackProgress by remember(documentId) { mutableStateOf(false) }
    var progressIndex by remember(documentId) { mutableIntStateOf(0) }
    LaunchedEffect(documentId, sections.size) {
        if (!restored && sections.isNotEmpty()) {
            val saved = runCatching { db.readingProgressDao().get(documentId)?.progressPercent ?: 0f }
                .getOrDefault(0f)
            val target = (saved * sections.size).toInt().coerceIn(0, sections.size - 1)
            progressIndex = target
            if (target > 0) runCatching { listState.scrollToItem(target + 1) }
            restored = true
            delay(600)
            trackProgress = true
        }
    }

    // Progress writes are debounced and guarded: a busy download transaction must never be
    // able to take the reader down with it.
    LaunchedEffect(documentId, trackProgress, sections.size) {
        if (!trackProgress || sections.isEmpty()) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { first ->
                progressIndex = first
                delay(700)
                val progress = (first.toFloat() / sections.size).coerceIn(0f, 1f)
                runCatching {
                    db.readingProgressDao().upsert(ReadingProgressEntity(documentId, progress))
                }
            }
    }

    var showToc by remember { mutableStateOf(false) }
    var showType by remember { mutableStateOf(false) }
    var showNotes by remember { mutableStateOf(false) }
    var showHighlights by remember { mutableStateOf(false) }
    var overrideEnabled by remember(docOverride) { mutableStateOf(docOverride != null) }

    val notes by remember(documentId) {
        db.noteDao().observeForDocument(documentId)
    }.collectAsState(initial = emptyList())
    val highlights by remember(documentId) {
        db.highlightDao().observeForDocument(documentId)
    }.collectAsState(initial = emptyList())

    val resolver = remember(db) { DocLinkResolver(db) }
    var topicOffer by remember { mutableStateOf<DocLinkTarget.NeedsTopic?>(null) }
    var unavailable by remember { mutableStateOf<DocLinkTarget.Unavailable?>(null) }
    val uriHandler = remember(onOpenDocument) {
        object : UriHandler {
            override fun openUri(uri: String) {
                scope.launch {
                    val current = vm.state.value
                    when (val target = resolver.resolve(uri, current.slug, current.document?.relativePath)) {
                        is DocLinkTarget.Local -> onOpenDocument(target.documentId)
                        is DocLinkTarget.NeedsTopic -> topicOffer = target
                        is DocLinkTarget.Unavailable -> unavailable = target
                        is DocLinkTarget.External -> openExternally(context, target.url)
                    }
                }
            }
        }
    }

    ReaderTheme(mode = readerTheme, fontScale = fontScale) {
        val typography = MaterialTheme.typography
        // Body text carries the palette, but markdown needs a few more colours than the
        // Material scheme exposes: inline code chips, code blocks and table fills.
        val mdColors = markdownColor(
            text = palette.text,
            codeBackground = palette.text.copy(alpha = 0.05f),
            inlineCodeBackground = palette.text.copy(alpha = 0.07f),
            dividerColor = palette.muted.copy(alpha = 0.22f),
            tableBackground = palette.text.copy(alpha = 0.03f),
        )
        // Headings step down gently: display sizes are for a title, not for an h4 deep in
        // a reference page. Links stay in body size so a paragraph keeps its rhythm, and are
        // marked by colour plus underline rather than bold, which used to look like shouting.
        val mdTypography = markdownTypography(
            h1 = typography.headlineMedium,
            h2 = typography.headlineSmall,
            h3 = typography.titleLarge,
            h4 = typography.titleMedium,
            h5 = typography.titleSmall,
            h6 = typography.labelLarge,
            text = typography.bodyLarge,
            paragraph = typography.bodyLarge,
            ordered = typography.bodyLarge,
            bullet = typography.bodyLarge,
            list = typography.bodyLarge,
            quote = typography.bodyLarge.copy(
                fontStyle = FontStyle.Italic,
                color = palette.muted,
            ),
            code = typography.bodySmall.copy(fontFamily = JetBrainsMono),
            inlineCode = typography.bodyMedium.copy(fontFamily = JetBrainsMono),
            textLink = TextLinkStyles(
                style = typography.bodyLarge.copy(
                    color = palette.accent,
                    textDecoration = TextDecoration.Underline,
                ).toSpanStyle(),
            ),
        )
        // A little more air between blocks than the library default of 2dp, and a wider
        // list indent so wrapped list items stay visually inside their bullet.
        val mdPadding = markdownPadding(
            block = 5.dp,
            list = 6.dp,
            listItemTop = 3.dp,
            listItemBottom = 3.dp,
            listIndent = 12.dp,
        )

        androidx.compose.runtime.CompositionLocalProvider(LocalUriHandler provides uriHandler) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                // A link to a topic that isn't on the device: offer the download instead of
                // silently throwing the reader into a browser.
                topicOffer?.let { offer ->
                    AlertDialog(
                        onDismissRequest = { topicOffer = null },
                        title = { Text("Ã¢â‚¬Å“${offer.publication.title}Ã¢â‚¬Â isn't downloaded") },
                        text = {
                            Text(
                                "This page is part of that topic. Download it once and this link, " +
                                    "and every other link into that topic, will open here offline."
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val publication = offer.publication
                                    topicOffer = null
                                    scope.launch {
                                        runCatching {
                                            ServiceLocator.downloadRepository.enqueue(listOf(publication))
                                        }
                                    }
                                },
                            ) { Text("Download") }
                        },
                        dismissButton = {
                            Row {
                                TextButton(onClick = {
                                    topicOffer = null
                                    openExternally(context, offer.webUrl)
                                }) { Text("Open online") }
                                TextButton(onClick = { topicOffer = null }) { Text("Cancel") }
                            }
                        },
                    )
                }
                unavailable?.let { missing ->
                    AlertDialog(
                        onDismissRequest = { unavailable = null },
                        title = { Text("Not in the offline copy") },
                        text = {
                            Text(
                                "This page isn't part of the downloaded copy of " +
                                    "Ã¢â‚¬Å“${missing.topicTitle}Ã¢â‚¬Â. Refreshing the topic may add it."
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                unavailable = null
                                openExternally(context, missing.webUrl)
                            }) { Text("Open online") }
                        },
                        dismissButton = {
                            TextButton(onClick = { unavailable = null }) { Text("Close") }
                        },
                    )
                }

                when {
                    state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = palette.accent)
                    }
                    state.missing || sections.isEmpty() -> Box(
                        Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "This page isn't saved on the device",
                                style = typography.titleMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Refresh the topic from Discover to download it again.",
                                style = typography.bodyMedium,
                                color = palette.muted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                            Spacer(Modifier.height(12.dp))
                            TextButton(onClick = onBackClick) { Text("Go back") }
                        }
                    }
                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .widthIn(max = 620.dp)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(
                            start = 22.dp,
                            end = 22.dp,
                            top = statusBarTop + 72.dp,
                            bottom = navBarBottom + 104.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        item {
                            Column {
                                Text(
                                    doc?.topicType?.replace('-', ' ')?.uppercase() ?: "SERVICENOW DOCS",
                                    style = typography.labelSmall,
                                    color = palette.accent,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    doc?.title.orEmpty(),
                                    style = typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = typography.headlineLarge.lineHeight,
                                )
                                val meta = buildString {
                                    doc?.readingTimeMinutes?.let { append("$it min read") }
                                    doc?.lastUpdated?.let {
                                        if (isNotEmpty()) append(" Ã‚Â· ")
                                        append("updated $it")
                                    }
                                }
                                if (meta.isNotEmpty()) {
                                    Text(meta, style = typography.bodySmall, color = palette.muted)
                                }
                                Spacer(Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = {
                                        if (sections.isEmpty()) 0f
                                        else progressIndex.toFloat() / sections.size
                                    },
                                    modifier = Modifier.fillMaxWidth().height(2.dp).clip(CircleShape),
                                    color = palette.accent,
                                    trackColor = palette.muted.copy(alpha = 0.15f),
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                        }

                        itemsIndexed(sections) { index, section ->
                            val parts = remember(section.markdown) {
                                HtmlTableParser.splitSegments(section.markdown)
                            }
                            Column(Modifier.fillMaxWidth()) {
                                if (index > 0) Spacer(Modifier.height(14.dp))
                                Column {
                                    parts.forEach { seg ->
                                            when (seg) {
                                                is Segment.Text -> if (seg.content.isNotBlank()) {
                                                    Markdown(
                                                        content = seg.content,
                                                        colors = mdColors,
                                                        typography = mdTypography,
                                                        padding = mdPadding,
                                                    )
                                                }
                                                is Segment.Table -> AdaptiveTable(seg.model)
                                            }
                                        }
                                    }
                                }
                            }
                    }
                }

                // Floating chrome ------------------------------------------------
                AnimatedVisibility(
                    visible = !hideChrome && !state.loading,
                    enter = fadeIn() + slideInVertically { -it },
                    exit = fadeOut() + slideOutVertically { -it },
                    modifier = Modifier.align(Alignment.TopCenter),
                ) {
                    Surface(color = palette.background, contentColor = palette.text) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                            Text(
                                doc?.title.orEmpty(),
                                style = typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = {
                                scope.launch {
                                    runCatching {
                                        if (bookmarked) db.bookmarkDao().delete(documentId)
                                        else db.bookmarkDao().upsert(BookmarkEntity(documentId))
                                    }
                                }
                            }) {
                                Icon(
                                    if (bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                    contentDescription = if (bookmarked) "Remove bookmark" else "Bookmark",
                                    tint = if (bookmarked) palette.accent else palette.text,
                                )
                            }
                            IconButton(onClick = {
                                val engine = tts
                                if (speaking) {
                                    runCatching { engine?.stop() }
                                    speaking = false
                                } else if (engine != null) {
                                    // TTS has a hard input limit; a multi-megabyte page would
                                    // otherwise be rejected (or take the speech service down).
                                    val text = state.plainText.take(3_500)
                                    if (text.isNotBlank()) {
                                        runCatching {
                                            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "knownow-doc")
                                        }
                                        speaking = true
                                    }
                                }
                            }) {
                                Icon(
                                    if (speaking) Icons.Filled.PauseCircle
                                    else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = if (speaking) "Stop reading" else "Read aloud",
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = !hideChrome && !state.loading,
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
                            ReaderAction(
                                Icons.AutoMirrored.Filled.NoteAdd,
                                if (notes.isEmpty()) "Note" else "Notes (${notes.size})",
                            ) { showNotes = true }
                            ReaderAction(
                                Icons.Filled.Highlight,
                                if (highlights.isEmpty()) "Highlight" else "Marks (${highlights.size})",
                            ) { showHighlights = true }
                            doc?.canonicalUrl?.let { url ->
                                ReaderAction(Icons.AutoMirrored.Filled.OpenInNew, "Online") {
                                    openExternally(context, url)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- sheets

    if (showToc) {
        ModalBottomSheet(
            onDismissRequest = { showToc = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
                Text("Contents", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 460.dp)) {
                    itemsIndexed(sections) { index, section ->
                        val title = section.title ?: return@itemsIndexed
                        Surface(
                            onClick = {
                                showToc = false
                                scope.launch { runCatching { listState.animateScrollToItem(index + 1) } }
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
                                    start = ((section.level - 1).coerceAtLeast(0) * 12).dp,
                                    top = 10.dp,
                                    bottom = 10.dp,
                                ),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
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
                                if (overrideEnabled) settings.setReaderThemeOverride(documentId, mode)
                                else settings.setReaderTheme(mode)
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
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Remember for this document", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (overrideEnabled) "This page keeps its own palette."
                            else "Follows your global reading theme.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = overrideEnabled,
                        onCheckedChange = { enabled ->
                            overrideEnabled = enabled
                            settings.setReaderThemeOverride(
                                documentId,
                                if (enabled) readerTheme else null,
                            )
                        },
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Text size  Ã‚Â·  ${(fontScale * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                )
                Slider(
                    value = fontScale,
                    onValueChange = { settings.setFontScale(it) },
                    valueRange = 0.85f..1.35f,
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showNotes) {
        val noteState = remember(documentId) { NoteSheetState() }
        ModalBottomSheet(
            onDismissRequest = { showNotes = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            NoteSheet(
                notes = notes,
                state = noteState,
                documentTitle = doc?.title.orEmpty(),
                publicationTitle = state.publicationTitle.orEmpty(),
                onSave = { content, tags ->
                    scope.launch {
                        runCatching {
                            val existing = noteState.editingId?.let { db.noteDao().getById(it) }
                            db.noteDao().upsert(
                                NoteEntity(
                                    id = existing?.id ?: 0L,
                                    documentId = documentId,
                                    publicationId = doc?.publicationId ?: 0L,
                                    releaseName = existing?.releaseName.orEmpty(),
                                    documentTitleSnapshot = doc?.title ?: "Untitled",
                                    publicationTitleSnapshot = state.publicationTitle.orEmpty(),
                                    canonicalUrl = doc?.canonicalUrl,
                                    content = content,
                                    tags = tags,
                                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis(),
                                )
                            )
                        }
                        noteState.reset()
                    }
                },
                onEdit = { note ->
                    noteState.editingId = note.id
                    noteState.text = note.content
                    noteState.tags = note.tags
                },
                onDelete = { note ->
                    scope.launch { runCatching { db.noteDao().deleteById(note.id) } }
                },
            )
        }
    }

    if (showHighlights) {
        var text by remember(documentId) { mutableStateOf("") }
        ModalBottomSheet(
            onDismissRequest = { showHighlights = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
                Text("Highlights", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Save a passage") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(16.dp),
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        val value = text.trim()
                        if (value.isNotEmpty()) {
                            scope.launch { runCatching { db.highlightDao().upsert(HighlightEntity(documentId = documentId, text = value)) } }
                            text = ""
                        }
                    }) { Text("Save highlight") }
                    TextButton(onClick = { showHighlights = false }) { Text("Done") }
                }
                if (highlights.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    LazyColumn(Modifier.heightIn(max = 380.dp)) {
                        items(highlights, key = { it.id }) { item ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Box(
                                    Modifier
                                        .size(width = 4.dp, height = 20.dp)
                                        .clip(CircleShape)
                                        .background(palette.accent),
                                )
                                Spacer(Modifier.size(10.dp))
                                Text(
                                    item.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = {
                                    scope.launch { runCatching { db.highlightDao().delete(item) } }
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove highlight")
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/** Small holder so the note sheet keeps its draft while the list refreshes. */
class NoteSheetState {
    var editingId by mutableStateOf<Long?>(null)
    var text by mutableStateOf("")
    var tags by mutableStateOf("")

    fun reset() {
        editingId = null
        text = ""
        tags = ""
    }
}

@Composable
private fun NoteSheet(
    notes: List<NoteEntity>,
    state: NoteSheetState,
    documentTitle: String,
    publicationTitle: String,
    onSave: (String, String) -> Unit,
    onEdit: (NoteEntity) -> Unit,
    onDelete: (NoteEntity) -> Unit,
) {
    Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
        Text(
            if (state.editingId == null) "Notes" else "Edit note",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        if (documentTitle.isNotBlank()) {
            Text(
                listOf(documentTitle, publicationTitle).filter { it.isNotBlank() }.joinToString(" Ã‚Â· "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = state.text,
            onValueChange = { state.text = it },
            label = { Text("Your note") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            shape = RoundedCornerShape(16.dp),
        )
        OutlinedTextField(
            value = state.tags,
            onValueChange = { state.tags = it },
            label = { Text("Tags (comma separated)") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                enabled = state.text.isNotBlank(),
                onClick = { onSave(state.text.trim(), state.tags.trim()) },
            ) {
                Text(if (state.editingId == null) "Save note" else "Update note")
            }
            if (state.editingId != null) {
                TextButton(onClick = { state.reset() }) { Text("Cancel edit") }
            }
        }
        if (notes.isNotEmpty()) {
            Text(
                if (notes.size == 1) "1 note on this page" else "${notes.size} notes on this page",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            LazyColumn(Modifier.heightIn(max = 320.dp)) {
                items(notes, key = { it.id }) { note ->
                    Surface(
                        onClick = { onEdit(note) },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                Text(note.content, style = MaterialTheme.typography.bodyMedium)
                                if (note.tags.isNotBlank()) {
                                    Text(
                                        note.tags.split(',').joinToString("  ") { "#${it.trim()}" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            IconButton(onClick = { onDelete(note) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete note")
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
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
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

private fun openExternally(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, url.toUri())
        )
    }
}
