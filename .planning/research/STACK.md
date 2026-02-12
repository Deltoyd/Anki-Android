# Stack Research: Daily Puzzle Reveal Features

**Domain:** Android Custom View rendering with daily persistence mechanics
**Researched:** 2026-02-12
**Confidence:** HIGH

## Executive Summary

The new features (semi-transparent PNG rendering, daily card review tracking, daily reset persistence, full-reveal celebration) require **ZERO new dependencies**. The existing AnkiDroid stack already provides all necessary capabilities through Android's built-in Graphics APIs, SharedPreferences, and existing animation infrastructure.

This is a **pure implementation milestone** — no library additions, no version changes, just leveraging what's already validated and working.

## Recommended Stack

### Core Technologies (Already Present)

| Technology | Version | Purpose | Why Sufficient |
|------------|---------|---------|----------------|
| Android Canvas Graphics API | Android API 24+ | PNG transparency rendering | Built-in alpha channel support via Paint.alpha property, already used for piece fade animations |
| SharedPreferences | Android API 24+ | Daily reset persistence, review count tracking | Fast key-value storage, already used in MuseumPersistence.kt for streak tracking, perfect for daily counters |
| ValueAnimator | Android API 24+ | Triggering cinematic break animation | Already integrated in PuzzleBreakAnimationView.kt, no new setup needed |
| Kotlin Coroutines | 1.10.2 | Async persistence operations | Already used throughout (launchCatchingIO pattern), zero setup |

### Supporting Libraries (Already Integrated)

| Library | Version | Purpose | Integration Point |
|---------|---------|---------|-------------------|
| kotlinx-serialization-json | 1.9.0 | (Not needed for new features) | UserArtProgress serialization uses this, but new features use simpler SharedPreferences primitives |

## What's Already Built

### 1. Semi-Transparent PNG Rendering

**Status:** Capability exists, implementation needed

The codebase already renders PNG assets with transparency:
- `PaintingPuzzleView.kt` line 100: `Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)` handles bitmap filtering
- `PuzzleBreakAnimationView.kt` line 406: `bitmapPaint.alpha = piece.alpha` demonstrates alpha channel manipulation
- Android's `BitmapFactory.decodeResource()` (line 79 of PaintingPuzzleView.kt) automatically preserves PNG alpha channels

**Implementation approach:** Modify `drawLockedPiecePng()` to apply alpha based on daily progress:
```kotlin
// Pseudo-code
val dailyAlpha = calculateDailyAlpha(context) // 0-255 based on review count
pieceBitmapPaint.alpha = dailyAlpha
canvas.drawBitmap(bmp, null, tmpPieceRect, pieceBitmapPaint)
```

No new libraries. Paint.alpha is a standard Android property.

### 2. Daily Card Review Count Tracking

**Status:** Pattern exists, implementation needed

`MuseumPersistence.kt` already tracks daily data:
- Line 38: `SimpleDateFormat("yyyy-MM-dd", Locale.US)` for date keys
- Line 101-122: `updateStreak()` demonstrates daily check-in logic with date comparison
- Line 127-130: `getLastStudyDate()` retrieves last activity date

**Implementation approach:** Add to MuseumPersistence.kt:
```kotlin
private const val KEY_DAILY_REVIEW_COUNT = "daily_review_count"
private const val KEY_LAST_REVIEW_DATE = "last_review_date"

fun incrementDailyReviewCount(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val today = dateFormat.format(Date())
    val lastDate = prefs.getString(KEY_LAST_REVIEW_DATE, null)

    val currentCount = if (lastDate == today) {
        prefs.getInt(KEY_DAILY_REVIEW_COUNT, 0)
    } else {
        0 // New day, reset count
    }

    val newCount = currentCount + 1
    prefs.edit()
        .putInt(KEY_DAILY_REVIEW_COUNT, newCount)
        .putString(KEY_LAST_REVIEW_DATE, today)
        .apply()
    return newCount
}

fun getDailyReviewCount(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val today = dateFormat.format(Date())
    val lastDate = prefs.getString(KEY_LAST_REVIEW_DATE, null)
    return if (lastDate == today) {
        prefs.getInt(KEY_DAILY_REVIEW_COUNT, 0)
    } else {
        0 // Different day = count is 0
    }
}
```

No new dependencies. Uses existing SharedPreferences pattern from line 24-36.

### 3. Daily Reset Persistence

**Status:** Pattern exists, implementation needed

Daily reset is already implemented for streaks:
- Line 154-172: `isConsecutiveDay()` compares date strings
- Line 101-122: `updateStreak()` resets to 1 when streak breaks

**Implementation approach:** Combine with review count tracking above. The `incrementDailyReviewCount()` function already handles reset via date comparison. When `lastDate != today`, count returns 0.

No new logic pattern needed — reuse existing date-based reset mechanism.

### 4. Triggering Cinematic Animation on Full Reveal

**Status:** Infrastructure exists, trigger point needed

Celebration animation already implemented:
- `PuzzleBreakAnimationView.kt` lines 148-165: `startAnimation()` API
- `MuseumActivity.kt` lines 98-104: Animation trigger example on activity creation
- `MuseumViewModel.kt` lines 198-200: `PuzzleCompleted` event emission

**Implementation approach:** Modify MuseumViewModel.kt `unlockPiece()`:
```kotlin
fun unlockPiece(context: Context, pieceIndex: Int) {
    viewModelScope.launch {
        val wasNewlyUnlocked = MuseumPersistence.addUnlockedPiece(context, pieceIndex)

        if (wasNewlyUnlocked) {
            val updatedPieces = MuseumPersistence.getUnlockedPieces(context)

            _uiState.update {
                it.copy(
                    unlockedPieces = updatedPieces,
                    progressText = "${updatedPieces.size} / 100 pieces"
                )
            }

            _events.emit(MuseumEvent.PieceUnlocked(pieceIndex))

            // NEW: Check if this unlock completed the puzzle
            if (updatedPieces.size == 100) {
                _events.emit(MuseumEvent.PuzzleCompleted)
            }
        }
    }
}
```

Then in MuseumActivity.kt event handler (line 139):
```kotlin
is MuseumEvent.PuzzleCompleted -> {
    val painting = viewModel.uiState.value.painting
    val unlockedPieces = viewModel.uiState.value.unlockedPieces
    if (painting != null) {
        binding.puzzleBreakOverlay.startAnimation(painting, unlockedPieces) {
            showCompletionDialog()
        }
    } else {
        showCompletionDialog()
    }
}
```

No new dependencies. Reuse existing PuzzleBreakAnimationView.

## Installation

**No changes needed.** All capabilities present in current dependency set:

```gradle
// Already in AnkiDroid/build.gradle (line 362-496)
implementation libs.androidx.core.ktx           // Provides Kotlin extensions
implementation libs.kotlin.stdlib                // Kotlin standard library
implementation libs.kotlinx.coroutines.core      // Async operations

// Android framework (built-in, no explicit dependency)
// - android.graphics.Paint (alpha channel)
// - android.content.SharedPreferences (persistence)
// - android.animation.ValueAnimator (animations)
// - java.text.SimpleDateFormat (date handling)
// - java.util.Date, Calendar (time tracking)
```

## Alternatives Considered

| Recommended | Alternative | Why Not Alternative |
|-------------|-------------|---------------------|
| SharedPreferences for daily counters | Room database | Overkill: No complex queries, no relationships, no migration needs. SharedPreferences is faster for simple key-value reads on app launch. Pattern already proven in MuseumPersistence.kt |
| Paint.alpha for transparency | PorterDuff blending modes | Unnecessarily complex. Paint.alpha (0-255) is the standard Android approach for simple opacity. Already used in 3+ places in codebase (PuzzleBreakAnimationView line 406, 419) |
| Date-string comparison for daily reset | AlarmManager scheduled task | Over-engineered. App checks on launch anyway. No need for background scheduling when user-triggered check is sufficient and more battery-friendly |
| Existing PuzzleBreakAnimationView | New Lottie animation | Breaking existing patterns. PuzzleBreakAnimationView already handles puzzle-to-pieces transformation. Adding Lottie (2MB+) for one animation violates AnkiDroid's minimal-dependencies philosophy |

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| WorkManager for daily reset | No background work needed; reset happens on next app launch when user reviews cards | Date comparison in SharedPreferences (existing pattern) |
| Jetpack DataStore | Adds 500KB+ dependency for async SharedPreferences wrapper; AnkiDroid uses synchronous SharedPreferences everywhere (see MuseumPersistence.kt) | Existing SharedPreferences (already initialized, battle-tested) |
| Custom PNG decoder libraries | Android's BitmapFactory handles PNG alpha perfectly; adding dependency for feature Android provides is anti-pattern | BitmapFactory.decodeResource() (already used line 79 PaintingPuzzleView.kt) |
| RxJava for daily counter updates | AnkiDroid migrated to Kotlin Coroutines (see CLAUDE.md line 38-72); introducing RxJava reverses architectural decision | Kotlin Coroutines with launchCatchingIO (existing pattern) |

## Integration Points

### Semi-Transparent Rendering

**File:** `PaintingPuzzleView.kt`
**Function:** `drawLockedPiecePng()` (line 439)
**Change:** Add alpha calculation before `canvas.drawBitmap()`

```kotlin
// BEFORE (line 469)
canvas.drawBitmap(bmp, null, tmpPieceRect, pieceBitmapPaint)

// AFTER
val dailyProgress = getDailyProgress(context) // 0.0 to 1.0
val alpha = (255 * dailyProgress).toInt().coerceIn(0, 255)
pieceBitmapPaint.alpha = alpha
canvas.drawBitmap(bmp, null, tmpPieceRect, pieceBitmapPaint)
```

**Dependencies:** None (Paint.alpha is Android framework)

### Daily Review Count Tracking

**File:** `MuseumPersistence.kt`
**Integration:** Add functions after line 172 (after `isConsecutiveDay()`)
**Pattern:** Mirrors `updateStreak()` logic (line 101-122)

**Hook point:** Unknown currently — needs research into where card reviews are counted. Likely in ReviewerViewModel or Reviewer.kt. Will investigate in implementation phase.

### Daily Reset Logic

**File:** `MuseumPersistence.kt`
**Pattern:** Already implemented via date comparison (line 154-172)
**No changes needed:** Reset happens automatically in `incrementDailyReviewCount()` when date changes

### Cinematic Animation Trigger

**File:** `MuseumViewModel.kt`
**Function:** `unlockPiece()` (line 179)
**Change:** Add completion check at line 198-200

**File:** `MuseumActivity.kt`
**Function:** Event observer (line 139)
**Change:** Modify `PuzzleCompleted` handler to trigger animation before dialog

## Version Compatibility

All features use Android API 24+ capabilities (AnkiDroid minSdk = 24, see libs.versions.toml line 9):

| Feature | Android API | AnkiDroid Min | Compatible |
|---------|-------------|---------------|------------|
| Paint.alpha | API 1 | API 24 | ✅ Yes (23 API levels of headroom) |
| SharedPreferences | API 1 | API 24 | ✅ Yes |
| ValueAnimator | API 11 | API 24 | ✅ Yes (13 API levels of headroom) |
| SimpleDateFormat | API 1 | API 24 | ✅ Yes |

**No compatibility concerns.** All APIs predate minSdk by decades.

## Performance Considerations

### SharedPreferences Read Performance
- **Current usage:** MuseumPersistence reads on app launch (line 33-35 MuseumActivity.kt)
- **New usage:** Add 2 reads (daily count, last date)
- **Impact:** Negligible — SharedPreferences reads are ~0.1ms on modern devices
- **Pattern:** AnkiDroid already reads 6+ values on Museum launch (streak, lives, deck ID, topic ID, art piece ID, gallery position)

### Paint.alpha Rendering
- **Current usage:** Applied per-piece in PuzzleBreakAnimationView (100 pieces animated simultaneously)
- **New usage:** Applied once per locked piece per frame
- **Impact:** Zero — Paint.alpha is a simple integer property set, no GPU overhead
- **Validation:** PuzzleBreakAnimationView already sets alpha 100 times per frame (line 406) with no performance issues

### Date Comparison Frequency
- **Current usage:** Once on app launch (updateStreak in MuseumActivity.onCreate)
- **New usage:** Once per card review (in review callback)
- **Impact:** Negligible — String comparison is O(1) for date format "yyyy-MM-dd" (10 chars)
- **Pattern:** SimpleDateFormat is already used in hot path (MuseumPersistence line 38)

## Sources

- Android Canvas API: Built-in framework (confirmed via PaintingPuzzleView.kt line 285-314 usage)
- SharedPreferences pattern: MuseumPersistence.kt (line 24-254) — production code
- Animation trigger pattern: MuseumActivity.kt (line 98-104) — production code
- Daily reset pattern: MuseumPersistence.kt updateStreak() (line 101-122) — production code
- Paint.alpha usage: PuzzleBreakAnimationView.kt (line 406, 419), PaintingPuzzleView.kt (line 408) — production code
- AnkiDroid architecture patterns: CLAUDE.md (architecture section)
- Dependency versions: gradle/libs.versions.toml, AnkiDroid/build.gradle

---
*Stack research for: Daily puzzle reveal mechanics (semi-transparent rendering, review tracking, daily reset, celebration)*
*Researched: 2026-02-12*
*Confidence: HIGH — All capabilities verified in production codebase, zero new dependencies required*
