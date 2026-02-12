# Codebase Structure

**Analysis Date:** 2026-02-12

## Directory Layout

```
Anki-Android/
├── AnkiDroid/                  # Main application module (app)
├── libanki/                    # Business logic, no Android dependencies
├── common/                     # Shared utilities and base classes
├── api/                        # ContentProvider-based third-party API
├── testlib/                    # Shared testing utilities
├── vbpd/                       # View Binding Property Delegate library
├── lint-rules/                 # Custom Android Lint checks
├── tools/                      # Build and development tools
├── docs/                       # Documentation
├── annotations/                # Custom Android annotations
├── gradle/                     # Gradle wrapper and scripts
├── build.gradle.kts            # Root build configuration
├── settings.gradle.kts         # Module configuration
└── .planning/                  # GSD analysis and planning documents
```

## Directory Purposes

**AnkiDroid/:**
- Purpose: Main Android application with UI, activities, fragments, dialogs
- Contains: All user-facing code, activities, fragments, view logic, preferences UI
- Key subdirectories:
  - `src/main/java/com/ichi2/anki/` - Application code
  - `src/main/res/` - Resources (layouts, strings, drawables, animations)
  - `src/test/java/` - Unit tests (Robolectric)
  - `src/androidTest/java/` - Integration tests (Espresso)
  - `src/main/AndroidManifest.xml` - App manifest

**libanki/:**
- Purpose: Kotlin abstraction layer for Rust backend, zero Android dependencies
- Contains: `Collection`, card scheduling, deck management, templates, sync, import/export
- Key subdirectories:
  - `src/main/java/com/ichi2/anki/libanki/` - Core classes
  - `src/main/java/com/ichi2/anki/libanki/backend/` - Backend integration
  - `src/main/java/com/ichi2/anki/libanki/sched/` - Scheduling algorithms
  - `src/main/java/com/ichi2/anki/libanki/utils/` - Utilities (HTML, LaTeX, etc.)
  - `src/test/java/` - Unit tests (runs fast, no Android)

**common/:**
- Purpose: Shared utilities, annotations, and base interfaces
- Contains: Utility functions, annotations, extensions, no Android-specific code
- Key subdirectories:
  - `src/main/java/com/ichi2/anki/common/` - Common utilities
  - `src/main/java/com/ichi2/anki/utils/` - Extension functions and helpers

**api/:**
- Purpose: Third-party developer integration via ContentProvider
- Contains: `CardContentProvider`, contract definitions, URI handling
- Exposes: Decks, notes, models, schedule, media
- Pattern: All operations return Cursor or require background execution

**testlib/:**
- Purpose: Shared test utilities and test runners
- Contains: Custom test runners, fixtures, common test setup
- Key files: `NewCollectionPathTestRunner`, test utilities

**vbpd/:**
- Purpose: View Binding Property Delegate library (internal utility)
- Contains: Type-safe view binding helpers for Activities/Fragments/ViewHolders

**lint-rules/:**
- Purpose: Custom Android Lint checks enforcing AnkiDroid patterns
- Contains: Lint rule implementations for project-specific conventions

**tools/:**
- Purpose: Build scripts, development utilities, maintenance scripts
- Subdirectories: `quality-check/`, `localization/`, `test-decks/`, `emulator_performance/`, etc.

**docs/:**
- Purpose: Developer documentation and architectural notes
- Contains: Code conventions guide, storage info, graphics, marketing materials

## Key File Locations

**Entry Points:**

- `AnkiDroid/src/main/AndroidManifest.xml`: App manifest, permissions, services, receivers, content providers
- `AnkiDroid/src/main/java/com/ichi2/anki/AnkiDroidApp.kt`: Application class, backend initialization, crash reporting setup
- `AnkiDroid/src/main/java/com/ichi2/anki/DeckPicker.kt`: Main Activity (deck list screen)
- `AnkiDroid/src/main/java/com/ichi2/anki/reviewer/AbstractFlashcardViewer.kt`: Card review Activity
- `AnkiDroid/src/main/java/com/ichi2/anki/CardBrowser.kt`: Card search/edit Activity

**Configuration:**

- `build.gradle.kts`: Root build configuration, shared tasks, lint setup
- `settings.gradle.kts`: Module includes and repository configuration
- `gradle.properties`: Gradle properties (version, flags)
- `AnkiDroid/build.gradle.kts`: App module build config, dependencies, flavors, signing
- `libanki/build.gradle.kts`: LibAnki module config, dependencies on rsdroid backend

**Core Logic:**

- `AnkiDroid/src/main/java/com/ichi2/anki/CollectionManager.kt`: Singleton for serialized collection access
- `AnkiDroid/src/main/java/com/ichi2/anki/CoroutineHelpers.kt`: Async coroutine helpers and error handling
- `AnkiDroid/src/main/java/com/ichi2/anki/observability/ChangeManager.kt`: Observer pattern for backend changes
- `libanki/src/main/java/com/ichi2/anki/libanki/Collection.kt`: Collection abstraction over Rust backend
- `libanki/src/main/java/com/ichi2/anki/libanki/Decks.kt`: Deck management operations

**Testing:**

- `AnkiDroid/src/test/java/`: Unit tests (run via Robolectric)
- `AnkiDroid/src/androidTest/java/`: Integration tests (run on emulator/device)
- `libanki/src/test/java/`: LibAnki unit tests
- `testlib/src/main/java/com/ichi2/anki/testlib/`: Test utilities and runners

## Naming Conventions

**Files:**

- Activities: `DeckPicker.kt`, `CardBrowser.kt`, `CardTemplateEditor.kt` (PascalCase, Action/Screen name)
- Fragments: `PreferencesFragment.kt`, `StudyFragment.kt` (PascalCase + Fragment suffix)
- Dialogs: `SimpleConfirmDialog.kt`, `RescheduleDialog.kt` (PascalCase + Dialog suffix)
- Services: `AlarmManagerService.kt`, `NotificationService.kt` (PascalCase + Service suffix)
- ViewModels: `DeckPickerViewModel.kt`, `ReviewerViewModel.kt` (PascalCase + ViewModel suffix)
- Data classes: `Card.kt`, `Note.kt`, `Deck.kt` (PascalCase, singular entity name)
- Utilities: `CardUtils.kt`, `PermissionUtils.kt` (PascalCase + Utils suffix)
- Extensions: In same file or dedicated file with `Ext.kt` suffix
- Backend/libanki classes: Named after Anki Python terminology (Collection, Decks, Notetypes, Card, Note)

**Directories:**

- Feature packages: `reviewer/`, `browser/`, `cardviewer/`, `deckpicker/`, `noteeditor/` (lowercase, feature name)
- Layer packages: `ui/`, `widget/`, `servicelayer/`, `backend/`, `database/` (lowercase, layer name)
- Domain packages: `preferences/`, `settings/`, `scheduling/`, `multimedia/`, `analytics/` (lowercase, domain name)
- Utility packages: `utils/`, `utils/ext/` (lowercase, utility type)

## Where to Add New Code

**New Feature (Screen/Activity):**
- Primary code: Create new Activity/Fragment in `AnkiDroid/src/main/java/com/ichi2/anki/[feature]/`
- ViewModel: `AnkiDroid/src/main/java/com/ichi2/anki/viewmodels/[Feature]ViewModel.kt`
- Layout: `AnkiDroid/src/main/res/layout/activity_[feature].xml` or `fragment_[feature].xml`
- Tests: `AnkiDroid/src/test/java/com/ichi2/anki/[feature]/[Feature]ActivityTest.kt`
- Strings: Add to `AnkiDroid/src/main/res/values/strings.xml` with key `feature_name_*`

**New Business Logic (Collection operation):**
- Implementation: Add method to `libanki/src/main/java/com/ichi2/anki/libanki/Collection.kt` or create new class
- Pattern: Return `OpChanges` protobuf, don't handle UI
- Tests: `libanki/src/test/java/com/ichi2/anki/libanki/[Feature]Test.kt`
- Usage: Call from ViewModel via `CollectionManager.withCol {}`

**New Dialog:**
- File: Create in `AnkiDroid/src/main/java/com/ichi2/anki/ui/dialogs/[Name]Dialog.kt`
- Pattern: Extend `DialogFragment` or use helper in `ActivityAgnosticDialogs`
- Layout: `AnkiDroid/src/main/res/layout/dialog_[name].xml`

**New Preference/Setting:**
- Enum: Add to `AnkiDroid/src/main/java/com/ichi2/anki/settings/Prefs.kt`
- UI: Add `Preference` entry in `AnkiDroid/src/main/res/xml/preferences.xml`
- Implementation: Create Fragment in `AnkiDroid/src/main/java/com/ichi2/anki/ui/preferences/`

**New Utility:**
- File: `AnkiDroid/src/main/java/com/ichi2/anki/utils/[Feature]Utils.kt` or `common/src/main/java/com/ichi2/anki/utils/[Feature]Util.kt`
- Pattern: Object with functions (functional style, no classes) or utility functions
- Tests: `AnkiDroid/src/test/java/com/ichi2/anki/utils/[Feature]UtilsTest.kt`

**New Service/Background Operation:**
- File: Create in `AnkiDroid/src/main/java/com/ichi2/anki/servicelayer/` for business logic
- Or `AnkiDroid/src/main/java/com/ichi2/anki/services/` for Android Services
- Pattern: Use `launchCatchingIO` or service-based approach depending on duration
- Manifest: Register in `AndroidManifest.xml` if it's a Service/BroadcastReceiver

## Special Directories

**res/ (Resources):**
- Purpose: Android resources (layouts, strings, drawables, animations, colors, etc.)
- Generated: No (hand-authored)
- Committed: Yes
- Key subdirectories:
  - `layout/`: XML layouts for Activities, Fragments, Dialogs, list items
  - `values/`: String resources, colors, dimensions, styles (base language = English)
  - `values-*`: Language-specific strings (values-es/, values-ja/, etc.)
  - `drawable/`: Vector and raster images
  - `menu/`: Menu definitions
  - `anim/`: View animation definitions

**build/ (Build Output):**
- Purpose: Build artifacts, intermediates, APKs, reports
- Generated: Yes (gradle build output)
- Committed: No (.gitignore)
- Contents: APKs, coverage reports, lint reports, build intermediates

**test/ and androidTest/ (Tests):**
- Purpose: Unit and integration tests respectively
- Generated: No (hand-authored)
- Committed: Yes
- Execution: Unit tests run on host JVM (Robolectric), android tests on emulator/device

**.gradle/ (Gradle Cache):**
- Purpose: Gradle build cache and downloaded dependencies
- Generated: Yes (gradle)
- Committed: No (.gitignore)

**docs/code_conventions/:**
- Purpose: Developer-facing coding style and architecture guidelines
- Contents: Markdown docs on conventions, patterns, anti-patterns

---

*Structure analysis: 2026-02-12*
