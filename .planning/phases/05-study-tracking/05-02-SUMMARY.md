---
phase: 05-study-tracking
plan: 02
type: execute
completed: 2026-02-13
duration: 570s
subsystem: study-tracking
tags: [viewmodel, integration, revlog-integration, ui-state]

dependency-graph:
  requires:
    - StudyTrackingRepository (Plan 05-01): "Pure data layer for study stats computation"
  provides:
    - ViewModel integration: "MuseumViewModel loads study data from revlog"
    - Updated UI state: "MuseumUiState exposes streak, grace days, study time, cards reviewed"
  affects:
    - Phase 6 (Streak Widget): "Will consume graceDaysAvailable and todayStudyTimeMs from MuseumUiState"
    - MuseumActivity: "Renders updated state fields in UI"

tech-stack:
  added:
    - java.time.LocalDate: "Date handling in ViewModel layer"
  patterns:
    - Helper method extraction: "loadStudyStats() to avoid duplication between loadMuseumData and refreshData"
    - State update pattern: "Triple<StreakInfo, Long, Int> for multi-value return"

key-files:
  created: []
  modified:
    - AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/MuseumViewModel.kt: "Wired StudyTrackingRepository, updated state model"
    - AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/StudyTrackingRepository.kt: "Removed suspend from queryRevlog (blocking issue fix)"

decisions:
  - decision: "Remove suspend modifier from StudyTrackingRepository.queryRevlog"
    rationale: "withCol {} lambda is not a suspend context - it's a regular Collection.() -> T lambda. queryRevlog performs synchronous DB operations, doesn't need suspend."
    alternatives: ["Keep suspend and restructure ViewModel calls (would break withCol pattern)"]
  - decision: "Extract loadStudyStats() helper returning Triple"
    rationale: "Both loadMuseumData and refreshData need same computation - DRY principle"
    alternatives: ["Duplicate logic in both methods (violates DRY)"]

metrics:
  tasks-completed: 1
  tests-passing: 23
  files-modified: 2
  lines-added: 30
  lines-removed: 11
---

# Phase 05 Plan 02: ViewModel Integration Summary

**One-liner:** Wired StudyTrackingRepository into MuseumViewModel, replacing SharedPreferences-based streak with revlog-derived study data

## What Was Built

Integrated the StudyTrackingRepository (from Plan 01) into MuseumViewModel to provide real-time study statistics from Anki's revlog table instead of the placeholder SharedPreferences values.

**Key changes:**

1. **Updated MuseumUiState data class:**
   - Added `graceDaysAvailable: Int` (replaces old `extraLives` concept)
   - Added `todayStudyTimeMs: Long` (capped study time for today)
   - Added `todayCardsReviewed: Int` (cards reviewed today)
   - Removed `extraLives: Int` field

2. **Created loadStudyStats() helper:**
   - Queries revlog for last 365 days via `StudyTrackingRepository.queryRevlog()`
   - Computes daily data, streak info, and today's stats
   - Returns `Triple<StreakInfo, Long, Int>` for use in both load and refresh methods

3. **Updated loadMuseumData() and refreshData():**
   - Both now call `loadStudyStats()` instead of reading from SharedPreferences
   - Update UI state with revlog-derived values
   - No longer call `MuseumPersistence.getStreakDays()` or `getExtraLives()`

4. **Data flow established:**
   - Revlog table → StudyTrackingRepository → MuseumViewModel → MuseumUiState → UI (Phase 6)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Removed suspend modifier from queryRevlog**
- **Found during:** Task 1, compilation
- **Issue:** `StudyTrackingRepository.queryRevlog()` was marked `suspend`, but `withCol {}` lambda is `Collection.() -> T`, not a suspend context. This caused compilation error: "Suspension functions can only be called within coroutine body."
- **Root cause:** Plan 01 marked `queryRevlog` as suspend in anticipation of being called from coroutines, but `withCol`'s lambda executes synchronously on the Collection object. The DB query itself is synchronous (cursor iteration).
- **Fix:** Removed `suspend` modifier from `queryRevlog` function signature. Updated documentation to clarify it's a synchronous DB operation called within `withCol` context.
- **Files modified:** `StudyTrackingRepository.kt` (lines 20-24)
- **Commit:** Included in 599bb68
- **Rationale:** `queryRevlog` performs synchronous database operations (cursor iteration). The `withCol` function is already suspend and handles coroutine context switching, but its lambda parameter is not a suspend lambda. Removing `suspend` fixes the blocking compilation error without changing behavior.
- **Tests verified:** All 23 StudyTrackingRepositoryTest tests still pass (no regressions)

## Integration Points

**Phase 6 (Streak Widget):**
- Will display `streakDays`, `graceDaysAvailable`, `todayStudyTimeMs`, and `todayCardsReviewed` from `MuseumUiState`
- Bottom sheet will consume these values for streak display and grace day indicators

**MuseumPersistence:**
- `getStreakDays()` and `getExtraLives()` methods retained for backward compatibility
- No longer called from MuseumViewModel (can be cleaned up in future phase)

## Success Criteria Met

- [x] MuseumViewModel computes streak data from revlog on load and refresh
- [x] MuseumUiState exposes streak count, grace days available, today's study time, and today's card count
- [x] SharedPreferences-based streak/extraLives no longer used in MuseumViewModel (0 references)
- [x] Project compiles successfully
- [x] Plan 01 tests still pass (23/23 tests passing, no regressions)
- [x] `StudyTrackingRepository` referenced 3+ times in ViewModel
- [x] `graceDaysAvailable` appears in UiState definition and both update calls

## Self-Check: PASSED

**Modified files exist:**
```
FOUND: AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/MuseumViewModel.kt
FOUND: AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/StudyTrackingRepository.kt
```

**Commit exists:**
```
FOUND: 599bb68 (feat(05-02): wire StudyTrackingRepository into MuseumViewModel)
```

**SharedPreferences removed from ViewModel:**
```
grep "MuseumPersistence.getStreakDays" MuseumViewModel.kt → 0 matches
grep "MuseumPersistence.getExtraLives" MuseumViewModel.kt → 0 matches
```

**Repository integration verified:**
```
grep "StudyTrackingRepository" MuseumViewModel.kt → 3 matches
  - Line 70: StudyTrackingRepository.queryRevlog(db, fromDate, today)
  - Line 71: StudyTrackingRepository.getDailyStudyData(entries, fromDate, today)
  - Line 72: StudyTrackingRepository.calculateStreak(dailyData, today)
```

**UiState fields verified:**
```
grep "graceDaysAvailable" MuseumViewModel.kt → 3 occurrences
  - Line 53: graceDaysAvailable = streakInfo.graceDaysAvailable (loadMuseumData)
  - Line 229: graceDaysAvailable = streakInfo.graceDaysAvailable (refreshData)
  - Line 245: val graceDaysAvailable: Int = 0 (UiState definition)
```

**Tests still passing:**
```
./gradlew :AnkiDroid:testPlayDebugUnitTest --tests "com.ichi2.anki.ui.museum.StudyTrackingRepositoryTest"
BUILD SUCCESSFUL - 23 tests completed, 0 failed
```
