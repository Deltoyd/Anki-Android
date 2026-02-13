# Phase 5: Study Tracking & Data Layer - Context

**Gathered:** 2026-02-13
**Status:** Ready for planning

<domain>
## Phase Boundary

Data infrastructure that tracks card reviews, calculates streak counts with a grace day system, computes study time per day, and provides intensity data for downstream heatmap views (Phase 7). Derives all data from Anki's existing revlog. No UI in this phase — pure data layer consumed by Phases 6 and 7.

</domain>

<decisions>
## Implementation Decisions

### What counts as study time
- Only card review time counts (not deck browsing, editing, or other app activity)
- Study time per card is capped at 30 seconds — any time beyond that is ignored (prevents idle inflation)
- Answering 1 card marks the day as studied (low friction trigger)
- Data source: Anki's existing revlog database (no separate tracking system)

### Data persistence
- Compute all stats on-the-fly from revlog — no caching layer
- Always accurate, no stale data concerns
- Data durability tied to the Anki collection — if collection exists and syncs via AnkiWeb, streak data exists
- No independent backup or SharedPreferences storage

### Streak rules
- Day boundary: midnight local time (not Anki's configurable "next day starts at" setting)
- Grace day system:
  - Every 3 consecutive study days earns 1 grace day
  - Maximum 5 grace days can be accumulated
  - Missing a day with grace available: 1 grace day consumed, streak continues
  - Missing a day with 0 grace: streak resets to 0
- Grace day count is visible to the user (displayed in streak bottom sheet — Phase 6)
- Grace-used days have a distinct visual state in heatmaps (not same as studied — Phase 7)

### Intensity calculation
- Metric: cards reviewed (not study time)
- Scale: relative to user's personal daily average (adaptive, not fixed thresholds)
- 5 intensity levels (like GitHub contributions)
- Study time displayed in bottom sheet uses capped time (30s/card max), consistent with internal tracking

### Claude's Discretion
- How to query revlog efficiently for on-the-fly computation
- Exact algorithm for computing the adaptive daily average (rolling window, all-time, etc.)
- How to map the 5 intensity levels to card counts relative to the average
- Data structure/interface design for the study tracking API
- Whether to use a ViewModel, repository pattern, or direct query helper

</decisions>

<specifics>
## Specific Ideas

- Grace days are a reward for consistency — "study 3 days, earn a safety net"
- The 30-second cap per card keeps study time honest — someone who falls asleep mid-review shouldn't get credit for hours
- Relative intensity means a casual studier (10 cards/day) and a power user (200 cards/day) both see meaningful color variation
- Grace-used days need a distinct data flag so Phase 7 can render them differently from actual study days

</specifics>

<deferred>
## Deferred Ideas

- None — discussion stayed within phase scope

</deferred>

---

*Phase: 05-study-tracking*
*Context gathered: 2026-02-13*
