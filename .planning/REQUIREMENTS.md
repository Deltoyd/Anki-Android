# Requirements: Museum Daily Puzzle Reveal

**Defined:** 2026-02-12
**Core Value:** Studying flashcards progressively reveals a masterpiece — the puzzle is a daily visual reward that connects effort to beauty.

## v1.1 Requirements

Requirements for daily puzzle reveal mechanics. Each maps to roadmap phases.

### Transparency

- [ ] **TRANS-01**: User sees the painting through locked puzzle pieces at 50% opacity
- [ ] **TRANS-02**: Unlocked pieces render at full opacity (existing behavior unchanged)

### Unlock

- [ ] **UNLK-01**: User unlocks one random puzzle piece per card review
- [ ] **UNLK-02**: Random piece selection is fair (each locked piece has equal probability)
- [ ] **UNLK-03**: Unlocked pieces persist across app restarts within the same day
- [ ] **UNLK-04**: Museum homescreen reflects today's review count when user visits

### Reset

- [ ] **RSET-01**: At midnight (local timezone), incomplete puzzles reset to 0% — all pieces re-lock
- [ ] **RSET-02**: Reset does not interrupt active sessions (grace period or idle detection)

### Collection

- [ ] **COLL-01**: When user completes all 100 pieces, the painting is permanently saved to the gallery
- [ ] **COLL-02**: Collected paintings remain visible in the gallery forever (not affected by reset)
- [ ] **COLL-03**: After completion cinematic, user is offered to choose a new painting for tomorrow

### Celebration

- [ ] **CELB-01**: Existing puzzle break cinematic plays when user unlocks all 100 pieces
- [ ] **CELB-02**: Celebration triggers exactly once per completion (no replay on rotation)

## Future Requirements

### UX Polish

- **UXP-01**: Unlock sound effect on piece reveal
- **UXP-02**: Daily completion badge indicator
- **UXP-03**: Progress counter showing pieces unlocked / total

### Analytics

- **ANLT-01**: Track average pieces-per-day completion rate
- **ANLT-02**: Track time-to-completion distribution

## Out of Scope

| Feature | Reason |
|---------|--------|
| Streak counter | Creates loss aversion anxiety; conflicts with Anki's flexible study philosophy |
| Adjustable opacity slider | Settings creep; 50% is proven effective |
| Multiple simultaneous puzzles | Adds complexity; validate single puzzle first |
| Difficulty modes (5x5, 20x20) | 10x10 grid already designed; test before expanding |
| Social sharing of completed puzzles | Privacy concerns in Anki context |
| Partial piece unlocks (5 reviews = 1 piece) | Violates 1:1 immediate gratification principle |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| TRANS-01 | — | Pending |
| TRANS-02 | — | Pending |
| UNLK-01 | — | Pending |
| UNLK-02 | — | Pending |
| UNLK-03 | — | Pending |
| UNLK-04 | — | Pending |
| RSET-01 | — | Pending |
| RSET-02 | — | Pending |
| COLL-01 | — | Pending |
| COLL-02 | — | Pending |
| COLL-03 | — | Pending |
| CELB-01 | — | Pending |
| CELB-02 | — | Pending |

**Coverage:**
- v1.1 requirements: 13 total
- Mapped to phases: 0
- Unmapped: 13

---
*Requirements defined: 2026-02-12*
*Last updated: 2026-02-12 after initial definition*
