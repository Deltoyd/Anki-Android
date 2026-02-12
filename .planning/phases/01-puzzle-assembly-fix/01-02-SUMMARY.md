---
phase: 01-puzzle-assembly-fix
plan: 02
subsystem: ui
tags: [android, kotlin, puzzle, rendering, variant-alternation]

# Dependency graph
requires:
  - phase: 01-01
    provides: 14 new gray gradient puzzle piece PNG assets with variant support
provides:
  - 14-entry pieceBitmaps map with variant-suffixed keys
  - Checkerboard variant alternation logic using (row+col) % 2 formula
  - Proper tab/hole interlocking rendering for locked puzzle pieces
affects: []

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Checkerboard alternation via (row+col) % 2 for jigsaw tab/hole complementarity"
    - "Per-piece body offset calculation using PNG_BODY_SIZE constant for gap elimination"

key-files:
  created: []
  modified:
    - AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/PaintingPuzzleView.kt

key-decisions:
  - "Inverted variant formula from (row+col)%2==0→1 to (row+col)%2==0→2 for left/right borders (opposite of top/bottom/interior)"
  - "Replaced uniform scaling with per-piece body offset calculations to eliminate gaps"
  - "Used PNG_BODY_SIZE=77f for body dimension extraction from puzzle piece PNGs"

patterns-established:
  - "Variant alternation: Interior/top/bottom use (row+col)%2==0→variant 1, left/right use opposite formula"
  - "Rendering: Calculate body offsets per piece, render PNG at grid cell with offsets to align tab centers"

# Metrics
duration: 25min
completed: 2026-02-12
---

# Phase 01 Plan 02: Placement and Rendering Logic Summary

**Checkerboard variant alternation with per-piece body offsets achieves gap-free tab/hole interlocking in 10×10 locked puzzle grid**

## Performance

- **Duration:** ~25 min (across checkpoint iterations)
- **Started:** 2026-02-12 (task execution)
- **Completed:** 2026-02-12T18:51:44Z
- **Tasks:** 2 (1 auto + 1 human-verify checkpoint)
- **Files modified:** 1

## Accomplishments
- Updated pieceBitmaps map from 9 to 14 entries with variant-suffixed keys
- Implemented (row+col) % 2 checkerboard variant alternation in getPieceType()
- Fixed piece rendering gaps with per-piece body offset calculations
- Achieved visual confirmation of unified gray jigsaw with proper tab/hole interlocking

## Task Commits

Each task was committed atomically:

1. **Task 1: Update pieceBitmaps map and getPieceType() for 14-piece variant system** - `65577ab` (feat)
2. **Task 2: Visual verification checkpoint** - APPROVED by user

**Checkpoint fix iterations:**
- `d8eaa82` - Fix piece rendering (gap elimination with body offsets, aspect ratio preservation)
- `848737d` - Swap left/right border variant ordering to fix corner appearance and border gaps

**Plan metadata:** (pending final commit)

## Files Created/Modified
- `AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/PaintingPuzzleView.kt` - Updated pieceBitmaps map (14 entries), implemented checkerboard variant alternation in getPieceType(), replaced uniform scaling with per-piece body offset calculations in drawLockedPiecePng()

## Decisions Made

**1. Inverted variant formula for tab/hole complementarity**
- **Context:** Initial implementation used (row+col)%2==0→variant 1 uniformly, but visual checkpoint revealed tabs meeting tabs and holes meeting holes
- **Decision:** Inverted the variant selection formula to ensure adjacent pieces have complementary shapes
- **Rationale:** Proper jigsaw puzzles require alternating tab/hole patterns between neighbors

**2. Per-piece body offset calculation for gap elimination**
- **Context:** Initial rendering showed gaps between pieces due to uniform scaling approach
- **Decision:** Calculate offsets using PNG_BODY_SIZE=77f to position each piece's body content within its grid cell
- **Rationale:** PNG pieces have variable tab overflow; aligning bodies (not bounding boxes) eliminates gaps

**3. Opposite variant formula for left/right borders**
- **Context:** Corner pieces appeared incorrect and border gaps persisted after initial fixes
- **Decision:** Swapped left/right border variant formula to opposite of top/bottom/interior
- **Rationale:** PNG asset tab/hole orientation required different alternation pattern for vertical vs horizontal borders

## Deviations from Plan

The plan specified 2 tasks: (1) update code for 14-piece system, (2) visual verification. During checkpoint verification, rendering issues were discovered and fixed.

### Auto-fixed Issues

**1. [Rule 1 - Bug] Inverted variant formula for tab/hole complementarity**
- **Found during:** Task 2 (visual verification checkpoint)
- **Issue:** Initial (row+col)%2==0→variant 1 formula produced tabs meeting tabs and holes meeting holes. Visual inspection revealed pieces didn't interlock properly.
- **Fix:** Inverted formula so adjacent pieces receive complementary variants. Changed variant assignment from `if ((row + col) % 2 == 0) 1 else 2` to inverse.
- **Files modified:** AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/PaintingPuzzleView.kt (drawLockedPiecePng function)
- **Verification:** Visual inspection showed proper interlocking with tabs meeting holes
- **Committed in:** d8eaa82 (checkpoint fix iteration)

**2. [Rule 1 - Bug] Fixed piece rendering gaps with body offset calculations**
- **Found during:** Task 2 (visual verification checkpoint)
- **Issue:** Uniform scaling approach produced visible gaps between pieces because PNG pieces have variable tab overflow extending beyond the puzzle body rectangle
- **Fix:** Replaced uniform scaling with per-piece body offset calculation using PNG_BODY_SIZE=77f constant. Each piece's body (not bounding box) is now positioned to fill its grid cell, with tabs overflowing naturally.
- **Files modified:** AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/PaintingPuzzleView.kt (drawLockedPiecePng function)
- **Verification:** Visual inspection showed gap-free puzzle assembly
- **Committed in:** d8eaa82 (checkpoint fix iteration)

**3. [Rule 1 - Bug] Swapped left/right border variant ordering**
- **Found during:** Task 2 (visual verification checkpoint, after initial fixes)
- **Issue:** Corners appeared incorrect and border pieces had gaps after initial fix. Left/right borders were using same variant formula as top/bottom/interior.
- **Fix:** Changed left/right border variant formula to opposite of top/bottom/interior: `if ((row + col) % 2 == 0) 2 else 1` (note the swap of 1 and 2)
- **Files modified:** AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/PaintingPuzzleView.kt (getPieceType function)
- **Verification:** Visual inspection confirmed proper corner appearance and border interlocking
- **Committed in:** 848737d (checkpoint fix iteration)

---

**Total deviations:** 3 auto-fixed (all Rule 1 - Bug fixes discovered during visual verification)
**Impact on plan:** All fixes were necessary for correctness. The plan anticipated visual verification would validate the implementation; discovered rendering issues were bugs that required fixing to achieve the success criteria (unified gray puzzle with proper interlocking). No scope creep.

## Issues Encountered

**Checkpoint iterations required for visual correctness**
- The automated implementation passed code verification (14 entries in map, correct formula structure, successful build)
- Visual verification revealed rendering issues not detectable by automated checks
- Required 3 fix iterations across 2 commits to achieve proper visual result
- Resolution: Applied deviation Rule 1 (auto-fix bugs) for each visual issue, iterated until user approved

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

Phase 01 complete. Locked puzzle pieces now display as one unified gray jigsaw with proper tab/hole interlocking.

**What's ready:**
- All 14 puzzle piece assets in place with proper naming
- Code correctly loads and alternates variants using checkerboard pattern
- Rendering eliminates gaps and achieves visual cohesion
- User-confirmed visual verification passed

**No blockers.** Phase goal achieved: "Locked puzzle pieces display as one unified gray jigsaw with proper tab/hole interlocking."

## Self-Check: PASSED

All claims verified:
```bash
# Verify commits exist
$ git log --oneline | grep -E "(65577ab|d8eaa82|848737d)"
848737d fix(01-02): swap left/right border variant ordering for proper interlocking
d8eaa82 fix(01-02): align piece bodies to grid cells with correct tab overflow
65577ab feat(01-02): update pieceBitmaps map and getPieceType for 14-piece variant system

# Verify file modifications
$ git show 65577ab --stat | grep PaintingPuzzleView.kt
 AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/PaintingPuzzleView.kt | 22 +++++++++++++++++-----

$ git show d8eaa82 --stat | grep PaintingPuzzleView.kt
 AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/PaintingPuzzleView.kt | 32 ++++++++++++++++++++------------

$ git show 848737d --stat | grep PaintingPuzzleView.kt
 AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/PaintingPuzzleView.kt | 4 ++--
```

All commits exist, all file modifications confirmed, user approved visual verification.

---
*Phase: 01-puzzle-assembly-fix*
*Completed: 2026-02-12*
