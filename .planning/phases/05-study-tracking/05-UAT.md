---
status: testing
phase: 05-study-tracking
source: 05-01-SUMMARY.md, 05-02-SUMMARY.md
started: 2026-02-13T14:50:00Z
updated: 2026-02-13T14:50:00Z
---

## Current Test

number: 1
name: App launches without crash
expected: |
  Open AnkiDroid. The homescreen loads showing the gallery with paintings. No crash or error dialog appears. The MuseumViewModel now queries revlog on startup — this confirms the database integration doesn't break app launch.
awaiting: user response

## Tests

### 1. App launches without crash
expected: Open AnkiDroid. The homescreen loads showing the gallery with paintings. No crash or error on startup.
result: [pending]

### 2. Gallery still displays correctly
expected: The gallery area shows paintings as before — active painting with puzzle overlay, locked paintings blurred. Swiping between paintings works. No visual regressions from the UiState changes.
result: [pending]

### 3. No errors after returning from study session
expected: Start a review session (review at least 1 card), then return to the homescreen. The app returns to the homescreen without crash. The ViewModel's refreshData now queries revlog — this confirms the refresh path works.
result: [pending]

## Summary

total: 3
passed: 0
issues: 0
pending: 3
skipped: 0

## Gaps

[none yet]
