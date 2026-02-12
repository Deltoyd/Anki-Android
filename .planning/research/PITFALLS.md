# Pitfalls Research

**Domain:** Daily puzzle reveal mechanics with semi-transparent rendering and progressive unlocking
**Researched:** 2026-02-12
**Confidence:** HIGH

## Critical Pitfalls

### Pitfall 1: Alpha Overdraw Performance Degradation

**What goes wrong:**
Rendering 100 semi-transparent PNG pieces on every frame causes severe overdraw issues. Unlike opaque rendering where Android can optimize by skipping occluded pixels, transparent rendering requires the GPU to read existing framebuffer pixels, perform blending calculations, and write back for every pixel in every semi-transparent piece. With 100 pieces at 50% opacity, you're essentially rendering the screen multiple times per frame, leading to frame drops, battery drain, and device overheating.

**Why it happens:**
Developers assume adding `paint.alpha = 127` to existing opaque rendering is a simple, zero-cost change. The existing `PaintingPuzzleView.kt` renders locked pieces as opaque PNGs with `canvas.drawBitmap(bmp, null, tmpPieceRect, pieceBitmapPaint)` at line 469. Adding transparency to `pieceBitmapPaint` seems trivial but fundamentally changes GPU workload from simple blit operations to expensive read-modify-write cycles for every pixel.

**How to avoid:**
1. **Don't modify existing Paint object globally** - The current code uses a single `pieceBitmapPaint` Paint instance (line 100) shared across all locked pieces. Adding alpha to this would affect all pieces every frame.

2. **Use hardware layers for static content** - For pieces that don't change during a frame, render them once to an offscreen hardware texture using `View.setLayerType(LAYER_TYPE_HARDWARE, paint)` at the individual piece level or cache the entire locked puzzle state as a single bitmap.

3. **Batch transparency** - Instead of 100 individual semi-transparent draws, render all locked pieces to an offscreen Canvas at full opacity, then draw that single bitmap with 50% alpha. This reduces overdraw from 100x to 2x.

4. **Alternative visual approach** - Use a single semi-transparent gray overlay on top of opaque locked pieces rather than making each piece transparent. This achieves similar visual effect with minimal overdraw.

**Warning signs:**
- Profile GPU Rendering shows bars exceeding 16ms threshold (Settings > Developer Options > Profile GPU Rendering)
- Debug GPU Overdraw shows red/pink tinting (4x+ overdraw) across the puzzle area when enabled
- Frame drops visible in logcat: "Skipped X frames! The application may be doing too much work on its main thread"
- Device becomes warm to touch during normal puzzle viewing
- Battery drain increases significantly when homescreen with puzzle is visible

**Phase to address:**
**Phase 1 (Semi-transparent rendering)** - Must be solved during initial implementation. Testing should include GPU profiling on low-end devices (e.g., devices from 2020 with limited GPU fill rate). Success criteria: maintain 60fps with GPU overdraw showing no more than 2x in puzzle area.

---

### Pitfall 2: Timezone Handling for Daily Reset Creates Unfair Global Experience

**What goes wrong:**
Using UTC midnight for daily reset gives users in different timezones dramatically different amounts of time to earn unlocks. A user in New Zealand loses their puzzle pieces at 1:00 PM local time (when UTC hits midnight), while a user in California loses pieces at 4:00 PM the previous day. Even worse, users changing timezones (traveling) can lose progress unexpectedly or gain extra time by gaming the system. DST transitions cause the reset time to shift by an hour twice per year in local perception, breaking user expectations.

**Why it happens:**
Developers default to storing timestamps as UTC milliseconds (standard practice for backend systems) and compare against UTC midnight because it's simpler - no timezone library required, no edge case handling. The Rust backend in AnkiDroid already uses UTC for card review timestamps (`collection.crt` field), making it tempting to reuse this pattern for daily reset.

**How to avoid:**
1. **Store user's timezone preference** - Add `puzzle_timezone` field to SharedPreferences or Rust backend settings. Initialize from `TimeZone.getDefault().id` on first launch. Allow users to override in settings for travelers.

2. **Calculate midnight in user timezone** - When checking if reset is needed:
   ```kotlin
   // WRONG - UTC comparison
   val lastResetDay = lastResetTimestamp / (24 * 60 * 60 * 1000)
   val currentDay = System.currentTimeMillis() / (24 * 60 * 60 * 1000)
   if (currentDay > lastResetDay) { resetPuzzle() }

   // RIGHT - User timezone comparison
   val userTimezone = ZoneId.of(preferences.getString("puzzle_timezone"))
   val lastResetDate = Instant.ofEpochMilli(lastResetTimestamp)
       .atZone(userTimezone).toLocalDate()
   val currentDate = Instant.now().atZone(userTimezone).toLocalDate()
   if (currentDate.isAfter(lastResetDate)) { resetPuzzle() }
   ```

3. **Handle DST transitions** - Use `java.time` APIs (LocalDate, ZoneId) which automatically handle DST. Don't use `Calendar` or manual offset calculations which break during DST transitions.

4. **Add grace period** - Allow actions within 3-6 hours after midnight to count for previous day to handle users who are "minutes late" due to device clock skew or DST confusion. Store both the reset timestamp and the date it represents to detect grace period eligibility.

**Warning signs:**
- User reports: "My puzzle reset in the middle of the day"
- Analytics show reset happening at wildly different UTC hours across user base
- Spike in resets during DST transition weekends (March/November)
- Users in +12 timezones (New Zealand, Fiji) report losing progress "early"
- Test with device timezone set to multiple zones shows different reset behavior

**Phase to address:**
**Phase 3 (Daily reset logic)** - Must include comprehensive timezone testing. Test matrix should cover: UTC+12 (earliest), UTC-10 (latest), DST transition dates in multiple timezones, timezone changes while app running. Document expected reset time in user-local time in UI/docs.

---

### Pitfall 3: Race Condition Between Review Count Update and Piece Unlock Animation

**What goes wrong:**
When a user completes a card review, multiple things happen concurrently: (1) Rust backend updates review count and triggers `OpChanges`, (2) ViewModel fetches new count via `CollectionManager.withCol{}`, (3) UI determines which piece to unlock, (4) unlock animation starts. If the ViewModel fetches review count during an animation, it may trigger unlocking a new piece while the previous animation is still running. Since `PaintingPuzzleView.animateUnlock()` cancels the `currentAnimator` (line 250), the previous piece's animation gets truncated mid-fade, creating jarring visual jumps. Worse, if multiple reviews complete rapidly (user doing cards quickly), pieces unlock in unpredictable order or skip animation entirely.

**Why it happens:**
AnkiDroid's architecture uses `ChangeManager` to broadcast collection changes (line 74 of CollectionManager.kt comments), which triggers observers to reload data. The reviewer activity completes a card, emits `OpChanges` with `studyQueues=true`, and every subscriber (including the Museum homescreen if it's in the backstack) receives this notification simultaneously. There's no coordination between "review count changed" and "animation in progress", so these fire at different rates on different threads. The existing animation code at lines 249-283 of `PaintingPuzzleView.kt` only protects against one animation at a time via `currentAnimator?.cancel()`, not against rapid successive unlock requests.

**How to avoid:**
1. **Queue unlock requests in ViewModel** - Don't call `animateUnlock()` directly from StateFlow updates. Instead:
   ```kotlin
   // In ViewModel
   private val _unlockQueue = MutableSharedFlow<Int>(extraBufferCapacity = 100)
   val unlockQueue: SharedFlow<Int> = _unlockQueue.asSharedFlow()

   // When review count updates trigger unlock
   fun onReviewCountChanged(newCount: Int) {
       val piecesToUnlock = calculateUnlockedPieces(newCount)
       val newPieces = piecesToUnlock - currentlyUnlocked
       newPieces.forEach { _unlockQueue.emit(it) }
   }

   // In Fragment/Activity
   viewLifecycleOwner.lifecycleScope.launch {
       viewModel.unlockQueue.collect { pieceIndex ->
           // Only process when current animation finishes
           puzzleView.animateUnlockWhenReady(pieceIndex)
       }
   }
   ```

2. **Make animation await-able** - Convert `PaintingPuzzleView.animateUnlock()` to suspend function or return a `Deferred`:
   ```kotlin
   suspend fun animateUnlock(index: Int) = suspendCancellableCoroutine { cont ->
       currentAnimator?.cancel()
       unlockedPieces = unlockedPieces + index

       currentAnimator = ValueAnimator.ofInt(0, 255).apply {
           // ... existing setup ...
           addListener(object : Animator.AnimatorListener {
               override fun onAnimationEnd(animation: Animator) {
                   animatingPieces.remove(index)
                   invalidate()
                   cont.resume(Unit) // Signal completion
               }
               override fun onAnimationCancel(animation: Animator) {
                   animatingPieces.remove(index)
                   cont.cancel() // Propagate cancellation
               }
               // ...
           })
           start()
       }
   }
   ```

3. **Debounce StateFlow updates** - If review count comes from rapid card reviews, debounce the unlock trigger:
   ```kotlin
   viewModel.reviewCount
       .debounce(300) // Wait for burst of reviews to finish
       .distinctUntilChanged()
       .collect { count -> updateUnlockedPieces(count) }
   ```

4. **Separate data state from animation state** - Store "pieces that should be unlocked" separately from "pieces currently animating". The view should catch up animations when idle, not interrupt ongoing ones.

**Warning signs:**
- Animation appears to "jump" or skip from 0 to full opacity instantly
- Logcat shows "Animation cancelled" messages in bursts
- Multiple pieces appear to unlock simultaneously instead of sequentially
- Visual stuttering when returning to homescreen after rapid card reviews
- Animation state (`animatingPieces` map) grows unbounded because `onAnimationEnd` never fires due to cancellation

**Phase to address:**
**Phase 2 (Review tracking integration)** - Critical to solve here because Phase 4 (celebration cinematic) depends on all 100 pieces unlocking smoothly. Test with rapid review sessions (20+ cards in under a minute). Add integration test: trigger 10 reviews in quick succession, verify all 10 pieces animate fully without cancellation.

---

### Pitfall 4: Piece Unlock Randomness is Not Stable Across App Restarts

**What goes wrong:**
When determining which puzzle pieces to unlock based on review count (e.g., "user reviewed 23 cards today, unlock 23 random pieces"), developers often use `Random().shuffle()` or `(0..99).shuffled()` each time the calculation runs. This causes different pieces to appear unlocked every time the app restarts or the screen rotates, even though the review count hasn't changed. Users see piece #42 unlocked, close the app, reopen it with the same 23 reviews, and now piece #17 is unlocked instead. This breaks the mental model of "progress persists" and feels buggy, especially if a user was close to completing a recognizable section of the painting.

**Why it happens:**
Developers treat unlock calculation as pure function: `reviewCount -> Set<Int>`, using randomness to add variety. They don't realize that this function gets called on every app launch (to restore UI state), every configuration change (rotation), and potentially every time the ViewModel is recreated. Without a stable seed, `Random()` uses system time or other non-deterministic sources, producing different results each call.

**How to avoid:**
1. **Store unlocked piece indices explicitly** - Don't recalculate from review count. Store the actual set of unlocked pieces:
   ```kotlin
   // In SharedPreferences or Rust backend
   // Store: "unlocked_pieces" -> "3,7,12,15,18,..." (CSV of indices)
   // Or use JSON array if using DataStore

   fun unlockNewPieces(currentReviewCount: Int) {
       val currentUnlocked = loadUnlockedPieces() // e.g., setOf(3, 7, 12)
       val targetCount = currentReviewCount
       val piecesToAdd = targetCount - currentUnlocked.size

       if (piecesToAdd > 0) {
           val remainingPieces = (0..99).toSet() - currentUnlocked
           val newPieces = remainingPieces.shuffled().take(piecesToAdd)
           saveUnlockedPieces(currentUnlocked + newPieces)
       }
   }
   ```

2. **If you must recalculate, use deterministic seed** - Seed Random with something stable like user ID + current date:
   ```kotlin
   // Deterministic but date-specific (resets daily)
   val userId = getUserId() // from backend
   val dateString = LocalDate.now().toString() // "2026-02-12"
   val seed = "${userId}_${dateString}".hashCode().toLong()
   val random = Random(seed)
   val unlockedIndices = (0..99).shuffled(random).take(reviewCount).toSet()
   ```

3. **Hybrid approach for performance** - Store explicit list, but recalculate with seed if storage is corrupted:
   ```kotlin
   fun getUnlockedPieces(reviewCount: Int): Set<Int> {
       val stored = loadUnlockedPieces()
       if (stored.size == reviewCount) return stored

       // Storage corrupted or count changed - recalculate deterministically
       return recalculateWithSeed(reviewCount, getCurrentDateSeed())
           .also { saveUnlockedPieces(it) }
   }
   ```

**Warning signs:**
- User reports: "My puzzle looks different every time I open the app"
- QA testing shows different pieces unlocked across test runs with identical setup
- Debug logs show `setUnlockedPieces()` called with different sets on every `onCreate`
- Rotation causes pieces to reshuffle visibly
- After reaching 100 reviews then dropping to 99 (due to date change), a completely different set of 99 pieces appears

**Phase to address:**
**Phase 2 (Review tracking integration)** - Decide on storage strategy before implementing unlock logic. Add unit test: call unlock calculation twice with same review count, verify identical results. Add integration test: unlock 50 pieces, kill app, relaunch, verify same 50 pieces are unlocked.

---

### Pitfall 5: Daily Reset Mid-Session Causes Data Loss and State Corruption

**What goes wrong:**
User opens the app at 11:50 PM, unlocks several pieces by reviewing cards, then continues using the app past midnight. The daily reset logic detects the date change (comparing stored `last_reset_date` against current date) and immediately clears all unlocked pieces and resets review count to zero. The user loses all progress made in the current session, including reviews completed before midnight. Worse, if reset happens while the unlock animation is running, the app may crash due to `animatingPieces` map containing indices no longer in `unlockedPieces`, or the animation completes but shows a now-locked piece as unlocked until the next `invalidate()` call.

**Why it happens:**
Developers check for date change on every app resume or ViewModel initialization because they want to handle "user left app open overnight" scenarios. They add something like:
```kotlin
// In ViewModel.init or onResume
if (getCurrentDate() > getLastResetDate()) {
    resetDailyProgress() // Immediately clears everything
}
```
This fires synchronously in the UI rendering path, potentially mid-frame or mid-animation. The reset logic doesn't wait for ongoing operations to complete or give users a chance to finish their current action.

**How to avoid:**
1. **Defer reset until app is idle** - Only reset when app comes to foreground from background, not during active session:
   ```kotlin
   // In Application or Activity
   override fun onActivityResumed(activity: Activity) {
       val shouldReset = shouldPerformDailyReset()
       if (shouldReset && !userIsActivelyReviewing()) {
           scheduleReset() // Queue for next idle period
       }
   }
   ```

2. **Grace period around midnight** - Don't reset immediately at 00:00:00. Allow current session to complete:
   ```kotlin
   fun shouldReset(): Boolean {
       val now = Instant.now().atZone(userTimezone)
       val lastReset = Instant.ofEpochMilli(lastResetTime).atZone(userTimezone)

       // Only reset if we're in a new day AND at least 2 hours past midnight
       return now.toLocalDate().isAfter(lastReset.toLocalDate()) &&
              now.hour >= 2
   }

   // Or: only reset when app fully stops/starts, not during session
   ```

3. **Preserve in-flight progress** - If reset must happen during session, save current session's review count separately:
   ```kotlin
   data class DailyProgress(
       val date: LocalDate,
       val persistedReviews: Int, // Reviews from completed sessions
       val sessionReviews: Int,   // Reviews in current session
       val unlockedPieces: Set<Int>
   )

   fun onDailyReset() {
       // Don't reset current session, just clear persisted state
       _progress.update {
           it.copy(
               date = LocalDate.now(),
               persistedReviews = 0,
               unlockedPieces = emptySet()
               // Keep sessionReviews intact
           )
       }
   }
   ```

4. **Show UI warning before reset** - If user is actively viewing the puzzle when midnight hits:
   ```kotlin
   if (shouldReset() && isAppInForeground()) {
       showSnackbar("New day starts soon - your puzzle will reset in 2 minutes")
       delay(120_000)
       resetDailyProgress()
   }
   ```

5. **Synchronize reset with animation state** - Wait for animations to complete:
   ```kotlin
   suspend fun resetDailyProgress() {
       // Wait for any ongoing animations
       currentAnimator?.let { animator ->
           suspendCancellableCoroutine<Unit> { cont ->
               animator.addListener(object : AnimatorListenerAdapter() {
                   override fun onAnimationEnd(animation: Animator) {
                       cont.resume(Unit)
                   }
               })
           }
       }

       // Now safe to reset
       clearUnlockedPieces()
       clearReviewCount()
   }
   ```

**Warning signs:**
- Crash reports around midnight in user's timezone with NPE in animation code
- User reports: "My puzzle disappeared while I was using it"
- State inconsistency: `reviewCount=10` but `unlockedPieces.size=0`
- Logcat shows reset happening multiple times in rapid succession (reset, user reviews card, triggers reset check again)
- Visual bug: pieces appear unlocked but are gray (animation finished but state reset)

**Phase to address:**
**Phase 3 (Daily reset logic)** - Must solve before release. Add scenario tests: start session at 11:55 PM (simulated), review cards, advance time to 12:05 AM, verify no data loss. Test reset during animation. Document expected reset behavior in user-facing docs (e.g., "Puzzle resets at 2:00 AM your local time").

---

### Pitfall 6: Celebration Cinematic Doesn't Trigger Due to Race Condition in "All Unlocked" Check

**What goes wrong:**
When the 100th piece unlocks, the celebration cinematic should trigger automatically. However, the check for "all pieces unlocked" happens in the ViewModel's StateFlow update, while the piece unlock animation happens asynchronously in the View. The ViewModel sees `unlockedPieces.size == 100` and emits a "show celebration" event, but the 100th piece's animation hasn't started yet (or is mid-fade). The cinematic starts playing while piece #100 is still visually locked or fading in, creating a confusing UX where the celebration happens before the puzzle looks complete. Alternatively, if the check happens in `PaintingPuzzleView.animateUnlock()`, configuration changes (rotation) can cause the check to re-run and trigger the cinematic again.

**Why it happens:**
There are two natural places to check for completion:
1. **In ViewModel after updating unlocked set** - This fires as soon as the data model says "100 pieces", but before the animation runs.
2. **In View's `onAnimationEnd` callback** - This fires after animation, but gets lost on rotation because animation state is View-specific and doesn't survive config changes.

Developers often choose option 1 for simplicity (ViewModels survive rotation), but this creates the visual race condition. Option 2 creates the rotation bug.

**How to avoid:**
1. **Trigger celebration from animation completion, persist trigger flag** - Use ViewModel to track "should celebrate when animations finish":
   ```kotlin
   // ViewModel
   private val _shouldCelebrate = MutableStateFlow(false)
   val shouldCelebrate: StateFlow<Boolean> = _shouldCelebrate.asStateFlow()

   fun onReviewCountChanged(newCount: Int) {
       val unlocked = calculateUnlockedPieces(newCount)
       _unlockedPieces.value = unlocked

       if (unlocked.size == 100 && !_hasCompletedToday.value) {
           _shouldCelebrate.value = true // Set flag, don't trigger yet
       }
   }

   // View
   suspend fun animateUnlock(index: Int) {
       // ... animation code ...
       addListener(object : AnimatorListenerAdapter() {
           override fun onAnimationEnd(animation: Animator) {
               animatingPieces.remove(index)
               invalidate()

               // Check if this was final piece AND ViewModel says to celebrate
               if (index == lastPieceToUnlock && viewModel.shouldCelebrate.value) {
                   viewModel.onCelebrationShown() // Clear flag
                   triggerCelebration()
               }
           }
       })
   }
   ```

2. **Use SharedFlow for one-time events** - Don't use StateFlow for celebration trigger (StateFlow replays last value on rotation):
   ```kotlin
   // ViewModel
   private val _celebrationEvent = MutableSharedFlow<Unit>(replay = 0)
   val celebrationEvent: SharedFlow<Unit> = _celebrationEvent.asSharedFlow()

   // Only emit when actually completing, not on rotation
   fun checkCompletion() {
       if (unlockedPieces.size == 100 && !celebratedToday) {
           celebratedToday = true
           viewLifecycleScope.launch {
               // Wait for animations to finish
               delay(animationDuration + 100)
               _celebrationEvent.emit(Unit)
           }
       }
   }
   ```

3. **Coordinate via animation sequence** - Make piece unlocks sequential, celebrate after last one:
   ```kotlin
   suspend fun unlockPiecesSequentially(pieces: List<Int>) {
       pieces.forEach { pieceIndex ->
           animateUnlock(pieceIndex) // suspend function, waits for completion
           delay(50) // Small gap between pieces
       }
       if (pieces.contains(100th piece)) {
           triggerCelebration()
       }
   }
   ```

4. **Separate completion state from celebration state** - Track if puzzle is complete independently from if celebration has been shown:
   ```kotlin
   data class PuzzleState(
       val unlockedPieces: Set<Int>,
       val isComplete: Boolean = unlockedPieces.size == 100,
       val hasShownCelebration: Boolean = false,
       val animationsInProgress: Int = 0
   )

   // Only celebrate when: complete AND not yet celebrated AND no animations running
   if (state.isComplete && !state.hasShownCelebration && state.animationsInProgress == 0) {
       showCelebration()
   }
   ```

**Warning signs:**
- Celebration cinematic plays with puzzle pieces still fading in
- Rotation causes celebration to replay
- Logcat shows "Celebration triggered" but no cinematic appears
- Celebration never triggers even when visually complete (animations never reported completion)
- Multiple celebration triggers in quick succession

**Phase to address:**
**Phase 4 (Completion celebration)** - This phase integrates the existing cinematic with new unlock system. Add integration test: unlock 99 pieces, then unlock final piece, verify celebration triggers exactly once after animation completes. Test rotation during celebration trigger.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Using single shared Paint instance with global alpha for all locked pieces | Simple one-line change: `pieceBitmapPaint.alpha = 127` | Severe overdraw performance issues, requires refactor to offscreen rendering or hardware layers | Never - overdraw compounds with 100 pieces |
| Storing only review count, recalculating unlocked pieces each time | Small storage footprint (1 integer vs 100 booleans) | Non-deterministic unlock pattern on app restart, user confusion, requires careful seeding | Only if deterministic seed is implemented from day 1 |
| Checking date change on every ViewModel init/onResume | Ensures daily reset never missed | Can reset mid-session causing data loss, requires idle detection | Only in MVP with explicit "reset happens when app reopens" UX documentation |
| Using StateFlow for celebration trigger event | Simple reactive pattern, survives rotation | Celebration replays on every rotation, requires complex "already shown" flag management | Never - use SharedFlow or Channel for one-time events |
| Hardcoding UTC for daily reset | No timezone complexity, simple timestamp comparison | Unfair global experience, user complaints from non-UTC timezones | Only for internal beta with single-timezone users |
| Skipping GPU profiling in development | Faster development iteration | Performance issues discovered by users, requires architectural changes post-launch | Never - 100 pieces with transparency requires profiling from day 1 |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| CollectionManager.withCol{} for review count | Calling from UI thread expecting immediate result | Always call from coroutine (ViewModel.launchCatchingIO), use StateFlow to emit result to UI |
| ChangeManager.subscribe() for tracking reviews | Subscribing in ViewModel instead of Activity/Fragment | Subscribe in onCreate/onViewCreated, ViewModel receives updates via separate mechanism (e.g., manual trigger after withCol) |
| Animation completion callbacks | Using `postDelayed(animationDuration)` to detect finish | Use proper AnimatorListener callbacks (`onAnimationEnd`), handle `onAnimationCancel` separately |
| SharedPreferences for unlocked pieces | Storing as single comma-separated string, parsing on every read | Use DataStore with typed preferences or JSON serialization, or store as StringSet |
| System time for date comparison | Using `System.currentTimeMillis()` and dividing by day length | Use java.time APIs (Instant.now().atZone(), LocalDate) to handle DST and timezone correctly |
| Random piece selection | Using `Random()` without seed, different results each run | Either store explicit list OR use deterministic seed (userId + date) |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Calling `invalidate()` on every review count update | UI thread messages in logcat, sluggish scrolling | Only invalidate when unlocked pieces actually change, debounce rapid updates | >10 reviews per second (user doing rapid card reviews) |
| Allocating new Paint objects in `onDraw()` | Frequent GC pauses, stuttering animations | Pre-allocate Paint instances as class fields, reuse with `paint.alpha = ...` | With 100 pieces drawn at 60fps = 6000 allocations/second |
| Loading PNG bitmaps on every piece unlock | Memory spikes, OutOfMemoryError on low-end devices | Lazy-load bitmaps once in `by lazy {}` (already done in PaintingPuzzleView.kt line 77) | On devices with <2GB RAM |
| Reading SharedPreferences synchronously in ViewModel init | ANR (Application Not Responding) dialog on slow storage | Use DataStore with Flow-based reading, or read preferences in background coroutine | On older devices with slow flash storage, >100 keys |
| Recalculating which pieces to unlock on every State update | Wasted CPU cycles, battery drain | Cache calculation result, only recalculate when review count changes (`distinctUntilChanged()`) | Review count updates rapidly (>5/second during review session) |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| No visual feedback during daily reset | User sees puzzle suddenly blank, thinks app is broken | Show brief animation or snackbar: "New day! Your puzzle has reset." |
| Celebration triggers before final piece animation completes | Anti-climactic payoff, celebration feels glitchy | Wait for all animations to finish before celebration, or integrate celebration into final unlock animation |
| Pieces unlock in unpredictable random order during rapid reviews | Jarring visual, user can't appreciate unlock pattern | Queue unlocks, animate sequentially with small delays between pieces |
| No indication of how many reviews needed for next unlock | User doesn't know how close they are to next unlock | Show progress: "Review 3 more cards to unlock next piece" |
| Locked pieces completely opaque (current state) makes comparison with unlocked pieces difficult | User can't tell what the painting will look like | Semi-transparent locked pieces (goal of this milestone) solves this |
| Puzzle resets exactly at midnight without warning | User loses streak feeling if they review at 11:59 PM | Grace period (3-6 hours) or explicit "day starts at 2 AM" messaging |

## "Looks Done But Isn't" Checklist

- [ ] **Semi-transparent rendering:** Often missing hardware layer optimization or batching — verify GPU Overdraw shows <3x in puzzle area, Profile GPU Rendering stays under 16ms
- [ ] **Review tracking:** Often missing debouncing for rapid reviews — verify 20 rapid reviews in 10 seconds all get counted and trigger unlocks without skipping
- [ ] **Daily reset:** Often missing timezone handling — verify reset happens at expected local time across UTC-10, UTC+0, UTC+12 timezones
- [ ] **Daily reset:** Often missing DST transition handling — verify correct reset time on DST transition dates (March/November in Northern Hemisphere)
- [ ] **Piece unlock animation:** Often missing cancellation handling — verify rotating device mid-animation doesn't crash or leave pieces in inconsistent state
- [ ] **Random piece selection:** Often missing deterministic seeding — verify app restart with same review count shows identical unlocked pieces
- [ ] **Completion celebration:** Often missing "already shown" flag — verify rotation doesn't replay celebration cinematic
- [ ] **Completion celebration:** Often missing animation coordination — verify celebration waits for 100th piece's fade-in to complete before starting
- [ ] **Unlock persistence:** Often missing explicit storage — verify unlocked pieces survive app kill + restart (not just rotation)
- [ ] **State consistency:** Often missing validation — verify `unlockedPieces.size <= reviewCount` invariant always holds after any operation

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Alpha overdraw performance issues discovered post-launch | HIGH | 1. Add hardware layer caching for locked piece group 2. Pre-render locked state to single bitmap 3. Consider switching to Jetpack Compose Canvas for better layering 4. Worst case: remove transparency, use different visual treatment (border, shadow) |
| Non-deterministic piece unlocking causing user complaints | MEDIUM | 1. Add explicit storage for unlocked pieces Set<Int> 2. Migration: on app update, recalculate with deterministic seed based on current review count + date 3. Add A/B test flag to compare user satisfaction |
| Timezone bugs causing unfair reset times | MEDIUM | 1. Add timezone preference to settings 2. Migration: detect user's timezone from device, backfill stored timezone 3. Add grace period to minimize impact during rollout 4. Show reset time in UI so users can verify it's correct |
| Celebration triggers before animation completes | LOW | 1. Add `animationInProgress` state to ViewModel 2. Delay celebration trigger by `animationDuration + safetyMargin` 3. If already shipped, add config flag to disable celebration temporarily |
| Daily reset mid-session causing data loss | HIGH | 1. Change reset trigger to only fire on app cold start 2. Add "session reviews" vs "persisted reviews" separation 3. Migrate existing users by preserving today's progress if reset happened <1 hour ago 4. Show apologetic in-app message about bug fix |
| Race condition in review count update + animation | MEDIUM | 1. Implement unlock queue in ViewModel (MutableSharedFlow) 2. Convert animation to suspend function or Deferred 3. Add integration test to prevent regression 4. If unfixable quickly, add rate limiting: max 1 unlock per 500ms |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| Alpha overdraw performance | Phase 1: Semi-transparent rendering | GPU profiling on Pixel 4a (mid-range 2020 device), overdraw shows ≤2x, 60fps maintained |
| Random piece instability | Phase 2: Review tracking integration | Unit test: 100 runs with same review count produce identical piece sets. Integration test: app restart preserves exact unlocked pieces |
| Review count race condition with animation | Phase 2: Review tracking integration | Integration test: 10 rapid reviews trigger 10 sequential animations without cancellation |
| Timezone handling bugs | Phase 3: Daily reset logic | Test matrix: 5 timezones × 3 scenarios (normal day, DST spring forward, DST fall back) = 15 passing tests |
| Daily reset mid-session data loss | Phase 3: Daily reset logic | Scenario test: simulated time advance from 11:55 PM to 12:05 AM during active review session, no progress lost |
| Celebration race condition | Phase 4: Completion celebration | Integration test: unlock 100th piece, verify celebration triggers exactly once after animation completes. Rotation test: rotate during celebration, verify no replay |

## Sources

**Performance and Rendering:**
- [Reduce overdraw | Android Developers](https://developer.android.com/topic/performance/rendering/overdraw)
- [Optimize a custom view | Android Developers](https://developer.android.com/training/custom-views/optimizing-view.html)
- [Debug GPU Overdraw: Android UI Performance](https://think-it.io/insights/Android-Performance-GPU-overdraw)
- [Android Canvas Scaling Guide](https://copyprogramming.com/howto/how-to-scale-canvas-in-android)
- [Android: Speeding up canvas.drawBitmap](http://www.independent-software.com/android-speeding-up-canvas-drawbitmap.html)

**Timezone and Date Handling:**
- [Handling Time Zones in Global Gamification Features](https://trophy.so/blog/handling-time-zones-gamification)
- [Implementing a Daily Streak System: A Practical Guide](https://tigerabrodi.blog/implementing-a-daily-streak-system-a-practical-guide)
- [Streaks: The Gamification Feature Everyone Gets Wrong](https://medium.com/design-bootcamp/streaks-the-gamification-feature-everyone-gets-wrong-6506e46fa9ca)
- [Setting Dates in Android SharedPreferences: 2026 Guide](https://copyprogramming.com/howto/setting-date-to-a-shared-prefernece-in-android-studio)
- [Time overview | Android Open Source Project](https://source.android.com/docs/core/connect/time)

**State Management and Race Conditions:**
- [Managing ViewModel State with StateFlow: Preventing Race Conditions](https://medium.com/@mahmoud.afarideh/managing-viewmodel-state-with-stateflow-preventing-race-conditions-dedaca6a8c24)
- [Best practices for coroutines in Android](https://developer.android.com/kotlin/coroutines/coroutines-best-practices)
- [Advanced Concurrency and Race Condition Mitigation in Android](https://medium.com/@engineervishvnath/advanced-concurrency-and-race-condition-mitigation-in-android-898424e47dc8)
- [How to Prevent Race Conditions in Coroutines](https://typealias.com/articles/prevent-race-conditions-in-coroutines/)

**Animation:**
- [Animation.AnimationListener | Android Developers](https://developer.android.com/reference/android/view/animation/Animation.AnimationListener)
- [Not getting onAnimationEnd() callback if cancelled](https://github.com/airbnb/lottie-android/issues/1963)

**Random Number Generation:**
- [Weak PRNG | Security | Android Developers](https://developer.android.com/privacy-and-security/risks/weak-prng)
- [Random Seeds and Reproducibility](https://medium.com/data-science/random-seeds-and-reproducibility-933da79446e3)

---
*Pitfalls research for: Daily puzzle reveal mechanics with semi-transparent rendering*
*Researched: 2026-02-12*
