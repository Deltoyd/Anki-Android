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

- [ ] Replace 9 old PNG assets with 14 new gray gradient puzzle pieces
- [ ] Fix piece placement logic so tabs always interlock with adjacent holes
- [ ] Corners placed at exactly the 4 grid corners
- [ ] Border variants alternate along edges so adjacent pieces interlock
- [ ] Interior variants alternate in checkerboard pattern so all neighbors interlock
- [ ] Visual result: one unified gray puzzle, not a disordered grid

### Out of Scope

- Changing unlocked piece shapes (PuzzlePiecePathGenerator stays as-is) — user confirmed locked-only
- Changing the puzzle grid dimensions (stays 10x10) — not requested
- Changing the Museum layout, gallery, or animation systems — not requested
- Changing the unlock/reward mechanics — not requested

## Context

**Current state:** `PaintingPuzzleView.kt` uses `getPieceType()` which maps grid positions to 9 piece types (4 corners, 4 borders, 1 middle). No alternation between variants, so the same border/middle PNG is used everywhere regardless of tab orientation. This means tabs face tabs and holes face holes in many positions.

**New assets:** 14 PNG pieces in `/Users/rolandharper/Projects/Ikasi/Puzzle-shapes/New-gray-radient-puzzle/`:
- 4 corners: top-left, top-right, bottom-left, bottom-right
- 2 top border variants: top-middle-1, top-middle-2
- 2 bottom border variants: bottom-middle-1, bottom-middle-2
- 2 left border variants: left-middle-1, left-middle-2
- 2 right border variants: right-middle-1, right-middle-2
- 2 interior variants: only-middle, only-middle-2

Variants alternate using `(row+col) % 2` so that every piece's tabs meet its neighbor's holes.

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
| Use 14 PNGs with 2 variants per category | Allows proper tab/hole interlocking between neighbors | — Pending |
| Checkerboard alternation via (row+col) % 2 | Standard jigsaw pattern ensures adjacent pieces always complement | — Pending |
| Keep PuzzlePiecePathGenerator unchanged | Unlocked pieces work fine; only locked display needs fixing | — Pending |

---
*Last updated: 2026-02-12 after initialization*
