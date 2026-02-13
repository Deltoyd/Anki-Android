---
phase: 04-gallery-redesign
plan: 01
subsystem: ui
tags: [android, layout, viewpager2, museum, gallery]

# Dependency graph
requires:
  - phase: 03-transparency-fix
    provides: "Working puzzle piece display with proper transparency"
provides:
  - "Expanded painting area filling available vertical space"
  - "Title and artist labels below gallery pager"
  - "Homescreen without heatmap or deck stats"
affects: [07-heatmap-views, homescreen-polish]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Weight-based layout for flexible gallery sizing"
    - "Separate TextViews for title and artist with distinct styling"

key-files:
  created: []
  modified:
    - AnkiDroid/src/main/res/layout/activity_museum.xml
    - AnkiDroid/src/main/java/com/ichi2/anki/MuseumActivity.kt

key-decisions:
  - "Removed heatmap from homescreen to maximize masterpiece visibility"
  - "Removed deck stats row to reduce visual clutter"
  - "Used layout_weight=1 for gallery to fill remaining vertical space"

patterns-established:
  - "Gallery pager uses weight-based sizing to adapt to available space"
  - "Title shown in 16sp bold, artist in 13sp secondary color"

# Metrics
duration: 7min
completed: 2026-02-13
---

# Phase 04 Plan 01: Gallery Redesign Summary

**Homescreen painting area expanded to fill available space with heatmap and stats removed, title and artist labels added below gallery**

## Performance

- **Duration:** 7 min 19 sec
- **Started:** 2026-02-13T11:34:37Z
- **Completed:** 2026-02-13T11:41:56Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- Removed heatmap section (year label, subheader, HeatmapView) from homescreen layout
- Removed deck stats row (streak and today's cards) from homescreen
- Expanded gallery pager to fill all available vertical space using layout weights
- Added separate artist TextView below title with proper typography styling
- Cleaned up MuseumActivity by removing setupHeatmap, setupStats, and generateMockActivityData methods

## Task Commits

Each task was committed atomically:

1. **Task 1: Remove heatmap and stats from homescreen** - `463d06f` (refactor)
2. **Task 2: Expand painting area and add artist label** - `9833131` (feat)

## Files Created/Modified
- `AnkiDroid/src/main/res/layout/activity_museum.xml` - Removed stats row and heatmap section, expanded gallery ConstraintLayout with weight-based sizing, added artistText TextView
- `AnkiDroid/src/main/java/com/ichi2/anki/MuseumActivity.kt` - Removed heatmap and stats methods, updated page callbacks to set both title and artist

## Decisions Made
- Used `layout_weight="1"` on gallery ConstraintLayout to allow painting to fill all remaining vertical space after top navigation bar
- Changed parent LinearLayout to `layout_height="match_parent"` to enable weight-based children
- Removed aspect ratio constraint from ViewPager2, allowing it to fill parent ConstraintLayout
- Created separate artistText TextView instead of combining with caption for cleaner styling
- Set artist to empty string when null (no fallback "Unknown artist" string needed)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None - all planned changes compiled and built successfully on first attempt.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

Homescreen gallery redesign complete. The masterpiece now dominates the screen as intended. Ready for:
- Phase 05 (Study Tracking) - streak counter implementation
- Phase 06 (Streak Widget) - homescreen streak indicator
- Phase 07 (Heatmap Views) - relocated heatmap in bottom sheet

HeatmapView.kt preserved for reuse in Phase 07 streak bottom sheet.

## Self-Check: PASSED

All files and commits verified:
- FOUND: activity_museum.xml
- FOUND: MuseumActivity.kt
- FOUND: commit 463d06f (Task 1)
- FOUND: commit 9833131 (Task 2)

---
*Phase: 04-gallery-redesign*
*Completed: 2026-02-13*
