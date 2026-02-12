# Architecture Integration Research

**Domain:** Daily puzzle reveal mechanics for AnkiDroid Museum
**Researched:** 2026-02-12
**Confidence:** HIGH

## Integration Overview

This is a **SUBSEQUENT MILESTONE** that adds daily mechanics to existing Museum puzzle infrastructure. The architecture focuses on four new behaviors:

1. **Semi-transparent locked pieces** (50% opacity rendering)
2. **Daily review count tracking** (1 review = 1 random unlock)
3. **Daily reset persistence** (reset counter at midnight)
4. **Completion celebration trigger** (cinematic on full reveal)

### Existing Architecture (Unchanged)

```
┌─────────────────────────────────────────────────────────────┐
│                      UI Layer                                │
├─────────────────────────────────────────────────────────────┤
│  MuseumActivity                                              │
│  ┌──────────────┐  ┌──────────────────────────────────┐     │
│  │ ViewPager2   │  │ GalleryPagerAdapter              │     │
│  │ (Gallery)    │  │  └─> PaintingPuzzleView (Custom) │     │
│  └──────────────┘  └──────────────────────────────────┘     │
├─────────────────────────────────────────────────────────────┤
│                   ViewModel Layer                            │
├─────────────────────────────────────────────────────────────┤
│  MuseumViewModel (StateFlow pattern)                         │
│    - uiState: StateFlow<MuseumUiState>                       │
│    - events: SharedFlow<MuseumEvent>                         │
│    - unlockPiece(), loadGalleryData()                        │
├─────────────────────────────────────────────────────────────┤
│                 Persistence Layer                            │
├─────────────────────────────────────────────────────────────┤
│  MuseumPersistence (SharedPreferences)                       │
│  ArtProgressRepository (SharedPreferences + JSON)            │
│    - UserArtProgress: current puzzle state                   │
│    - revealedPieces: "0,5,12,33" (CSV)                       │
├─────────────────────────────────────────────────────────────┤
│                   View Layer                                 │
├─────────────────────────────────────────────────────────────┤
│  PaintingPuzzleView.kt (Custom View)                         │
│    - onDraw() → drawPuzzlePieces()                           │
│    - drawLockedPiecePng() → gray PNG assets                  │
│    - drawUnlockedPiece() → painting reveal with clip path    │
│    - animateUnlock() → ValueAnimator fade-in                 │
└─────────────────────────────────────────────────────────────┘
```

## New Components Required

### 1. DailyReviewTracker (New Service)

**Location:** `com.ichi2.anki.services.DailyReviewTracker`

**Purpose:** Tracks daily review count and triggers puzzle unlocks

**Responsibilities:**
- Observe review completion events via ChangeManager
- Track reviews-per-day in SharedPreferences
- Trigger random piece unlock on each review
- Reset counter at midnight (UTC-based rollover)
- Emit unlock events to MuseumViewModel

**Why New Component:**
- Decouples review tracking from MuseumViewModel (single responsibility)
- Follows AnkiDroid's service pattern (e.g., ArtAssetService, ArtProgressRepository)
- Allows background observation of ChangeManager without UI lifecycle coupling

**Implementation Pattern:**

```kotlin
class DailyReviewTracker(
    private val context: Context
) : ChangeManager.Subscriber {

    private val prefs = context.getSharedPreferences("daily_review_tracker", Context.MODE_PRIVATE)
    private val _unlockEvents = MutableSharedFlow<Int>()
    val unlockEvents: SharedFlow<Int> = _unlockEvents.asSharedFlow()

    fun start() {
        ChangeManager.subscribe(this)
        checkAndResetDaily()
    }

    override fun opExecuted(changes: OpChanges, handler: Any?) {
        if (changes.card) { // Review completed
            viewModelScope.launch {
                incrementDailyCount()
                triggerRandomUnlock()
            }
        }
    }

    private suspend fun checkAndResetDaily() {
        val today = dateFormat.format(Date())
        val lastReviewDate = prefs.getString(KEY_LAST_REVIEW_DATE, null)

        if (lastReviewDate != today) {
            // Daily reset
            prefs.edit()
                .putInt(KEY_DAILY_COUNT, 0)
                .putString(KEY_LAST_REVIEW_DATE, today)
                .apply()
        }
    }

    private suspend fun triggerRandomUnlock() {
        val lockedPieces = getLockedPieces()
        if (lockedPieces.isEmpty()) return

        val randomIndex = lockedPieces.random()
        _unlockEvents.emit(randomIndex)
    }
}
```

**Data Flow:**

```
Review Answer
    ↓
ReviewerViewModel.answerCard()
    ↓
undoableOp { sched.answerCard() }
    ↓
OpChanges(card = true)
    ↓
ChangeManager.notifySubscribers()
    ↓
DailyReviewTracker.opExecuted()
    ↓
incrementDailyCount() + triggerRandomUnlock()
    ↓
unlockEvents.emit(randomIndex)
    ↓
MuseumViewModel observes unlockEvents
    ↓
MuseumViewModel.unlockPiece(randomIndex)
    ↓
PaintingPuzzleView.animateUnlock(randomIndex)
```

### 2. Modified: PaintingPuzzleView

**Changes:** Add semi-transparent rendering for locked pieces

**Current State:**
- Locked pieces: `drawLockedPiecePng()` renders gray PNG at 100% opacity
- Unlocked pieces: `drawUnlockedPiece()` reveals painting through clip path

**New Behavior:**
- Locked pieces: Render gray PNG at **50% opacity** (alpha = 127/255)
- Unlocked pieces: Unchanged (100% opacity painting reveal)

**Implementation:**

```kotlin
// MODIFY EXISTING METHOD
private fun drawLockedPiecePng(
    canvas: Canvas,
    row: Int,
    col: Int,
    left: Float,
    top: Float,
) {
    val pieceType = getPieceType(row, col)
    val bmp = pieceBitmaps[pieceType] ?: return

    val bmpWidth = bmp.width.toFloat()
    val bmpHeight = bmp.height.toFloat()

    val scaleX = pieceWidth / PNG_BODY_SIZE
    val scaleY = pieceHeight / PNG_BODY_SIZE

    val (bodyX, bodyY) = PIECE_BODY_OFFSETS[pieceType] ?: Pair(0f, 0f)

    val destLeft = left - bodyX * scaleX
    val destTop = top - bodyY * scaleY
    val destRight = destLeft + bmpWidth * scaleX
    val destBottom = destTop + bmpHeight * scaleY

    tmpPieceRect.set(destLeft, destTop, destRight, destBottom)

    // NEW: Apply 50% opacity for semi-transparent locked pieces
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        alpha = 127 // 50% opacity (0-255 range)
    }

    canvas.drawBitmap(bmp, null, tmpPieceRect, paint)
}
```

**Why This Approach:**
- Minimal modification to existing rendering pipeline
- Uses Android's built-in Paint.alpha (0-255 range) for transparency
- No need for PorterDuff modes or additional compositing layers
- Matches existing animation pattern (animateUnlock already uses Paint.alpha)

**Sources:**
- [Paint.setAlpha Examples](https://www.tabnine.com/code/java/methods/android.graphics.Paint/setAlpha)
- [Android SDK: Drawing with Opacity](https://code.tutsplus.com/tutorials/android-sdk-drawing-with-opacity--mobile-19682)
- [Property Animation Overview](https://developer.android.com/develop/ui/views/animations/prop-animation)

### 3. Modified: MuseumViewModel

**Changes:** Integrate DailyReviewTracker and expose daily count state

**New State:**

```kotlin
data class MuseumUiState(
    val painting: Bitmap? = null,
    val unlockedPieces: Set<Int> = emptySet(),
    val streakDays: Int = 0,
    val extraLives: Int = 3,
    val progressText: String = "0 / 100 pieces",
    val currentDeckName: String = "Default",
    val galleryItems: List<GalleryArtItem> = emptyList(),
    val activePageIndex: Int = 0,
    val currentArtTitle: String = "",

    // NEW: Daily review tracking
    val dailyReviewCount: Int = 0,
    val dailyReviewGoal: Int = 10, // Configurable target
)
```

**New Methods:**

```kotlin
class MuseumViewModel : ViewModel() {
    // ... existing properties ...

    // NEW: Daily review tracker
    private lateinit var dailyTracker: DailyReviewTracker

    fun initializeDailyTracking(context: Context) {
        dailyTracker = DailyReviewTracker(context)
        dailyTracker.start()

        // Observe unlock events
        viewModelScope.launch {
            dailyTracker.unlockEvents.collect { pieceIndex ->
                unlockPiece(context, pieceIndex)
            }
        }

        // Load current daily count
        loadDailyCount(context)
    }

    private fun loadDailyCount(context: Context) {
        val count = dailyTracker.getDailyCount()
        _uiState.update { it.copy(dailyReviewCount = count) }
    }
}
```

**Integration Point:**

```kotlin
// MuseumActivity.onCreate()
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // ... existing setup ...

    viewModel.loadMuseumData(this)
    viewModel.initializeDailyTracking(this) // NEW
}
```

### 4. Modified: MuseumPersistence

**Changes:** Add daily review count tracking

**New Methods:**

```kotlin
object MuseumPersistence {
    // ... existing constants ...
    private const val KEY_DAILY_REVIEW_COUNT = "daily_review_count"
    private const val KEY_LAST_REVIEW_DATE = "last_review_date"

    fun getDailyReviewCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_DAILY_REVIEW_COUNT, 0)
    }

    fun incrementDailyReviewCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = getDailyReviewCount(context)
        val updated = current + 1
        prefs.edit().putInt(KEY_DAILY_REVIEW_COUNT, updated).apply()
        return updated
    }

    fun resetDailyReviewCount(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = dateFormat.format(Date())
        prefs.edit()
            .putInt(KEY_DAILY_REVIEW_COUNT, 0)
            .putString(KEY_LAST_REVIEW_DATE, today)
            .apply()
    }

    fun getLastReviewDate(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_REVIEW_DATE, null)
    }
}
```

**Why SharedPreferences:**
- Follows existing MuseumPersistence pattern
- Simple key-value storage (no complex queries)
- Fast read/write for counter increments
- Matches AnkiDroid's conventions (see CLAUDE.md patterns)

**Note on DataStore Migration:**
Google recommends Jetpack DataStore over SharedPreferences in 2026, but AnkiDroid codebase currently uses SharedPreferences throughout. For consistency with existing Museum code and AnkiDroid patterns, SharedPreferences is appropriate here. Migration to DataStore would be a project-wide refactor beyond this milestone's scope.

**Sources:**
- [Save simple data with SharedPreferences](https://developer.android.com/training/data-storage/shared-preferences)
- [SharedPreferences Best Practices](https://yakivmospan.com/blog/best-practice-shared-preferences/)

## Data Flow Changes

### Current Flow: Manual Unlock

```
User taps piece
    ↓
MuseumViewModel.unlockPiece(index)
    ↓
MuseumPersistence.addUnlockedPiece()
    ↓
_uiState.update { unlockedPieces + index }
    ↓
_events.emit(MuseumEvent.PieceUnlocked)
    ↓
GalleryPagerAdapter notifies PaintingPuzzleView
    ↓
PaintingPuzzleView.animateUnlock(index)
```

### New Flow: Daily Review Unlock

```
User answers card in Reviewer
    ↓
ReviewerViewModel.answerCard()
    ↓
undoableOp { sched.answerCard() }
    ↓
OpChanges(card = true) → ChangeManager
    ↓
DailyReviewTracker.opExecuted()
    ↓
checkAndResetDaily() (if new day)
    ↓
incrementDailyCount()
    ↓
triggerRandomUnlock()
    ↓
_unlockEvents.emit(randomIndex)
    ↓
MuseumViewModel observes unlockEvents
    ↓
MuseumViewModel.unlockPiece(randomIndex)
    ↓
[Same as manual unlock from here]
```

### Daily Reset Flow

```
App Launch / Review Event
    ↓
DailyReviewTracker.checkAndResetDaily()
    ↓
Compare today vs lastReviewDate
    ↓
If different day:
    MuseumPersistence.resetDailyReviewCount()
    ↓
_uiState.update { dailyReviewCount = 0 }
```

### Completion Celebration Flow

```
MuseumViewModel.unlockPiece()
    ↓
Check: updatedPieces.size == 100?
    ↓
YES → _events.emit(MuseumEvent.PuzzleCompleted)
    ↓
MuseumActivity observes events
    ↓
showCompletionDialog() (EXISTING)
```

**No changes needed** — completion detection already exists in MuseumViewModel.unlockPiece() (lines 198-200).

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| **ChangeManager** | Observer subscription | DailyReviewTracker subscribes to OpChanges notifications |
| **CollectionManager** | withCol {} blocks | No new usage — review events come via ChangeManager |
| **SharedPreferences** | Direct access via Context | Existing pattern for MuseumPersistence |
| **ValueAnimator** | Existing animation system | No changes — PaintingPuzzleView.animateUnlock() unchanged |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| **DailyReviewTracker → MuseumViewModel** | SharedFlow (unlockEvents) | Lifecycle-safe one-way event stream |
| **MuseumViewModel → PaintingPuzzleView** | StateFlow + Adapter | Existing pattern via GalleryPagerAdapter.notifyItemChanged() |
| **MuseumPersistence → DailyReviewTracker** | Direct function calls | Simple read/write for counters and dates |
| **ReviewerViewModel → DailyReviewTracker** | ChangeManager (indirect) | No direct coupling — uses observer pattern |

## Component Interaction Diagram

```
┌──────────────────────────────────────────────────────────────┐
│                     Reviewer Screen                          │
│  ReviewerViewModel.answerCard()                              │
│    ↓                                                          │
│  undoableOp { sched.answerCard() }                           │
└────────────────────┬─────────────────────────────────────────┘
                     │
                     ↓ OpChanges(card=true)
         ┌───────────────────────┐
         │    ChangeManager      │
         │  notifySubscribers()  │
         └───────────┬───────────┘
                     │
                     ↓ opExecuted(changes)
         ┌───────────────────────────────────┐
         │    DailyReviewTracker (NEW)       │
         │  - checkAndResetDaily()           │
         │  - incrementDailyCount()          │
         │  - triggerRandomUnlock()          │
         │  - unlockEvents: SharedFlow<Int>  │
         └───────────┬───────────────────────┘
                     │
                     ↓ unlockEvents.collect
         ┌───────────────────────────────────┐
         │      MuseumViewModel              │
         │  observes unlockEvents            │
         │  unlockPiece(randomIndex)         │
         │  _uiState.update()                │
         │  _events.emit(PieceUnlocked)      │
         └───────────┬───────────────────────┘
                     │
                     ↓ uiState.collect
         ┌───────────────────────────────────┐
         │      MuseumActivity               │
         │  observes uiState changes         │
         │  galleryAdapter.notifyDataChanged │
         └───────────┬───────────────────────┘
                     │
                     ↓ updateActivePainting()
         ┌───────────────────────────────────┐
         │   PaintingPuzzleView (MODIFIED)   │
         │  - drawLockedPiecePng() + alpha   │
         │  - animateUnlock(index)           │
         └───────────────────────────────────┘
```

## Suggested Build Order

Based on dependencies and testability:

### Phase 1: Rendering Foundation
**Goal:** Visual change only, no behavioral changes

1. **Modify PaintingPuzzleView.drawLockedPiecePng()**
   - Add Paint with alpha = 127
   - Verify rendering with existing unlocked pieces
   - **Test:** Visual inspection in Museum gallery
   - **Risk:** Low — isolated change to rendering logic

### Phase 2: Daily Persistence
**Goal:** Data layer for daily tracking, no UI yet

2. **Extend MuseumPersistence**
   - Add daily review count methods
   - Add daily reset logic with date comparison
   - **Test:** Unit test daily rollover logic
   - **Risk:** Low — follows existing pattern

3. **Update MuseumViewModel state**
   - Add dailyReviewCount to MuseumUiState
   - Add loadDailyCount() method
   - **Test:** StateFlow updates correctly
   - **Risk:** Low — additive change only

### Phase 3: Review Integration
**Goal:** Connect reviews to unlocks

4. **Create DailyReviewTracker service**
   - Implement ChangeManager.Subscriber
   - Wire checkAndResetDaily()
   - Wire incrementDailyCount()
   - Implement random piece selection
   - **Test:** Mock OpChanges → verify unlock events
   - **Risk:** Medium — ChangeManager subscription lifecycle

5. **Integrate DailyReviewTracker with MuseumViewModel**
   - Add initializeDailyTracking()
   - Observe unlockEvents SharedFlow
   - **Test:** End-to-end review → unlock flow
   - **Risk:** Medium — coroutine flow collection

### Phase 4: UI Polish
**Goal:** User-facing feedback

6. **Add daily count UI (optional)**
   - Display dailyReviewCount in MuseumActivity
   - Show progress toward daily goal
   - **Test:** Manual verification
   - **Risk:** Low — UI only

### Phase 5: Completion Trigger
**Goal:** Verify existing celebration works

7. **Test completion flow**
   - Unlock 100th piece manually or via script
   - Verify showCompletionDialog() triggers
   - **Test:** Unlock last piece → dialog appears
   - **Risk:** None — uses existing code

## Anti-Patterns to Avoid

### Anti-Pattern 1: Direct Collection Access in DailyReviewTracker

**What NOT to do:**
```kotlin
// BAD: Direct collection polling
class DailyReviewTracker {
    suspend fun pollReviewCount() {
        CollectionManager.withCol {
            val todayCount = cards.studiedToday() // Doesn't exist
        }
    }
}
```

**Why it's wrong:**
- Collection doesn't track "today's reviews" natively
- Violates single source of truth (ChangeManager exists for this)
- Creates tight coupling to collection internals

**Do this instead:**
```kotlin
// GOOD: Observer pattern via ChangeManager
class DailyReviewTracker : ChangeManager.Subscriber {
    override fun opExecuted(changes: OpChanges, handler: Any?) {
        if (changes.card) incrementDailyCount()
    }
}
```

### Anti-Pattern 2: Caching MuseumViewModel in DailyReviewTracker

**What NOT to do:**
```kotlin
// BAD: Bidirectional coupling
class DailyReviewTracker(
    private val viewModel: MuseumViewModel // DON'T
) {
    fun onReview() {
        viewModel.unlockPiece(randomIndex) // Tight coupling
    }
}
```

**Why it's wrong:**
- Creates circular dependency (ViewModel → Tracker → ViewModel)
- Violates unidirectional data flow
- Lifecycle management becomes complex

**Do this instead:**
```kotlin
// GOOD: Event emission with SharedFlow
class DailyReviewTracker {
    private val _unlockEvents = MutableSharedFlow<Int>()
    val unlockEvents: SharedFlow<Int> = _unlockEvents.asSharedFlow()

    suspend fun onReview() {
        _unlockEvents.emit(randomIndex) // One-way event
    }
}

// ViewModel observes events
viewModelScope.launch {
    dailyTracker.unlockEvents.collect { index ->
        unlockPiece(context, index)
    }
}
```

### Anti-Pattern 3: Synchronous SharedPreferences Writes

**What NOT to do:**
```kotlin
// BAD: Blocking commit() on main thread
fun incrementDailyCount(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit()
        .putInt(KEY_DAILY_COUNT, count + 1)
        .commit() // BLOCKS UI THREAD
}
```

**Why it's wrong:**
- commit() writes synchronously and blocks until complete
- Can cause UI jank on slow storage
- AnkiDroid best practice uses apply()

**Do this instead:**
```kotlin
// GOOD: Asynchronous apply()
fun incrementDailyCount(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit()
        .putInt(KEY_DAILY_COUNT, count + 1)
        .apply() // Asynchronous, non-blocking
}
```

**Source:** [SharedPreferences Best Practices](https://yakivmospan.com/blog/best-practice-shared-preferences/)

### Anti-Pattern 4: Custom Alpha Animation Instead of Paint.alpha

**What NOT to do:**
```kotlin
// BAD: Over-engineered custom shader
private fun drawLockedPiecePng(...) {
    val shader = BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    val paint = Paint().apply {
        setShader(shader)
        setXfermode(PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP))
        // Complex multi-layer compositing...
    }
}
```

**Why it's wrong:**
- Over-engineered for simple transparency
- Harder to understand and maintain
- Slower rendering than built-in alpha

**Do this instead:**
```kotlin
// GOOD: Simple Paint.alpha
private fun drawLockedPiecePng(...) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        alpha = 127 // 50% opacity
    }
    canvas.drawBitmap(bmp, null, tmpPieceRect, paint)
}
```

## Scaling Considerations

### 0-10K daily active users (Current)
- SharedPreferences sufficient for daily counters
- Single-threaded ChangeManager subscription handles load
- In-memory StateFlow for UI updates

### 10K-100K users (Future)
- Consider batching unlock events (unlock N pieces per review batch)
- Monitor ChangeManager subscriber count (WeakReferences cleanup)
- Add analytics for daily unlock patterns

### 100K+ users (Hypothetical)
- Migrate to DataStore for type-safe preferences
- Consider WorkManager for midnight reset (vs in-app checks)
- Add server-side validation for unlock integrity

**Current recommendation:** Keep existing patterns. SharedPreferences and ChangeManager scale to AnkiDroid's user base.

## Testing Strategy

### Unit Tests

**DailyReviewTracker:**
```kotlin
@Test
fun `daily reset clears count when date changes`() = runTest {
    val tracker = DailyReviewTracker(context)
    tracker.incrementDailyCount()
    tracker.incrementDailyCount()

    // Simulate next day
    setSystemDate(tomorrow)
    tracker.checkAndResetDaily()

    assertEquals(0, tracker.getDailyCount())
}
```

**MuseumPersistence:**
```kotlin
@Test
fun `incrementDailyReviewCount returns updated value`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    MuseumPersistence.resetDailyReviewCount(context)

    val count = MuseumPersistence.incrementDailyReviewCount(context)

    assertEquals(1, count)
    assertEquals(1, MuseumPersistence.getDailyReviewCount(context))
}
```

### Integration Tests

**Review → Unlock Flow:**
```kotlin
@Test
fun `answering card triggers puzzle piece unlock`() = runTest {
    val viewModel = MuseumViewModel()
    val tracker = DailyReviewTracker(context)

    // Connect tracker to viewModel
    viewModel.initializeDailyTracking(context)

    // Simulate review answer
    val changes = opChanges { card = true }
    tracker.opExecuted(changes, null)

    // Verify unlock event
    advanceUntilIdle()
    assertTrue(viewModel.uiState.value.unlockedPieces.isNotEmpty())
}
```

### Visual Tests

**Semi-transparent Rendering:**
- Manual verification: Compare locked pieces before/after alpha change
- Screenshot test: Capture PaintingPuzzleView with mix of locked/unlocked pieces
- Alpha validation: Use Robolectric shadow to verify Paint.alpha == 127

## Risks and Mitigations

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| ChangeManager subscription leak | Memory leak | Low | Use WeakReference pattern (already implemented) |
| Midnight rollover missed | Stale daily count | Medium | Check on app launch AND review events |
| Random unlock picks already-unlocked piece | No unlock animation | Low | Filter lockedPieces before random selection |
| Completion dialog triggers multiple times | Annoying UX | Low | hasPlayedBreakAnimation flag pattern (existing) |
| Alpha rendering breaks PNG interlocking | Visual artifacts | Low | Test on multiple devices/API levels |

## Sources

**Official Android Documentation:**
- [Save simple data with SharedPreferences](https://developer.android.com/training/data-storage/shared-preferences)
- [Property Animation Overview](https://developer.android.com/develop/ui/views/animations/prop-animation)

**Technical References:**
- [SharedPreferences Best Practices](https://yakivmospan.com/blog/best-practice-shared-preferences/)
- [Paint.setAlpha Examples](https://www.tabnine.com/code/java/methods/android.graphics.Paint/setAlpha)
- [Android SDK: Drawing with Opacity](https://code.tutsplus.com/tutorials/android-sdk-drawing-with-opacity--mobile-19682)

**AnkiDroid Codebase:**
- Existing ChangeManager.Subscriber pattern (ReviewerViewModel, MuseumViewModel)
- Existing SharedPreferences patterns (MuseumPersistence)
- Existing StateFlow + SharedFlow patterns (all ViewModels)
- Existing ValueAnimator pattern (PaintingPuzzleView.animateUnlock)

---
*Architecture integration research for: Daily puzzle reveal mechanics*
*Researched: 2026-02-12*
