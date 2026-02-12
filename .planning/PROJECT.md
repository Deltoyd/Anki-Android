# Museum Puzzle & Daily Reveal

## What This Is

The AnkiDroid Museum homescreen features a 10x10 jigsaw puzzle overlay on masterpiece paintings. Locked pieces are gray gradient PNGs with proper interlocking. As users review flashcards, puzzle pieces unlock to reveal the painting underneath. The visual goal is a seamless jigsaw where locked and unlocked pieces share the exact same shape.

## Core Value

Studying flashcards progressively reveals a masterpiece — the puzzle is a daily visual reward that connects effort to beauty.

## Current Milestone: v1.1 Puzzle Shape & Transparency Fix

**Goal:** Fix unlocked piece shapes to match locked PNG shapes, and make locked pieces semi-transparent so the painting teases through.

**Target features:**
- Unlocked pieces clip to the same jigsaw shape as their locked PNG counterpart
- Semi-transparent locked pieces (80% opacity) showing painting underneath

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

- [ ] Unlocked pieces use the same jigsaw shape as their locked PNG counterpart
- [ ] Locked pieces rendered at 80% opacity so painting is visible underneath

### Validated (v1.0)

- ✓ Replace 9 old PNG assets with 14 new gray gradient puzzle pieces — v1.0
- ✓ Fix piece placement logic so tabs always interlock with adjacent holes — v1.0
- ✓ Corners placed at exactly the 4 grid corners — v1.0
- ✓ Border variants alternate along edges so adjacent pieces interlock — v1.0
- ✓ Interior variants alternate in checkerboard pattern so all neighbors interlock — v1.0
- ✓ Visual result: one unified gray puzzle, not a disordered grid — v1.0 (user verified)

### Out of Scope

- Changing the puzzle grid dimensions (stays 10x10) — not requested
- Changing the Museum layout, gallery, or animation systems — not requested
- Card review-driven unlocking — deferred to v1.2
- Daily midnight reset — deferred to v1.2
- Completion celebration & collection — deferred to v1.2

## Context

**Current state (post v1.0):** `PaintingPuzzleView.kt` uses 14 gray gradient PNG assets with a checkerboard variant system. `getPieceType()` maps grid positions using `(row+col) % 2` alternation (with inverted formula for left/right borders). `drawLockedPiecePng()` uses per-piece body offset calculations (`PIECE_BODY_OFFSETS` map) to position each jigsaw piece so tabs extend into neighboring cells and holes receive neighbors' tabs. The locked puzzle displays as one cohesive, fully-assembled gray jigsaw.

**Known issue:** Unlocked pieces use `PuzzlePiecePathGenerator` to clip the painting, but that generator produces a different jigsaw shape than the 14 PNG assets. This causes a visible mismatch — unlocked pieces look like they belong to a different puzzle. v1.1 fixes this by deriving the clip path from the actual PNG shape.

**Shipped v1.0** on 2026-02-12: 1 phase, 2 plans, 23 files changed, +1,383/-242 lines.

**Key files:**
- `AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/PaintingPuzzleView.kt` — piece rendering and placement
- `AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/PuzzlePiecePathGenerator.kt` — jigsaw clip paths for unlocked pieces (currently mismatched)
- `AnkiDroid/src/main/res/drawable-nodpi/puzzle_*.png` — 14 gray gradient PNG assets

## Constraints

- **Assets**: User-provided PNGs must be used as-is (no procedural generation for locked pieces)
- **Compatibility**: Must not break unlocked piece rendering or animations
- **Module**: Changes confined to AnkiDroid module only

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Use 14 PNGs with 2 variants per category | Allows proper tab/hole interlocking between neighbors | ✓ Good |
| Checkerboard alternation via (row+col) % 2 | Standard jigsaw pattern ensures adjacent pieces always complement | ✓ Good |
| Keep PuzzlePiecePathGenerator unchanged | Was locked-only scope for v1.0; unlocked shape mismatch now being fixed in v1.1 | ⚠️ Revisit |
| Inverted variant formula for left/right borders | Left/right PNG variants have swapped tab orientations vs top/bottom | ✓ Good |
| Per-piece body offset rendering | Each PNG has different body-to-tab ratios; uniform scaling caused gaps | ✓ Good |

| 80% opacity for locked piece transparency | User tested 50%, preferred more opaque. 80% keeps painting visible but gray dominant | — Pending |

---
*Last updated: 2026-02-12 after v1.1 milestone redefined*
