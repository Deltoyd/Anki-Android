---
phase: 03-transparent-locked-pieces
plan: 01
subsystem: ui
tags: [android, canvas, porterDuff, alpha-compositing, kotlin]

# Dependency graph
requires:
  - phase: 02-shape-system-fix
    provides: PNG alpha mask compositing pattern, unified shape system
provides:
  - Locked pieces render painting underneath at 80% opacity
  - saveLayer + DST_IN compositing reused from unlocked piece pattern
affects: [visual polish complete for v1.1]

# Tech tracking
tech-stack:
  added: []
  patterns: [saveLayer compositing for locked pieces, alpha overlay rendering]

key-files:
  created: []
  modified:
    - AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/PaintingPuzzleView.kt
  deleted: []

key-decisions:
  - "Reuse existing alphaMaskPaint (DST_IN) for locked piece painting clipping"
  - "pieceBitmapPaint.alpha = 204 for 80% opacity, reset to 255 after each piece"

patterns-established:
  - "Locked piece compositing: saveLayer → draw painting (DST) → PNG mask with DST_IN → restore → gray PNG at alpha 204"

# Metrics
duration: 5min
completed: 2026-02-13
---

# Phase 3 Plan 1: Transparent Locked Pieces Summary

**Locked puzzle pieces now show the masterpiece painting underneath at 80% opacity, creating a teasing effect that motivates study**

## Performance

- **Duration:** ~5 min
- **Completed:** 2026-02-13
- **Tasks:** 2 (1 implementation, 1 human verification)
- **Files modified:** 1

## Accomplishments

- Added saveLayer + DST_IN compositing to `drawLockedPiecePng()` to render painting underneath
- Gray PNG drawn at alpha 204 (80% opacity) on top, allowing 20% of the painting to show through
- Alpha properly reset to 255 after each piece to prevent accumulation
- Painting visible through tabs as well as body (full piece shape)
- Unlocked piece rendering completely untouched (TRANS-02 satisfied)

## Task Commits

1. **Task 1: Add painting-through-locked-pieces rendering with 80% opacity overlay** - `f04d222` (feat)
2. **Task 2: Visual verification on Pixel 7a** - Checkpoint approved by user

## Files Created/Modified

**Modified:**
- `AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/PaintingPuzzleView.kt` - Added saveLayer + DST_IN compositing and alpha 204 overlay in `drawLockedPiecePng()`

## Decisions Made

**1. Reuse existing alphaMaskPaint for locked piece clipping**
- Rationale: Same PorterDuff DST_IN compositing pattern established in Phase 2 for unlocked pieces
- Result: Consistent, minimal code addition

**2. Alpha 204 for 80% opacity**
- Rationale: User-tested value from earlier iteration; 80% provides good balance between showing the painting hint and keeping locked pieces visually distinct

## Deviations from Plan

None — plan executed exactly as written.

## Issues Encountered

None.

## Next Phase Readiness

**v1.1 milestone complete.** All 4 requirements satisfied:
- SHPE-01: Unlocked pieces clip to PNG shapes ✓
- SHPE-02: No shape mismatch ✓
- TRANS-01: Locked pieces at 80% opacity with painting visible ✓
- TRANS-02: Unlocked pieces at full opacity ✓

Ready for `/gsd:complete-milestone v1.1`.

---
*Phase: 03-transparent-locked-pieces*
*Completed: 2026-02-13*
