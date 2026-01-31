# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AnkiDroid is a semi-official Android port of Anki, a spaced repetition flashcard system. The codebase is written primarily in Kotlin and uses a Rust-based backend for core business logic.

## Essential Build Commands

### Building
```bash
./gradlew assemblePlayDebug         # Build debug APK
./gradlew assemblePlayRelease       # Build release APK (signed)
./gradlew installPlayDebug          # Build and install debug APK to connected device
```

### Testing
```bash
# Unit tests (fast, runs via Robolectric)
./gradlew jacocoUnitTestReport --daemon

# Run tests for specific module
./gradlew :AnkiDroid:testPlayDebugUnitTest
./gradlew :libanki:test

# Run a single test class
./gradlew :AnkiDroid:testPlayDebugUnitTest --tests "com.ichi2.anki.ExampleTest"

# Run a single test method
./gradlew :AnkiDroid:testPlayDebugUnitTest --tests "com.ichi2.anki.ExampleTest.testMethod"

# Integration tests (slow, runs on emulator/device)
./gradlew jacocoAndroidTestReport --daemon
./gradlew connectedPlayDebugAndroidTest
```

### Code Quality
```bash
# Lint checks (Kotlin + Android)
./gradlew lintPlayDebug :api:lintDebug :libanki:lintDebug ktLintCheck lintVitalFullRelease lint-rules:test --daemon

# Auto-format with ktlint
./gradlew ktlintFormat

# Install pre-commit hook (auto-runs on build)
./gradlew installGitHook
```

## Module Structure

The project is organized into multiple Gradle modules with clear separation of concerns:

- **AnkiDroid**: Main application module containing all Activities, Fragments, and UI logic
- **api**: Public ContentProvider-based API for third-party applications to integrate with AnkiDroid
- **libanki**: Business logic layer with **no Android dependencies** (allows faster tests and cleaner architecture)
- **common**: Shared utilities and interfaces used across modules (base of dependency tree)
- **testlib**: Shared testing utilities and test runners
- **libanki:testutils**: Testing utilities specific to the libanki module
- **vbpd**: View Binding Property Delegate library (internal utility)
- **lint-rules**: Custom Android Lint checks specific to AnkiDroid patterns

## Core Architecture Patterns

### Collection Management (Central Pattern)

The `CollectionManager` object is the **primary access point** for database operations:

```kotlin
// All collection access should go through these suspend functions
CollectionManager.withCol { collection ->
    // Your database operations here
}

// For nullable collection (doesn't throw if collection unavailable)
CollectionManager.withOpenColOrNull { collection ->
    // Your database operations here
}

// Direct backend access (use sparingly)
val backend = CollectionManager.getBackend()
```

**Key Rules:**
- Collection access is **serialized** through a single-threaded dispatcher for thread safety
- Always use `withCol {}` from a coroutine context (suspend function)
- Never cache `Collection` references; always access through CollectionManager
- The collection remains open between calls for performance

### Coroutine-Based Async Architecture

Heavy use of Kotlin coroutines with custom helpers in `CoroutineHelpers.kt`:

```kotlin
// In ViewModels - launches with viewModelScope
launchCatchingIO {
    // Background work on IO dispatcher with automatic error handling
}

// For tasks that return results
val result = asyncIO {
    // Background work
}.await()

// General coroutine launch with error handling
launchCatching {
    // Work here with automatic error dialogs on exception
}
```

**Error Handling:**
- `BackendException`: Shows localized error messages to users
- Other exceptions: Logged and reported as crashes
- `CancellationException`: Re-thrown to preserve coroutine semantics

### Observable Changes Pattern

`ChangeManager` implements an observer pattern for reacting to collection changes:

```kotlin
// In Activity/Fragment onCreate/onViewCreated
ChangeManager.subscribe(this)  // 'this' must implement ChangeManager.Subscriber

// Implement the callback
override fun opExecuted(changes: OpChanges, handler: Any?) {
    if (changes.studyQueues) {
        // Update study queue UI
    }
    if (changes.card) {
        // Reload card data
    }
}
```

**Key Points:**
- Automatically unsubscribes on lifecycle destroy
- Uses WeakReferences to prevent memory leaks
- Backend operations return `OpChanges` protobuf messages indicating what changed

### ViewModel + StateFlow Pattern

Modern MVVM with Flow-based state management:

```kotlin
class MyViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MyState())
    val uiState: StateFlow<MyState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MyEvent>()
    val events: SharedFlow<MyEvent> = _events.asSharedFlow()

    fun loadData() = launchCatchingIO {
        val data = withCol { /* database operation */ }
        _uiState.update { it.copy(data = data) }
    }
}
```

### Backend Integration

The Rust-based `Backend` is accessed through `CollectionManager`:

```kotlin
// Backend is initialized lazily by AnkiDroidApp
val backend = CollectionManager.getBackend()

// Most operations go through Collection, which uses backend internally
withCol {
    // collection.someOperation() calls backend.method() internally
    // Returns protobuf messages (anki.* packages)
}

// Access translations
val translatedString = CollectionManager.TR.someTranslationKey()
```

**Backend Flow:**
1. `AnkiDroidApp.makeBackendUsable()` loads native "rsdroid" library
2. `BackendFactory.getBackend()` creates singleton Backend instance
3. Operations return protobuf messages and `OpChanges`
4. `ChangeManager` notifies subscribers of changes

## Testing Patterns

### Unit Tests (src/test/java)
- Run via **Robolectric** for fast feedback without emulator
- Use `@RunWith(AndroidJUnit4::class)` for Android framework classes
- Use JUnit 5 (`@Test` from `org.junit.jupiter.api`) for new tests
- Mock collection access or use `testlib` utilities

```kotlin
@RunWith(AndroidJUnit4::class)
class MyTest {
    @Test
    fun testSomething() = runTest {
        // Robolectric provides Android framework
        val context = ApplicationProvider.getApplicationContext<Application>()
        // Your test here
    }
}
```

### Integration Tests (src/androidTest/java)
- Run on real devices/emulators via **Espresso**
- Use for UI testing and full system integration
- Access to full Android framework

```kotlin
@RunWith(AndroidJUnit4::class)
class MyIntegrationTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MyActivity::class.java)

    @Test
    fun testUserFlow() {
        onView(withId(R.id.button)).perform(click())
        onView(withId(R.id.text)).check(matches(withText("Expected")))
    }
}
```

### Test Utilities
- `testlib` module provides common test utilities
- Custom test runner: `NewCollectionPathTestRunner` for collection setup
- Use `AnkiDroidApp.sharedPreferencesTestingOverride` for test preferences

## Important Conventions

### Activity/Fragment Lifecycle
- Extend `AnkiActivity` for common lifecycle management
- Subscribe to `ChangeManager` in `onCreate()`/`onViewCreated()`
- Use ViewModels for data persistence across configuration changes
- Prefer Fragments over Activities for new UI screens

### View Binding
Use the vbpd library for type-safe view access:

```kotlin
class MyActivity : AppCompatActivity() {
    private val binding by viewBinding(ActivityMyBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.myButton.setOnClickListener { /* ... */ }
    }
}
```

### Error Handling in UI
```kotlin
// In ViewModel
launchCatchingIO {
    withCol { /* operation */ }
}  // Automatically shows error dialog on exception

// For custom error handling
launchCatchingIO(
    block = { withCol { /* operation */ } },
    onError = { exception ->
        // Custom error handling
    }
)
```

### String Resources
- All user-facing strings must be in `strings.xml` (for internationalization)
- Use `@StringRes` annotation for string resource IDs
- Backend translations available via `CollectionManager.TR`

### Git Commit Messages
- Follow conventional commits format: `type(scope): description`
- Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`
- Backend changes tracked at backend version: v0.1.63-anki25.09.2

## Product Flavors

The project has three product flavors for different app stores:
- **play**: Google Play Store (default flavor)
- **amazon**: Amazon Appstore (no camera permissions for Fire TV)
- **full**: F-Droid/GitHub (full permissions)

Most development uses `play` flavor. Use `assemblePlayDebug` for standard builds.

## Local Configuration

Create `local.properties` for local development options:

```properties
# Disable test coverage for faster builds
enable_coverage=false

# Only build with English strings (much faster)
enable_languages=false

# Disable LeakCanary
enable_leak_canary=false

# Use local Anki backend build (advanced)
local_backend=true
```

## API Module

The `api` module provides a ContentProvider-based API for third-party apps:

- Contract: `com.ichi2.anki.FlashCardsContract`
- URIs: notes, models, decks, schedule, media
- Implementation: `CardContentProvider`
- All operations are asynchronous

## Common Pitfalls

1. **Don't cache Collection references**: Always use `CollectionManager.withCol {}`
2. **Don't use `Dispatchers.Main` for backend calls**: Use `launchCatchingIO` or similar
3. **Serial access is critical**: The queue in CollectionManager prevents race conditions
4. **libanki has no Android dependencies**: Don't import android.* in libanki module
5. **Test both unit and integration**: Unit tests are fast, integration tests catch UI bugs
6. **Watch for memory leaks**: Use Lifecycle-aware subscriptions and WeakReferences
7. **Backend exceptions are localized**: They're meant to be shown to users

## CI/CD

GitHub Actions runs on all PRs:
- Kotlin and JavaScript linting
- Unit tests with coverage (JaCoCo)
- Emulator tests with coverage
- CodeQL analysis

All checks must pass before merge. Run checks locally before submitting PRs.

## Key Dependencies

- **Backend**: `net.ankiweb.rsdroid` (Rust backend, version tracked in libs.versions.toml)
- **Coroutines**: kotlinx-coroutines (1.10.2)
- **AndroidX**: Latest stable (lifecycle, fragment, work, etc.)
- **Testing**: JUnit 5, Robolectric, Espresso, Mockito, MockK
- **Crash Reporting**: ACRA
- **Logging**: Timber + SLF4J

## AI Policy

**Important**: New contributors (less than 3 merged PRs) may NOT use AI tools for code, docs, or PR comments. After 3 merged PRs, AI usage must be disclosed in commits using `Assisted-by:` git trailers. See AI_POLICY.md for details.
