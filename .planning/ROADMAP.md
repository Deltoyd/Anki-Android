# Roadmap: Museum Puzzle & Daily Reveal

## Milestones

- ✅ **v1.0 Museum Puzzle Piece Display Fix** - Phase 1 (shipped 2026-02-12)
- ✅ **v1.1 Puzzle Shape & Transparency Fix** - Phases 2-3 (shipped 2026-02-13)
- ✅ **v1.2 Streak & Gallery Redesign** - Phases 4-7 (shipped 2026-02-13)

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

<details>
<summary>✅ v1.2 Streak & Gallery Redesign (Phases 4-7) - SHIPPED 2026-02-13</summary>

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

#### Phase 6: Streak Widget & Bottom Sheet ✓
**Goal**: User can access streak data via toolbar widget and bottom sheet
**Depends on**: Phase 5 (needs study data)
**Requirements**: STRK-01, STRK-02, STRK-03, STRK-04
**Status**: Complete — 2026-02-13
**Plans**: 1 plan, 1 wave

Plans:
- [x] 06-01-PLAN.md -- Add streak pill to toolbar, create streak bottom sheet with streak count and total study time

#### Phase 7: Heatmap Views ✓
**Goal**: User can view study history across weekly, monthly, and yearly timeframes
**Depends on**: Phase 6 (needs bottom sheet container)
**Requirements**: WEEK-01, WEEK-02, WEEK-03, MNTH-01, MNTH-02, MNTH-03, MNTH-04, YEAR-01, YEAR-02, YEAR-03
**Status**: Complete — 2026-02-13
**Plans**: 1 plan, 1 wave

Plans:
- [x] 07-01-PLAN.md -- Week/Month/Year heatmap views with tabs in streak bottom sheet

**Shipped:** 2026-02-13 (7 plans across 4 phases, 28 files changed)

</details>

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
| 6. Streak Widget | v1.2 | 1/1 | Complete | 2026-02-13 |
| 7. Heatmap Views | v1.2 | 1/1 | Complete | 2026-02-13 |

---
*Last updated: 2026-02-13 — v1.2 SHIPPED*
