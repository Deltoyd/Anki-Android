---
phase: 06-streak-widget-bottom-sheet
plan: 01
type: execute
completed: 2026-02-13
duration: 443s
subsystem: streak-widget
tags: [ui, bottom-sheet, streak, study-time, grace-days]

dependency-graph:
  requires:
    - StudyTrackingRepository (Phase 05-01): "getStudyTimeForPeriod for total study time calculation"
    - MuseumViewModel (Phase 05-02): "Exposes uiState with streak and study data"
  provides:
    - Streak pill widget: "Visible pill-shaped button in toolbar showing current streak"
    - StreakBottomSheet: "Bottom sheet displaying detailed streak stats"
  affects:
    - Phase 7 (Heatmap Views): "Bottom sheet has placeholder for Week/Month/Year tabs"
    - MuseumActivity: "New click handler for streak pill"

tech-stack:
  added:
    - BottomSheetDialogFragment: "Material Design bottom sheet for streak details"
    - View binding delegate (vbpd): "Type-safe view access in bottom sheet"
  patterns:
    - StateFlow collection: "Reactive UI updates from ViewModel state"
    - Time formatting helper: "formatStudyTime() converts ms to human-readable format"
    - Material Design pill button: "TonalButton style with warm #d36b52 color family"

key-files:
  created:
    - AnkiDroid/src/main/res/layout/bottomsheet_streak.xml: "Bottom sheet layout with streak, study time, grace days"
    - AnkiDroid/src/main/res/drawable/bottomsheet_handle.xml: "Drag handle drawable for bottom sheet"
    - AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/StreakBottomSheet.kt: "Bottom sheet fragment with data formatting logic"
  modified:
    - AnkiDroid/src/main/res/layout/activity_museum.xml: "Added streak pill to top navigation bar"
    - AnkiDroid/src/main/res/values/colors.xml: "Added 4 streak colors in #d36b52 family"
    - AnkiDroid/src/main/res/values/02-strings.xml: "Added 10 streak-related string resources"
    - AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/MuseumViewModel.kt: "Added totalStudyTimeMs to state, created StudyStats data class"
    - AnkiDroid/src/main/java/com/ichi2/anki/MuseumActivity.kt: "Wired streak pill click handler and text updates"

decisions:
  - decision: "Place streak pill as left-most element in toolbar"
    rationale: "User spec requires top-left placement. Deck selector moves to second position. This maximizes visibility and follows 'left to right' reading order."
    alternatives: ["Replace deck selector (would lose deck switching functionality)", "Place after deck selector (less prominent)"]
  - decision: "Use fire emoji (🔥) in pill text and bottom sheet"
    rationale: "Emoji provides immediate visual association with streak concept. Universally understood symbol for 'fire streak' in gamification."
    alternatives: ["SVG fire icon (requires drawable resource)", "Text-only 'Streak' label (less engaging)"]
  - decision: "Format study time as hours/minutes or minutes/seconds"
    rationale: "Matches user mental model of time. Hours matter for long-term tracking, seconds matter for minimal study sessions."
    alternatives: ["Always show hours (cluttered for short times)", "Always show minutes (loses precision for quick reviews)"]
  - decision: "Include grace days in bottom sheet stats row"
    rationale: "Grace days are core to streak system (Phase 5). Users need visibility to understand streak resilience."
    alternatives: ["Hide grace days until used (less transparent)", "Show only in separate screen (reduces accessibility)"]

metrics:
  tasks-completed: 2
  tests-passing: 0
  files-modified: 8
  lines-added: 262
  lines-removed: 12
---

# Phase 06 Plan 01: Streak Widget & Bottom Sheet Summary

**One-liner:** Pill-shaped streak widget in toolbar with tappable bottom sheet showing current streak, total study time, and grace days in #d36b52 warm color palette

## What Was Built

Added a visual streak widget to the MuseumActivity toolbar and created an interactive bottom sheet that provides detailed study statistics.

**Key components:**

1. **Streak Pill Widget (Toolbar):**
   - Pill-shaped MaterialButton at top-left of navigation bar
   - Shows "Streak N 🔥" format with fire emoji
   - Uses TonalButton style with warm background (#FFF0EC) and accent text (#D36B52)
   - Updates reactively when streak changes

2. **StreakBottomSheet Fragment:**
   - Material Design bottom sheet with drag handle
   - Large fire emoji (48sp) + bold streak count (36sp) centered at top
   - Two-column stats row: Total Study Time | Grace Days
   - Time formatting logic: hours+minutes, minutes, or seconds based on magnitude
   - Collects ViewModel uiState via StateFlow for reactive updates

3. **ViewModel Integration:**
   - Added `totalStudyTimeMs: Long` to MuseumUiState
   - Created `StudyStats` data class to replace Triple return type
   - Calls `StudyTrackingRepository.getStudyTimeForPeriod()` to sum all study time from last 365 days
   - Updates both in `loadMuseumData()` and `refreshData()`

4. **Activity Wiring:**
   - Click listener on streakPill shows StreakBottomSheet
   - StateFlow collection updates pill text: `getString(R.string.streak_pill_format, state.streakDays)`
   - Import and fragment manager integration

5. **Resources:**
   - 4 new colors: pill background, text, accent, accent light
   - 10 new string resources with format placeholders (%d)
   - Bottom sheet layout with dividers, centered content, placeholder comment for Phase 7 heatmap

## Deviations from Plan

None - plan executed exactly as written. All layout specs, color values, string resources, and code structure matched the plan document.

## Integration Points

**Phase 7 (Heatmap Views):**
- Bottom sheet has explicit placeholder comment: `<!-- Placeholder for heatmap (Phase 7) -->`
- Space reserved below stats row for Week/Month/Year tabs
- Bottom sheet will be expanded to include interactive heatmap visualizations

**Phase 5 (Study Tracking):**
- Consumes `streakDays`, `graceDaysAvailable`, `totalStudyTimeMs` from MuseumUiState
- Depends on `StudyTrackingRepository.getStudyTimeForPeriod()` for accurate time calculation
- Grace days now visible to users (previously backend-only concept)

## Success Criteria Met

- [x] STRK-01: Pill-shaped streak widget visible at top-left of homescreen toolbar with "Streak", day count, and fire icon
- [x] STRK-02: Tapping the streak pill opens the streak bottom sheet
- [x] STRK-03: Bottom sheet displays current streak count (consecutive days studied)
- [x] STRK-04: Bottom sheet displays total study time formatted as hours/minutes
- [x] Project compiles successfully (BUILD SUCCESSFUL in 3m 19s)
- [x] Existing functionality preserved (deck selector, menu, review button all intact)

## Self-Check: PASSED

**Created files exist:**
```
FOUND: AnkiDroid/src/main/res/layout/bottomsheet_streak.xml
FOUND: AnkiDroid/src/main/res/drawable/bottomsheet_handle.xml
FOUND: AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/StreakBottomSheet.kt
```

**Modified files exist:**
```
FOUND: AnkiDroid/src/main/res/layout/activity_museum.xml
FOUND: AnkiDroid/src/main/res/values/colors.xml
FOUND: AnkiDroid/src/main/res/values/02-strings.xml
FOUND: AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/MuseumViewModel.kt
FOUND: AnkiDroid/src/main/java/com/ichi2/anki/MuseumActivity.kt
```

**Commits exist:**
```
FOUND: 7917cd0 (feat(06-01): add streak pill widget and bottom sheet layout)
FOUND: 9bcd173 (feat(06-01): create StreakBottomSheet and wire to MuseumActivity)
```

**Streak pill present in layout:**
```
grep "streakPill" activity_museum.xml → 1 match (android:id="@+id/streakPill")
```

**Click wiring verified:**
```
grep "streakPill.*setOnClickListener" MuseumActivity.kt → 1 match
  binding.streakPill.setOnClickListener {
      StreakBottomSheet().show(supportFragmentManager, StreakBottomSheet.TAG)
  }
```

**Data flow verified:**
```
grep "totalStudyTimeMs" MuseumViewModel.kt → 5 occurrences
  - Line 5: val totalStudyTimeMs: Long (StudyStats data class)
  - Line 16: totalStudyTimeMs = totalTime (assignment in loadStudyStats)
  - Line 37: totalStudyTimeMs = stats.totalStudyTimeMs (loadMuseumData)
  - Line 52: totalStudyTimeMs = stats.totalStudyTimeMs (refreshData)
  - Line 70: val totalStudyTimeMs: Long = 0 (MuseumUiState definition)

grep "getStudyTimeForPeriod" MuseumViewModel.kt → 1 match
  val totalTime = StudyTrackingRepository.getStudyTimeForPeriod(dailyData)
```

**Build verification:**
```
./gradlew :AnkiDroid:assemblePlayDebug
BUILD SUCCESSFUL in 3m 19s
126 actionable tasks: 17 executed, 109 up-to-date
```

## Visual Design Notes

**Color palette (#d36b52 family):**
- `museolingo_streak_pill_bg`: #FFF0EC (very light warm tint for pill background)
- `museolingo_streak_text`: #D36B52 (user-specified primary color for text)
- `museolingo_streak_accent`: #D36B52 (used for bold streak count in bottom sheet)
- `museolingo_streak_accent_light`: #E8998A (lighter variant for future use)

**Typography:**
- Pill: 13sp text, 36dp height, 18dp corner radius (tight, compact)
- Bottom sheet fire: 48sp emoji
- Bottom sheet streak count: 36sp bold serif
- Bottom sheet stats: 20sp bold for values, 12sp for labels

**Layout hierarchy:**
- Top nav bar order: streakPill → languageSelector → appTitle (weight=1, centered) → menuButton
- Bottom sheet: handle → fire+count → divider → stats row (50/50 split)
- 24dp padding on bottom sheet for breathing room

## Phase Completion Status

Phase 6 Plan 1 complete. Ready for Phase 7 (Heatmap Views) which will add Week/Month/Year tabs to this bottom sheet.
