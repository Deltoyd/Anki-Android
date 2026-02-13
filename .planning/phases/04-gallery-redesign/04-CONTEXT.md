# Phase 4: Gallery Redesign - Context

**Gathered:** 2026-02-13
**Status:** Ready for planning

<domain>
## Phase Boundary

Remove the heatmap from the homescreen, expand the masterpiece painting area, and render paintings in two states: active (with puzzle overlay) and locked (blurred/dimmed). Completed paintings are NOT shown in the main gallery — they belong in a future collection/achievement feature. The existing deck list and study UI elements remain alongside the gallery.

</domain>

<decisions>
## Implementation Decisions

### Layout & spacing
- Painting gets the large majority of available space, with visible padding/margins around it (not edge-to-edge)
- Maintain original aspect ratio of paintings — no cropping, use letterbox if needed
- Rounded corners on the painting area (card/frame feel)
- Title and artist name displayed below the painting
- Deck stats (cards due, new, etc.) removed from homescreen — user accesses through other screens
- Existing deck list and study buttons remain on the homescreen alongside the gallery

### Gallery navigation
- Dot indicators below the painting showing current position (iOS-style page dots)
- Gallery wraps around — swiping past the last painting loops to the first
- Only active and locked paintings displayed in the gallery (no completed paintings)
- Always starts on the active painting when homescreen loads
- Swipe left/right to browse paintings

### Transition from current layout
- Permanent layout change — remove heatmap code entirely, not a runtime animation
- Heatmap area is simply removed, painting area expands into the freed space
- No replacement widget in the heatmap's old position (streak pill is a separate phase)

### Claude's Discretion
- Exact padding/margin values
- Blur intensity and dimming level for locked paintings
- Dot indicator styling and position
- Typography for painting title/artist label
- How to handle very wide or very tall paintings within the rounded frame

</decisions>

<specifics>
## Specific Ideas

- Gallery should feel like browsing an art collection — clean, focused on the masterpiece
- Active painting is always first when opening the homescreen
- Locked paintings give a sense of "what's coming next" without revealing too much (blurred/dimmed)

</specifics>

<deferred>
## Deferred Ideas

- Completed paintings collection / achievement gallery — separate feature/phase for displaying fully revealed artworks the user has earned
- Streak pill widget on homescreen — Phase 6

</deferred>

---

*Phase: 04-gallery-redesign*
*Context gathered: 2026-02-13*
