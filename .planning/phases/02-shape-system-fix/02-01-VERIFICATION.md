---
phase: 02-shape-system-fix
verified: 2026-02-13T05:46:00Z
status: passed
score: 4/4 must-haves verified
re_verification: null
gaps: []
human_verification: []
---

# Phase 2: Shape System Fix Verification Report

**Phase Goal:** Unlocked pieces use the same jigsaw shape as their locked PNG counterpart
**Verified:** 2026-02-13T05:46:00Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| #   | Truth                                                                                              | Status     | Evidence                                                                                                                             |
| --- | -------------------------------------------------------------------------------------------------- | ---------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| 1   | Unlocked pieces clip the painting to the exact same jigsaw outline as their locked PNG counterpart | ✓ VERIFIED | PNG alpha mask compositing (DST_IN) at lines 433-452; uses pieceBitmaps[pieceType] for both locked and unlocked rendering          |
| 2   | No visible shape mismatch between locked gray puzzle sections and unlocked painting sections       | ✓ VERIFIED | User approved visual checkpoint on Pixel 7a; shared dest rect calculation ensures pixel-perfect alignment (lines 422-431, 481-498) |
| 3   | Tab and hole boundaries align at piece intersections                                               | ✓ VERIFIED | Both locked and unlocked use same PNG assets via pieceBitmaps; same PIECE_BODY_OFFSETS map (lines 41-63)                           |
| 4   | All 14 piece variants render correctly for unlocked pieces                                         | ✓ VERIFIED | getPieceType() logic unchanged from v1.0 (lines 507-539); all 14 keys in pieceBitmaps map (lines 75-92)                            |

**Score:** 4/4 truths verified

### Required Artifacts

| Artifact                                                                | Expected                              | Status     | Details                                                                                                                              |
| ----------------------------------------------------------------------- | ------------------------------------- | ---------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| `AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/PaintingPuzzleView.kt` | PNG alpha mask clipping for unlocked pieces | ✓ VERIFIED | Lines 100-104 define alphaMaskPaint with DST_IN; lines 433-452 implement saveLayer compositing; pattern "extractAlpha\|DST_IN\|alphaMask" found |

**Artifact Status: ✓ VERIFIED**
- Exists: Yes
- Substantive: Yes (86 lines modified, complete compositing implementation)
- Wired: Yes (alphaMaskPaint used at line 450; pieceBitmaps accessed at lines 385, 420, 479)

### Key Link Verification

| From                                              | To                      | Via                                         | Status     | Details                                                                                                         |
| ------------------------------------------------- | ----------------------- | ------------------------------------------- | ---------- | --------------------------------------------------------------------------------------------------------------- |
| PaintingPuzzleView.drawPuzzlePiece (unlocked branch) | pieceBitmaps[pieceType] | PNG alpha used as clip mask for painting    | ✓ WIRED    | Line 420: maskBmp = pieceBitmaps[pieceType]; Line 450: canvas.drawBitmap(maskBmp, null, tmpPieceRect, alphaMaskPaint) |
| PaintingPuzzleView.drawPeekMode                   | pieceBitmaps[pieceType] | PNG alpha used for peek mode outlines       | ✓ WIRED    | Line 385: bmp = pieceBitmaps[pieceType]; Line 398: canvas.drawBitmap(bmp, null, tmpPieceRect, peekPiecePaint)   |

**Wiring Detail:**
- **Unlocked rendering:** Complete compositing pipeline with saveLayer (line 435) → draw painting (lines 444-446) → apply alpha mask with DST_IN (line 450) → restore (line 452)
- **Peek mode:** PNG-based outlines replace old pathGenerator approach (lines 377-400)
- **No pathGenerator references:** grep confirms PuzzlePiecePathGenerator not found in codebase

### Requirements Coverage

| Requirement | Description                                                                          | Status       | Supporting Evidence                                                      |
| ----------- | ------------------------------------------------------------------------------------ | ------------ | ------------------------------------------------------------------------ |
| SHPE-01     | Unlocked pieces clip the painting to the same jigsaw shape as their locked PNG counterpart | ✓ SATISFIED  | Truth 1 verified; PNG alpha mask compositing ensures pixel-identical shapes |
| SHPE-02     | Unlocked piece borders match locked piece outlines (no visible shape mismatch)       | ✓ SATISFIED  | Truth 2 verified; user approved visual checkpoint; shared dest rect calculation |

### Anti-Patterns Found

**None detected.**

Scanned file: `AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/PaintingPuzzleView.kt`

- No TODO/FIXME/PLACEHOLDER comments
- No empty implementations (return null, return {}, etc.)
- No console.log-only implementations
- Complete compositing logic with proper saveLayer → DST → SRC → restore pattern

### Human Verification Required

**None.**

User already completed visual verification on Pixel 7a device and approved the checkpoint. All observable truths can be verified programmatically through code inspection:
- PNG alpha mask compositing implementation is complete and wired
- Same bitmap source (pieceBitmaps) used for both locked and unlocked pieces
- Dest rect calculation shared between locked and unlocked rendering

### Commit Verification

**Commit:** 1d69856 (feat)
**Files Changed:** 2 files, +59/-266 lines
**Modified:** AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/PaintingPuzzleView.kt
**Deleted:** AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/PuzzlePiecePathGenerator.kt (239 lines)

Commit exists in git history and matches SUMMARY.md claims.

### Phase Completion Assessment

**Status: ✓ PASSED**

All must-haves verified:
1. ✓ PNG alpha mask compositing implemented with PorterDuff DST_IN
2. ✓ Unlocked and locked pieces share same bitmap source (pieceBitmaps)
3. ✓ Dual-shape system eliminated (PuzzlePiecePathGenerator deleted)
4. ✓ User visual verification completed and approved
5. ✓ All 14 piece variants supported
6. ✓ Peek mode updated to use PNG-based outlines
7. ✓ No anti-patterns or stubs detected

**Phase Goal Achieved:** Unlocked pieces now use the exact same jigsaw shape as their locked PNG counterparts.

**Ready for Phase 3:** Shape system unified; transparency work can proceed on solid foundation.

---

_Verified: 2026-02-13T05:46:00Z_
_Verifier: Claude (gsd-verifier)_
