# Requirements: Museum Puzzle & Daily Reveal

**Defined:** 2026-02-13
**Core Value:** Studying flashcards progressively reveals a masterpiece — the puzzle is a daily visual reward that connects effort to beauty.

## v1.2 Requirements

Requirements for Streak & Gallery Redesign. Each maps to roadmap phases.

### Streak

- [ ] **STRK-01**: User sees a pill-shaped streak widget at the top-left of the homescreen showing "Streak" label, day count, and 🔥 icon
- [ ] **STRK-02**: User taps the streak pill to open the streak bottom sheet
- [ ] **STRK-03**: User sees their current streak count (consecutive days studied) in the bottom sheet
- [ ] **STRK-04**: User sees total study time in the bottom sheet
- [ ] **STRK-05**: Reviewing at least 1 card marks a day as studied

### Weekly View

- [ ] **WEEK-01**: User sees 7 rounded day circles (M, T, W, T, F, S, S) in the Week tab
- [ ] **WEEK-02**: Day circles are gray when unstudied, #d36b52 shades when studied (darker = more study)
- [ ] **WEEK-03**: Current day is visually highlighted

### Monthly View

- [ ] **MNTH-01**: User can switch to Month tab showing a calendar grid for the current month
- [ ] **MNTH-02**: Calendar days are rendered as circles colored by study intensity
- [ ] **MNTH-03**: Current day is highlighted with a ring
- [ ] **MNTH-04**: Total study time for the month is displayed

### Yearly View

- [ ] **YEAR-01**: User can switch to Year tab showing a compact dot heatmap
- [ ] **YEAR-02**: Dots colored by study intensity (gray → #d36b52 shades)
- [ ] **YEAR-03**: Total study time for the year is displayed

### Gallery

- [ ] **GALR-01**: Heatmap is removed from the homescreen
- [ ] **GALR-02**: Masterpiece painting area takes the full lower homescreen
- [ ] **GALR-03**: Active painting displays the puzzle overlay (existing behavior)
- [ ] **GALR-04**: Completed paintings display the full revealed artwork
- [ ] **GALR-05**: Locked paintings display as blurred/dimmed
- [ ] **GALR-06**: User can swipe through the gallery to browse all paintings

## Future Requirements

Deferred to v1.3+. Tracked but not in current roadmap.

### Puzzle Interaction

- **PZLI-01**: User unlocks one puzzle piece per card reviewed
- **PZLI-02**: Incomplete puzzles reset at midnight
- **PZLI-03**: Completed puzzles trigger a celebration animation
- **PZLI-04**: Completed paintings added to permanent gallery collection
- **PZLI-05**: Piece unlock plays a sound effect
- **PZLI-06**: User sees a progress counter (pieces unlocked / total)

## Out of Scope

| Feature | Reason |
|---------|--------|
| Changing puzzle grid dimensions | Stays 10x10, not requested |
| Adjustable opacity slider | Settings creep; 80% is user-tested |
| Multiple simultaneous puzzles | Adds complexity; validate single puzzle first |
| Study goals / minimum thresholds | Any card reviewed counts as studied |
| Push notifications for streak | Not requested |
| Streak leaderboards / social | Not requested; personal motivation only |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| — | — | — |

**Coverage:**
- v1.2 requirements: 20 total
- Mapped to phases: 0
- Unmapped: 20 ⚠️

---
*Requirements defined: 2026-02-13*
*Last updated: 2026-02-13 after initial definition*
