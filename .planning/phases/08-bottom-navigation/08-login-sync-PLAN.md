# Plan: AnkiWeb Login & Sync Integration

## Goal
Add AnkiWeb login and deck sync to the new bottom navigation flow so users can log in during onboarding or from the Home tab, and decks auto-sync on app open.

## Overview
Two workstreams:
1. **Onboarding rework** — Replace TopicSelectionActivity with a Welcome screen offering "Quick Start" or "I have an account" (login + sync)
2. **Home tab sync** — Add sync icon to Home toolbar using existing SyncActionProvider, with auto-sync on app open

---

## Part 1: Onboarding Welcome Screen

### 1.1 Create `WelcomeActivity`
**File:** `AnkiDroid/src/main/java/com/ichi2/anki/ui/onboarding/WelcomeActivity.kt`

- New activity replacing `TopicSelectionActivity` as the first onboarding screen
- Two buttons:
  - **"Quick Start"** → goes directly to `ArtSelectionActivity` (uses default deck)
  - **"I have an account"** → launches `AccountActivity` for result
- On login success (RESULT_OK): trigger sync via `SyncWorker.start()`, then proceed to `ArtSelectionActivity`
- Uses `registerForActivityResult` pattern (same as IntroductionActivity)

### 1.2 Create `activity_welcome.xml`
**File:** `AnkiDroid/src/main/res/layout/activity_welcome.xml`

- museolingo_bg background
- App title/welcome text
- Two Material buttons styled with museolingo_amber
- Simple, clean layout matching existing onboarding style

### 1.3 Update `MainContainerActivity` onboarding redirect
**File:** `AnkiDroid/src/main/java/com/ichi2/anki/MainContainerActivity.kt`

- Change onboarding redirect from `TopicSelectionActivity` → `WelcomeActivity`

### 1.4 Register `WelcomeActivity` in manifest
**File:** `AnkiDroid/src/main/AndroidManifest.xml`

- Add `<activity>` entry for WelcomeActivity

### 1.5 Add string resources
**File:** `AnkiDroid/src/main/res/values/02-strings.xml`

- `welcome_title`: "Welcome to MuseoLingo"
- `welcome_subtitle`: "Learn languages through art"
- `welcome_quick_start`: "Quick Start"
- `welcome_have_account`: "I have an account"
- `welcome_syncing`: "Syncing your decks..."

---

## Part 2: Home Tab Sync Icon

### 2.1 Create Home toolbar menu
**File:** `AnkiDroid/src/main/res/menu/home_toolbar.xml`

- Single menu item `action_sync` with `SyncActionProvider` (same pattern as deck_picker.xml)
```xml
<item
    android:id="@+id/action_sync"
    android:icon="@drawable/ic_sync"
    android:title="@string/button_sync"
    ankidroid:actionProviderClass="com.ichi2.anki.SyncActionProvider"
    ankidroid:showAsAction="always"/>
```

### 2.2 Add Toolbar to `fragment_home.xml`
**File:** `AnkiDroid/src/main/res/layout/fragment_home.xml`

- Add `MaterialToolbar` at top of the layout (above the streak pill row)
- Toolbar hosts the sync menu action

### 2.3 Wire sync into `HomeFragment`
**File:** `AnkiDroid/src/main/java/com/ichi2/anki/ui/home/HomeFragment.kt`

- Set up toolbar with menu inflation (`home_toolbar.xml`)
- Handle `action_sync` menu item click:
  - If not logged in → show `SyncErrorDialog.DIALOG_USER_NOT_LOGGED_IN_SYNC` or launch `AccountActivity`
  - If logged in → call sync via activity
- Delegate actual sync execution to `MainContainerActivity`

### 2.4 Implement `SyncErrorDialogListener` on `MainContainerActivity`
**File:** `AnkiDroid/src/main/java/com/ichi2/anki/MainContainerActivity.kt`

Changes:
- Implement `SyncErrorDialogListener` interface with required methods:
  - `sync(conflict)` — check login, handle metered warning, call `SyncWorker.start()` or foreground `handleNewSync`
  - `loginToSyncServer()` — launch AccountActivity for result
  - `showSyncErrorDialog(type, message)` — show SyncErrorDialog fragment
  - `mediaCheck()` / `integrityCheck()` — delegate to existing flows
- Add `loginForSyncLauncher` using `registerForActivityResult`
- Add `syncOnResume` flag — set true after login success, triggers sync in onResume
- Add `automaticSync()` method — called from `onResume()`, checks conditions (logged in, interval passed, online, not metered) and calls `SyncWorker.start()`
- Handle `INTENT_SYNC_FROM_LOGIN` extra (for post-onboarding sync)

### 2.5 Add Toolbar to `activity_main_container.xml` (optional)
The toolbar lives inside `fragment_home.xml`, not the activity layout. No changes needed to the activity layout.

---

## Part 3: Auto-Sync on App Open

### 3.1 In `MainContainerActivity.onResume()`
- If `syncOnResume` flag is set → clear flag, call `sync()`
- Otherwise → call `automaticSync()` which:
  - Checks `Prefs.isAutoSyncEnabled`
  - Checks `isLoggedIn()`
  - Checks network availability
  - Checks sync interval (10 min minimum)
  - Uses `SyncWorker.start()` for background sync

---

## Execution Order
1. Create `activity_welcome.xml` layout
2. Create `WelcomeActivity.kt`
3. Update `MainContainerActivity.kt` (onboarding redirect + SyncErrorDialogListener + auto-sync)
4. Add `home_toolbar.xml` menu
5. Update `fragment_home.xml` with toolbar
6. Update `HomeFragment.kt` with sync menu handling
7. Update `AndroidManifest.xml`
8. Update `02-strings.xml`
9. Build and test

## Key Design Decisions
- **Use `SyncWorker` for background sync** rather than refactoring `handleNewSync()` (which is a DeckPicker extension). SyncWorker already handles collection sync properly as a background worker.
- **Foreground sync for manual tap** — when user taps sync icon, we need progress UI. We can reuse `SyncWorker` with foreground notification, or show a simple progress dialog.
- **SyncErrorDialog requires activity to implement `SyncErrorDialogListener`** — MainContainerActivity must implement this interface.
- **WelcomeActivity replaces TopicSelectionActivity** as first onboarding screen. TopicSelectionActivity is removed from the flow (ArtSelectionActivity follows directly after Welcome).
