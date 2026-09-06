# Reader interaction polish

Improve three touch interactions without changing Yomu's persisted library model or mount behavior.

## Rename field

- A long Album display name remains editable beyond the currently visible text range.
- Dragging either the insertion cursor or either selection boundary toward a horizontal edge automatically scrolls the field.
- Scrolling accelerates toward the edge and stops when the drag stops or leaves the edge.
- Opening the dialog does not force a new focus or keyboard policy.

## Zoomed image movement

- Direct dragging remains 1:1 with the finger.
- Releasing a faster drag produces a longer inertial movement.
- A new touch interrupts inertia immediately.
- Movement is clamped to the displayed bitmap's real scaled bounds; reaching an edge stops inertia without changing pages or exposing avoidable black space.

## Reader progress slider

- The visible track is 3 dp high with rounded ends.
- The thumb is a 14 dp circle.
- Existing theme colors, progress semantics, page seeking, and a comfortable touch target are retained.

## Out of scope

- Android's system directory picker remains unchanged. Yomu will not request broad all-files access.
- Reading Position continues to identify an image, not a fractional position within an image.
