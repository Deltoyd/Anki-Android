# Roadmap: Museum Puzzle & Daily Reveal

## Milestones

- ✅ **v1.0 Museum Puzzle Piece Display Fix** - Phase 1 (shipped 2026-02-12)
- 🚧 **v1.1 Puzzle Shape & Transparency Fix** - Phases 2-3 (in progress)

## Overview

The v1.1 milestone fixes two visual issues from v1.0: unlocked pieces now clip to match their locked PNG shapes (creating a unified jigsaw), and locked pieces become semi-transparent (80% opacity) to show the painting underneath. This completes the core visual puzzle experience.

## Phases

<details>
<summary>✅ v1.0 Museum Puzzle Piece Display Fix (Phase 1) - SHIPPED 2026-02-12</summary>

### Phase 1: Puzzle Asset & Rendering Foundation
**Goal**: Replace old puzzle PNGs and fix piece placement logic for proper interlocking
**Depends on**: Nothing (first phase)
**Plans**: 2 plans

Plans:
- [x] 01-01: Replace 9 old PNG assets with 14 new gray gradient pieces
- [x] 01-02: Implement checkerboard variant system and per-piece body offset rendering

**Shipped:** 2026-02-12 (2 plans, 23 files changed, +1,383/-242 lines)

</details>

### 🚧 v1.1 Puzzle Shape & Transparency Fix (In Progress)

**Milestone Goal:** Fix unlocked piece shapes to match locked PNG shapes, and make locked pieces semi-transparent so the painting teases through.

#### Phase 2: Shape System Fix
**Goal**: Unlocked pieces use the same jigsaw shape as their locked PNG counterpart
**Depends on**: Phase 1
**Requirements**: SHPE-01, SHPE-02
**Success Criteria** (what must be TRUE):
  1. User sees unlocked pieces clip to the exact same jigsaw outline as their locked PNG neighbors
  2. No visible shape mismatch between locked gray puzzle and unlocked painting sections
  3. Tab and hole boundaries align perfectly at piece intersections (within 1px tolerance)
  4. Shape system works correctly for all 14 piece variants (4 corners, 8 borders, 2 interiors)
**Plans**: 1 plan

Plans:
- [x] 02-01: Replace path-based clipping with PNG alpha mask clipping for unlocked pieces

**Shipped:** 2026-02-12 (1 plan, 2 files changed, +59/-266 lines)

#### Phase 3: Transparent Locked Pieces
**Goal**: Locked pieces show the painting underneath at 80% opacity
**Depends on**: Phase 2
**Requirements**: TRANS-01, TRANS-02
**Success Criteria** (what must be TRUE):
  1. User sees the masterpiece painting through all locked puzzle pieces
  2. Locked pieces render at 80% opacity (semi-transparent gray overlay teasing the painting)
  3. Unlocked pieces remain at full opacity (existing behavior unchanged)
**Plans**: 1 plan

Plans:
- [ ] 03-01-PLAN.md -- Add painting-through-locked-pieces rendering with 80% opacity overlay + visual verification

## Progress

**Execution Order:**
Phases execute in numeric order: 2 → 3

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. Puzzle Asset & Rendering Foundation | v1.0 | 2/2 | Complete | 2026-02-12 |
| 2. Shape System Fix | v1.1 | 1/1 | Complete | 2026-02-12 |
| 3. Transparent Locked Pieces | v1.1 | 0/1 | Planned | - |

---
*Last updated: 2026-02-13 — Phase 3 planned*
