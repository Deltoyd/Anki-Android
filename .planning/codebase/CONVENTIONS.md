# Coding Conventions

**Analysis Date:** 2026-02-12

## Naming Patterns

**Files:**
- PascalCase for classes: `BadgeDrawableBuilder.kt`, `MuseumViewModel.kt`, `ActivityTransitionAnimation.kt`
- camelCase for utility/extension files: `CoroutineHelpers.kt`, `ColorUtils.kt`
- Package structure follows class location: `com.ichi2.ui`, `com.ichi2.anki.libanki`

**Functions:**
- camelCase for all function names (private and public)
- Helper functions for coroutine launching: `launchCatching()`, `launchCatchingIO()`, `asyncIO()`
- Extension functions use lowercase: `withCol {}`, `withProgress {}`, `show {}` (from builder pattern libraries)
- Test functions: descriptive camelCase with underscores allowed: `test_unwrapContext_will_get_activity()`, `searchForBuriedReturnsManuallyAndSiblingBuried()`

**Variables:**
- camelCase for all variables (local, parameters, properties): `isIncludeAndroidResources`, `testLogging`, `maxHeapSize`
- Private properties prefixed with underscore for mutable state: `_uiState`, `_events` (see `MuseumViewModel.kt`)
- Boolean variables use prefixes: `is...`, `has...`, `can...`, `should...`, `enabled...`

**Types:**
- PascalCase for all type names: classes, interfaces, sealed classes, data classes, enums
- Sealed class/object for event/state sealed hierarchies: `sealed class MuseumEvent { data class PieceUnlocked(...) : MuseumEvent() }`, `data object PuzzleCompleted : MuseumEvent()`

## Code Style

**Formatting:**
- Tool: **prettier** via `ktlint` gradle plugin
- Key settings from `.prettierrc`:
  - Arrow parens: `avoid` (single parameters: `x -> x`)
  - Tab width: 4 spaces
  - Print width: 100 characters
  - Semicolons: enabled
  - Bracket spacing: true
  - Bracket same line: false
  - End of line: auto
  - Trailing commas: all

**Linting:**
- Tool: **ktlint** (gradle: `org.jlleitschuh.gradle.ktlint`)
- Configured in build.gradle.kts subprojects block
- Run via: `./gradlew ktlintFormat` (auto-format), `./gradlew ktLintCheck` (check)
- Custom disables in `.editorconfig`:
  - `ktlint_standard_no-consecutive-comments = disabled` (allows block comments after KDoc)
  - `ktlint_standard_when-entry-bracing = disabled` (reduces verbosity in when expressions)
  - `ktlint_standard_blank-line-between-when-conditions = disabled` (reduces blank lines)

**Kotlin Compiler Options:**
- `allWarningsAsErrors = true` (when `fatal_warnings != "false"` in local.properties)
- `-Xannotation-default-target=param-property` (applies @StringRes to both constructor params and generated properties)
- `-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi` (all modules except `api`)
- `-Xcontext-parameters` (enables context parameters)

## Import Organization

**Order:**
1. Android framework imports: `android.app.*`, `android.content.*`
2. AndroidX imports: `androidx.*`
3. Third-party libraries: `kotlinx.*`, `net.ankiweb.*`, `timber.*`, `org.junit.*`
4. Local project imports: `com.ichi2.*`
5. Kotlin imports: `kotlin.*`

**Path Aliases:**
- Used extensively via gradle depedencies
- Accessed through `libs.versions` and `libs.plugins` in gradle files
- No documented project-level path aliases in source code

## Error Handling

**Patterns:**
- Backend exceptions (`BackendException` and subtypes) are localized and safe to show users
- Custom exception hierarchy in `net.ankiweb.rsdroid.exceptions`: `BackendInterruptedException`, `BackendNetworkException`, `BackendSyncException`, `BackendInvalidInputException`
- `CancellationException` is always re-thrown to preserve coroutine semantics
- Other exceptions are logged with Timber and converted to crash reports

**Primary Handler:**
- `launchCatching()` in `CoroutineHelpers.kt` catches and logs exceptions
- Separates `BackendException` (shows localized message), `CancellationException` (rethrows), and general `Exception` (shows toString)
- Uses `errorMessageHandler` callback for custom error UI (e.g., emit to Flow)

**In ViewModels:**
- `OnErrorListener` interface: `val onError: MutableSharedFlow<String>`
- `ViewModel.launchCatchingIO()` extension automatically emits errors to `onError` flow
- See `MuseumViewModel` pattern at `/Users/rolandharper/StudioProjects/Anki-Android/AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/MuseumViewModel.kt`

## Logging

**Framework:**
- **Timber** (com.jakewharton.timber:timber)
- Tree implementation in tests uses println for JVM compatibility

**Patterns:**
- `Timber.d()` for debug logging
- `Timber.w()` for warnings (includes exceptions)
- `Timber.i()` for informational (progress, state changes)
- `Timber.e()` for errors (with exception)
- Caller stack trace appended in DEBUG mode only (performance impact)

**Special Handling:**
- Backend logging: filters `Backend$checkMainThreadOp` tag in tests to reduce noise
- Progress logging: logs dialog display status, cancellation reasons, fork counts
- CI logging: test results appended to GitHub Actions summary

## Comments

**When to Comment:**
- Block comments representing implementation details after KDoc are acceptable (ktlint rule disabled)
- TODO/FIXME comments tracked in issues (e.g., "TODO Tracked in https://github.com/ankidroid/Anki-Android/issues/5019")
- HACK comments for workarounds requiring discussion

**JSDoc/TSDoc:**
- KDoc used for public functions and classes
- Parameter documentation: `@param name description`
- Return documentation: rarely used in practice
- See `CoroutineHelpers.kt` for comprehensive KDoc patterns

**Examples:**
- `/**  Overridable reference to [Dispatchers.IO]. Useful if tests can't use it */` in `CoroutineHelpers.kt`
- Links to issues via `TODO Tracked in https://github.com/...` pattern

## Function Design

**Size:**
- No hard limit, but single-responsibility principle applied
- Small helper functions extracted for reuse and testability
- Example: `dismissDialogIfShowing()` extracts dialog dismissal logic (see `CoroutineHelpers.kt` line 492)

**Parameters:**
- Suspend functions for long-running operations
- Extension functions for receiver scope context (e.g., `ViewModel.launchCatchingIO { }`)
- Default parameters used liberally: `errorMessage: String? = null`, `skipCrashReport: ((Exception) -> Boolean)? = null`
- Named arguments encouraged for clarity

**Return Values:**
- Suspend functions return values directly: `suspend fun loadData(): MyData`
- Coroutine-launching functions return `Job` or `Deferred<T>`
- Functions with side effects return `Unit` (implicit in Kotlin)
- Nullable returns for optional operations: `T?` (see `MuseumViewModel.col` nullable access pattern)

## Module Design

**Exports:**
- Public classes explicitly public (no default package visibility in Kotlin)
- Internal visibility (`internal`) for module-specific utilities
- Private visibility (`private`) for implementation details within files
- Companion objects for factory methods and static utilities

**Barrel Files:**
- Not commonly used pattern observed in codebase
- Extensions and utilities imported directly from their files
- Example: `CoroutineHelpers.kt` exports multiple coroutine helpers

## Data Classes

**Pattern:**
- Used for UI state, events, and configuration objects
- See `MuseumUiState(...)` at line 223 of `MuseumViewModel.kt`
- Automatically generate `equals()`, `hashCode()`, `toString()`, `copy()`
- Use `copy()` with named arguments for immutable state updates: `_uiState.update { it.copy(streakDays = newDays) }`

**Sealed Classes:**
- Used for discriminated unions: `sealed class MuseumEvent`
- Subclasses use `data class` or `data object` (Kotlin 1.7+)
- Example: `object PuzzleCompleted : MuseumEvent()` (line 240 of `MuseumViewModel.kt`)

## Visibility Modifiers

**Defaults:**
- Public for public APIs
- Internal for module-internal utilities
- Private for file-local or class-local implementation
- Protected for open classes (rarely used)

**Testing:**
- `@VisibleForTesting` annotation used to mark implementation details exposed only for tests (see `CoroutineHelpers.kt` line 88, 92)
- Test package structure mirrors main package structure

## Copyright Headers

**Pattern:**
- GPL v3 copyright header required on all source files
- Format: `/*\n * Copyright (c) [YEAR] [Author]\n * This program is free software; you can redistribute it and/or modify it under\n * the terms of the GNU General Public License as published by the Free Software\n * Foundation; either version 3 of the License, or (at your option) any later\n * version.\n`
- Enforced via lint rule: `CopyrightHeaderExistsTest`

## Suppress Warnings

**Common Suppressions:**
- `@Suppress("DEPRECATION")` for PreferenceActivity usage (issue #5019)
- `@Suppress("Deprecation")` for ProgressDialog (legacy API still used)
- `@Suppress("OVERRIDE_DEPRECATION")` for overriding deprecated methods
- `@file:Suppress("DEPRECATION")` for file-level suppressions
- File-level: `@SuppressLint("PrintStackTraceUsage")` for test logging

---

*Convention analysis: 2026-02-12*
