# Roadmap: Museum Puzzle Piece Display Fix

## Overview

Single-phase focused fix to replace 9 old puzzle piece PNG assets with 14 new gray gradient pieces and update the placement logic in PaintingPuzzleView.kt so locked pieces display as a unified, fully-assembled jigsaw with proper tab/hole interlocking.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Puzzle Assembly Fix** - Replace assets and fix placement logic for proper interlocking (completed 2026-02-12)

## Phase Details

### Phase 1: Puzzle Assembly Fix
**Goal**: Locked puzzle pieces display as one unified gray jigsaw with proper tab/hole interlocking
**Depends on**: Nothing (first phase)
**Requirements**: ASSET-01, ASSET-02, PLAC-01, PLAC-02, PLAC-03, REND-01, REND-02, REND-03
**Success Criteria** (what must be TRUE):
  1. All 14 new gray gradient PNG assets exist in drawable-nodpi/ with correct naming
  2. Old 9 puzzle piece PNGs are removed from drawable-nodpi/
  3. Corner pieces appear at exactly the four grid corners (0,0), (0,9), (9,0), (9,9)
  4. Border and interior pieces alternate correctly using (row+col) % 2 pattern
  5. When all pieces are locked, the visual result is one cohesive gray puzzle with tabs meeting holes
**Plans**: 2 plans

Plans:
- [x] 01-01-PLAN.md -- Copy 14 new gray gradient PNGs to drawable-nodpi/ and remove 5 old no-variant PNGs
- [x] 01-02-PLAN.md -- Update pieceBitmaps map (14 entries) and getPieceType() with (row+col) % 2 variant alternation

## Progress

**Execution Order:**
Phases execute in numeric order.

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Puzzle Assembly Fix | 2/2 | Complete | 2026-02-12 |
