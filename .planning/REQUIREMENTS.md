# Requirements: Museum Puzzle Shape & Transparency Fix

**Defined:** 2026-02-12
**Core Value:** Studying flashcards progressively reveals a masterpiece — the puzzle is a daily visual reward that connects effort to beauty.

## v1.1 Requirements

Requirements for fixing unlocked piece shapes and adding locked piece transparency.

### Shape Fix

- [ ] **SHPE-01**: Unlocked pieces clip the painting to the same jigsaw shape as their locked PNG counterpart
- [ ] **SHPE-02**: Unlocked piece borders match locked piece outlines (no visible shape mismatch)

### Transparency

- [ ] **TRANS-01**: Locked pieces render at 80% opacity so the painting is visible underneath
- [ ] **TRANS-02**: Unlocked pieces remain at full opacity (existing behavior unchanged)

## Future Requirements

### Daily Reveal (v1.2)

- **UNLK-01**: User unlocks one random puzzle piece per card review
- **UNLK-02**: Random piece selection is fair (each locked piece has equal probability)
- **UNLK-03**: Unlocked pieces persist across app restarts within the same day
- **UNLK-04**: Museum homescreen reflects today's review count when user visits
- **RSET-01**: At midnight (local timezone), incomplete puzzles reset to 0%
- **RSET-02**: Reset does not interrupt active sessions
- **CELB-01**: Existing puzzle break cinematic plays when user unlocks all 100 pieces
- **CELB-02**: Celebration triggers exactly once per completion
- **COLL-01**: Completed painting permanently saved to gallery
- **COLL-02**: Collected paintings remain visible forever
- **COLL-03**: After completion, user offered to choose a new painting

### UX Polish (v1.2+)

- **UXP-01**: Unlock sound effect on piece reveal
- **UXP-02**: Daily completion badge indicator
- **UXP-03**: Progress counter showing pieces unlocked / total

## Out of Scope

| Feature | Reason |
|---------|--------|
| Streak counter | Creates loss aversion anxiety; conflicts with Anki's flexible study philosophy |
| Adjustable opacity slider | Settings creep; 80% is user-tested |
| Multiple simultaneous puzzles | Adds complexity; validate single puzzle first |
| Difficulty modes (5x5, 20x20) | 10x10 grid already designed; test before expanding |
| Social sharing of completed puzzles | Privacy concerns in Anki context |
| Card review-driven unlocking | Deferred to v1.2 |
| Daily midnight reset | Deferred to v1.2 |
| Completion celebration & collection | Deferred to v1.2 |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| SHPE-01 | TBD | Pending |
| SHPE-02 | TBD | Pending |
| TRANS-01 | TBD | Pending |
| TRANS-02 | TBD | Pending |

**Coverage:**
- v1.1 requirements: 4 total
- Mapped to phases: 0
- Unmapped: 4 (pending roadmap)

---
*Requirements defined: 2026-02-12*
*Last updated: 2026-02-12 after v1.1 redefinition*
