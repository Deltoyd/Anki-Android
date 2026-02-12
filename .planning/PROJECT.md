# Museum Puzzle Piece Display Fix

## What This Is

A UI fix for the AnkiDroid Museum homescreen's locked puzzle display. The 10x10 jigsaw puzzle grid currently shows pieces in wrong positions — tabs don't meet holes, creating a disordered appearance. This replaces the existing 9 PNG assets with 14 new gray gradient puzzle pieces and fixes the placement logic so locked pieces display as a clean, fully-assembled gray puzzle.

## Core Value

When all pieces are locked, the puzzle must look like a single cohesive, fully-assembled jigsaw in gray — orderly and waiting to be unlocked by the user through study.

## Requirements

### Validated

- ✓ Museum homescreen with 10x10 puzzle grid — existing
- ✓ Locked pieces rendered as gray PNG overlays — existing
- ✓ Unlocked pieces reveal painting through clipped jigsaw paths — existing
- ✓ Piece unlock animation with fade-in — existing
- ✓ Peek mode to temporarily show full painting — existing
- ✓ Gallery pager with multiple art pieces — existing
- ✓ Puzzle break cinematic animation — existing

### Active

(None — milestone complete)

### Validated (v1.0)

- ✓ Replace 9 old PNG assets with 14 new gray gradient puzzle pieces — v1.0
- ✓ Fix piece placement logic so tabs always interlock with adjacent holes — v1.0
- ✓ Corners placed at exactly the 4 grid corners — v1.0
- ✓ Border variants alternate along edges so adjacent pieces interlock — v1.0
- ✓ Interior variants alternate in checkerboard pattern so all neighbors interlock — v1.0
- ✓ Visual result: one unified gray puzzle, not a disordered grid — v1.0 (user verified)

### Out of Scope

- Changing unlocked piece shapes (PuzzlePiecePathGenerator stays as-is) — user confirmed locked-only
- Changing the puzzle grid dimensions (stays 10x10) — not requested
- Changing the Museum layout, gallery, or animation systems — not requested
- Changing the unlock/reward mechanics — not requested

## Context

**Current state (post v1.0):** `PaintingPuzzleView.kt` uses 14 gray gradient PNG assets with a checkerboard variant system. `getPieceType()` maps grid positions using `(row+col) % 2` alternation (with inverted formula for left/right borders). `drawLockedPiecePng()` uses per-piece body offset calculations (`PIECE_BODY_OFFSETS` map) to position each jigsaw piece so tabs extend into neighboring cells and holes receive neighbors' tabs. The locked puzzle displays as one cohesive, fully-assembled gray jigsaw.

**Shipped v1.0** on 2026-02-12: 1 phase, 2 plans, 23 files changed, +1,383/-242 lines.

**Key files:**
- `AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/PaintingPuzzleView.kt` — piece rendering and placement
- `AnkiDroid/src/main/res/drawable-nodpi/puzzle_*.png` — current 9 PNG assets to replace

## Constraints

- **Assets**: User-provided PNGs must be used as-is (no procedural generation for locked pieces)
- **Compatibility**: Must not break unlocked piece rendering or animations
- **Module**: Changes confined to AnkiDroid module only

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Use 14 PNGs with 2 variants per category | Allows proper tab/hole interlocking between neighbors | ✓ Good |
| Checkerboard alternation via (row+col) % 2 | Standard jigsaw pattern ensures adjacent pieces always complement | ✓ Good |
| Keep PuzzlePiecePathGenerator unchanged | Unlocked pieces work fine; only locked display needs fixing | ✓ Good |
| Inverted variant formula for left/right borders | Left/right PNG variants have swapped tab orientations vs top/bottom | ✓ Good |
| Per-piece body offset rendering | Each PNG has different body-to-tab ratios; uniform scaling caused gaps | ✓ Good |

---
*Last updated: 2026-02-12 after v1.0 milestone*
