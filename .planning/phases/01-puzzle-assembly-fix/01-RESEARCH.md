# Phase 1: Puzzle Assembly Fix - Research

**Researched:** 2026-02-12
**Domain:** Android custom View rendering with drawable resources and Kotlin
**Confidence:** HIGH

## Summary

Phase 1 replaces 9 puzzle piece PNG assets with 14 new gray gradient pieces and updates the placement logic in `PaintingPuzzleView.kt` to display locked pieces as a unified, fully-assembled jigsaw with proper tab/hole interlocking. The implementation involves two primary tasks: (1) asset file operations (copy 14 new PNGs, delete 9 old ones), and (2) code changes to the `pieceBitmaps` map and `getPieceType()` function.

The research confirms that the existing codebase already uses the correct patterns (lazy initialization with `by lazy`, `BitmapFactory.decodeResource`, and standard Android drawable naming conventions). The new assets are verified as valid PNG files with proper naming that follows Android resource conventions (lowercase, underscore-separated). The placement algorithm requires a straightforward when-expression update to implement checkerboard alternation using `(row+col) % 2`.

**Primary recommendation:** Follow the existing codebase patterns exactly. Use `by lazy` for the `pieceBitmaps` map, maintain the exact naming and structure patterns already in use, and extend the `getPieceType()` function with additional cases for variant selection.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Android SDK (Canvas/Bitmap) | API 21+ | Custom View rendering | Core Android graphics framework for custom UI |
| Kotlin stdlib | Per project | Language features (lazy, when) | Project standard language |
| BitmapFactory | Android SDK | PNG decoding from resources | Standard Android API for bitmap loading |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Paint (android.graphics) | Android SDK | Drawing styling and filtering | Already used for bitmap filtering |
| RectF (android.graphics) | Android SDK | Drawable bounds calculation | Already used for piece positioning |

### Alternatives Considered
None. This phase uses only standard Android framework APIs already in use by the existing code.

**Installation:**
No additional dependencies required. All necessary APIs are part of the Android SDK already in use.

## Architecture Patterns

### Recommended Project Structure
```
AnkiDroid/src/main/
├── res/
│   └── drawable-nodpi/         # Density-independent PNG resources
│       ├── puzzle_corner_tl.png
│       ├── puzzle_corner_tr.png
│       ├── puzzle_corner_bl.png
│       ├── puzzle_corner_br.png
│       ├── puzzle_border_top_1.png
│       ├── puzzle_border_top_2.png
│       ├── puzzle_border_bottom_1.png
│       ├── puzzle_border_bottom_2.png
│       ├── puzzle_border_left_1.png
│       ├── puzzle_border_left_2.png
│       ├── puzzle_border_right_1.png
│       ├── puzzle_border_right_2.png
│       ├── puzzle_middle_1.png
│       └── puzzle_middle_2.png
└── java/com/ichi2/anki/ui/museum/
    └── PaintingPuzzleView.kt    # Custom View with piece rendering
```

### Pattern 1: Lazy Resource Map Loading
**What:** Initialize drawable resources lazily using Kotlin's `by lazy` delegate
**When to use:** When loading multiple bitmap resources that should be decoded once and reused
**Example:**
```kotlin
// Source: Existing pattern in PaintingPuzzleView.kt:47-59
private val pieceBitmaps by lazy {
    mapOf(
        "corner_tl" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_corner_tl),
        "corner_tr" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_corner_tr),
        // ... additional entries
    )
}
```

**Benefits:**
- Thread-safe by default (synchronized mode)
- Bitmaps decoded only once on first access
- Prevents allocations in onDraw() (critical for performance)
- Memory efficient (lazy evaluation)

### Pattern 2: Checkerboard Alternation with Modulo
**What:** Use `(row + col) % 2` to alternate between two variants in a grid pattern
**When to use:** When you need adjacent grid cells to use different variants (checkerboard pattern)
**Example:**
```kotlin
val variant = if ((row + col) % 2 == 0) "variant_1" else "variant_2"
```

**Why this works:**
- Row 0, Col 0: (0+0) % 2 = 0 → variant 1
- Row 0, Col 1: (0+1) % 2 = 1 → variant 2
- Row 1, Col 0: (1+0) % 2 = 1 → variant 2
- Row 1, Col 1: (1+1) % 2 = 0 → variant 1

Result: Perfect checkerboard where no two adjacent cells share the same variant.

### Pattern 3: When Expression for Grid Position Mapping
**What:** Use Kotlin's `when` expression with multiple conditions to map grid coordinates to piece types
**When to use:** When mapping grid positions to different categories (corners, borders, interior)
**Example:**
```kotlin
// Source: Existing pattern in PaintingPuzzleView.kt:422-436
private fun getPieceType(row: Int, col: Int): String =
    when {
        row == 0 && col == 0 -> "corner_tl"
        row == 0 && col == COLS - 1 -> "corner_tr"
        row == ROWS - 1 && col == 0 -> "corner_bl"
        row == ROWS - 1 && col == COLS - 1 -> "corner_br"
        row == 0 -> "border_top"
        row == ROWS - 1 -> "border_bottom"
        col == 0 -> "border_left"
        col == COLS - 1 -> "border_right"
        else -> "middle"
    }
```

**Extension strategy for variants:**
Replace single piece type strings with variant selection logic:
```kotlin
row == 0 -> if ((row + col) % 2 == 0) "border_top_1" else "border_top_2"
```

### Pattern 4: Drawable Resource Naming
**What:** Use lowercase letters, numbers, and underscores only; no hyphens or spaces
**When to use:** All Android drawable resource files
**Example:**
```
Correct:   puzzle_border_top_1.png
Incorrect: puzzle-border-top-1.png (hyphens not allowed)
Incorrect: puzzleBorderTop1.png (camelCase not allowed)
Incorrect: Puzzle_Border_Top_1.png (uppercase not allowed)
```

**Critical:** The new asset files use hyphens (e.g., `top-middle-1.png`) which MUST be renamed to underscores (e.g., `puzzle_border_top_1.png`) during the copy operation.

### Anti-Patterns to Avoid
- **Allocating objects in onDraw():** Never create new Paint, Path, or Bitmap objects inside onDraw() as this causes garbage collection and frame drops
- **Loading bitmaps in onDraw():** Always load bitmaps outside of onDraw() (use lazy initialization)
- **Calling invalidate() unnecessarily:** Only invalidate when state actually changes
- **Using wrong density folders:** PNGs in drawable-nodpi should not exist in other density folders (mdpi, hdpi, etc.)

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Bitmap loading from resources | Custom file reading/decoding | `BitmapFactory.decodeResource()` | Handles all PNG formats, color profiles, density scaling, and memory optimization |
| Lazy initialization | Manual singleton pattern with double-checked locking | Kotlin `by lazy` | Built-in thread safety, cleaner syntax, compiler-optimized |
| Grid coordinate mapping | Nested if-else chains or lookup tables | `when` expression with conditions | More readable, exhaustive checking, compiler-optimized |
| Resource caching | Manual Map with initialization flags | `by lazy { mapOf(...) }` | Automatic single initialization guarantee, cleaner code |

**Key insight:** Android framework APIs and Kotlin language features already solve all common patterns needed for this phase. Custom solutions would add complexity without benefit.

## Common Pitfalls

### Pitfall 1: Resource Naming Violations
**What goes wrong:** Asset files with hyphens (e.g., `top-middle-1.png`) cause Android resource compilation errors
**Why it happens:** Android's resource naming convention only allows lowercase letters, numbers, and underscores
**How to avoid:** Rename all files during copy operation to replace hyphens with underscores and add `puzzle_` prefix
**Warning signs:** Build errors like "Invalid resource directory name" or "invalid symbol"

**Mapping required:**
```
Source                    → Destination
top-left-corner.png       → puzzle_corner_tl.png
top-right-corner.png      → puzzle_corner_tr.png
bottom-left-corner.png    → puzzle_corner_bl.png
bottom-right-corner.png   → puzzle_corner_br.png
top-middle-1.png          → puzzle_border_top_1.png
top-middle-2.png          → puzzle_border_top_2.png
bottom-middle-1.png       → puzzle_border_bottom_1.png
bottom-middle-2.png       → puzzle_border_bottom_2.png
left-middle-1.png         → puzzle_border_left_1.png
left-middle-2.png         → puzzle_border_left_2.png
right-middle-1.png        → puzzle_border_right_1.png
right-middle-2.png        → puzzle_border_right_2.png
only-middle.png           → puzzle_middle_1.png
only-middle-2.png         → puzzle_middle_2.png
```

### Pitfall 2: Forgetting to Remove Old Assets
**What goes wrong:** Old 9 puzzle piece PNGs remain in drawable-nodpi, wasting APK size and potentially causing confusion
**Why it happens:** Focus on adding new files without cleaning up replaced resources
**How to avoid:** Explicitly delete all 9 old files as part of asset replacement task
**Warning signs:** APK size doesn't decrease as expected, unused resource warnings in lint

**Files to delete:**
```
puzzle_corner_tl.png
puzzle_corner_tr.png
puzzle_corner_bl.png
puzzle_corner_br.png
puzzle_border_top.png
puzzle_border_bottom.png
puzzle_border_left.png
puzzle_border_right.png
puzzle_middle.png
```

### Pitfall 3: Incorrect Variant Key Naming
**What goes wrong:** `pieceBitmaps` map uses keys like `"border_top_1"` but `getPieceType()` returns `"top_border_1"` causing null bitmap references
**Why it happens:** Inconsistent naming between map keys and function return values
**How to avoid:** Maintain exact naming convention throughout: `{category}_{position}_{variant}` format
**Warning signs:** Locked pieces not rendering, NullPointerException when accessing bitmap

**Consistent naming pattern:**
```
Category: corner, border, middle
Position: tl, tr, bl, br, top, bottom, left, right (omitted for middle)
Variant: 1, 2 (omitted for corners)

Examples:
"corner_tl"      ✓ (no variant for corners)
"border_top_1"   ✓
"middle_1"       ✓
"top_border_1"   ✗ (wrong order)
"border_top"     ✗ (missing variant)
```

### Pitfall 4: Off-by-One Errors in Grid Boundaries
**What goes wrong:** Corner detection uses `col == 10` instead of `col == 9` (COLS - 1), causing corners to never match
**Why it happens:** Confusion between grid size (10) and maximum index (9)
**How to avoid:** Always use `COLS - 1` and `ROWS - 1` for boundary comparisons, never hardcoded values
**Warning signs:** Corners rendering as border pieces, visual inspection shows wrong piece types at grid corners

**Correct boundary checks:**
```kotlin
row == 0              // Top row (index 0)
row == ROWS - 1       // Bottom row (index 9 for 10x10 grid)
col == 0              // Left column (index 0)
col == COLS - 1       // Right column (index 9 for 10x10 grid)
```

### Pitfall 5: Lazy Initialization with Side Effects
**What goes wrong:** Using `by lazy` with code that depends on mutable state or has side effects
**Why it happens:** Misunderstanding that lazy blocks execute exactly once and cache the result
**How to avoid:** Only use lazy initialization for immutable resource loading that doesn't depend on external state
**Warning signs:** Resources not updating when expected, stale cached values

**Safe (immutable resource loading):**
```kotlin
private val pieceBitmaps by lazy {
    mapOf("corner_tl" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_corner_tl))
}
```

**Unsafe (depends on mutable state):**
```kotlin
private val currentPiece by lazy {
    pieceBitmaps[getCurrentType()]  // Don't do this - getCurrentType() called only once
}
```

## Code Examples

Verified patterns from official sources:

### Lazy Map Initialization for Drawable Resources
```kotlin
// Source: PaintingPuzzleView.kt:47-59 (existing pattern)
private val pieceBitmaps by lazy {
    mapOf(
        "corner_tl" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_corner_tl),
        "corner_tr" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_corner_tr),
        "corner_bl" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_corner_bl),
        "corner_br" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_corner_br),
        "border_top_1" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_border_top_1),
        "border_top_2" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_border_top_2),
        "border_bottom_1" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_border_bottom_1),
        "border_bottom_2" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_border_bottom_2),
        "border_left_1" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_border_left_1),
        "border_left_2" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_border_left_2),
        "border_right_1" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_border_right_1),
        "border_right_2" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_border_right_2),
        "middle_1" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_middle_1),
        "middle_2" to BitmapFactory.decodeResource(resources, R.drawable.puzzle_middle_2),
    )
}
```

### Grid Position to Piece Type Mapping with Variant Alternation
```kotlin
// Source: PaintingPuzzleView.kt:422-436 (pattern to extend)
private fun getPieceType(row: Int, col: Int): String =
    when {
        row == 0 && col == 0 -> "corner_tl"
        row == 0 && col == COLS - 1 -> "corner_tr"
        row == ROWS - 1 && col == 0 -> "corner_bl"
        row == ROWS - 1 && col == COLS - 1 -> "corner_br"
        row == 0 -> {
            val variant = if ((row + col) % 2 == 0) 1 else 2
            "border_top_$variant"
        }
        row == ROWS - 1 -> {
            val variant = if ((row + col) % 2 == 0) 1 else 2
            "border_bottom_$variant"
        }
        col == 0 -> {
            val variant = if ((row + col) % 2 == 0) 1 else 2
            "border_left_$variant"
        }
        col == COLS - 1 -> {
            val variant = if ((row + col) % 2 == 0) 1 else 2
            "border_right_$variant"
        }
        else -> {
            val variant = if ((row + col) % 2 == 0) 1 else 2
            "middle_$variant"
        }
    }
```

### Drawing Locked Piece with Bitmap from Map
```kotlin
// Source: PaintingPuzzleView.kt:396-417 (existing pattern - no changes needed)
private fun drawLockedPiecePng(
    canvas: Canvas,
    row: Int,
    col: Int,
    left: Float,
    top: Float,
) {
    val pieceType = getPieceType(row, col)
    val bmp = pieceBitmaps[pieceType] ?: return  // Null-safe lookup

    val tabW = pieceWidth * TAB_OVERFLOW
    val tabH = pieceHeight * TAB_OVERFLOW

    tmpPieceRect.set(
        left - if (col > 0) tabW else 0f,
        top - if (row > 0) tabH else 0f,
        left + pieceWidth + if (col < COLS - 1) tabW else 0f,
        top + pieceHeight + if (row < ROWS - 1) tabH else 0f,
    )

    canvas.drawBitmap(bmp, null, tmpPieceRect, pieceBitmapPaint)
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| 9 static piece types | 14 pieces with 2 variants | This phase | Proper tab/hole interlocking in assembled puzzle |
| No variant alternation | Checkerboard alternation via (row+col) % 2 | This phase | Adjacent pieces complement each other |
| Hyphenated filenames (source assets) | Underscore-separated resource names | This phase | Compliance with Android naming conventions |

**Deprecated/outdated:**
- None. All patterns used are current Android SDK best practices as of 2026.

## Open Questions

1. **Should bitmap memory be considered for optimization?**
   - What we know: 14 small PNG files (1-2KB each), total ~25KB, decoded to RGBA bitmaps
   - What's unclear: Whether decoded bitmap memory (14 × ~77×100 pixels × 4 bytes) warrants optimization
   - Recommendation: No optimization needed. Total decoded memory ~425KB is negligible for modern devices. Lazy initialization already provides sufficient memory efficiency.

2. **Should old assets be verified as unused before deletion?**
   - What we know: The 9 old assets match exactly the current `pieceBitmaps` map keys
   - What's unclear: Whether any other code references these drawables
   - Recommendation: Perform grep search for R.drawable.puzzle_* references before deletion to verify they're only used in PaintingPuzzleView.kt

## Sources

### Primary (HIGH confidence)
- AnkiDroid codebase (verified by direct file inspection)
  - `/AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/PaintingPuzzleView.kt` - existing patterns for lazy bitmap loading, when expressions, and drawable resource usage
  - `/AnkiDroid/src/main/res/drawable-nodpi/` - current asset structure
  - `CLAUDE.md` - AnkiDroid project conventions and architecture patterns
- New PNG assets verified at `/Users/rolandharper/Projects/Ikasi/Puzzle-shapes/New-gray-radient-puzzle/` (14 files, valid PNG format)
- Android official documentation
  - [Android Canvas API reference](https://developer.android.com/reference/android/graphics/Canvas)

### Secondary (MEDIUM confidence)
- [Android drawable-nodpi resource qualifiers](https://commonsware.com/blog/2015/12/21/nodpi-anydpi-wtf.html) - density-independent resource usage
- [Android resource naming conventions](https://github.com/leapfrogtechnology/android-guidelines/blob/master/ResourcesGuidelines.md) - lowercase, underscore-separated naming
- [Kotlin lazy initialization guide](https://www.baeldung.com/kotlin/lazy-initialization) - thread-safe lazy patterns
- [Android custom View performance optimization](https://stuff.mit.edu/afs/sipb/project/android/docs/training/custom-views/optimizing-view.html) - avoiding allocations in onDraw()
- [Android Canvas clipping best practices](https://google-developer-training.github.io/android-developer-advanced-course-practicals/unit-5-advanced-graphics-and-views/lesson-11-canvas/11-1c-p-apply-clipping-to-a-canvas/11-1c-p-apply-clipping-to-a-canvas.html) - clipPath usage (not directly applicable but confirms existing implementation is correct)

### Tertiary (LOW confidence)
None. All research findings were verified against primary sources (codebase inspection) or official Android documentation.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All APIs verified in existing codebase and Android SDK documentation
- Architecture: HIGH - Patterns already in use and verified by code inspection
- Pitfalls: HIGH - Verified through Android resource naming documentation and common Android development issues
- Asset mapping: HIGH - Verified by inspecting actual source files and required naming conventions

**Research date:** 2026-02-12
**Valid until:** 2026-03-12 (30 days - stable Android SDK patterns)
