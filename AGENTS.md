# AGENTS.md

Guidance and complete project documentation for AI coding agents working on the Keep NC repository.

## Project Overview

Android client for **Nextcloud Notes**, with a UI inspired by Google Keep (card grid layout, Material 3 theming, categories, interactive checklists, offline-first sync). The developer is a beginner in Android development — prioritize clarity, clean architecture, and explicit explanations.

---

## Tech Stack

| Layer | Choice |
|---|---|
| Language | Kotlin 2.1.0 |
| UI | XML Views (classic View system) + RecyclerView + StaggeredGridLayoutManager + Material 3 |
| Architecture | MVVM (ViewModel + StateFlow) + Single Activity |
| Networking | Retrofit 2.11.0 + OkHttp 4.12.0 (Nextcloud Notes API v1 + Login Flow v2) |
| Local storage | Room 2.7.1 (offline-first source of truth) |
| DI | Hilt 2.56.2 |
| Markdown | Markwon 4.6.2 (with task list, strikethrough, tables, linkify, soft line break plugin) |
| Min SDK | 28 (Android 9.0) |
| Compile / Target SDK | 35 |

---

## Architecture Conventions & Rules

1. **Layers**:
   - `data`: Room entities (`NoteEntity`), DAOs (`NoteDao`), Retrofit API (`NotesApi`), DTOs (`NoteDto`), dynamic auth interceptor (`AuthInterceptor`), token storage (`TokenStorage`), repository (`NotesRepository`).
   - `ui`: Activities (`MainActivity`, `LoginActivity`), Fragments (`NotesFragment`, `EditorFragment`), BottomSheets (`CategoryPickerBottomSheet`), ViewModels, Adapters.
2. **Single Source of Truth**:
   - Room database is the **only** source of truth for the UI layer.
   - ViewModels observe Room flows; the UI never calls Retrofit or handles network responses directly.
3. **Offline-First Bidirectional Sync**:
   - Local writes (create, update, delete) happen immediately in Room with `SyncStatus.DIRTY` or `SyncStatus.PENDING_DELETE`.
   - `NotesRepository.syncWithServer()` reconciles local and remote notes using `etag` and `modified` timestamps, handles server-side deletions (Step 4 cleanup), and gracefully handles HTTP 404.
   - Saving a note in the editor immediately launches server sync in the background.
4. **State Exposure**:
   - ViewModels expose `StateFlow`, never raw suspend functions or callbacks, to the UI layer.
   - State models must handle `Loading`, `Empty`, `Success`, and `Error` (with `@StringRes` localization support).

---

## Project Structure & Key Components

```
app/src/main/
├── java/com/keepnc/
│   ├── data/
│   │   ├── auth/          (Credentials, TokenStorage, LoginFlowService)
│   │   ├── local/         (AppDatabase, NoteDao, NoteEntity, SyncStatus, Converters)
│   │   ├── remote/        (AuthInterceptor, NotesApi, dto/NoteDto)
│   │   └── repository/    (NotesRepository)
│   ├── di/                (AppModule, AuthModule)
│   ├── ui/
│   │   ├── auth/          (LoginActivity, LoginViewModel, LoginState)
│   │   ├── editor/        (EditorFragment, EditorViewModel, EditorUiState, LargeTaskCheckboxSpan, CategoryPickerBottomSheet, EditorActionsBottomSheet)
│   │   ├── notes/         (NotesFragment, NotesViewModel, NotesUiState, NotesFilter, NoteCardAdapter)
│   │   ├── MarkwonFactory.kt (Unified Markwon configuration with SoftLineBreak newline handler)
│   │   ├── MainActivity.kt
│   │   └── MainViewModel.kt
│   └── work/              (SyncWorker for background periodic sync)
└── res/
    ├── layout/            (activity_main, activity_login, fragment_notes, fragment_editor, item_note_card, dialog_category_picker, item_category_picker, dialog_editor_actions, nav_header)
    ├── values/            (strings.xml - English default, colors.xml, themes.xml)
    └── values-ru/         (strings.xml - Russian localization)
```

---

## Critical Implementation Rules & Lessons Learned (DO NOT REPEAT MISTAKES)

### 1. Custom Spans & View Constructor Signatures
- **`LargeTaskCheckboxSpan`**: Constructor signature is `(context: Context, checked: Boolean, onToggle: (Boolean) -> Unit = {})`.
- **Always verify constructor signatures**: Before instantiating custom spans, views, or helper classes, verify their parameter list in the definition file to avoid argument type mismatch compile errors.

### 2. Localization (i18n)
- **Zero hardcoded user-facing strings**: All text shown in the UI, snackbars, dialogs (`MaterialAlertDialogBuilder`), and error states must use string resources (`R.string.*`).
- Both `res/values/strings.xml` (English fallback) and `res/values-ru/strings.xml` (Russian) must have matching string keys 1:1.
- Use `@StringRes val messageRes: Int?` in UI state classes (`EditorUiState.Error`, `LoginState.Error`) to allow ViewModels to emit localized error resources cleanly.

### 3. Markdown Rendering & Title Deduplication
- **Soft line breaks**: Standard CommonMark converts single newlines (`\n`) to spaces. We use a custom `AbstractMarkwonPlugin` in `MarkwonFactory.kt` with `builder.on(SoftLineBreak::class.java) { visitor, _ -> visitor.ensureNewLine() }` so every newline renders as an actual line break on screen.
- **Title stripping on note open**: Nextcloud Notes web / browser clients often put the title as the first line of content. When opening an existing note in `EditorViewModel.loadNote(id)`:
  - If `note.title.isNotBlank()` and `note.content.substringBefore('\n').trim() == note.title.trim()`:
    - The first line is stripped from `content` (`note.content.substringAfter('\n', missingDelimiterValue = "").trimStart('\r', '\n')`).
    - The cleaned note is immediately saved to Room (`repository.updateNote(...)`) and synced to the Nextcloud server (`repository.syncWithServer()`).
    - `initialContent` in `EditorViewModel` is initialized with the cleaned content so `hasChanges()` returns `false` (no save dialog is shown on exit unless the user made additional edits).
- **Title deduplication in cards**: In note cards on the main screen (`NoteCardAdapter`), visual deduplication is maintained as a fallback for notes synced from the server that have not yet been opened in the editor.

### 4. Interactive Checklists
- In Markwon 4.6.2+, `MarkwonFactory.createForEditor(context)` configures `builder.blockMargin(32dp)` with `TaskListPlugin.create(LargeTaskCheckboxDrawable(context, 16f))` for 16dp checkboxes with a 16dp gap before text in the note editor (Google Keep style).
- In note cards on the main screen, `MarkwonFactory.createForCard(context)` uses standard compact checkboxes.
- Checked checklist items automatically render with strikethrough (`~~text~~`). Tapping flips state between `- [ ] text` and `- [x] ~~text~~`.
- In Preview mode, `tv_preview` is hosted in `NestedScrollView` with touch-slop gesture discrimination in `setOnTouchListener` so vertical dragging scrolls smoothly while precise taps flip `- [ ]` ↔ `- [x]`.
- In Edit mode, pressing Enter on a checklist line auto-inserts `${indent}- [ ] ` on the new line (preserving indentation). Pressing Enter on an empty `- [ ] ` line removes the prefix to cleanly exit the checklist.
- Switching to Preview mode or saving automatically cleans up and removes any trailing/empty checklist lines (`- [ ] ` with no text).

### 5. Category Management
- Category selection is handled via `CategoryPickerBottomSheet` (not inline text inputs).
- In Preview mode, the category chip is a read-only badge (or hidden if empty).
- In Edit mode, clicking the chip opens the bottom sheet to pick an existing category, remove categorization («Без категории»), or create a new one on the fly.

### 6. Save Confirmation on Exit
- In `EditorFragment`, exiting via back button or toolbar arrow checks `viewModel.hasChanges()`.
- If unsaved changes exist, show `MaterialAlertDialogBuilder` with Save, Discard, and Cancel options.

### 7. Material 3 FABs & Editor Actions BottomSheet
- Editor bottom row provides 3 Floating Action Buttons:
  - Bottom-Left: `fab_favorite` (Favorite toggle: `ic_star_outline` / `ic_star_filled`).
  - Bottom-Right: `fab_more` (`⋮` 3-dots overflow FAB opening `EditorActionsBottomSheet`).
  - Bottom-Right (left of `fab_more`): `fab_mode` (Preview / Edit mode toggle: `ic_preview` / `ic_edit`).
- `EditorActionsBottomSheet` provides:
  - "Сделать список" / "Обычный текст" ("Make checklist" / "Plain text" — dynamic title based on whether the note currently contains checkboxes).
  - "Категория" (opens `CategoryPickerBottomSheet`).
  - "Удалить заметку" (shows confirmation dialog, deletes via `EditorViewModel.deleteNote()`, syncs with server, and navigates back).
- Checklist conversion:
  - Text ➔ Checklist: prepends `- [ ] ` to each non-empty line.
  - Checklist ➔ Text: strips `- [ ] `, `- [x] `, and `~~...~~` strikethroughs.

### 8. Testing & Architecture Notes
- Unit tests are located in `app/src/test/java/com/keepnc/ui/editor/EditorViewModelTest.kt` using `kotlinx-coroutines-test` and JUnit 4.
- In unit tests, `NotesRepository` dependencies (`NoteDao`, `NotesApi`) can be faked, and `TokenStorage` instantiated via reflection without Android Context.

---

## Build & Test Commands (Windows Environment)

On this Windows development environment, `grep` is not in PATH (use `Get-ChildItem -Recurse | Select-String` or `view_file`). Java and Gradle must be run pointing to Android Studio's JBR and Gradle dists:

```powershell
# Run Unit Tests
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; & "C:\Users\Sabirzjanov\.gradle\wrapper\dists\gradle-8.11.1-bin\bpt9gzteqjrbo1mjrsomdt32c\gradle-8.11.1\bin\gradle.bat" testDebugUnitTest

# Assemble Debug APK
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; & "C:\Users\Sabirzjanov\.gradle\wrapper\dists\gradle-8.11.1-bin\bpt9gzteqjrbo1mjrsomdt32c\gradle-8.11.1\bin\gradle.bat" assembleDebug
```
