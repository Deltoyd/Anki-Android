# Project Research Summary

**Project:** Daily Puzzle Reveal Features for AnkiDroid Museum
**Domain:** Android gamified learning app with progressive unlock mechanics
**Researched:** 2026-02-12
**Confidence:** HIGH

## Executive Summary

The daily puzzle reveal features represent a **pure implementation milestone** requiring zero new dependencies. All necessary capabilities exist in AnkiDroid's current stack: Android's Canvas Graphics API for semi-transparent rendering, SharedPreferences for daily persistence, and the existing PuzzleBreakAnimationView for celebration cinematics. This is not a "build something new" project—it's a "wire existing pieces together" project.

The recommended approach follows established AnkiDroid patterns: ChangeManager for observing review completions, SharedPreferences with date-based reset logic (mirroring MuseumPersistence.kt's streak system), and StateFlow/SharedFlow for reactive UI updates. The architecture introduces one new component (DailyReviewTracker service) that acts as a bridge between the reviewer and museum systems via the existing ChangeManager observer pattern.

The primary risks are performance-related (alpha overdraw with 100 semi-transparent pieces), timing-related (timezone-aware daily reset, race conditions between animations), and state management (deterministic piece unlock randomization). All risks have proven mitigation strategies from production AnkiDroid code: hardware layers for rendering optimization, java.time APIs for timezone handling, and explicit state storage to avoid recalculation bugs.

## Key Findings

### Recommended Stack

**No new dependencies required.** All features leverage existing AnkiDroid infrastructure with zero library additions or version changes.

**Core technologies:**
- **Android Canvas Graphics API (API 24+)**: Semi-transparent PNG rendering via Paint.alpha property—already used in PuzzleBreakAnimationView.kt for piece animations
- **SharedPreferences (API 24+)**: Daily review count and reset persistence—existing pattern in MuseumPersistence.kt for streak tracking proves this scales
- **ValueAnimator (API 24+)**: Triggering completion cinematic—PuzzleBreakAnimationView already integrated, just needs event trigger
- **Kotlin Coroutines (1.10.2)**: Async persistence and event flow—ubiquitous throughout AnkiDroid, launchCatchingIO pattern established

**Key insight from stack research:** The existing codebase already demonstrates every capability needed. PaintingPuzzleView.kt shows Paint.alpha usage (line 406), MuseumPersistence.kt shows daily date-based reset logic (line 101-122), and MuseumViewModel shows StateFlow patterns. This is assembly, not invention.

### Expected Features

**Must have (table stakes):**
- **Daily midnight reset (timezone-aware)**: Core habit-forming mechanic—users expect consistency with other daily puzzle apps (Chess.com, LinkedIn Games)
- **Fair randomization (Fisher-Yates)**: Prevents clustered/predictable unlocks—table stakes for perceived fairness
- **Persistent unlock state**: State must survive app restarts within same day—fundamental expectation
- **1:1 action-to-reward ratio**: 1 review = 1 piece unlock for immediate gratification—proven in gamified learning research

**Should have (competitive differentiators):**
- **Semi-transparent locked pieces (50% opacity)**: Unlike typical hidden puzzles, painting always partially visible—maintains aesthetic while showing progress
- **Completion cinematic reuse**: Existing puzzle break animation becomes celebration—zero implementation cost, maintains visual consistency
- **Streak-free pressure model**: No penalties for missed days—differentiates from Duolingo's anxiety-inducing streaks, aligns with Anki's self-paced philosophy
- **Review-driven unlock**: Pieces unlock through study action, not time-gating—respects user agency

**Defer (v2+):**
- **Analytics on unlock patterns**: Track completion rates, pieces-per-day—wait for 100+ users with 7+ days usage
- **Unlock sound effects**: Subtle audio feedback—add only if users report "progress is unclear"
- **Multiple simultaneous puzzles**: Parallel painting unlocks—adds complexity, validate single puzzle first
- **Social sharing**: Share completed puzzles—privacy concerns in Anki context, focus on personal motivation first

### Architecture Approach

The architecture introduces **one new service component (DailyReviewTracker)** while modifying existing components minimally. DailyReviewTracker acts as a bridge between AnkiDroid's Reviewer system and Museum system, subscribing to ChangeManager notifications and emitting unlock events via SharedFlow.

**Major components:**
1. **DailyReviewTracker (NEW)** — Subscribes to ChangeManager for review events, tracks daily count in SharedPreferences, emits random piece unlock events via SharedFlow, handles midnight reset detection
2. **PaintingPuzzleView (MODIFIED)** — Adds 50% alpha to locked piece rendering via Paint.alpha in drawLockedPiecePng() method, no changes to unlock animation or event handling
3. **MuseumViewModel (MODIFIED)** — Observes DailyReviewTracker.unlockEvents SharedFlow, adds dailyReviewCount to MuseumUiState, calls existing unlockPiece() method when events received
4. **MuseumPersistence (EXTENDED)** — Adds getDailyReviewCount(), incrementDailyReviewCount(), resetDailyReviewCount() using existing date-based pattern from updateStreak()

**Data flow:** Review completion → OpChanges(card=true) → ChangeManager → DailyReviewTracker.opExecuted() → unlockEvents.emit(randomIndex) → MuseumViewModel observes → unlockPiece(index) → PaintingPuzzleView.animateUnlock(). This follows AnkiDroid's established ChangeManager observer pattern used throughout the codebase.

### Critical Pitfalls

1. **Alpha Overdraw Performance Degradation** — Rendering 100 semi-transparent pieces per frame causes GPU overdraw issues (4x+ overdraw = red zones). **Prevention:** Batch transparency by rendering all locked pieces to offscreen Canvas at full opacity, then draw single bitmap with 50% alpha (reduces 100x overdraw to 2x). Test with GPU Overdraw debugging on Pixel 4a, verify ≤2x overdraw and 60fps maintained.

2. **Timezone Handling for Daily Reset** — UTC midnight gives users in New Zealand puzzle reset at 1:00 PM local time while California users reset at 4:00 PM previous day. DST transitions shift reset time by 1 hour. **Prevention:** Use java.time APIs (LocalDate, ZoneId) for user timezone comparison, not UTC timestamps. Store puzzle_timezone preference, calculate midnight in user's timezone. Test across UTC-10, UTC+0, UTC+12 and DST transition dates.

3. **Race Condition Between Review Count Update and Animation** — Rapid reviews trigger multiple unlock requests while previous animation running, causing ValueAnimator cancellation and jarring visual jumps. **Prevention:** Queue unlock requests in ViewModel via MutableSharedFlow with extraBufferCapacity, convert animateUnlock() to suspend function with AnimatorListener callbacks, or debounce StateFlow updates with 300ms delay. Test 20 rapid reviews in 10 seconds.

4. **Piece Unlock Randomness Not Stable Across Restarts** — Using Random().shuffle() without seed produces different unlocked pieces every app launch with same review count. **Prevention:** Store explicit Set<Int> of unlocked piece indices in SharedPreferences (CSV format "3,7,12,15"), don't recalculate from review count. If recalculation needed, use deterministic seed (userId + dateString).hashCode().

5. **Daily Reset Mid-Session Causes Data Loss** — User reviews at 11:55 PM, continues past midnight, daily reset clears progress during active session. **Prevention:** Defer reset until app comes to foreground from background (not during active session), add 2-3 hour grace period past midnight, or separate sessionReviews from persistedReviews. Show UI warning before reset if user active.

6. **Completion Cinematic Timing Race Condition** — ViewModel detects 100 pieces unlocked and emits celebration event before 100th piece animation completes, cinematic plays while piece still fading. **Prevention:** Use SharedFlow (not StateFlow) for one-time celebration event, delay emission by animationDuration + 100ms, or coordinate via suspend animateUnlock() that signals completion. Add "hasShownCelebration" flag to prevent rotation replays.

## Implications for Roadmap

Based on research, suggested 4-phase structure that follows dependency order and isolates risks:

### Phase 1: Semi-Transparent Rendering Foundation
**Rationale:** Visual-only change with zero behavioral dependencies. Establishes rendering approach before wiring to review system. Allows early GPU performance validation on low-end devices.

**Delivers:**
- PaintingPuzzleView.drawLockedPiecePng() modified to apply 50% alpha via Paint.alpha = 127
- Visual confirmation painting shows through locked pieces
- GPU performance baseline established (overdraw ≤2x, 60fps on Pixel 4a)

**Addresses:**
- Semi-transparent locked pieces (50% opacity) feature
- Mitigates Alpha Overdraw Performance pitfall early via GPU profiling

**Avoids:**
- Pitfall #1 (overdraw) caught before integration with review system
- Rendering bugs isolated from review tracking complexity

**Research needed:** NO—Paint.alpha is proven in existing code (PuzzleBreakAnimationView line 406), implementation pattern clear

---

### Phase 2: Daily Persistence & Review Tracking
**Rationale:** Data layer comes second. Enables testing daily reset logic independently of UI. Review count tracking needed before unlock triggers can work.

**Delivers:**
- MuseumPersistence extended with dailyReviewCount methods
- DailyReviewTracker service created with ChangeManager subscription
- MuseumViewModel updated with dailyReviewCount in state
- Unit tests for daily rollover, review count persistence
- Integration test: review answer → count increment → StateFlow update

**Uses:**
- SharedPreferences pattern from MuseumPersistence.updateStreak() (line 101-122)
- ChangeManager observer pattern from ReviewerViewModel
- StateFlow/SharedFlow from all ViewModels

**Implements:**
- Daily review count tracking feature
- Review-based unlock hookpoint (1 review = 1 piece)

**Avoids:**
- Pitfall #3 (review count race conditions) via unlock queue pattern
- Pitfall #4 (randomness instability) via explicit piece storage

**Research needed:** MAYBE—if ChangeManager subscription lifecycle unclear during implementation, may need `/gsd:research-phase` on "ChangeManager memory leak prevention patterns"

---

### Phase 3: Daily Reset Logic
**Rationale:** Reset mechanism depends on review tracking being stable. Timezone complexity isolated to single phase. Must come before completion celebration (which assumes 100-piece logic works).

**Delivers:**
- Timezone-aware midnight reset using java.time APIs
- Date comparison logic in DailyReviewTracker.checkAndResetDaily()
- Grace period handling (reset deferred until app foreground)
- Tests across UTC-10, UTC+0, UTC+12 timezones
- DST transition tests (March/November dates)

**Uses:**
- java.time.LocalDate, ZoneId, Instant for timezone calculations
- Existing date pattern from MuseumPersistence (SimpleDateFormat line 38)

**Implements:**
- Daily midnight reset (timezone-aware) feature
- Daily reset persistence feature

**Avoids:**
- Pitfall #2 (timezone bugs) via LocalDate comparison, not UTC timestamps
- Pitfall #5 (mid-session reset) via foreground-only reset trigger

**Research needed:** NO—timezone patterns well-documented in Android Time documentation, existing code shows SimpleDateFormat usage

---

### Phase 4: Completion Celebration Trigger
**Rationale:** Final integration piece. Depends on all 100 pieces unlocking reliably (Phase 2) and reset not interfering (Phase 3). Reuses existing cinematic, minimal new code.

**Delivers:**
- MuseumViewModel.unlockPiece() checks for completion (size == 100)
- SharedFlow emission of PuzzleCompleted event
- MuseumActivity observes event and triggers existing showCompletionDialog()
- Integration test: unlock 100th piece → cinematic plays exactly once
- Rotation test: celebration doesn't replay

**Uses:**
- Existing PuzzleBreakAnimationView.startAnimation() (line 148-165)
- Existing MuseumEvent.PuzzleCompleted pattern (MuseumViewModel line 198-200)
- SharedFlow for one-time events (prevents rotation replays)

**Implements:**
- Completion celebration (reuse existing cinematic) feature

**Avoids:**
- Pitfall #6 (celebration timing) via animation completion coordination
- StateFlow replay bug via SharedFlow with replay = 0

**Research needed:** NO—cinematic already implemented, just needs trigger condition. Pattern proven in existing event handling.

---

### Phase Ordering Rationale

**Why this order:**
1. **Rendering first (Phase 1)** because it's isolated—no dependencies on review system, can validate GPU performance before integration complexity
2. **Persistence before reset (Phase 2 → 3)** because reset logic needs daily count tracking to exist first, can't reset something that doesn't exist
3. **Celebration last (Phase 4)** because it depends on unlock system working reliably through all 100 pieces and reset not corrupting state

**Why this grouping:**
- **Phase 1 standalone** isolates rendering risk from behavioral logic
- **Phase 2+3 together** could work (both SharedPreferences work) but split allows isolating timezone complexity
- **Phase 4 minimal** because celebration is trivial IF phases 1-3 stable

**How this avoids pitfalls:**
- GPU overdraw caught in Phase 1 via profiling before review integration
- Review race conditions solved in Phase 2 via queue pattern before reset adds complexity
- Timezone bugs contained in Phase 3 with comprehensive test matrix
- Celebration timing solved in Phase 4 after animation system proven stable

### Research Flags

**Phases likely needing deeper research during planning:**
- **Phase 2 (Review Tracking)** — May need research if ChangeManager subscription lifecycle unclear or WeakReference cleanup behavior uncertain. Topic: "ChangeManager memory leak prevention in long-running services"

**Phases with standard patterns (skip research-phase):**
- **Phase 1 (Rendering)** — Paint.alpha is standard Android, existing code shows pattern (PuzzleBreakAnimationView line 406)
- **Phase 3 (Daily Reset)** — java.time APIs well-documented, existing SimpleDateFormat usage shows pattern
- **Phase 4 (Celebration)** — Reuses existing animation, event pattern proven in codebase

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | All capabilities verified in production AnkiDroid code, zero new dependencies. Paint.alpha (line 406), SharedPreferences (line 24-36), ValueAnimator (line 249-283) proven. |
| Features | MEDIUM | Table stakes validated via competitor analysis (Chess.com, Duolingo, LinkedIn Games). Differentiators logical but untested with users. MVP definition clear. |
| Architecture | HIGH | Follows established AnkiDroid patterns: ChangeManager observers (ReviewerViewModel), StateFlow (all ViewModels), SharedPreferences (MuseumPersistence). New component (DailyReviewTracker) mimics existing service patterns. |
| Pitfalls | HIGH | Six critical pitfalls identified from Android performance docs, timezone handling research, and animation lifecycle analysis. All have proven prevention strategies. Phase-to-pitfall mapping complete. |

**Overall confidence:** HIGH

### Gaps to Address

**Gap: Review completion event observation timing**
- **Issue:** ChangeManager.opExecuted() may fire before or after UI updates depending on thread scheduling
- **Handle:** Phase 2 testing must verify unlock events arrive in correct order relative to review count updates. Add logging to measure latency between OpChanges and StateFlow updates.

**Gap: GPU performance on 2020 low-end devices**
- **Issue:** Research shows overdraw theory but hasn't tested actual 100-piece semi-transparent rendering performance
- **Handle:** Phase 1 must profile on Pixel 4a or equivalent (2020 mid-range with limited GPU fill rate). If overdraw >2x or fps <60, implement offscreen batching before proceeding to Phase 2.

**Gap: User preference for daily reset time**
- **Issue:** Research assumes midnight reset but some users may prefer different time (e.g., 2 AM after late-night study sessions)
- **Handle:** Phase 3 can implement fixed midnight initially. Defer configurable reset time to v1.1 if users request it. Document "reset at midnight" in UI/docs to set expectations.

**Gap: Unlock animation queue capacity limits**
- **Issue:** MutableSharedFlow extraBufferCapacity sizing unknown—how many rapid unlocks can queue before drops?
- **Handle:** Phase 2 testing should stress-test with 50+ rapid reviews. Set extraBufferCapacity = 100 initially (supports unlocking entire puzzle in burst), monitor for "SharedFlow buffer overflow" logs.

## Sources

### Primary (HIGH confidence)
- **AnkiDroid codebase** (PaintingPuzzleView.kt, MuseumPersistence.kt, MuseumViewModel.kt, PuzzleBreakAnimationView.kt, CLAUDE.md) — Production patterns for Paint.alpha, SharedPreferences, ChangeManager, StateFlow, daily reset logic
- **Android Developers Official Docs** — Paint.setAlpha, SharedPreferences, Property Animation, Time APIs, GPU Overdraw debugging
- **Android Performance Guides** — Reduce overdraw, Optimize custom views, Profile GPU Rendering

### Secondary (MEDIUM confidence)
- **Duolingo gamification research** — Streak engagement statistics (3.6x completion rate with 7-day streak), habit-forming design patterns
- **Daily puzzle app patterns** — Chess.com streaks, LinkedIn Games, Flow Free daily reset mechanics, timezone handling approaches
- **Progressive disclosure UX research** — 50% opacity standards for overlays, semi-transparent puzzle piece design patterns
- **Android time handling guides** — Timezone best practices, DST transition handling, java.time migration from Calendar

### Tertiary (LOW confidence)
- **Fisher-Yates shuffle algorithm** — Fair randomization theory, needs validation with stored unlock indices approach
- **Confetti/celebration patterns** — General mobile game celebration research, not directly applicable (using existing cinematic instead)

---
*Research completed: 2026-02-12*
*Ready for roadmap: yes*
