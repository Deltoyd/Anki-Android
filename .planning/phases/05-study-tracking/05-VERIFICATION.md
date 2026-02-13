---
phase: 05-study-tracking
verified: 2026-02-13T00:00:00Z
status: passed
score: 4/4 must-haves verified
must_haves:
  truths:
    - "System marks a day as studied when user reviews at least 1 card"
    - "System calculates consecutive days studied (streak count)"
    - "System tracks total study time per day"
    - "System provides study intensity data (for coloring heatmap views)"
  artifacts:
    - path: "AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/StudyTrackingRepository.kt"
      status: verified
    - path: "AnkiDroid/src/test/java/com/ichi2/anki/ui/museum/StudyTrackingRepositoryTest.kt"
      status: verified
    - path: "AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/MuseumViewModel.kt"
      status: verified
  key_links:
    - from: "MuseumViewModel"
      to: "StudyTrackingRepository"
      via: "direct method calls within withCol block"
      status: wired
    - from: "StudyTrackingRepository"
      to: "revlog table"
      via: "SQL query: SELECT id, time FROM revlog WHERE id >= ? AND id < ?"
      status: wired
---

# Phase 05: Study Tracking & Data Layer Verification Report

**Phase Goal:** System tracks study sessions and calculates streak/intensity data
**Verified:** 2026-02-13T00:00:00Z
**Status:** PASSED
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | System marks a day as studied when user reviews at least 1 card | ✓ VERIFIED | `DailyStudyData.wasStudied = cardsReviewed >= 1` (line 66) |
| 2 | System calculates consecutive days studied (streak count) | ✓ VERIFIED | `calculateStreak()` method with two-pass algorithm (lines 93-185) |
| 3 | System tracks total study time per day | ✓ VERIFIED | `DailyStudyData.studyTimeMs` with 30s cap per card (line 65) |
| 4 | System provides study intensity data (for coloring heatmap views) | ✓ VERIFIED | `StudyIntensity` enum with 5 levels + NONE (lines 281-288) |

**Score:** 4/4 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `StudyTrackingRepository.kt` | Study tracking data layer | ✓ VERIFIED | 289 lines, all 5 core methods implemented |
| `StudyTrackingRepositoryTest.kt` | Unit tests for all logic | ✓ VERIFIED | 321 lines, 23 passing tests |
| `MuseumViewModel.kt` | ViewModel integration | ✓ VERIFIED | Modified, wired to repository via `loadStudyStats()` |

**Artifact Details:**

**1. StudyTrackingRepository.kt (289 lines)**
- Level 1 (Exists): ✓ File exists
- Level 2 (Substantive): ✓ Contains all required methods:
  - `queryRevlog()` - queries revlog table with date range
  - `getDailyStudyData()` - groups entries by date with 30s cap
  - `calculateStreak()` - two-pass algorithm for grace days
  - `calculateIntensity()` - 5 levels relative to average
  - `computeDailyAverage()` - average across studied days only
  - `getStudyTimeForPeriod()` - sums capped study time
- Level 3 (Wired): ✓ Used by MuseumViewModel in `loadStudyStats()` (lines 70-72)
- Exports: ✓ All data models present: `RevlogEntry`, `DailyStudyData`, `StreakInfo`, `DayStatus`, `StudyIntensity`

**2. StudyTrackingRepositoryTest.kt (321 lines)**
- Level 1 (Exists): ✓ File exists
- Level 2 (Substantive): ✓ 23 comprehensive tests covering:
  - `getDailyStudyData`: 4 tests (empty, single, cap, multi-day)
  - `calculateStreak`: 7 tests (no history, consecutive, grace earn/consume/break, cap at 5, today unstudied)
  - `calculateIntensity`: 7 tests (zero, 5 levels, no-history fallback)
  - `computeDailyAverage`: 3 tests
  - `getStudyTimeForPeriod`: 2 tests
- Level 3 (Wired): ✓ All tests pass (verified via `./gradlew :AnkiDroid:testPlayDebugUnitTest`)

**3. MuseumViewModel.kt (modified)**
- Level 1 (Exists): ✓ File exists
- Level 2 (Substantive): ✓ Contains repository integration:
  - `loadStudyStats()` helper (lines 65-76)
  - `MuseumUiState` updated with streak fields (lines 244-247)
  - SharedPreferences calls removed (0 matches for getStreakDays/getExtraLives)
- Level 3 (Wired): ✓ Called from `loadMuseumData()` (line 34) and `refreshData()` (line 224)

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| MuseumViewModel | StudyTrackingRepository | Direct method calls | ✓ WIRED | 3 calls in `loadStudyStats()`: queryRevlog, getDailyStudyData, calculateStreak |
| StudyTrackingRepository | revlog table | SQL query | ✓ WIRED | Line 34: `SELECT id, time FROM revlog WHERE id >= ? AND id < ?` |
| MuseumViewModel | MuseumUiState | State updates | ✓ WIRED | Lines 52-55, 228-231: graceDaysAvailable, todayStudyTimeMs, todayCardsReviewed |

**Key Link Details:**

**Link 1: ViewModel → Repository**
- Pattern: Function calls within `withCol` block
- Evidence:
  ```kotlin
  // MuseumViewModel.kt lines 70-72
  val entries = StudyTrackingRepository.queryRevlog(db, fromDate, today)
  val dailyData = StudyTrackingRepository.getDailyStudyData(entries, fromDate, today)
  val streakInfo = StudyTrackingRepository.calculateStreak(dailyData, today)
  ```
- Status: ✓ WIRED — Repository methods called and results assigned to variables

**Link 2: Repository → revlog table**
- Pattern: SQL query with cursor iteration
- Evidence:
  ```kotlin
  // StudyTrackingRepository.kt line 34
  db.query("SELECT id, time FROM revlog WHERE id >= ? AND id < ?", fromMs, toMs).use { cursor ->
      while (cursor.moveToNext()) {
          entries.add(RevlogEntry(id = cursor.getLong(0), timeMs = cursor.getInt(1)))
      }
  }
  ```
- Status: ✓ WIRED — Query executed, results extracted and returned

**Link 3: Repository → ViewModel State**
- Pattern: State flow update with computed values
- Evidence:
  ```kotlin
  // MuseumViewModel.kt lines 52-55
  streakDays = streakInfo.currentStreak,
  graceDaysAvailable = streakInfo.graceDaysAvailable,
  todayStudyTimeMs = todayTimeMs,
  todayCardsReviewed = todayCards,
  ```
- Status: ✓ WIRED — Repository results flow into UI state

### Requirements Coverage

Phase 05 implements **STRK-05** from REQUIREMENTS.md:

| Requirement | Status | Supporting Truth |
|-------------|--------|------------------|
| STRK-05: Backend tracks streak/grace/intensity | ✓ SATISFIED | Truths 1-4: All study tracking features implemented |

### Anti-Patterns Found

**NONE** - No anti-patterns detected.

Scanned files:
- `StudyTrackingRepository.kt`: No TODOs, FIXMEs, placeholder comments, empty returns, or console.logs
- `MuseumViewModel.kt`: No TODOs, FIXMEs, placeholder comments, empty returns, or console.logs
- `StudyTrackingRepositoryTest.kt`: 23 substantive tests, no stub implementations

### Implementation Quality

**1. 30-Second Cap Verified:**
```kotlin
// Line 13
private const val CARD_TIME_CAP_MS = 30_000L

// Line 65
val studyTimeMs = dayEntries.sumOf { minOf(it.timeMs.toLong(), CARD_TIME_CAP_MS) }
```

**2. Grace Day System Verified:**
- Earn: Every 3 consecutive study days → +1 grace (max 5)
- Consume: Missing a day with grace > 0 → -1 grace, streak continues
- Break: Missing a day with grace == 0 → streak resets to 0
- Test coverage: 7 streak tests including grace accumulation, consumption, exhaustion, and cap

**3. Intensity Levels Verified:**
- NONE: 0 cards
- LEVEL_1: ≤ 0.5x average
- LEVEL_2: ≤ 1.0x average
- LEVEL_3: ≤ 1.5x average
- LEVEL_4: ≤ 2.0x average
- LEVEL_5: > 2.0x average
- Fallback: Any study when no history → LEVEL_5

**4. Date Handling Verified:**
- Uses `java.time.LocalDate` with `ZoneId.systemDefault()` for midnight boundaries
- Does NOT use Anki's configurable "next day starts at" setting (as specified in plan)
- Line 66: `val today = LocalDate.now()`

**5. Pure Computation Layer Verified:**
- All methods in companion object (stateless)
- No SharedPreferences imports (0 matches)
- No caching — always computes from revlog entries
- Test coverage: 23 unit tests, all pure functions

**6. SharedPreferences Migration Verified:**
- `MuseumPersistence.getStreakDays()` calls: 0 matches in ViewModel (removed)
- `MuseumPersistence.getExtraLives()` calls: 0 matches in ViewModel (removed)
- `graceDaysAvailable` usage: 3 occurrences (UiState definition + 2 updates)

### Build & Test Verification

**Compilation:**
```bash
./gradlew :AnkiDroid:compilePlayDebugKotlin
BUILD SUCCESSFUL in 24s
```

**Unit Tests:**
```bash
./gradlew :AnkiDroid:testPlayDebugUnitTest --tests "com.ichi2.anki.ui.museum.StudyTrackingRepositoryTest"
BUILD SUCCESSFUL in 57s
130 actionable tasks: 8 executed, 122 up-to-date
```
✓ All 23 tests pass, no failures

### Data Flow Verification

**End-to-End Flow:**
1. ✓ User reviews a card → revlog table entry created (Anki core)
2. ✓ ViewModel calls `loadStudyStats()` → queries last 365 days via `StudyTrackingRepository.queryRevlog(db, fromDate, today)`
3. ✓ Repository executes SQL → `SELECT id, time FROM revlog WHERE id >= ? AND id < ?`
4. ✓ Raw entries processed → `getDailyStudyData()` groups by date, caps time at 30s per card
5. ✓ Daily data analyzed → `calculateStreak()` computes streak with grace days
6. ✓ Results returned → `Triple(streakInfo, todayTimeMs, todayCards)`
7. ✓ State updated → `_uiState.update { ... graceDaysAvailable, todayStudyTimeMs, todayCardsReviewed ... }`
8. ✓ UI renders → Phase 6 will display these values in streak widget/bottom sheet

**Integration Points Ready:**
- ✓ Phase 6 (Streak Widget): Can consume `streakDays`, `graceDaysAvailable`, `todayStudyTimeMs`, `todayCardsReviewed` from `MuseumUiState`
- ✓ Phase 7 (Heatmap Views): Can call `getDailyStudyData()` and `calculateIntensity()` for calendar visualizations

### Human Verification Required

**NONE** - All success criteria are programmatically verifiable and verified.

The phase goal is purely backend/data layer — no UI components to visually inspect. All functionality is:
- Unit tested (23 tests pass)
- Wired into ViewModel (verified via code inspection)
- Compiles successfully
- Ready for Phase 6 consumption

---

## Summary

**Status: PASSED** ✓

Phase 05 successfully achieves its goal: "System tracks study sessions and calculates streak/intensity data"

**All 4 success criteria met:**
1. ✓ System marks a day as studied when user reviews at least 1 card (`wasStudied = cardsReviewed >= 1`)
2. ✓ System calculates consecutive days studied (two-pass streak algorithm with grace days)
3. ✓ System tracks total study time per day (30s cap per card, aggregated by date)
4. ✓ System provides study intensity data (5 levels + NONE, relative to personal average)

**Key achievements:**
- Pure computation layer with zero state/caching
- Comprehensive test coverage (23 unit tests, all pass)
- Grace day system (earn 1 per 3 consecutive, max 5, consumes on gaps)
- 30-second time cap per card (prevents idle inflation)
- Revlog-derived data (always accurate, no SharedPreferences)
- ViewModel integration complete (ready for Phase 6 UI)

**Ready to proceed:** Phase 6 (Streak Widget & Bottom Sheet) can now consume study data from `MuseumUiState`.

---

_Verified: 2026-02-13T00:00:00Z_
_Verifier: Claude (gsd-verifier)_
