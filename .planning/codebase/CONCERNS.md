# Codebase Concerns

**Analysis Date:** 2026-02-12

## Tech Debt

### Large Monolithic Activities and Fragments

**Issue:** Several core components exceed healthy file sizes and contain mixed responsibilities.

- `NoteEditorFragment.kt` (3,157 lines) - Handles note editing, multimedia, validation, UI state
- `AbstractFlashcardViewer.kt` (2,852 lines) - Combines web view management, gestures, audio, JavaScript interaction
- `DeckPicker.kt` (2,521 lines) - Main activity mixing deck display, sync, menu, dialogs, permissions
- `Reviewer.kt` (2,021 lines) - Review state, card display, gestures, timing

**Files:**
- `AnkiDroid/src/main/java/com/ichi2/anki/NoteEditorFragment.kt`
- `AnkiDroid/src/main/java/com/ichi2/anki/AbstractFlashcardViewer.kt`
- `AnkiDroid/src/main/java/com/ichi2/anki/DeckPicker.kt`
- `AnkiDroid/src/main/java/com/ichi2/anki/Reviewer.kt`

**Impact:** Difficult to test, high cognitive load, increased likelihood of bugs when making changes, resistance to refactoring.

**Fix approach:**
- Extract large components into separate fragments/view models
- Use composition to separate concerns (e.g., separate audio handling into dedicated components)
- For `AbstractFlashcardViewer`: Move gesture handling to dedicated `GestureProcessor`, web view client logic to separate classes
- For `DeckPicker`: Extract sync logic to ViewModel, menu handling to MenuProvider, dialog management to separate handlers

### Widespread Null Assertion Operators (!!)

**Issue:** Code uses non-null assertion operator frequently, reducing null safety benefits.

**Files:**
- `AnkiDroid/src/main/java/com/ichi2/ui/BadgeDrawable.kt` - Multiple !! on text assertions
- `AnkiDroid/src/main/java/com/ichi2/ui/AppCompatPreferenceActivity.kt` - !! on getString conversions to primitives
- `AnkiDroid/src/main/java/com/ichi2/compat/CompatV26.kt` - `throw e.cause!!` (2 instances)
- `AnkiDroid/src/main/java/com/ichi2/compat/CompatV29.kt` - `openOutputStream(newImageUri!!)`

**Impact:** Potential NullPointerExceptions at runtime, nullability guarantees violated, runtime safety compromised.

**Fix approach:**
- Replace !! with safe navigation ?.let { } or use requireNotNull() with error message
- For getString conversions, provide default values or use getOrNull pattern
- For DirectoryIteratorException handling, verify cause is not null before throwing

### Android API Deprecation Handling

**Issue:** Code suppresses deprecation warnings rather than migrating to modern APIs.

**Files:**
- `AnkiDroid/src/main/java/com/ichi2/widget/AnkiDroidWidgetSmall.kt` - Line marked TODO for `onStartCommand` deprecation
- `AnkiDroid/src/main/java/com/ichi2/anim/ActivityTransitionAnimation.kt` - Uses deprecated back gesture API
- `AnkiDroid/src/main/java/com/ichi2/anki/ReadText.kt` - ReadText service is deprecated, workarounds in place

**Impact:** Technical debt increases with each Android API level, harder to migrate later, potential removal of APIs in future versions.

**Fix approach:**
- Migrate AnkiDroidWidgetSmall to use onStartCommand(Intent, int, int)
- Implement predictive back gesture handling for API 34+ using new back callback framework
- Plan deprecation path for ReadText service or replace with modern TTS approach

### Incomplete Feature Implementation

**Issue:** Several features have partial implementations marked with deprecation or hidden status.

**Files:**
- `libanki/src/main/java/com/ichi2/anki/libanki/Collection.kt` - Multiple methods marked `@Deprecated("not implemented")`
- `libanki/src/main/java/com/ichi2/anki/libanki/Notetypes.kt` - ChangeNoteTypeDialog replacement pending

**Impact:** Confusing API surface, unclear contract, potential for misuse of incomplete features.

**Fix approach:** Remove stub implementations, provide clear migration paths in API docs.

---

## Known Bugs

### Cross-Profile Query Security Exception

**Issue:** SecurityException thrown by PackageManager on some Xiaomi phones during cross-profile queries.

**Symptoms:** App crashes with SecurityException when querying intent activities on Xiaomi devices.

**Files:**
- `AnkiDroid/src/main/java/com/ichi2/compat/CompatV33.kt` (line comment references #19711)
- `AnkiDroid/src/main/java/com/ichi2/compat/CustomTabsHelper.kt`

**Trigger:** Calling `queryIntentActivities()` on Xiaomi Android 12+ devices with cross-profile app visibility restrictions.

**Workaround:** Code has try-catch in place, but error may still be logged.

### MediaPlayer Resource Leaks

**Issue:** MediaPlayer instances may not be properly released in all code paths.

**Files:**
- `AnkiDroid/src/main/java/com/ichi2/anki/cardviewer/CardMediaPlayer.kt`
- `AnkiDroid/src/main/java/com/ichi2/anki/multimedia/audio/AudioRecordingController.kt`

**Trigger:** When audio playback is interrupted, cancelled, or activity destroyed during playback.

**Workaround:** Mutex-based locking and coroutine cancellation in place, but incomplete cleanup paths may exist.

---

## Security Considerations

### Backend Exception Information Disclosure

**Issue:** BackendException messages are shown directly to users via UI dialogs, which may contain sensitive information or implementation details.

**Risk:** Potential exposure of database paths, internal error details, or sensitive validation rules.

**Files:**
- `AnkiDroid/src/main/java/com/ichi2/anki/CoroutineHelpers.kt` - BackendException handling
- `AnkiDroid/src/main/java/com/ichi2/ui/AppCompatPreferenceActivity.kt` - Direct BackendException display

**Current mitigation:** Backend exceptions include localized messages intended for user display.

**Recommendations:**
- Audit BackendException message content for sensitive information leaks
- Consider sanitizing exception messages in production builds
- Log full exception details server-side instead of showing to users

### Collection Database Access

**Issue:** Collection access through `CollectionManager` is serialized but relies on single global backend instance.

**Risk:** If backend becomes corrupted or locked, entire application is blocked.

**Files:**
- `AnkiDroid/src/main/java/com/ichi2/anki/CollectionManager.kt`

**Current mitigation:** Collection access uses ReentrantLock and queue-based serialization.

**Recommendations:**
- Implement automatic fallback to new collection creation on permanent database corruption
- Add database integrity checks on startup
- Implement graceful degradation for read-only operations when database is locked

### Sync Credentials in Memory

**Issue:** User credentials used for Anki Web sync may persist in memory after logout.

**Files:**
- `AnkiDroid/src/main/java/com/ichi2/anki/pages/RemoveAccountFragment.kt`

**Recommendations:**
- Use KeyStore for credential storage
- Explicitly clear sensitive data from memory on logout
- Implement anti-debugging measures if available

---

## Performance Bottlenecks

### Large File Operations During Sync

**Issue:** Collection import/export and sync operations may block UI thread.

**Symptoms:** UI freezing during large deck imports or syncs.

**Files:**
- `AnkiDroid/src/main/java/com/ichi2/anki/libanki/importCollectionPackage.kt`
- `AnkiDroid/src/main/java/com/ichi2/anki/deckpicker/DeckPickerViewModel.kt`

**Cause:** Database operations and file I/O on main thread or insufficient async boundaries.

**Improvement path:**
- Ensure all collection operations use IO dispatcher via `withContext(Dispatchers.IO)`
- Implement progress tracking for long operations
- Consider streaming/chunked processing for large backups

### Note Editor Field Rendering

**Issue:** NoteEditorFragment with multiple rich-text fields may have slow render times.

**Symptoms:** Lag when editing notes with many fields or complex HTML.

**Files:**
- `AnkiDroid/src/main/java/com/ichi2/anki/NoteEditorFragment.kt` (lines handling WebView fields)

**Cause:** Rich-text field rendering with WebView, potential multiple redraws on text changes.

**Improvement path:**
- Implement debouncing for field updates
- Use virtual scrolling for field list if many fields present
- Profile WebView rendering with Chrome DevTools

### Card Browser Search Performance

**Issue:** Card browser search may be slow on large collections.

**Files:**
- `AnkiDroid/src/main/java/com/ichi2/anki/CardBrowser.kt`
- `AnkiDroid/src/main/java/com/ichi2/anki/browser/CardBrowserViewModel.kt` (1,544 lines)

**Cause:** Full collection search without pagination or query optimization.

**Improvement path:**
- Implement pagination for search results
- Add indexed search queries to backend
- Cache recent searches

### Bitmap Memory Usage

**Issue:** Deck picker background images and multimedia may consume excessive memory.

**Files:**
- `AnkiDroid/src/main/java/com/ichi2/anki/deckpicker/BackgroundImage.kt`
- Reference: `BITMAP_BYTES_PER_PIXEL` constant in deckpicker

**Cause:** Unscaled bitmap loading, multiple bitmap instances in memory.

**Improvement path:**
- Implement bitmap sampling/downscaling based on screen size
- Use memory pool/cache for frequent images
- Monitor heap usage during image operations

---

## Fragile Areas

### WebView and JavaScript Bridge

**Issue:** WebView-based card rendering with JavaScript API bridge is fragile.

**Files:**
- `AnkiDroid/src/main/java/com/ichi2/anki/AbstractFlashcardViewer.kt` (WebView setup)
- `AnkiDroid/src/main/java/com/ichi2/anki/AnkiDroidJsAPI.kt`

**Why fragile:**
- JavaScript execution context can become invalid if WebView reloads
- Network errors during resource loading aren't always caught
- RenderProcess crashes can crash app (see WebChromeClient.onRenderProcessGoneDetail)

**Safe modification:**
- Always check WebView.getUrl() != null before JS execution
- Wrap JS calls in try-catch with proper error recovery
- Test WebView lifecycle during device rotation and back navigation
- Keep JS API minimal and versioned

**Test coverage:** Limited coverage of WebView error scenarios.

### ChangeManager Observer Pattern

**Issue:** ChangeManager uses WeakReferences to track subscribers, but cleanup can be fragile.

**Files:**
- `AnkiDroid/src/main/java/com/ichi2/anki/CollectionManager.kt` (uses ChangeManager)
- Related: Any class implementing `ChangeManager.Subscriber`

**Why fragile:**
- WeakReferences can be garbage collected unexpectedly
- Lifecycle subscription/unsubscription must match exactly
- No explicit cleanup required, relying on GC

**Safe modification:**
- Always call `ChangeManager.subscribe(this)` in onCreate/onViewCreated
- Verify unsubscription happens in Fragment lifecycle
- Add unit tests verifying subscription lifecycle

**Test coverage:** ChangeManager subscription patterns not fully tested.

### ContentProvider API Surface

**Issue:** ContentProvider contract must remain stable for third-party app compatibility.

**Files:**
- `AnkiDroid/src/main/java/com/ichi2/anki/provider/CardContentProvider.kt` (1,397 lines)
- `api/src/main/java/com/ichi2/anki/FlashCardsContract.kt` (1,000 lines)

**Why fragile:**
- ContentProvider URI schemes cannot be changed without breaking third-party apps
- New columns must be backward compatible
- Removal of endpoints breaks external integrations

**Safe modification:**
- Only add new columns/tables, never remove or rename
- Version ContentProvider contract carefully
- Add deprecation warnings for features to remove in next major version

**Test coverage:** Integration tests in `AnkiDroid/src/androidTest/java/com/ichi2/anki/tests/ContentProviderTest.kt` (1,577 lines).

### Preference Migration and Upgrades

**Issue:** SharedPreferences migration logic is complex and fragile.

**Files:**
- `AnkiDroid/src/main/java/com/ichi2/anki/servicelayer/PreferenceUpgradeService.kt` (797 lines)

**Why fragile:**
- Preference defaults must be maintained across multiple version upgrades
- Migration logic must handle skipped version upgrades (e.g., v1 → v4)
- No schema validation, manual key management

**Safe modification:**
- Always test preference migration from minimum supported version
- Add preference version sanity check at startup
- Document preference keys and migration paths

**Test coverage:** Limited automated testing of preference upgrade paths.

---

## Scaling Limits

### Single Backend Instance

**Issue:** Application uses single global Backend instance, limiting concurrency.

**Current capacity:** Sequential operations queued through CollectionManager.

**Limit:** Backend must wait for collection lock, blocking parallel operations.

**Scaling path:**
- Evaluate multi-collection scenarios if needed (unlikely for AnkiDroid)
- Implement read-only replica support for queries if backend supports
- Consider async backend APIs for heavy operations

### Database Size Limits

**Issue:** Collection database must fit in device storage and memory.

**Current capacity:** Tested up to ~500K cards (typical limits based on Anki ecosystem).

**Limit:** Large collections may cause slow search, backup, or sync operations.

**Scaling path:**
- Implement collection splitting for very large decks
- Optimize database queries with better indexing
- Implement incremental sync for large changes

### MediaPlayer Queue

**Issue:** Audio playback queued in sequence, no parallel audio streams.

**Files:**
- `AnkiDroid/src/main/java/com/ichi2/anki/cardviewer/CardMediaPlayer.kt`

**Current capacity:** Single audio stream at a time.

**Limit:** Cannot play overlapping audio or background audio.

**Scaling path:**
- Extend MediaPlayer to support audio mixing if needed
- Use ExoPlayer for more advanced audio features

---

## Dependencies at Risk

### Rust Backend (rsdroid)

**Risk:** Anki backend is native Rust library, versioning complexity and platform-specific issues.

**Impact:** Backend crashes crash entire app. Incompatible backend versions cause fatal errors.

**Files:**
- `AnkiDroid/src/main/java/com/ichi2/anki/CollectionManager.kt` (backend initialization)
- `libs.versions.toml` (backend version tracking)

**Migration plan:**
- Monitor backend version compatibility across Android API levels
- Implement version negotiation/compatibility checking at startup
- Plan migration strategy if backend API breaks

### WebKit/WebView

**Risk:** WebView is system component, may behave differently across Android versions.

**Impact:** Card rendering inconsistencies, JavaScript API compatibility issues.

**Files:**
- `AnkiDroid/src/main/java/com/ichi2/anki/AbstractFlashcardViewer.kt`

**Migration plan:**
- Maintain compatibility matrix of tested WebView behaviors
- Consider moving to custom rendering solution if WebView compatibility becomes untenable

### Deprecated TTS API

**Risk:** Text-to-speech implementation uses deprecated UtteranceProgressListener.

**Impact:** May not work on future Android versions.

**Files:**
- `AnkiDroid/src/main/java/com/ichi2/anki/ReadText.kt`
- `AnkiDroid/src/main/java/com/ichi2/compat/UtteranceProgressListenerCompat.kt`

**Migration plan:**
- Evaluate TextToSpeech.Engine API as replacement
- Test on Android 14+ for deprecation warnings

---

## Missing Critical Features

### Comprehensive Error Recovery

**Problem:** Collection corruption or database errors have limited recovery options.

**Blocks:** Users with corrupted collections cannot recover without manual intervention.

**Files:**
- `AnkiDroid/src/main/java/com/ichi2/anki/dialogs/DatabaseErrorDialog.kt`

**Recommendations:**
- Implement automatic database repair/rebuild
- Add collection verification at startup with repair suggestions
- Implement partial collection recovery from backups

### Offline Sync Queuing

**Problem:** Sync operations may fail with no queue of pending changes.

**Blocks:** Users cannot reliably sync when reconnecting to network.

**Recommendations:**
- Implement local sync operation queue
- Queue changes during offline and replay on reconnect
- Implement conflict detection and resolution

### Comprehensive Logging

**Problem:** Limited crash logs and operation tracing for debugging issues.

**Blocks:** Hard to diagnose user-reported issues without reproduction.

**Files:**
- Uses Timber for logging, but configuration limited

**Recommendations:**
- Implement structured logging with correlation IDs
- Add operation tracing around collection access
- Implement log upload for debugging user issues

---

## Test Coverage Gaps

### WebView Rendering and JavaScript Bridge

**Untested:** WebView lifecycle, JavaScript error handling, card rendering edge cases.

**Files:**
- `AnkiDroid/src/main/java/com/ichi2/anki/AbstractFlashcardViewer.kt`
- `AnkiDroid/src/main/java/com/ichi2/anki/cardviewer/JavascriptEvaluator.kt`
- `AnkiDroid/src/main/java/com/ichi2/anki/AnkiDroidJsAPI.kt`

**Risk:** Card rendering bugs undetected until user reports, especially on specific devices.

**Priority:** High - affects core user experience.

### Multimedia Playback Edge Cases

**Untested:** MediaPlayer cleanup on various lifecycle transitions, audio interruption handling, video rendering.

**Files:**
- `AnkiDroid/src/main/java/com/ichi2/anki/cardviewer/CardMediaPlayer.kt`
- `AnkiDroid/src/main/java/com/ichi2/anki/multimedia/audio/AudioRecordingController.kt`

**Risk:** Memory leaks, resource exhaustion, audio/video failures during review.

**Priority:** High - affects core review functionality.

### Collection Corruption Scenarios

**Untested:** Database corruption recovery, backup restoration, sync conflicts.

**Files:**
- `AnkiDroid/src/main/java/com/ichi2/anki/dialogs/DatabaseErrorDialog.kt`
- Collection access throughout codebase

**Risk:** Users with corrupted collections cannot recover, may lose data.

**Priority:** Critical - affects data integrity.

### Preference Migration Edge Cases

**Untested:** Upgrading from very old versions, skipped versions, preference corruption.

**Files:**
- `AnkiDroid/src/main/java/com/ichi2/anki/servicelayer/PreferenceUpgradeService.kt`

**Risk:** Settings lost or incorrect after upgrade, user experience degradation.

**Priority:** Medium - affects user settings but not core functionality.

### Gesture Recognition and Binding

**Untested:** Custom gesture handling, edge cases in gesture recognition, keyboard shortcut conflicts.

**Files:**
- `AnkiDroid/src/main/java/com/ichi2/anki/android/input/` (gesture/input handling)

**Risk:** Input handling inconsistencies across devices, user gesture events misinterpreted.

**Priority:** Medium - affects user input handling.

### Large Collection Performance

**Untested:** Search performance on 100K+ card collections, sync with very large updates.

**Files:**
- `AnkiDroid/src/main/java/com/ichi2/anki/browser/CardBrowserViewModel.kt`

**Risk:** Performance degradation undetected until user experiences slowdown.

**Priority:** Medium - affects power users with large collections.

---

## Architecture Concerns

### Collection State Serialization

**Issue:** Collection state must be serialized/managed carefully due to backend dependency.

**Concern:** Collection reference is mutable global state, making testing and state management difficult.

**Files:**
- `AnkiDroid/src/main/java/com/ichi2/anki/CollectionManager.kt`
- `libanki/src/main/java/com/ichi2/anki/libanki/LibAnki.kt`

**Recommendations:**
- Consider immutable state patterns where possible
- Add state validation at critical points
- Implement state machine for collection lifecycle

### Coroutine Context Complexity

**Issue:** Multiple custom coroutine helpers and error handling patterns.

**Files:**
- `AnkiDroid/src/main/java/com/ichi2/anki/CoroutineHelpers.kt` (748 lines)

**Concern:** Complex error handling with multiple patterns (launchCatching, launchCatchingIO, asyncIO) may lead to inconsistent behavior.

**Recommendations:**
- Document each helper's exact error handling contract
- Standardize on fewer patterns
- Add type-safe error handling alternatives

---

*Concerns audit: 2026-02-12*
