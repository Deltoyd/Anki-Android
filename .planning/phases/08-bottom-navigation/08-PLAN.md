# Phase 08: Bottom Navigation Redesign

## Overview

Replace the current single-screen Museum homescreen with a 3-tab bottom navigation architecture:
- **Home** — Today's masterpiece puzzle (current Museum screen, simplified top bar)
- **Library** — Your decks list with edit functionality + shared decks browser
- **Settings** — All settings, sync, about, etc.

## Reference

Based on: `/Users/rolandharper/Projects/Ikasi/References/2026-02-13 17.07.21.jpg`

Key design elements:
- 3-item bottom navigation (Home, Library, Settings)
- Minimal top bar on Home (streak only)
- Library top bar with title + edit button
- Deck list with chevron indicators
- FAB for adding decks in Library

## Requirements

### Navigation Structure
- [ ] **NAV-01**: App has bottom navigation with 3 tabs: Home, Library, Settings
- [ ] **NAV-02**: Tab state persists during session (doesn't reset on tab switch)
- [ ] **NAV-03**: Back button behavior follows standard bottom nav patterns

### Home Tab
- [ ] **HOME-01**: Top bar shows only streak indicator (🔥 N) on left
- [ ] **HOME-02**: Remove deck selector from Home (moves to Library)
- [ ] **HOME-03**: Remove menu button from Home
- [ ] **HOME-04**: Remove app title from Home top bar
- [ ] **HOME-05**: Masterpiece gallery and review button remain unchanged

### Library Tab
- [ ] **LIB-01**: Top bar shows "Library" title on left
- [ ] **LIB-02**: Top bar has edit button (✏️) on right
- [ ] **LIB-03**: Displays list of user's decks
- [ ] **LIB-04**: Each deck row shows: name, cards due count, chevron (>)
- [ ] **LIB-05**: Tapping deck row opens deck for study/review
- [ ] **LIB-06**: FAB (+) button to create new deck or import
- [ ] **LIB-07**: Edit mode allows reordering/deleting decks
- [ ] **LIB-08**: Section for browsing shared decks (AnkiWeb)

### Settings Tab
- [ ] **SET-01**: Top bar shows "Settings" title
- [ ] **SET-02**: Contains all existing settings categories
- [ ] **SET-03**: Includes sync, about, advanced options
- [ ] **SET-04**: No "Premium" button (not in our app)

## Architecture

### Option A: Single Activity + Fragments (Recommended)
```
MainContainerActivity
├── BottomNavigationView
├── FragmentContainerView
│   ├── HomeFragment (current Museum content)
│   ├── LibraryFragment (deck list)
│   └── SettingsFragment (preferences)
```

**Pros:**
- Shared BottomNavigationView
- Fragment state preservation
- Standard Android pattern
- Easier shared ViewModel access

**Cons:**
- Need to refactor MuseumActivity content into HomeFragment

### Option B: Navigation Component
```
MainContainerActivity
├── BottomNavigationView
├── NavHostFragment
│   ├── home_graph
│   ├── library_graph
│   └── settings_graph
```

**Pros:**
- Modern architecture
- Built-in back stack handling
- Type-safe navigation

**Cons:**
- More setup complexity
- May be overkill for 3 simple tabs

### Decision: Option A (Single Activity + Fragments)

Simpler implementation, sufficient for 3 static tabs.

## Implementation Plan

### Wave 1: Foundation (Plans 08-01, 08-02)

**08-01: Create MainContainerActivity with BottomNavigationView**
- Create `MainContainerActivity` with `BottomNavigationView` and `FragmentContainerView`
- Create bottom navigation menu resource (3 items)
- Create bottom navigation icons (home, library, settings)
- Wire tab selection to fragment switching
- Update AndroidManifest to launch MainContainerActivity

**08-02: Extract HomeFragment from MuseumActivity**
- Create `HomeFragment` with Museum content
- Move gallery, streak pill, review button logic to fragment
- Simplify top bar to streak-only
- Share MuseumViewModel via activityViewModels()
- Deprecate/remove standalone MuseumActivity

### Wave 2: Library Tab (Plans 08-03, 08-04)

**08-03: Create LibraryFragment with Deck List**
- Create `LibraryFragment` with RecyclerView
- Create `DeckListAdapter` with deck row layout
- Create `LibraryViewModel` to load decks
- Wire deck tap to open study options
- Add top bar with "Library" title + edit button placeholder

**08-04: Add Library Edit Mode and FAB**
- Implement edit mode (tap edit → show delete/reorder controls)
- Add FAB with options: Create deck, Import deck
- Wire create deck dialog
- Wire import to existing import flow

### Wave 3: Settings & Polish (Plans 08-05, 08-06)

**08-05: Create SettingsContainerFragment**
- Create `SettingsContainerFragment` wrapping existing preferences
- Reuse `HeaderFragment` or equivalent preference structure
- Add top bar with "Settings" title
- Ensure all existing settings accessible

**08-06: Polish and Integration**
- Update app entry point (InitialActivity → MainContainerActivity)
- Handle deep links and shortcuts
- Test navigation flows
- Remove deprecated activities/code
- Update onboarding flow if needed

## File Structure

### New Files
```
AnkiDroid/src/main/java/com/ichi2/anki/
├── MainContainerActivity.kt
├── ui/
│   ├── home/
│   │   └── HomeFragment.kt
│   ├── library/
│   │   ├── LibraryFragment.kt
│   │   ├── LibraryViewModel.kt
│   │   ├── DeckListAdapter.kt
│   │   └── DeckItemViewHolder.kt
│   └── settings/
│       └── SettingsContainerFragment.kt

AnkiDroid/src/main/res/
├── layout/
│   ├── activity_main_container.xml
│   ├── fragment_home.xml
│   ├── fragment_library.xml
│   ├── fragment_settings_container.xml
│   └── item_deck_row.xml
├── menu/
│   └── bottom_navigation.xml
└── drawable/
    ├── ic_home.xml
    ├── ic_library.xml
    └── ic_settings.xml
```

### Modified Files
```
AndroidManifest.xml — Update launcher activity
MuseumActivity.kt — Deprecate or remove
InitialActivity.kt — Update navigation target
```

## Visual Specifications

### Bottom Navigation Bar
```
┌─────────────────────────────────────────────────────────────┐
│   🏠        📚         ⚙️    │
│  Home    Library    Settings │
└─────────────────────────────────────────────────────────────┘
```
- Height: 80dp (Material 3 standard)
- Background: #FFFFFF (white)
- Active color: #2196F3 (blue) or museolingo_amber
- Inactive color: #9E9E9E (gray)
- Icons: 24dp, labels below

### Home Top Bar
```
┌─────────────────────────────────────────────────────────────┐
│ 🔥 5                                                        │
└─────────────────────────────────────────────────────────────┘
```
- Height: 56dp
- Background: museolingo_bg (#FAF8F4)
- Streak: left-aligned, tappable (opens StreakBottomSheet)

### Library Top Bar
```
┌─────────────────────────────────────────────────────────────┐
│ Library                                                ✏️  │
└─────────────────────────────────────────────────────────────┘
```
- Height: 56dp
- Background: #FFFFFF or museolingo_bg
- Title: 20sp, bold, left
- Edit: icon button, right

### Deck Row
```
┌─────────────────────────────────────────────────────────────┐
│ Ultimate Geography v4.1                                  >  │
│ Cards for today: 4                                          │
├─────────────────────────────────────────────────────────────┤
│ Japanese Core 2000                                       >  │
│ Cards for today: 12                                         │
└─────────────────────────────────────────────────────────────┘
```
- Row height: 72dp
- Title: 16sp, bold, black
- Subtitle: 14sp, regular, gray
- Divider: 1dp, gray, full-width

### Settings Top Bar
```
┌─────────────────────────────────────────────────────────────┐
│ Settings                                                    │
└─────────────────────────────────────────────────────────────┘
```
- Same style as Library, no edit button

## Success Criteria

1. App launches to MainContainerActivity with Home tab selected
2. Bottom navigation switches between 3 tabs without data loss
3. Home shows simplified streak-only top bar with masterpiece
4. Library shows deck list with edit button and FAB
5. Settings shows all existing preferences
6. Back button exits app from any primary tab
7. Build compiles successfully
8. No regressions in review/study functionality

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Breaking existing DeckPicker functionality | Library uses new code, DeckPicker remains for advanced features |
| ViewModel sharing issues | Use activityViewModels() for shared state |
| Deep link/shortcut breakage | Test shortcuts, update manifest carefully |
| Onboarding flow disruption | Update onboarding to navigate to MainContainerActivity |

## Estimated Timeline

| Plan | Effort | Description |
|------|--------|-------------|
| 08-01 | 30min | MainContainerActivity + bottom nav |
| 08-02 | 45min | HomeFragment extraction |
| 08-03 | 45min | LibraryFragment + deck list |
| 08-04 | 30min | Edit mode + FAB |
| 08-05 | 30min | SettingsContainerFragment |
| 08-06 | 30min | Polish + integration |

**Total: ~3.5 hours**

## Dependencies

- Phase 6 (StreakBottomSheet) — used in Home
- Phase 7 (Heatmap Views) — used in StreakBottomSheet
- Existing DeckPicker code — reference for deck operations
- Existing Preferences code — reused in Settings tab
