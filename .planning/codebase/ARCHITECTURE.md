# Architecture

**Analysis Date:** 2026-02-12

## Pattern Overview

**Overall:** Multi-layered Android application with Rust backend integration and coroutine-based async architecture.

**Key Characteristics:**
- Backend-driven architecture: All collection operations go through serialized `CollectionManager` interface
- Protocol buffer-based communication with Rust backend (returns `OpChanges` messages)
- Kotlin coroutines for async work with automatic error handling and UI updates
- Observer pattern (`ChangeManager`) for reactive UI updates on backend changes
- MVVM with ViewModels and StateFlow for modern screen-level state management
- Library module (`libanki`) provides business logic with zero Android dependencies

## Layers

**Backend Layer (Rust):**
- Purpose: Core business logic and database operations (card scheduling, deck management, templates, sync)
- Location: External dependency `net.ankiweb.rsdroid:Backend`
- Contains: Native "rsdroid" library loaded by `AnkiDroidApp`
- Used by: `CollectionManager` and `libanki` module exclusively
- Interface: Returns protobuf messages (anki.* packages) and `OpChanges` objects

**LibAnki Layer:**
- Purpose: Kotlin-based collection abstraction and business logic bridge between UI and Rust backend
- Location: `libanki/src/main/java/com/ichi2/anki/libanki/`
- Contains: `Collection`, `Card`, `Note`, `Deck`, `Decks`, `Media`, `Notetypes`, schedulers, sync, import/export
- Depends on: Backend only (NO Android dependencies)
- Used by: `AnkiDroid` main app module via `CollectionManager`
- Key files: `Collection.kt` (primary interface), `Decks.kt`, `Notetypes.kt`, `Card.kt`, `Note.kt`

**CollectionManager (Serialization Layer):**
- Purpose: Single-threaded serialization point for all collection access, ensuring thread-safe database operations
- Location: `AnkiDroid/src/main/java/com/ichi2/anki/CollectionManager.kt`
- Contains: `withCol {}` suspend functions, backend lifecycle management, queue management
- Depends on: `LibAnki.Collection`, `Backend`
- Used by: Every UI component that needs database access
- Key Pattern: All calls serialized through `Dispatchers.IO.limitedParallelism(1)` queue

**Service Layer:**
- Purpose: Business logic wrappers that orchestrate collection operations and domain-specific tasks
- Location: `AnkiDroid/src/main/java/com/ichi2/anki/servicelayer/`, `AnkiDroid/src/main/java/com/ichi2/anki/backend/`
- Contains: `BackupManager`, `DatabaseCheck`, `ThrowableFilterService`, `DebugInfoService`, `BackendImporting`, `BackendExporting`, `BackendBackups`
- Depends on: `CollectionManager`, `LibAnki`
- Used by: UI layer (Activities/Fragments) for complex operations

**ViewModel/State Layer:**
- Purpose: Screen-level state management and data persistence across configuration changes
- Location: `AnkiDroid/src/main/java/com/ichi2/anki/viewmodels/` and individual screen directories
- Contains: StateFlow-based state, event flows, coroutine-based data loading
- Depends on: `CollectionManager`, service layer
- Used by: Fragments and Activities via Jetpack's `viewModelScope`

**UI Layer (Activities/Fragments):**
- Purpose: User interaction, display, and lifecycle management
- Location: `AnkiDroid/src/main/java/com/ichi2/anki/` and subdirectories (reviewer, cardviewer, browser, deckpicker, etc.)
- Contains: Activities, Fragments, Dialogs, Views
- Depends on: `ViewModels`, `ChangeManager` for updates, `CollectionManager` for direct operations
- Patterns: Extend `AnkiActivity`, implement `ChangeManager.Subscriber`, use view binding via `vbpd`

**Common Layer:**
- Purpose: Shared utilities, annotations, and interfaces without Android dependencies
- Location: `common/src/main/java/com/ichi2/anki/common/`
- Contains: Utilities, annotations (`@KotlinCleanup`, `@NeedsTest`), base interfaces
- Depends on: Nothing (foundation layer)
- Used by: All modules

## Data Flow

**Typical UI-to-Backend Flow:**

1. User action (tap button, input text) in Fragment/Activity
2. Fragment/Activity calls ViewModel method
3. ViewModel uses `launchCatchingIO {}` to run coroutine
4. Coroutine calls `CollectionManager.withCol { ... }` (suspend function)
5. `CollectionManager` queues operation on single-threaded IO dispatcher
6. Operation executes: `Collection.operation()` calls backend via protobuf
7. Backend returns `OpChanges` protobuf message
8. `CollectionManager` broadcasts changes to registered `ChangeManager.Subscriber` instances
9. Subscribers (Fragments/Activities) receive `opExecuted()` callback
10. Subscriber updates UI reactively (via StateFlow updates or direct view manipulation)
11. If operation throws `BackendException`, `launchCatchingIO` shows localized error dialog automatically

**State Management Flow:**

1. ViewModel maintains `MutableStateFlow<UiState>` and `MutableSharedFlow<UiEvent>`
2. Fragment observes state via `viewModel.uiState.collectAsState()`
3. State changes trigger Compose recomposition (or manual view updates in traditional layouts)
4. ViewModel also subscribes to `ChangeManager` for external collection changes
5. When `opExecuted()` called, ViewModel may reload data and update state
6. Multiple subscribers can react independently to same backend operation

**Change Notification Flow:**

```
Backend Operation (Collection.method())
    ↓
Returns OpChanges protobuf with affected areas
    ↓
CollectionManager broadcasts to ChangeManager subscribers
    ↓
ChangeManager.opExecuted(changes, handler) called on all subscribers
    ↓
Subscribers inspect OpChanges.studyQueues, .card, .deckConfig, etc.
    ↓
Each subscriber updates its own UI state independently
```

## Key Abstractions

**Collection:**
- Purpose: Wraps Rust backend operations in a Kotlin-friendly API
- Examples: `libanki/src/main/java/com/ichi2/anki/libanki/Collection.kt`
- Pattern: Suspend functions return backend results, all database mutations return `OpChanges`

**Card/Note/Deck:**
- Purpose: Represent domain objects from backend
- Examples: `Card.kt`, `Note.kt`, `Deck.kt` in libanki
- Pattern: Data classes with properties, backed by backend via Collection methods

**Backend:**
- Purpose: Singleton Rust backend instance
- Pattern: Lazily created by `BackendFactory.getBackend()`, managed by `CollectionManager`
- Accessed via: `CollectionManager.getBackend()` (rarely used directly)

**OpChanges:**
- Purpose: Protobuf message describing what changed after a backend operation
- Pattern: Boolean flags for categories (`studyQueues`, `card`, `deckConfig`, `noteTypes`, etc.)
- Usage: Subscribers inspect flags to decide what UI to refresh

**Translations (TR):**
- Purpose: Backend-provided, localized strings managed by Rust backend
- Access: `CollectionManager.TR.translationKey()`
- Pattern: Replaces string resources for dynamic or backend-controlled text

## Entry Points

**Application Launch:**
- Location: `AnkiDroid/src/main/java/com/ichi2/anki/AnkiDroidApp.kt`
- Triggers: System app launch
- Responsibilities: Initialize backend, setup logging, register lifecycle observers, handle crash reporting

**Main Activity:**
- Location: `AnkiDroid/src/main/java/com/ichi2/anki/DeckPicker.kt`
- Triggers: User launches app or returns from deep link
- Responsibilities: Display deck list, navigate to study/browser/preferences, manage app lifecycle

**Card Viewer/Reviewer:**
- Location: `AnkiDroid/src/main/java/com/ichi2/anki/reviewer/AbstractFlashcardViewer.kt`
- Triggers: User selects deck to study
- Responsibilities: Display card, handle answer input, update card state via backend

**Card Browser:**
- Location: `AnkiDroid/src/main/java/com/ichi2/anki/CardBrowser.kt`
- Triggers: User opens card browser
- Responsibilities: Search/filter cards, bulk edit, delete, reorder operations

**Content Provider API:**
- Location: `api/src/main/java/com/ichi2/anki/api/CardContentProvider.kt`
- Triggers: Third-party app queries via content provider
- Responsibilities: Expose decks, notes, models, schedule, media to external apps (async operations)

## Error Handling

**Strategy:** Multi-level error handling with automatic user-facing dialogs for backend errors, proper coroutine cancellation semantics.

**Patterns:**

**Backend Errors (BackendException):**
- Contain localized message suitable for users
- Caught by `launchCatchingIO` and shown in error dialog automatically
- Specific subtypes: `BackendInvalidInputException`, `BackendNetworkException`, `BackendSyncException`, `BackendInterruptedException`
- Example: User shows "Invalid input" message directly to end user

**Cancellation:**
- `CancellationException` re-thrown to preserve coroutine semantics
- Prevents coroutine hierarchy collapse on normal cancellation
- Example: User navigates away cancels pending operation

**Generic Exceptions:**
- Logged via Timber
- Reported as crashes via ACRA
- May trigger crash dialog if user-facing

**Custom Error Handling:**
- Use `launchCatchingIO(onError = { exception -> ... })` for custom error behavior
- Use `asyncIO {}` for background work returning results, then call `.await()` with try-catch

## Cross-Cutting Concerns

**Logging:**
- Framework: Timber with SLF4J backend
- Debug: `DebugTree` in development
- Production: `ProductionCrashReportingTree` sends to ACRA
- Pattern: `Timber.d("message")`, `Timber.w(exception)`, `Timber.e(exception, "message")`

**Validation:**
- Collection methods validate inputs; backend returns `BackendInvalidInputException` on invalid data
- Fragment/ViewModel code responsible for pre-validation (e.g., non-empty deck name)
- UI constraints prevent many invalid states (e.g., min/max on SeekBar)

**Authentication:**
- Handled by backend sync operations
- Certificate stored in `currentSyncCertificate` during login flow
- Sync UI manages sign-in/out and error handling

**Preferences:**
- Stored in `SharedPreferences` for app settings
- Accessed via `Prefs` enum and `sharedPrefs()` extension
- Some preferences affect backend behavior (language, theme, study options)

---

*Architecture analysis: 2026-02-12*
