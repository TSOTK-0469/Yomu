# Remove the misleading album-open blocker

Type: bug
Status: resolved

Every Album open currently sets `openingAlbum`, which immediately renders a modal dialog saying images are being read and sorted. Indexed Albums are actually read from Room; only a legacy Album with no image rows performs a compatibility scan.

## Comments

- Decision: suppress feedback for the first 300 ms, then show non-blocking opening feedback.
- Decision: preserve explicit, cancellable feedback for genuine index backfill.
- Decision: delayed indexed-open feedback is shown on the selected Album card and does not block the library.
- Decision: the modal appears only for a genuinely absent image index and says that a one-time index is being built.
- Decision: duplicate taps are ignored; navigating to another library destination cancels a pending open.
- Acceptance: automated coverage distinguishes fast indexed open, delayed indexed open, and missing-index backfill states.

## Answer

Implemented a 300 ms feedback policy. Indexed Albums open silently when fast and show only a card-local spinner when slow. Only a genuinely absent image index opens the cancellable one-time indexing dialog; duplicate opens are ignored and leaving the library cancels pending work.
