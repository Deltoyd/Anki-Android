---
phase: 02-shape-system-fix
plan: 01
subsystem: ui
tags: [android, canvas, porterDuff, alpha-compositing, kotlin]

# Dependency graph
requires:
  - phase: 01-puzzle-assembly-fix
    provides: 14 PNG piece assets with proper tab/hole interlocking
provides:
  - Unlocked pieces now clip using PNG alpha masks (same shapes as locked pieces)
  - PorterDuff DST_IN compositing pattern for shape clipping
  - Eliminated dual-shape system (PuzzlePiecePathGenerator removed)
affects: [future puzzle rendering enhancements, transparency work]

# Tech tracking
tech-stack:
  added: []
  patterns: [PorterDuff alpha mask compositing, saveLayer for offscreen rendering]

key-files:
  created: []
  modified:
    - AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/PaintingPuzzleView.kt
  deleted:
    - AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/PuzzlePiecePathGenerator.kt

key-decisions:
  - "Use PorterDuff.Mode.DST_IN with saveLayer for PNG alpha mask compositing"
  - "Delete PuzzlePiecePathGenerator entirely (no other usages in codebase)"
  - "Update peek mode to use PNG-based outlines instead of bezier paths"

patterns-established:
  - "Alpha mask compositing: saveLayer → draw painting (DST) → draw PNG mask with DST_IN (SRC) → restore"
  - "Shared dest rect calculation for locked and unlocked piece positioning"

# Metrics
duration: 15min
completed: 2026-02-13
---

# Phase 2 Plan 1: Shape System Fix Summary

**Unlocked puzzle pieces now use PNG alpha mask clipping via PorterDuff DST_IN compositing, eliminating the shape mismatch with locked pieces**

## Performance

- **Duration:** 15 min (estimated, continuation after checkpoint)
- **Started:** 2026-02-13T00:04:57+01:00
- **Completed:** 2026-02-13T05:45:39Z
- **Tasks:** 2 (1 implementation, 1 human verification)
- **Files modified:** 1
- **Files deleted:** 1

## Accomplishments

- Replaced bezier-curve path clipping with PNG alpha mask clipping for unlocked pieces
- Unlocked and locked pieces now share identical jigsaw shapes (both derive from PNG assets)
- Eliminated 239 lines of dead code (PuzzlePiecePathGenerator.kt)
- User verification confirmed: no visible shape mismatch on Pixel 7a device

## Task Commits

Each task was committed atomically:

1. **Task 1: Replace path-based clipping with PNG alpha mask clipping for unlocked pieces** - `1d69856` (feat)
2. **Task 2: Visual verification of shape match between locked and unlocked pieces** - Checkpoint approved by user

**Plan metadata:** (to be committed after this summary)

## Files Created/Modified

**Modified:**
- `AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/PaintingPuzzleView.kt` - Replaced path-based clipping with PorterDuff DST_IN alpha mask compositing for unlocked pieces, updated peek mode to use PNG outlines, removed all PuzzlePiecePathGenerator references

**Deleted:**
- `AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/PuzzlePiecePathGenerator.kt` - No longer needed, eliminated dual-shape system (239 lines removed)

## Decisions Made

**1. PorterDuff.Mode.DST_IN for alpha masking**
- Rationale: Canvas saveLayer with DST_IN compositing provides pixel-perfect shape clipping using the PNG's alpha channel as a mask
- Alternative considered: Tracing PNG outlines to Path objects (rejected: fragile, complex, unnecessary)

**2. Delete PuzzlePiecePathGenerator entirely**
- Rationale: No other files use it (confirmed via grep), removing dead code eliminates confusion about shape sources
- Result: 239 lines of bezier curve generation code removed

**3. Update peek mode to use PNG-based outlines**
- Rationale: Peek mode previously used pathGenerator paths, would crash after deletion
- Implementation: Draw piece PNGs at low alpha (40) to show faint jigsaw outlines

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

**Ready for Phase 2 Plan 2 (transparency work):**
- Shape system unified (locked and unlocked pieces share PNG shapes)
- Unlocked piece rendering pipeline established (saveLayer → painting → alpha mask)
- Visual verification complete (user confirmed on device)

**No blockers.** The shape fix is complete and verified. Next phase can proceed with transparency adjustments knowing the shape foundation is solid.

## Self-Check

Verifying documented claims:

```bash
# Check modified file exists
[ -f "AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/PaintingPuzzleView.kt" ] && echo "FOUND" || echo "MISSING"
# FOUND

# Check deleted file is gone
[ ! -f "AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/PuzzlePiecePathGenerator.kt" ] && echo "DELETED" || echo "STILL EXISTS"
# DELETED

# Check commit exists
git log --oneline --all | grep -q "1d69856" && echo "FOUND" || echo "MISSING"
# FOUND
```

## Self-Check: PASSED

All documented files, commits, and changes verified.

---
*Phase: 02-shape-system-fix*
*Completed: 2026-02-13*
