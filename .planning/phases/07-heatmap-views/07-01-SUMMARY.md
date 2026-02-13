---
phase: 07-heatmap-views
plan: 01
type: execute
completed: 2026-02-13
duration: 420s
subsystem: heatmap
tags: [ui, custom-view, heatmap, week, month, year, study-tracking]

dependency-graph:
  requires:
    - StreakBottomSheet (Phase 06-01): "Container for heatmap tabs"
    - StudyTrackingRepository (Phase 05-01): "getDailyStudyData, calculateIntensity, computeDailyAverage"
    - MuseumViewModel (Phase 05-02): "Exposes dailyStudyData in uiState"
  provides:
    - WeekHeatmapView: "7-day horizontal circle display with intensity colors"
    - MonthHeatmapView: "Calendar grid with circles colored by intensity"
    - YearHeatmapView: "365-day GitHub-style dot heatmap"
    - TabLayout + ViewFlipper: "Week/Month/Year tab switching"
  affects:
    - StreakBottomSheet: "Now contains tabbed heatmap views"

tech-stack:
  added:
    - Custom View (Canvas): "WeekHeatmapView, MonthHeatmapView, YearHeatmapView"
    - TabLayout + ViewFlipper: "Tab switching for heatmap views"
  patterns:
    - Custom drawing: "onDraw() with Paint for circles/dots"
    - Intensity mapping: "HeatmapColors maps StudyIntensity to color values"
    - Data-driven views: "setData() method updates views reactively"

key-files:
  created:
    - AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/heatmap/HeatmapColors.kt: "Intensity-to-color mapping helper"
    - AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/heatmap/WeekHeatmapView.kt: "7-day circle heatmap"
    - AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/heatmap/MonthHeatmapView.kt: "Calendar grid heatmap"
    - AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/heatmap/YearHeatmapView.kt: "365-day dot heatmap"
  modified:
    - AnkiDroid/src/main/res/layout/bottomsheet_streak.xml: "Added TabLayout, ViewFlipper, and heatmap views"
    - AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/StreakBottomSheet.kt: "Wire tabs, set heatmap data"
    - AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/MuseumViewModel.kt: "Expose dailyStudyData in MuseumUiState"
    - AnkiDroid/src/main/res/values/colors.xml: "Added 7 heatmap intensity colors"
    - AnkiDroid/src/main/res/values/02-strings.xml: "Added tab labels and study time format"

decisions:
  - decision: "Use ViewFlipper instead of ViewPager2 for tab content"
    rationale: "Simpler implementation, no adapter needed, content is static after load"
    alternatives: ["ViewPager2 (overkill for 3 static views)", "Show/hide views (more complex state)"]
  - decision: "Custom Canvas-based views instead of RecyclerView grids"
    rationale: "More control over precise positioning, no view recycling needed for small datasets"
    alternatives: ["RecyclerView grid (heavier, harder to customize)", "Compose (not used in this codebase)"]
  - decision: "Intensity colors in #d36b52 family per user spec"
    rationale: "Matches streak pill/bottom sheet color palette for visual consistency"
    alternatives: ["Green heatmap (GitHub style)", "Blue heatmap (generic)"]
  - decision: "HorizontalScrollView for YearHeatmapView"
    rationale: "365 days (53 columns) may exceed screen width, scrolling allows full view"
    alternatives: ["Scale to fit (dots too small)", "Show only recent months (loses context)"]

metrics:
  tasks-completed: 1
  tests-passing: 0
  files-modified: 9
  lines-added: 997
  lines-removed: 15
---

# Phase 07 Plan 01: Heatmap Views Summary

**One-liner:** Tabbed heatmap views (Week, Month, Year) added to streak bottom sheet with #d36b52 intensity colors and study time display

## What Was Built

Added three interactive heatmap views to the StreakBottomSheet, switchable via Material TabLayout.

**Key components:**

1. **WeekHeatmapView:**
   - 7 horizontal circles representing Monday-Sunday
   - Each circle colored by study intensity (gray → #d36b52 shades)
   - Current day highlighted with dark ring
   - Day labels (M, T, W, T, F, S, S) below circles
   - Shows week's total study time

2. **MonthHeatmapView:**
   - Calendar grid (7 columns × up to 6 rows)
   - Day header row (M, T, W, T, F, S, S)
   - Adjacent month days shown with reduced opacity
   - Current day highlighted with ring
   - Shows month's total study time

3. **YearHeatmapView:**
   - GitHub-style dot heatmap (53 columns × 7 rows)
   - Each dot represents one day, colored by intensity
   - Today highlighted with subtle ring
   - Horizontally scrollable for full 365-day view
   - Shows year's total study time

4. **HeatmapColors Helper:**
   - Maps StudyIntensity enum to color resources
   - 6 intensity levels: NONE (gray), LEVEL_1-5 (#d36b52 gradients)
   - getCurrentDayRingColor() for highlight ring

5. **Tab Integration:**
   - Material TabLayout with 3 tabs (Week | Month | Year)
   - ViewFlipper for efficient view switching
   - Tab indicator in #d36b52 accent color

## Color Palette

| Intensity | Color | Usage |
|-----------|-------|-------|
| NONE | #E8E8E8 | Unstudied days |
| LEVEL_1 | #F5D6CF | <= 0.5x average |
| LEVEL_2 | #EBB8AD | <= 1.0x average |
| LEVEL_3 | #E19A8B | <= 1.5x average |
| LEVEL_4 | #D77C69 | <= 2.0x average |
| LEVEL_5 | #D36B52 | > 2.0x average |

## Success Criteria Met

- [x] WEEK-01: 7 rounded day circles (M-S) in Week tab
- [x] WEEK-02: Gray unstudied, #d36b52 shades for studied
- [x] WEEK-03: Current day visually highlighted
- [x] MNTH-01: Month tab with calendar grid
- [x] MNTH-02: Calendar days colored by study intensity
- [x] MNTH-03: Current day highlighted with ring
- [x] MNTH-04: Monthly study time displayed
- [x] YEAR-01: Year tab with compact dot heatmap
- [x] YEAR-02: Dots colored by study intensity
- [x] YEAR-03: Yearly study time displayed
- [x] Build compiles successfully

## Self-Check: PASSED

**Created files exist:**
```
FOUND: AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/heatmap/HeatmapColors.kt
FOUND: AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/heatmap/WeekHeatmapView.kt
FOUND: AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/heatmap/MonthHeatmapView.kt
FOUND: AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/heatmap/YearHeatmapView.kt
```

**Build verification:**
```
./gradlew :AnkiDroid:compilePlayDebugKotlin
BUILD SUCCESSFUL
```

## Phase Completion Status

Phase 7 Plan 1 complete. All 10 requirements addressed.

---

**v1.2 Streak & Gallery Redesign: COMPLETE**

All phases (4-7) finished:
- Phase 4: Gallery Redesign ✓
- Phase 5: Study Tracking ✓
- Phase 6: Streak Widget & Bottom Sheet ✓
- Phase 7: Heatmap Views ✓
