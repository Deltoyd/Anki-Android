---
phase: 04-gallery-redesign
plan: 02
subsystem: ui
tags: [android, viewpager2, museum, gallery, blur, circular-navigation]

# Dependency graph
requires:
  - phase: 04-01
    provides: "Expanded painting area with title and artist labels"
provides:
  - "Gallery filtered to active and locked paintings only (no completed)"
  - "Locked paintings display with blur and dimming effect"
  - "Circular wrap-around navigation (infinite swipe left/right)"
  - "Gallery always starts on active painting on homescreen load"
  - "Dot indicators show current position among filtered paintings"
affects: [05-study-tracking, 06-streak-widget, 07-heatmap-views]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Scale-based bitmap blur for cross-API compatibility (no library dependency)"
    - "Circular ViewPager2 using virtual positions with modular arithmetic"
    - "Filter-first architecture: completed paintings excluded at ViewModel level"

key-files:
  created: []
  modified:
    - AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/MuseumViewModel.kt
    - AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/GalleryPagerAdapter.kt
    - AnkiDroid/src/main/res/layout/item_gallery_page.xml
    - AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/PageIndicatorHelper.kt
    - AnkiDroid/src/main/java/com/ichi2/anki/MuseumActivity.kt

key-decisions:
  - "Used scale-based blur (downscale + upscale) instead of RenderScript or Toolkit for API compatibility"
  - "Multiplier of 1000 for virtual ViewPager2 positions (sufficient for wrap-around without overflow)"
  - "Removed lock icon from locked paintings - blur effect alone communicates locked state"
  - "Removed gallery position persistence - always start on active painting"
  - "Filter out completed paintings at ViewModel level before adapter receives data"

patterns-established:
  - "Circular ViewPager2: getItemCount() = items.size * MULTIPLIER, getRealPosition() = position % items.size"
  - "Virtual position starts at middle of range: (MULTIPLIER / 2) * items.size"
  - "Locked state visualization: bitmap blur + 50% alpha dimming (no overlay icon)"
  - "Gallery items sorted: ACTIVE first (index 0), then LOCKED, COMPLETED filtered out"

# Metrics
duration: ~15min
completed: 2026-02-13
---

# Phase 04 Plan 02: Gallery Redesign Summary

**Gallery filtered to active and locked paintings with blur effect on locked, circular wrap-around navigation, and always-start-on-active behavior**

## Performance

- **Duration:** Approximately 15 minutes (2 auto tasks + human verification)
- **Started:** 2026-02-13 (prior session)
- **Completed:** 2026-02-13
- **Tasks:** 3 (2 auto + 1 checkpoint:human-verify)
- **Files modified:** 5

## Accomplishments
- Filtered gallery to show only active and locked paintings (completed paintings excluded)
- Implemented scale-based blur effect for locked paintings with 50% dimming
- Added circular wrap-around navigation using virtual ViewPager2 positions
- Gallery always starts on active painting (removed saved position persistence)
- Updated dot indicators to only show active (amber) and locked (hollow gray) states
- Simplified locked overlay layout (removed lock icon, kept blur + dim effect)

## Task Commits

Each task was committed atomically:

1. **Task 1: Filter gallery to active + locked, add blur to locked paintings, update indicators** - `f19542c` (feat)
2. **Task 2: Implement circular wrap-around and always-start-on-active** - `441f7ba` (feat)
3. **Task 3: Verify gallery redesign on device** - Checkpoint approved by user

## Files Created/Modified
- `AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/MuseumViewModel.kt` - Filter galleryItems to exclude COMPLETED state, removed savedPosition logic
- `AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/GalleryPagerAdapter.kt` - Added blurBitmap() helper, circular navigation with getRealPosition/getStartPosition, virtual item count
- `AnkiDroid/src/main/res/layout/item_gallery_page.xml` - Simplified lockedOverlay (removed lock icon/text, kept blurred ImageView with 50% alpha)
- `AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/PageIndicatorHelper.kt` - Removed COMPLETED state handling and COLOR_GREEN constant
- `AnkiDroid/src/main/java/com/ichi2/anki/MuseumActivity.kt` - Use adapter.getStartPosition() for initial page, convert virtual to real positions in callbacks, removed setGalleryPosition calls

## Decisions Made
- **Blur technique:** Used scale-based blur (downscale bitmap to 1/8 size, upscale back) instead of RenderScript or Toolkit.blur() for cross-API compatibility without library dependencies
- **Circular multiplier:** Set MULTIPLIER = 1000 for virtual positions, providing sufficient range for wrap-around without integer overflow
- **Lock icon removal:** Removed explicit lock icon overlay from locked paintings - the blur + dimming effect alone communicates locked state clearly
- **Position persistence removal:** Removed MuseumPersistence getGalleryPosition/setGalleryPosition calls - gallery always opens on active painting per UX decision
- **Filter architecture:** Applied COMPLETED filter at ViewModel level before data reaches adapter, ensuring single source of truth

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None - all planned changes built successfully and passed device verification.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

Gallery redesign complete (Phase 4 of v1.2). Homescreen now shows:
- Masterpiece dominates screen (heatmap/stats removed in 04-01)
- Title and artist labels below painting (added in 04-01)
- Only active and locked paintings in swipeable gallery
- Locked paintings teased with blur effect
- Circular navigation with wrap-around
- Always starts on active painting

**Verification completed:** User confirmed all 8 verification steps passed on device:
1. Homescreen layout correct (nav bar, large painting, dots, title/artist, review button)
2. Heatmap removed
3. Stats row removed
4. Locked paintings visible with blur/dim effect
5. Wrap-around when swiping past last painting
6. Wrap-around when swiping right from first painting
7. Dot indicators update correctly
8. Gallery always starts on active painting after app restart

Ready for Phase 5 (Study Tracking - streak counter implementation) in v1.2 milestone.

## Self-Check: PASSED

All commits verified:
- FOUND: commit f19542c (Task 1)
- FOUND: commit 441f7ba (Task 2)

All key files exist:
- FOUND: AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/MuseumViewModel.kt
- FOUND: AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/GalleryPagerAdapter.kt
- FOUND: AnkiDroid/src/main/res/layout/item_gallery_page.xml
- FOUND: AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/PageIndicatorHelper.kt
- FOUND: AnkiDroid/src/main/java/com/ichi2/anki/MuseumActivity.kt

---
*Phase: 04-gallery-redesign*
*Completed: 2026-02-13*
