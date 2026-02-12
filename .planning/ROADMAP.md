# Roadmap: Museum Puzzle & Daily Reveal

## Milestones

- ✅ **v1.0 Museum Puzzle Piece Display Fix** - Phase 1 (shipped 2026-02-12)
- 🚧 **v1.1 Daily Puzzle Reveal** - Phases 2-5 (in progress)

## Overview

The v1.1 milestone transforms the existing static museum puzzle into a dynamic daily motivation loop. Locked pieces become semi-transparent to show the painting underneath, pieces unlock as users review flashcards throughout the day (1 review = 1 piece), and the puzzle resets at midnight to create a fresh daily challenge. Completing all 100 pieces triggers a celebration cinematic and permanently saves the painting to the gallery.

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

### 🚧 v1.1 Daily Puzzle Reveal (In Progress)

**Milestone Goal:** Make locked puzzle pieces semi-transparent so the painting shows through, and tie piece unlocking to daily card reviews.

#### Phase 2: Semi-Transparent Rendering
**Goal**: Locked puzzle pieces show the painting underneath at 50% opacity
**Depends on**: Phase 1
**Requirements**: TRANS-01, TRANS-02
**Success Criteria** (what must be TRUE):
  1. User sees the masterpiece painting through all locked puzzle pieces
  2. Locked pieces appear at 50% opacity (semi-transparent gray overlay)
  3. Unlocked pieces remain at full opacity with no transparency
  4. GPU performance maintains 60fps on mid-range devices
**Plans**: TBD

Plans:
- [ ] 02-01: TBD

#### Phase 3: Review-Driven Unlock & Persistence
**Goal**: Card reviews unlock random puzzle pieces and persist across app restarts
**Depends on**: Phase 2
**Requirements**: UNLK-01, UNLK-02, UNLK-03, UNLK-04
**Success Criteria** (what must be TRUE):
  1. User unlocks one random puzzle piece per card review
  2. Each locked piece has equal probability of being selected
  3. Unlocked pieces persist when user closes and reopens the app within the same day
  4. Museum homescreen reflects today's review count when user visits
  5. 20 rapid reviews in 10 seconds unlock 20 distinct pieces without visual glitches
**Plans**: TBD

Plans:
- [ ] 03-01: TBD

#### Phase 4: Daily Midnight Reset
**Goal**: Puzzles reset at midnight local time to create daily motivation loop
**Depends on**: Phase 3
**Requirements**: RSET-01, RSET-02
**Success Criteria** (what must be TRUE):
  1. At midnight in the user's local timezone, incomplete puzzles reset to 0% (all pieces re-lock)
  2. Reset occurs when user brings app to foreground after midnight, not during active sessions
  3. Reset handles timezone changes and DST transitions correctly
  4. User who completes puzzle before midnight sees completion persist until midnight
**Plans**: TBD

Plans:
- [ ] 04-01: TBD

#### Phase 5: Completion Celebration & Collection
**Goal**: Completing all 100 pieces triggers cinematic and saves painting permanently
**Depends on**: Phase 4
**Requirements**: CELB-01, CELB-02, COLL-01, COLL-02, COLL-03
**Success Criteria** (what must be TRUE):
  1. Puzzle break cinematic plays when user unlocks the 100th piece
  2. Celebration triggers exactly once per completion (no replay on screen rotation)
  3. Completed painting is permanently saved to the gallery
  4. Collected paintings remain visible in gallery after daily reset
  5. After cinematic, user can choose a new painting for tomorrow's puzzle
**Plans**: TBD

Plans:
- [ ] 05-01: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 2 → 3 → 4 → 5

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. Puzzle Asset & Rendering Foundation | v1.0 | 2/2 | Complete | 2026-02-12 |
| 2. Semi-Transparent Rendering | v1.1 | 0/? | Not started | - |
| 3. Review-Driven Unlock & Persistence | v1.1 | 0/? | Not started | - |
| 4. Daily Midnight Reset | v1.1 | 0/? | Not started | - |
| 5. Completion Celebration & Collection | v1.1 | 0/? | Not started | - |

---
*Last updated: 2026-02-12 — v1.1 roadmap created*
