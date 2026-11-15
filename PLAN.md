# KnowNow — Android App Plan

## 0. Feasibility verdict

**Yes, this is feasible.** The ServiceNowDocs repository is a public, static, markdown-only
documentation store with a very regular shape:

- One git branch per release family (`brazil`, `australia`, `zurich`, `yokohama`,
  `mobile`, `other`, `xanadu`, …). Branches are listed by the GitHub branches API.
- Each branch contains `markdown/<publication>/` folders. Every publication has an
  `index.md` that is a complete, nested table of contents whose links are absolute
  raw GitHub URLs (`https://raw.githubusercontent.com/ServiceNow/ServiceNowDocs/{branch}/markdown/...`).
- Each content file is a single markdown document with YAML frontmatter
  (`title`, `description`, `canonical_url`, `release`, `topic_type`,
  `last_updated`, `reading_time_minutes`, `keywords`, `breadcrumb`).

That gives us everything an offline reader needs: a release list, a topic list, a
document list, the document bodies, and metadata. Nothing requires scraping the
JavaScript SPA at servicenow.com/docs.

The two real constraints to design around are:

1. **GitHub rate limits.** Unauthenticated API/raw requests are limited
   (~60/hour). A publication can contain hundreds to thousands of files, so a naive
   “one HTTP request per file” download will fail for large topics.
2. **Volume.** A single publication (e.g. IT Service Management) is hundreds of
   files and tens of megabytes. Downloads must be resumable, throttled and
   cancellable.

Both are solvable (see §6 and §7).

---

## 1. Product goals

1. **Choose a release** (branch) and a **topic** (publication) from the repo.
2. **Download** that topic’s documentation to the device, with progress.
3. **Read** it offline with fast markdown rendering.
4. **Navigate** from one topic item to the next (prev/next in TOC order, plus a
   document outline and the publication tree).
5. **Search** across everything downloaded.
6. **Study aids**: bookmarks, reading progress, adjustable typography.

Non-goals for v1: editing docs, syncing user notes to GitHub, images (the repo
contains none), or arbitrary non-ServiceNow repos. The data source is abstracted so
a generic GitHub-markdown source can be added later (see §3).

---

## 2. UX sketch

- **Home / Library** — downloaded topics with size, file count, last updated.
  Empty state invites “Browse releases”.
- **Browse** — release chips (latest first) → publication grid → TOC preview
  with per-section selection and “Download (N files, ~X MB)”.
- **Reader** — edge-to-edge, Material 3, dynamic color.
  - Top bar: back, title, bookmark, text settings, search-in-doc.
  - Body: rendered markdown, max readable width on tablets, code blocks with copy.
  - Bottom: previous / next document, reading progress.
  - Drawers: publication TOC (tree) and document outline (headings).
- **Search** — global FTS over downloaded docs with highlighted snippets.
- **Settings** — theme (system/light/dark), font size & line spacing, optional
  GitHub token, storage management, clear cache.

Visual language follows the existing `Habtab` project in this workspace: Compose,
Material 3, edge-to-edge, custom background brush, 320 ms FastOutSlowIn
transitions, haptics. Reuse that project’s build conventions (AGP 9.4, Kotlin
2.2.x, Compose BOM 2026.09, Room 2.8, Navigation Compose 2.10, minSdk 26,
target/compileSdk 37, Java 17).

---

## 3. Data source (GitHub)

### 3.1 Endpoints used

| Purpose | Endpoint |
|---|---|
| List releases | `GET /repos/ServiceNow/ServiceNowDocs/branches` |
| Publication list + TOC | `GET https://raw.githubusercontent.com/.../{branch}/llms.txt` (or list `markdown/` tree) |
| Publication TOC | `GET .../markdown/{pub}/index.md` |
| Document body | `GET .../markdown/{pub}/{path}.md` |
| (Optional) full tree | `GET /repos/.../git/trees/{branch}?recursive=1` |

Prefer `llms.txt` + `index.md` over the Trees API: fewer requests, and `index.md`
already gives the intended reading order and nesting. The Trees API is a fallback
for publications without an `index.md` and for computing sizes.

### 3.2 Parsing `index.md`

Format is nested markdown bullets:

```markdown
- [Title](https://raw.../file.md) -- description
  - [Child](https://raw.../child.md) -- description
```

Parser extracts `(depth, title, url, description)` using indentation (2 spaces per
level) and a regex over `[text](url) -- desc`. URLs are deduplicated by path;
cross-publication links (different `{pub}`) are flagged `external = true`.

### 3.3 Frontmatter

Small hand-written parser (no SnakeYAML dependency) for the flat key/value +
bracketed arrays used here. Fields: `title`, `description`, `locale`,
`canonical_url`, `release`, `topic_type`, `last_updated`, `reading_time_minutes`,
`keywords`, `breadcrumb`.

### 3.4 Source abstraction

```kotlin
interface DocSource {
    suspend fun releases(): List<Release>
    suspend fun publications(release: Release): List<Publication>
    suspend fun toc(release: Release, publication: Publication): TocNode
    suspend fun download(release, publication, selection, onProgress): DownloadResult
}
```

`ServiceNowDocSource` implements it. A future `GenericGitHubDocSource` can reuse
the same contract for any repo that follows the `markdown/<pub>/index.md`
convention.

---

## 4. Architecture

Single `:app` module, layered:

```
ui/            Compose screens + ViewModels (StateFlow)
domain/        use cases (BrowseReleases, DownloadTopic, ReadDocument, SearchDocs)
data/          repositories, Room, network, file storage
core/          parsers, dispatchers, result types
```

- **DI**: none beyond `viewModel()` + manual repository wiring in `Application`
  (matches Habtab). Hilt is optional if the graph grows.
- **Concurrency**: coroutines + `Flow`. Download work runs in `WorkManager` so it
  survives process death and shows a notification.
- **State**: unidirectional. Screens collect `StateFlow<UiState>`.

---

## 5. Data model (Room)

```kotlin
@Entity data class ReleaseEntity(
    @PrimaryKey val name: String,          // "brazil"
    val displayName: String,               // "Brazil"
    val commitSha: String?,
    val isLatest: Boolean,
    val syncedAt: Long
)

@Entity data class PublicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val releaseName: String,
    val slug: String,                      // "it-service-management"
    val title: String,
    val docCount: Int,
    val totalBytes: Long,
    val downloadedAt: Long?,
    val status: DownloadStatus             // NOT_DOWNLOADED / DOWNLOADING / READY / FAILED
)

@Entity data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val publicationId: Long,
    val title: String,
    val description: String?,
    val relativePath: String,              // "change-management/...md"
    val localPath: String?,                // absolute file path when downloaded
    val canonicalUrl: String?,
    val lastUpdated: String?,
    val readingTimeMinutes: Int?,
    val topicType: String?,
    val keywords: List<String>,
    val sortOrder: Int,
    val parentPath: String?,               // for tree navigation
    val contentHash: String?               // sha256 of body, for refresh
)

@Entity data class BookmarkEntity(
    @PrimaryKey val documentId: Long,
    val createdAt: Long,
    val scrollOffset: Int
)

@Entity data class ReadingProgressEntity(
    @PrimaryKey val documentId: Long,
    val lastReadAt: Long,
    val progressPercent: Float
)

// FTS4 virtual table over DocumentEntity.title + cached plain text
```

Files live under `context.filesDir/docs/{release}/{publication}/{relativePath}`,
so they are private, backed up with the app, and removable via “Clear storage”.

---

## 6. Download strategy (rate-limit aware)

1. **Fetch TOC first** (`index.md`) → build the document list and sizes (HEAD or
   a single Trees call per publication).
2. **Selection**: user can download the whole publication or a subtree. Default
   selection = whole publication.
3. **Queue**: `WorkManager` `CoroutineWorker` with a `ForegroundInfo` notification.
   - Concurrency cap of **3 parallel downloads**.
   - Per-request delay of ~250 ms when no token is configured (≈ 12 req/s burst
     but sustained under 60/hour → actually cap to ~1 req/s unauthenticated).
   - With a user-supplied token, raise to 8 parallel, no artificial delay.
4. **Resumability**: each file is downloaded to a `.part` file and renamed on
   success; already-present files (matching hash) are skipped. WorkManager
   retries with exponential backoff on 429/5xx.
5. **Cancellation**: cancelling a download deletes partial files and resets the
   publication status to `NOT_DOWNLOADED`.
6. **Refresh**: compare `last_updated` in frontmatter; re-download only changed
   files (content hash check).
7. **Alternative (optional)**: “Download release archive” uses
   `codeload.github.com/.../zip/{branch}` in one request, then extracts the
   selected publication. Faster but larger download. Offer as an advanced option.

Token storage: encrypted with `EncryptedSharedPreferences` (androidx.security).
Never log it. Show a “GitHub token (optional)” field in Settings with a link to
create a fine-grained read-only PAT.

---

## 7. Rendering & navigation

- **Markdown engine**: evaluate `com.mikepenz:multiplatform-markdown-renderer-m3`
  (Compose-native, Material 3) vs. Markwon via `AndroidView`. Decision gate in
  Phase 0 spike: test tables, fenced code, nested lists, and theming. Keep the
  renderer behind an interface so it can be swapped.
- **Document outline**: parse headings from the rendered markdown into a list and
  show in a bottom sheet; tap scrolls to heading.
- **Publication TOC**: tree from `index.md`, with checkmarks for downloaded docs;
  tap navigates; long-press shows description.
- **Prev/Next**: walk the flattened TOC (depth-first) of downloaded documents.
- **Cross-publication links**: if the target is downloaded, navigate; otherwise
  offer “Download this topic” or open the canonical URL in the browser.
- **Search in document**: client-side highlight over the rendered text.

---

## 7.1 Adaptive table & content rendering (phone-first)

Sampled files (`r_ITServiceManagement.md`, `itsm-apps-overview.md`,
`r_ChangeManagementProperties.md`, `r_ChangeRiskCalculator.md`,
`c_GlideFormAPI.md`) show four content shapes that a naive renderer handles
badly on a phone:

1. **HTML parameter tables** — `<table class="parameters">` with
   `Name | Type | Description`, where `Description` can contain long lists
   (e.g. 40 icon names). A 3-column grid with tiny text is unreadable.
2. **HTML property tables** — `Name | Description`, where the description is
   multi-line and contains nested lists/code.
3. **HTML nav-card tables** — 2×3 grids of linked cards (landing pages).
4. **GFM pipe tables** — both 2- and 3-column, sometimes with empty cells.

### 7a.1 Normalised table model

Parse both HTML (`<table>`) and pipe tables into one model:

```kotlin
data class TableModel(
    val caption: String?,
    val header: List<Cell>,      // empty => no header
    val rows: List<List<Cell>>,
    val kind: TableKind          // PARAMETERS, PROPERTIES, NAV_CARDS, GENERIC
)
data class Cell(val blocks: List<Block>, val span: Int = 1)
```

`TableKind` is inferred from header text / CSS class (`parameters`,
`nav-card`, etc.) and column count. This drives the rendering choice.

### 7a.2 Rendering strategy per kind

| Kind / shape | Phone rendering |
|---|---|
| `PROPERTIES` (2 cols, short key + long value) | **Card list**: key as title (monospace), value body with markdown. No horizontal scroll. |
| `PARAMETERS` (Name/Type/Description) | **Parameter cards**: name (bold) + type chip + description; collapsible “show all values” for long enumerations. |
| `NAV_CARDS` (grid of links) | **2-column card grid** (or horizontal pager on very small widths) with icon + title + subtitle. |
| `GENERIC` 2 cols, both short | Compact table (no scroll). |
| `GENERIC` 3 cols, short cells | Compact table with wrapped text. |
| `GENERIC` 4+ cols or any cell > ~40 chars wide | **Horizontally scrollable table** with sticky first column + zebra rows + a “View as cards” toggle that renders each row as a labelled card. |
| Very wide (≥ 6 cols) | Default to card view; table view available via toggle. |

Every table also gets: copy-as-TSV action, and a “open in browser” link to the
canonical page for users who want the original layout.

### 7a.3 Lazy, block-based document rendering

Whole-document `Text`/`AndroidView` composables cause jank on long pages. Instead:

- Parse markdown into a list of `Block` (heading, paragraph, list, table, code,
  quote, hr, html-block).
- Render with a single `LazyColumn` (or `LazyVerticalStaggeredGrid` for
  nav-cards), one item per block, with `contentType = block.type` for view
  recycling.
- Headings are sticky-ish via a “current section” indicator in the top bar
  (derived from scroll position), not pinned headers (avoids overlap bugs).
- Code blocks: horizontal scroll inside the block, copy button, optional
  line numbers for blocks > 10 lines.
- Lists: nested indentation capped at 3 levels on phone; deeper levels collapse
  into a “show more” affordance.

### 7a.4 Reading ergonomics

- Max content width 720 dp on tablets; phone uses full width with 16 dp gutters.
- Adjustable font scale (0.85×–1.3×) and line height; persisted in DataStore.
- “Focus mode”: hides top/bottom bars on scroll (like a book reader).
- Estimated reading time from frontmatter; progress saved per document.

---

## 8. Search
- Room FTS4 virtual table `DocumentFts` keyed by `documentId`, populated during
  download with the plain-text body (markdown stripped of markup).
- Query returns document + snippet. Debounce input 300 ms.
- Filters: current release, current publication.

---

## 8.1 Performance, memory & battery

Targets: smooth 60 fps scrolling on mid-range devices, < 150 MB peak memory for a
large document, no battery drain in background.

- **Rendering**: LazyColumn block rendering (§7a.3); `remember`/`derivedStateOf`
  for scroll-derived UI; avoid `Modifier.graphicsLayer` overuse; baseline
  profiles for cold start.
- **Parsing**: run on `Dispatchers.Default` via coroutines; cache parsed
  `List<Block>` keyed by (documentId, contentHash) in memory (LRU, 64 docs) and
  on disk (serialized). Re-parse only when content changes.
- **Database**: Room with indices on foreign keys; FTS4 populated in a
  background transaction after download; suspend functions + Flow; no main-thread
  DB access.
- **Downloads**: WorkManager with `NetworkType.CONNECTED` constraint; for large
  topics add `requiresCharging = true` option (user toggle); 3 parallel requests
  max; exponential backoff on 429/5xx; partial-file resume; skip-if-hash-matches.
- **Storage**: stream downloads to disk (no in-memory buffering); periodic
  cleanup of `.part` files; report per-topic size in UI.
- **Images**: none in this repo, but the markdown contains `[Omitted image ...]`
  placeholders — render as a subtle “image omitted” chip, no network fetch.
- **Memory hygiene**: no static caches of whole publications; `ViewModel` scopes
  cleared on navigation; `WeakReference`-free patterns via Compose state.
- **Release build**: R8 minification + resource shrinking + Baseline Profile;
  verify with `bundletool` and Macrobenchmark (startup + scroll jank).
- **Thermal/battery**: no foreground services except during active download;
  WorkManager handles deferral; no polling loops.

---

## 9. Risks & mitigations

| Risk | Mitigation |
|---|---|
| GitHub rate limit (60/h) | Throttle + optional PAT + archive option + skip-if-present |
| Huge publications (1000+ files) | Subtree selection, background WorkManager, progress UI |
| Filename length (Windows clone note) | We don’t clone; store by relative path, sanitize `:` etc. |
| Markdown dialect edge cases | Phase 0 rendering spike; renderer behind interface |
| Offline link targets missing | Graceful “not downloaded” affordance + on-demand fetch |
| Storage bloat | Show size per topic, bulk delete, clear cache |
| API shape changes | Source abstraction + versioned parsers, tests with fixtures |

---

## 13. Material 3 design language & modern coding practices

### Design (latest stable Material 3)
- Material 3 with dynamic color (Monet) on Android 12+, with a branded fallback
  palette for older devices (matches Habtab approach).
- Edge-to-edge layout, insets handling, predictive back gesture support.
- Material 3 components: `Scaffold`, `TopAppBar`, `NavigationBar`,
  `ModalBottomSheet`, `SearchBar`, `FilterChip`, `ElevatedCard`,
  `ListItem`, `ProgressIndicator`.
- Motion: shared element transitions (Compose 1.7+) between publication card →
  reader; 320 ms FastOutSlowIn for push/pop (Habtab convention).
- Typography: Material 3 type scale with a reader-specific “body large”
  override for long-form text; support font scaling up to 2×.
- Dark/light/system themes; per-screen “focus mode” hides chrome on scroll.
- Accessibility: TalkBack labels, minimum 48 dp touch targets, reduced-motion
  respect, content descriptions for omitted images.

### Coding practices
- Kotlin 2.2.x, coroutines + Flow, Compose (no XML views except where a
  library forces it — none planned).
- Room 2.8 with KSP, R8, Baseline Profiles.
- Architecture: UI → ViewModel (StateFlow) → UseCase → Repository → (Room +
  Retrofit/OkHttp + FileSystem). No business logic in Composables.
- Error handling: sealed `Result` types; `Snackbar` for recoverable errors;
  retry affordances.
- Tests: JUnit4 unit tests for parsers/rendering model; Room instrumented
  tests; Compose UI tests for key flows; screenshot tests (Paparazzi) optional.
- Quality gates: ktlint + detekt in CI; no warnings as errors initially, then
  tighten.
- Version catalog (`libs.versions.toml`) shared with Habtab versions for
  consistency.

---

## 10. Milestones

| Phase | Scope | Est. |
|---|---|---|
| 0 — Spike | Render sample docs (incl. HTML + pipe tables, parameter cards); confirm renderer; parse `index.md`/frontmatter; measure download sizes & memory | 1.5 wk |
| 1 — Scaffold | Project + Room + network + parsers + tests | 2 wk |
| 2 — Browse & download | Release/publication browse, selection, WorkManager download, library | 2 wk |
| 3 — Reader | Markdown render, TOC/outline, prev/next, bookmarks, progress | 2 wk |
| 4 — Search & settings | FTS, global search, typography settings, token, storage mgmt | 1.5 wk |
| 5 — Polish & release | Accessibility, animations, tests, signing, store assets | 1.5 wk |

Total ≈ 10 weeks for a v1, one developer.

---

## 11. Open questions for you

1. Is “choose a repo” meant literally (any GitHub repo), or “choose a release
   (branch) + topic”? This plan assumes the latter, with an abstraction that
   allows the former later.
2. Should downloaded files be user-visible (Storage Access Framework folder) or
   app-private (default, recommended)?
3. Do you want an optional GitHub token flow, or should the app always work
   anonymously (slower downloads)?
4. Target audience: just you (sideload) or Play Store? Affects signing, minSdk,
   and whether to include analytics/crash reporting (plan assumes none).

---

## 14. Edge cases & resilience (innovative handling)

### Notes & bookmarks vs. document lifecycle
- Notes store **snapshots** (`documentTitleSnapshot`, `publicationTitleSnapshot`,
  `canonicalUrl`, `releaseName`) in addition to soft foreign keys, so they remain
  useful after a topic/version is deleted.
- Deleting a topic marks its notes `isOrphaned = true` instead of deleting them;
  they appear under a "Detached notes" filter with a re-attach action (pick a new
  downloaded document) or export option.
- Bookmarks on deleted documents are removed (they are navigation shortcuts, not
  study content), but the user is notified.
- Re-downloading a topic with the same slug re-links previously orphaned notes by
  matching `canonicalUrl`/title snapshot where possible.

### Downloads
- Partial files use `.part` suffix and are resumed; interrupted downloads keep
  progress and retry with backoff.
- Per-file hash check skips already-downloaded content (idempotent re-runs).
- If `index.md` changes between releases, only new/changed files are fetched.
- 429/5xx responses trigger exponential backoff; after N failures the
  publication is marked `FAILED` with a retry action.
- Storage-full errors surface a clear message and pause the queue.

### Versioning
- Branch list is re-synced on launch; removed branches (old releases) are flagged
  rather than silently dropped, and their downloaded data is kept until the user
  deletes it.
- Cross-publication links to non-downloaded docs offer "Download this topic" or
  open the canonical URL.

### Rendering
- HTML tables are normalized; malformed/empty tables fall back to a monospace
  raw block.
- Very long code blocks are horizontally scrollable with a copy button.
- Oversized documents are streamed block-by-block via LazyColumn to keep memory
  bounded.
- Images (none in this repo) render as an "omitted image" chip, no network
  fetch.

### Search
- Empty/whitespace queries are ignored; FTS queries are escaped.
- Search runs on a background dispatcher; results are limited and paginated.

### Reliability
- All DB writes are transactional; UI state is derived from Room Flows.
- WorkManager handles process death; unique work names prevent duplicate queues.
- Destructive migration is dev-only; production will use Room migrations.

---

## 12. First concrete next step

Create the project skeleton by copying the `Habtab` build setup (AGP 9.4,
Kotlin 2.2.10, Compose BOM 2026.09, Room, Navigation) into a new
`ServiceNowDocsReader/` folder, then implement Phase 0: a throwaway screen that
downloads and renders `markdown/it-service-management/exploring-itsm.md` and a
parser test for `index.md`. That validates the two riskiest assumptions (rendering
and rate limits) before committing to the full build.
