# Add bounded inertial movement to zoomed images

Type: enhancement
Status: resolved

Panning a zoomed image stops with the finger and feels slow during fast navigation.

## Comments

- Preserve 1:1 direct dragging and derive inertia from release velocity.
- Stop at the real displayed-image bounds and never turn the page from a zoomed fling.
- A new touch cancels active inertia.

## Answer

Added release-velocity tracking and platform decay animation for one-finger zoomed-image drags. Panning and inertia now share fit-aware bitmap bounds, and any new touch cancels the active fling.
