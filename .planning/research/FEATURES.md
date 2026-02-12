# Feature Research

**Domain:** Daily puzzle reveal mechanics for gamified learning apps
**Researched:** 2026-02-12
**Confidence:** MEDIUM

## Feature Landscape

### Table Stakes (Users Expect These)

Features users assume exist. Missing these = product feels incomplete.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Visual feedback on unlock | Every gamified app shows immediate visual confirmation of progress | LOW | Existing fade-in animation already implemented; may need opacity adjustment |
| Daily reset mechanism | Daily puzzle/challenge apps universally reset at midnight to maintain habit loop | MEDIUM | Requires timezone-aware scheduling (WorkManager or AlarmManager) + persistent state tracking |
| Progress persistence | Users expect unlocked state to persist across app restarts until reset | LOW | SharedPreferences or Room database for state storage |
| Fair randomization | Random piece selection must feel fair, not clustered or predictable | MEDIUM | Fisher-Yates shuffle algorithm ensures each piece has equal unlock probability |
| 1:1 action-to-reward ratio | Gamified learning apps use direct 1:1 mapping (1 review = 1 piece) for immediate gratification | LOW | Direct coupling between review completion and piece unlock |

### Differentiators (Competitive Advantage)

Features that set the product apart. Not required, but valuable.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Semi-transparent locked pieces (50% opacity) | Unlike typical locked/hidden puzzles, painting always partially visible - maintains aesthetic while showing progress | MEDIUM | Requires alpha channel rendering on locked gray pieces; existing checkerboard might need opacity blending |
| Completion cinematic reuse | Existing puzzle break animation becomes daily completion celebration - no new animation needed | LOW | Already implemented; just needs triggering on 100-piece unlock |
| Museum painting context | Puzzle isn't arbitrary - it's your study progress revealing art you've chosen | LOW | Conceptual differentiator; implementation already exists in painting selection |
| Streak-free pressure model | No streaks or penalties - just daily fresh start encourages consistency without anxiety | LOW | Intentional absence of feature; simpler than streak systems |
| Review-driven unlock (not time-gated) | Pieces unlock through study action, not waiting - respects user agency | LOW | Core mechanic; aligns with Anki's self-paced philosophy |

### Anti-Features (Commonly Requested, Often Problematic)

Features that seem good but create problems.

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| Streak counter | Duolingo's 60% engagement boost makes streaks tempting | Loss aversion creates anxiety; missed day feels punishing; conflicts with Anki's flexible review philosophy | Daily fresh start - celebrate completion without penalty for missed days |
| Piece unlock hints/previews | Users want to know which piece will unlock next | Removes surprise/delight; complicates randomization; creates UI clutter | Keep randomization pure; celebration is in the reveal moment |
| Partial piece unlocks | Gradual reveal (5 reviews = 1 piece) might feel more granular | Weak action-to-reward feedback; violates 1:1 immediate gratification principle; introduces decimal tracking complexity | Stick to 1 review = 1 piece for clear progress |
| Manual reset option | "Let me start over whenever I want" | Undermines daily habit formation; reduces value of completion; creates edge cases in state management | Daily automatic reset provides structure and rhythm |
| Adjustable opacity slider | "Let me choose locked piece transparency" | Settings creep; most users won't touch it; 50% is proven effective in design patterns | Fixed 50% opacity as designed; avoid premature optimization |

## Feature Dependencies

```
[Daily Reset Mechanism]
    └──requires──> [Persistent State Storage]
                       └──requires──> [Last Reset Timestamp]
    └──requires──> [Unlock State Tracking (100-piece boolean array)]

[Fair Random Unlock]
    └──requires──> [Remaining Pieces Pool]
    └──requires──> [Fisher-Yates Shuffle Algorithm]

[Semi-Transparent Locked Pieces]
    └──requires──> [Existing Gray Piece Rendering]
    └──enhances──> [Painting Visibility]

[Completion Celebration]
    └──requires──> [100-piece Unlock Detection]
    └──reuses──> [Existing Puzzle Break Cinematic]

[Review-Based Unlock]
    └──requires──> [Hook into Review Completion Event]
    └──triggers──> [Fair Random Unlock]
    └──triggers──> [Piece Unlock Animation (existing)]
```

### Dependency Notes

- **Daily Reset requires Persistent State:** Must store unlock state, last reset date, remaining piece pool across app lifecycle
- **Fair Random Unlock requires Remaining Pieces Pool:** Fisher-Yates shuffle works by selecting from shrinking pool of unselected pieces
- **Semi-Transparent Locked Pieces enhances Painting Visibility:** 50% opacity lets painting show through while maintaining puzzle structure
- **Completion Celebration reuses Existing Cinematic:** Puzzle break animation already implemented; just needs trigger condition (all 100 pieces unlocked)
- **Review-Based Unlock triggers Fair Random Unlock:** Each review completion selects one random piece from remaining pool

## MVP Definition

### Launch With (v1)

Minimum viable product - what's needed to validate daily puzzle reveal mechanics.

- [x] **Semi-transparent locked pieces (50% opacity)** - Core visual differentiator; painting always partially visible
- [x] **1 review = 1 random piece unlock** - Core gamification mechanic; immediate gratification loop
- [x] **Fair randomization (Fisher-Yates)** - Table stakes; prevents clustered/unfair unlocks
- [x] **Daily midnight reset (timezone-aware)** - Core habit-forming mechanic; fresh start each day
- [x] **Persistent unlock state** - Table stakes; state survives app restarts within same day
- [x] **Completion celebration (reuse existing cinematic)** - Payoff moment when all 100 pieces revealed

### Add After Validation (v1.x)

Features to add once core mechanics are working and user feedback is collected.

- [ ] **Analytics on unlock patterns** - Track: average pieces/day, completion rate, time-to-completion distribution (trigger: after 100+ users using for 7+ days)
- [ ] **Unlock sound effect** - Subtle audio feedback on piece reveal (trigger: if users report feeling "progress is unclear")
- [ ] **Daily completion badge** - Visual indicator in UI if puzzle completed today (trigger: if users report wanting completion acknowledgment beyond cinematic)
- [ ] **Painting preview on complete** - Full painting view without puzzle overlay as reward (trigger: if users request ability to see full art)

### Future Consideration (v2+)

Features to defer until product-market fit is established.

- [ ] **Multiple simultaneous puzzles** - Unlock multiple paintings in parallel (why defer: adds significant complexity; validate single puzzle engagement first)
- [ ] **Difficulty modes** - 5x5 (25 pieces) or 20x20 (400 pieces) options (why defer: 10x10 grid is already designed; test before expanding)
- [ ] **Social sharing** - Share completed puzzle or progress (why defer: privacy concerns in Anki context; focus on personal motivation first)
- [ ] **Unlock history/calendar** - View which days puzzle was completed (why defer: adds UI complexity; validate core loop first)

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| 50% opacity locked pieces | HIGH | MEDIUM | P1 |
| 1 review = 1 piece unlock | HIGH | LOW | P1 |
| Fair randomization (Fisher-Yates) | HIGH | MEDIUM | P1 |
| Daily midnight reset | HIGH | MEDIUM | P1 |
| Persistent state (SharedPreferences) | HIGH | LOW | P1 |
| Completion cinematic trigger | HIGH | LOW | P1 |
| Unlock sound effect | MEDIUM | LOW | P2 |
| Daily completion badge | MEDIUM | LOW | P2 |
| Analytics tracking | LOW | MEDIUM | P2 |
| Painting preview on complete | MEDIUM | MEDIUM | P2 |
| Multiple simultaneous puzzles | MEDIUM | HIGH | P3 |
| Difficulty modes | LOW | HIGH | P3 |
| Social sharing | LOW | HIGH | P3 |
| Unlock history calendar | LOW | MEDIUM | P3 |

**Priority key:**
- P1: Must have for launch
- P2: Should have, add when possible
- P3: Nice to have, future consideration

## Implementation Pattern Analysis

### Daily Reset Mechanics (Researched from Mobile Game Patterns)

**Standard Approach:**
- Reset occurs at **midnight in user's local timezone**
- Games like LinkedIn Games, Chess.com Daily Puzzles, Flow Free use this pattern
- Android's Digital Wellbeing app timers reset at midnight as reference implementation

**Best Practice for Android:**
- Use **WorkManager** for scheduling daily reset (recommended over AlarmManager for non-critical timing)
- WorkManager handles Doze mode, app restarts, device reboots automatically
- Schedule with `PeriodicWorkRequest` with 24-hour interval and flexibility window
- Store "last reset date" in SharedPreferences to handle edge cases (manual time changes, device off at midnight)

**Edge Cases to Handle:**
- User travels across timezones: Reset based on device's current timezone
- Device off at midnight: Check on app launch if current date > stored last reset date
- Manual time manipulation: Validate against system time; don't punish legitimate timezone changes

### Review-Based Progressive Unlock (Researched from Gamified Learning Apps)

**Duolingo Pattern:**
- 1 lesson = XP points + skill level progress
- Immediate visual feedback (animations, sounds, progress bars)
- **Key insight:** 3.6x more likely to complete course with 7-day streak; 2.3x more likely to engage daily with 7+ day streak

**AnkiDroid Adaptation:**
- 1 card review = 1 piece unlock (simpler than XP accumulation)
- Hook into existing review completion event
- Immediate visual feedback: piece fade-in animation (already implemented)
- **Differentiator:** No streaks, no penalties - daily fresh start reduces anxiety

**Randomization Fairness (Researched from Puzzle Game Algorithms):**
- **Fisher-Yates shuffle** ensures each piece has equal probability
- Maintain pool of unlocked piece indices; remove from pool on unlock
- Alternative: Pre-shuffle all 100 pieces at midnight reset; unlock in shuffled order
- **Avoid:** Naive random selection (can pick same piece twice) or simple sequential unlock

### Semi-Transparent Overlay (Researched from Puzzle/Photo Reveal Apps)

**Common Opacity Levels:**
- 32-50% opacity for overlays that shouldn't dominate underlying image
- 46% used in jigsaw puzzle overlays for balance between visibility and structure
- **Recommendation:** 50% opacity as specified - high enough to show puzzle structure, low enough to see painting

**Implementation Approach:**
- Apply alpha channel to existing gray gradient PNG pieces
- Blend mode: Normal (not Luminosity or Multiply - keep simple)
- Checkerboard variant alternation still visible at 50% opacity
- **Already exists:** Painting clipping through unlocked pieces; just need opacity on locked pieces

### Completion Celebration (Researched from Mobile Game Celebrations)

**Common Patterns:**
- **Confetti/fireworks** - Canvas-based particle effects (js-confetti, canvas-confetti patterns)
- **Animations** - Full-screen celebration sequences (Lottie animations common)
- **Sound effects** - Fanfare, chimes, applause
- **Visual reveals** - Zoom out, flash, glow effects

**AnkiDroid Existing Asset:**
- Puzzle break cinematic already implemented
- **Recommendation:** Reuse existing cinematic - saves implementation time, maintains visual consistency
- **Trigger condition:** When 100th piece unlocked (all pieces in unlocked state)
- **Enhancement (P2):** Add subtle sound effect to cinematic if user feedback indicates need

## Competitor Feature Analysis

| Feature | Duolingo (Learning) | Chess.com Daily Puzzles | Flow Free Daily | AnkiDroid Museum Approach |
|---------|---------------------|-------------------------|-----------------|---------------------------|
| Daily reset | Midnight, timezone-aware | Midnight, timezone-aware | Midnight, timezone-aware | Same - midnight local timezone |
| Progress persistence | Streaks, XP, levels | Streak counter, best times | Streak (decreases by 1 if miss) | No streaks - just unlock state |
| Unlock mechanic | XP accumulation unlocks levels | 1 puzzle solve = streak +1 | 1 solve = streak +1 | 1 review = 1 piece (direct 1:1) |
| Visual feedback | Animations, confetti, characters | Checkmark, streak badge | Completion screen | Piece fade-in + cinematic on 100 |
| Penalty for missing | Streak breaks, lose progress | Streak resets to 0 | Streak decreases by 1 | None - fresh start daily |
| Randomization | Lesson order predetermined | Single puzzle per day | Single puzzle per day | 100 pieces, random unlock order |
| Completion celebration | Character animations, XP burst | Simple success message | Completion screen with stats | Puzzle break cinematic (reused) |

**Key Differentiators:**
1. **No streak anxiety** - Duolingo/Chess.com users report stress from broken streaks; AnkiDroid avoids this
2. **Painting always visible** - 50% opacity locked pieces unique; most puzzles hide unrevealed content
3. **1:1 review-to-unlock** - Simpler than XP systems; respects Anki's study philosophy
4. **Museum context** - Puzzle reveals art user chose, not arbitrary image

## Sources

**Gamified Learning Patterns:**
- [How Duolingo Streak Builds Habit](https://blog.duolingo.com/how-duolingo-streak-builds-habit/)
- [Duolingo Gamification Case Study](https://www.strivecloud.io/blog/gamification-examples-boost-user-retention-duolingo)
- [Habit-Forming Design: Duolingo](https://jenniferhandali.medium.com/habit-forming-design-gamify-motivate-retain-learn-how-duolingo-keeps-their-users-hooked-6812c85a0a42)
- [Anki vs Duolingo Comparison](https://speakada.com/anki-vs-duolingo-which-language-learning-app-really-works/)

**Daily Reset Mechanics:**
- [LinkedIn Games Streaks](https://www.linkedin.com/help/linkedin/answer/a6296670)
- [Chess.com Streaks](https://support.chess.com/en/articles/9714718-what-are-streaks)
- [Flow Free Daily Puzzles](https://flowfree.fandom.com/wiki/Daily_Puzzles)
- [Streaks and Milestones for Gamification](https://www.plotline.so/blog/streaks-for-gamification-in-mobile-apps)

**Progressive Disclosure UX:**
- [Progressive Disclosure in UX Design](https://blog.logrocket.com/ux-design/progressive-disclosure-ux-types-use-cases/)
- [Progressive Disclosure for Mobile Apps](https://uxplanet.org/design-patterns-progressive-disclosure-for-mobile-apps-f41001a293ba)
- [What is Progressive Disclosure - UXPin](https://www.uxpin.com/studio/blog/what-is-progressive-disclosure/)

**Randomization Algorithms:**
- [Random Numbers and Fair Distribution](https://blog.damnsoft.org/random-numbers-distributions-and-games-part-1/)
- [Randomizing Sliding Puzzle Tiles](https://www.sitepoint.com/randomizing-sliding-puzzle-tiles/)

**Celebration Patterns:**
- [Confetti Effects with JavaScript](https://blog.openreplay.com/adding-confetti-effects-javascript-fun-walkthrough/)
- [Confetti Flutter Package](https://fluttergems.dev/packages/confetti/)

**Android Implementation:**
- [Schedule Alarms - Android Developers](https://developer.android.com/develop/background-work/services/alarms)
- [WorkManager vs AlarmManager Best Practices](https://medium.com/@husayn.fakher/understanding-android-background-task-scheduling-workmanager-jobscheduler-and-alarmmanager-67448cc4c8bb)
- [SharedPreferences - Android Developers](https://developer.android.com/reference/android/content/SharedPreferences)
- [Shared Preferences Examples - GeeksforGeeks](https://www.geeksforgeeks.org/shared-preferences-in-android-with-examples/)

---
*Feature research for: AnkiDroid Museum daily puzzle reveal mechanics*
*Researched: 2026-02-12*
*Confidence: MEDIUM (based on WebSearch verification of established patterns in gamified learning apps, daily reset mechanics, and Android best practices)*
