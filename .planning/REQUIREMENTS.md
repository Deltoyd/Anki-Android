# Requirements: Museum Puzzle Piece Display Fix

**Defined:** 2026-02-12
**Core Value:** When all pieces are locked, the puzzle must look like a single cohesive, fully-assembled jigsaw in gray — orderly and waiting to be unlocked.

## v1 Requirements

### Assets

- [ ] **ASSET-01**: Copy 14 new gray gradient PNGs into `drawable-nodpi/` with proper naming
- [ ] **ASSET-02**: Remove old 9 puzzle piece PNGs that are no longer used

### Placement

- [ ] **PLAC-01**: 4 corner pieces placed at exactly (0,0), (0,9), (9,0), (9,9)
- [ ] **PLAC-02**: Border variants alternate along edges using `(row+col) % 2`
- [ ] **PLAC-03**: Interior variants alternate in checkerboard pattern using `(row+col) % 2`

### Rendering

- [ ] **REND-01**: `pieceBitmaps` map loads all 14 new drawable resources
- [ ] **REND-02**: `getPieceType()` returns correct variant key for each grid position
- [ ] **REND-03**: Visual result is one unified gray puzzle with proper interlocking

## v2 Requirements

(None — focused fix)

## Out of Scope

| Feature | Reason |
|---------|--------|
| Changing unlocked piece shapes (PuzzlePiecePathGenerator) | User confirmed locked-only scope |
| Changing puzzle grid dimensions | Stays 10x10, not requested |
| Museum layout, gallery, or animation changes | Not requested |
| Unlock/reward mechanic changes | Not requested |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| ASSET-01 | — | Pending |
| ASSET-02 | — | Pending |
| PLAC-01 | — | Pending |
| PLAC-02 | — | Pending |
| PLAC-03 | — | Pending |
| REND-01 | — | Pending |
| REND-02 | — | Pending |
| REND-03 | — | Pending |

**Coverage:**
- v1 requirements: 8 total
- Mapped to phases: 0
- Unmapped: 8 ⚠️

---
*Requirements defined: 2026-02-12*
*Last updated: 2026-02-12 after initial definition*
