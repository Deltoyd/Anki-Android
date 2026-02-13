---
phase: 05-study-tracking
plan: 01
type: tdd
completed: 2026-02-13
duration: 625s
subsystem: study-tracking
tags: [repository, data-layer, streak-logic, grace-days, tdd]

dependency-graph:
  requires: []
  provides:
    - StudyTrackingRepository: "Pure data layer computing study stats from revlog"
    - Grace day system: "Streak continuation with 1 grace per 3 consecutive days"
    - Intensity calculation: "5 levels relative to personal daily average"
  affects:
    - Phase 6 (Streak Widget): "Consumes StreakInfo for bottom sheet display"
    - Phase 7 (Heatmap Views): "Consumes DailyStudyData for calendar visualizations"

tech-stack:
  added:
    - java.time.LocalDate: "Midnight local time boundaries (not Anki's configurable cutoff)"
    - Revlog queries: "SELECT id, time FROM revlog WHERE id >= ? AND id < ?"
  patterns:
    - TDD: "RED-GREEN-REFACTOR with 23 unit tests"
    - Pure functions: "All computation methods in companion object, no state"
    - On-the-fly calculation: "No caching, always accurate from revlog"

key-files:
  created:
    - AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/StudyTrackingRepository.kt: "Repository with 5 core methods + data models"
    - AnkiDroid/src/test/java/com/ichi2/anki/ui/museum/StudyTrackingRepositoryTest.kt: "23 unit tests covering all logic"
  modified: []

decisions:
  - decision: "Use two-pass algorithm for streak calculation"
    rationale: "Walking backward to find streak extent, then forward to accumulate grace days correctly in chronological order"
    alternatives: ["Single backward pass (failed - grace accumulation order wrong)"]
  - decision: "Cap individual card time at 30 seconds"
    rationale: "Prevents idle inflation if user falls asleep mid-review"
    alternatives: ["No cap (honest but inflated)", "Cap at 60s (too generous)"]
  - decision: "Exclude zero-card days from daily average"
    rationale: "Average should reflect study days only, not gaps"
    alternatives: ["Include all days (dilutes average)"]
  - decision: "Use LocalDate with ZoneId.systemDefault() for midnight boundaries"
    rationale: "Simple local time boundaries, not Anki's configurable 'next day starts at' setting"
    alternatives: ["Use Anki's dayCutoff (more complex, unnecessary for this feature)"]

metrics:
  tasks-completed: 1
  tests-added: 23
  tests-passing: 23
  files-created: 2
  lines-added: 568
---

# Phase 05 Plan 01: Study Tracking Repository Summary

**One-liner:** Pure data layer computing daily study stats, grace-day streaks, and relative intensity from Anki's revlog without caching

## What Was Built

Created `StudyTrackingRepository` as a pure computation layer that queries Anki's revlog table and derives:
- **Daily study data**: Cards reviewed + capped study time per day
- **Streak with grace days**: Consecutive study days with 1 grace per 3 consecutive, max 5
- **Study intensity**: 5 levels relative to user's personal daily average
- **Daily average**: Computed across studied days only (excludes zero-card days)
- **Period totals**: Sum of capped study time across date range

All methods are pure functions in a companion object - no state, no caching, always accurate.

## TDD Process

### RED Phase (commit a91792b)
Wrote 23 failing tests covering:
- `getDailyStudyData`: Empty revlog, single review, 30s cap, multi-day grouping, midnight boundaries
- `calculateStreak`: No history, 1 day, 3 consecutive (earn grace), missed with grace (consume), missed without grace (break), 18 days (cap at 5 grace), today unstudied
- `calculateIntensity`: Zero cards, 5 level boundaries, no-history fallback
- `computeDailyAverage`: Empty data, studied days only, zero-day exclusion
- `getStudyTimeForPeriod`: Sum across period

Initial implementation had 3 failing tests due to incorrect grace day accumulation when walking backward.

### GREEN Phase (commit c6ea97f)
Fixed streak calculation with two-pass algorithm:
1. **Backward pass**: Collect potential streak days, stopping after 6 consecutive unstudied days (beyond max grace)
2. **Forward pass**: Walk oldest-to-newest to accumulate grace days in chronological order and find actual streak start

This ensures grace days are earned in the correct temporal sequence, not retroactively.

All 23 tests now passing.

### REFACTOR Phase
No refactoring needed - code is clean, well-documented, and efficient.

## Data Models

```kotlin
data class RevlogEntry(val id: Long, val timeMs: Int)
data class DailyStudyData(val date: LocalDate, val cardsReviewed: Int, val studyTimeMs: Long, val wasStudied: Boolean)
data class StreakInfo(val currentStreak: Int, val graceDaysAvailable: Int, val todayStatus: DayStatus)
enum class DayStatus { STUDIED, GRACE_USED, NOT_STUDIED }
enum class StudyIntensity { NONE, LEVEL_1, LEVEL_2, LEVEL_3, LEVEL_4, LEVEL_5 }
```

## Core Methods

1. **`queryRevlog(db, fromDate, toDate)`**: Suspending function that queries revlog table
2. **`getDailyStudyData(entries, fromDate, toDate)`**: Groups entries by date with 30s cap per card
3. **`calculateStreak(dailyData, today)`**: Two-pass algorithm for streak with grace days
4. **`calculateIntensity(cardsReviewed, dailyAverage)`**: Returns intensity level based on ratio
5. **`computeDailyAverage(dailyData)`**: Average across studied days only
6. **`getStudyTimeForPeriod(dailyData)`**: Sum of capped time

## Grace Day System Logic

- **Earn**: Every 3 consecutive actual study days → +1 grace day (max 5)
- **Consume**: Missing a day with grace > 0 → -1 grace day, streak continues, reset consecutive counter
- **Break**: Missing a day with grace = 0 → streak ends
- **Today unstudied**: `todayStatus = NOT_STUDIED`, streak counts up to yesterday

Example: Study days 1-3 → 3-day streak, 1 grace. Skip day 4 → 4-day streak, 0 grace. Study day 5 → 5-day streak, 0 grace.

## Deviations from Plan

None - plan executed exactly as written.

## Integration Points

**Phase 6 (Streak Widget)**: Will call `calculateStreak()` to display current streak and grace days in bottom sheet.

**Phase 7 (Heatmap Views)**: Will call `getDailyStudyData()` and `calculateIntensity()` to render calendar heatmap with 5-level color intensity.

**Production query pattern**: ViewModel will use `withCol { StudyTrackingRepository.queryRevlog(col.db, from, to) }` then pass entries to pure computation functions.

## Testing Coverage

- ✅ Empty data edge cases
- ✅ Single and multiple entries
- ✅ 30s time cap enforcement
- ✅ Date grouping at midnight boundaries
- ✅ Grace day accumulation (3-day cycles)
- ✅ Grace day consumption (gap filling)
- ✅ Grace day cap (max 5)
- ✅ Streak breaks (no grace available)
- ✅ Today unstudied handling
- ✅ Intensity levels (all 5 + NONE)
- ✅ Daily average calculation
- ✅ Period totals

All tests pass via Robolectric on JUnit.

## Success Criteria Met

- [x] StudyTrackingRepository computes all study data from revlog entries without caching
- [x] Streak calculation handles grace day accumulation (1 per 3 consecutive) and consumption correctly
- [x] Grace days capped at 5
- [x] Study time per card capped at 30 seconds
- [x] Intensity uses 5 levels relative to personal daily average
- [x] All edge cases covered by passing unit tests
- [x] Day boundary uses midnight local time (java.time.LocalDate)

## Self-Check: PASSED

**Created files exist:**
```
FOUND: AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/StudyTrackingRepository.kt
FOUND: AnkiDroid/src/test/java/com/ichi2/anki/ui/museum/StudyTrackingRepositoryTest.kt
```

**Commits exist:**
```
FOUND: a91792b (RED phase)
FOUND: c6ea97f (GREEN phase)
```

**Tests pass:**
```
BUILD SUCCESSFUL - 23 tests completed, 0 failed
```

**30s cap verified:**
```
Line 13: private const val CARD_TIME_CAP_MS = 30_000L
Line 64: dayEntries.sumOf { minOf(it.timeMs.toLong(), CARD_TIME_CAP_MS) }
```

**No SharedPreferences:**
```
0 matches - repository is pure computation
```
