# Roadmap: Museum Puzzle & Daily Reveal

## Milestones

- ✅ **v1.0 Museum Puzzle Piece Display Fix** - Phase 1 (shipped 2026-02-12)
- ✅ **v1.1 Puzzle Shape & Transparency Fix** - Phases 2-3 (shipped 2026-02-13)
- 🚧 **v1.2 Streak & Gallery Redesign** - Phases 4-7 (in progress)

## Phases

<details>
<summary>✅ v1.0 Museum Puzzle Piece Display Fix (Phase 1) - SHIPPED 2026-02-12</summary>

- [x] Phase 1: Puzzle Asset & Rendering Foundation (2/2 plans) — completed 2026-02-12

**Shipped:** 2026-02-12 (2 plans, 23 files changed, +1,383/-242 lines)

</details>

<details>
<summary>✅ v1.1 Puzzle Shape & Transparency Fix (Phases 2-3) - SHIPPED 2026-02-13</summary>

- [x] Phase 2: Shape System Fix (1/1 plan) — completed 2026-02-13
- [x] Phase 3: Transparent Locked Pieces (1/1 plan) — completed 2026-02-13

**Shipped:** 2026-02-13 (2 plans, 2 files changed, +72/-266 lines)

</details>

### 🚧 v1.2 Streak & Gallery Redesign (In Progress)

**Milestone Goal:** Replace the homescreen heatmap with a streak bottom sheet and redesign the gallery to give the masterpiece full screen presence.

#### Phase 4: Gallery Redesign ✓
**Goal**: Masterpiece takes full lower homescreen with proper state rendering
**Depends on**: Phase 3 (v1.1)
**Requirements**: GALR-01, GALR-02, GALR-03, GALR-04, GALR-05, GALR-06
**Status**: Complete — 2026-02-13
**Plans**: 2 plans, 2 waves

Plans:
- [x] 04-01-PLAN.md -- Remove heatmap and stats, expand painting area, add title/artist label
- [x] 04-02-PLAN.md -- Filter gallery to active+locked, add locked blur, implement circular wrap-around

**Shipped:** 2026-02-13 (2 plans, 4 tasks, 6 files changed)

#### Phase 5: Study Tracking & Data Layer ✓
**Goal**: System tracks study sessions and calculates streak/intensity data
**Depends on**: Nothing (independent data layer)
**Requirements**: STRK-05
**Status**: Complete — 2026-02-13
**Plans**: 2 plans, 2 waves

Plans:
- [x] 05-01-PLAN.md -- TDD: StudyTrackingRepository with revlog queries, streak/grace calculation, intensity levels
- [x] 05-02-PLAN.md -- Wire StudyTrackingRepository into MuseumViewModel, replace SharedPreferences streak

**Shipped:** 2026-02-13 (2 plans, 2 tasks, 3 files changed)

#### Phase 6: Streak Widget & Bottom Sheet
**Goal**: User can access streak data via toolbar widget and bottom sheet
**Depends on**: Phase 5 (needs study data)
**Requirements**: STRK-01, STRK-02, STRK-03, STRK-04
**Success Criteria** (what must be TRUE):
  1. User sees streak widget at top-left showing "Streak" label, day count, and 🔥 icon
  2. User taps the streak widget to open the streak bottom sheet
  3. User sees their current streak count in the bottom sheet
  4. User sees total study time in the bottom sheet
**Plans**: 1 plan, 1 wave

Plans:
- [ ] 06-01-PLAN.md -- Add streak pill to toolbar, create streak bottom sheet with streak count and total study time

#### Phase 7: Heatmap Views
**Goal**: User can view study history across weekly, monthly, and yearly timeframes
**Depends on**: Phase 6 (needs bottom sheet container)
**Requirements**: WEEK-01, WEEK-02, WEEK-03, MNTH-01, MNTH-02, MNTH-03, MNTH-04, YEAR-01, YEAR-02, YEAR-03
**Success Criteria** (what must be TRUE):
  1. User sees 7 day circles (M-S) in Week tab colored by study intensity (gray unstudied, #d36b52 shades studied)
  2. User sees current day highlighted in Week tab
  3. User sees calendar grid in Month tab with day circles colored by study intensity
  4. User sees current day highlighted with ring in Month tab
  5. User sees total study time for the month displayed
  6. User sees compact dot heatmap in Year tab colored by study intensity
  7. User sees total study time for the year displayed
**Plans**: TBD

Plans:
- [ ] 07-01: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 4 → 5 → 6 → 7

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. Puzzle Asset & Rendering Foundation | v1.0 | 2/2 | Complete | 2026-02-12 |
| 2. Shape System Fix | v1.1 | 1/1 | Complete | 2026-02-13 |
| 3. Transparent Locked Pieces | v1.1 | 1/1 | Complete | 2026-02-13 |
| 4. Gallery Redesign | v1.2 | 2/2 | Complete | 2026-02-13 |
| 5. Study Tracking | v1.2 | 2/2 | Complete | 2026-02-13 |
| 6. Streak Widget | v1.2 | 0/1 | Planned | - |
| 7. Heatmap Views | v1.2 | 0/1 | Not started | - |

---
*Last updated: 2026-02-13 — Phase 6 planned*
