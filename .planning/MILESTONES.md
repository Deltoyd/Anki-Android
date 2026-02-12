# Milestones

## v1.1 Puzzle Shape & Transparency Fix (Shipped: 2026-02-13)

**Phases completed:** 2 phases, 2 plans, 4 tasks
**Code:** 2 files changed, +72/-266 lines

**Key accomplishments:**
- Replaced bezier-curve path clipping with PNG alpha mask compositing (PorterDuff DST_IN)
- Eliminated PuzzlePiecePathGenerator — 239 lines of dead code removed
- Locked pieces now show painting at 80% opacity (alpha 204 gray overlay)
- All 14 piece variants render correctly for both locked and unlocked states

**Archive:** [v1.1 Roadmap](milestones/v1.1-ROADMAP.md) | [v1.1 Requirements](milestones/v1.1-REQUIREMENTS.md)

---

## v1.0 Museum Puzzle Piece Display Fix (Shipped: 2026-02-12)

**Phases completed:** 1 phase, 2 plans, 4 tasks

**Key accomplishments:**
- Replaced 9 old puzzle piece PNGs with 14 new gray gradient assets with variant support
- Implemented checkerboard variant alternation using (row+col) % 2 for proper tab/hole interlocking
- Built per-piece body offset rendering system for seamless jigsaw alignment
- Achieved core value: locked puzzle displays as one cohesive, fully-assembled gray jigsaw

---
