# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-02-13)

**Core value:** Studying flashcards progressively reveals a masterpiece — the puzzle is a daily visual reward that connects effort to beauty.
**Current focus:** v1.2 Streak & Gallery Redesign - Phase 4 (Gallery Redesign)

## Current Position

Phase: 5 of 7 (Study Tracking)
Plan: 2 of 2 complete
Status: Phase complete
Last activity: 2026-02-13 — Completed 05-02-PLAN.md

Progress: [█████░░░░░] 71% (5/7 phases in progress)

## Performance Metrics

**By Milestone:**

| Milestone | Phases | Plans | Status |
|-----------|--------|-------|--------|
| v1.0 Museum Puzzle Piece Display Fix | 1 | 2 | Shipped 2026-02-12 |
| v1.1 Puzzle Shape & Transparency Fix | 2 | 2 | Shipped 2026-02-13 |
| v1.2 Streak & Gallery Redesign | 4 | TBD | In Progress |

**v1.2 Phase Structure:**

| Phase | Requirements | Plans | Duration | Tasks | Files |
|-------|--------------|-------|----------|-------|-------|
| 4. Gallery Redesign | 6 (GALR) | 2/2 complete | 22m 19s | 5 | 7 |
| 5. Study Tracking | 1 (STRK-05) | 2/2 complete | 19m 55s | 2 | 4 |
| 6. Streak Widget | 4 (STRK) | TBD | - | - | - |
| 7. Heatmap Views | 10 (WEEK, MNTH, YEAR) | TBD | - | - | - |

## Accumulated Context

### Decisions

See PROJECT.md Key Decisions table for full list.

Recent decisions affecting v1.2:
- Streak bottom sheet replaces homescreen heatmap (frees space for masterpiece)
- Any card reviewed = studied day (low friction daily habit)
- #d36b52 color family for study intensity (user-specified warm tone)
- Removed heatmap from homescreen to maximize masterpiece visibility (04-01)
- Removed deck stats row to reduce visual clutter (04-01)
- Used layout_weight=1 for gallery to fill remaining vertical space (04-01)
- Scale-based blur technique for locked paintings (cross-API compatibility) (04-02)
- Removed lock icon overlay - blur alone communicates locked state (04-02)
- Gallery always starts on active painting (removed position persistence) (04-02)
- Filter completed paintings at ViewModel level (single source of truth) (04-02)
- [Phase 05-01]: Two-pass streak algorithm (backward to find extent, forward to accumulate grace)
- [Phase 05-02]: Removed suspend from queryRevlog (withCol lambda is not suspend context)

### Pending Todos

None.

### Blockers/Concerns

None.

## Session Continuity

Last session: 2026-02-13
Stopped at: Completed 05-02-PLAN.md (ViewModel Integration)
Resume: Phase 5 complete (2/2 plans). Ready for Phase 6 (Streak Widget).

---
*Next: Plan and execute Phase 5 (Study Tracking)*
