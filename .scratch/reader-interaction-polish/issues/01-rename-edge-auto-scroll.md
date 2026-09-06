# Auto-scroll long Album rename text

Type: bug
Status: resolved

Dragging the insertion cursor or a selection boundary to an edge of the Album rename field does not reveal text outside the visible range.

## Comments

- Support edge auto-scroll for both insertion and selection handles.
- Do not change the dialog's focus or keyboard-opening behavior.

## Answer

Migrated the rename input to the state-based Material text field with an explicit horizontal scroll state. The field retains its 100-character limit and single-line behavior while using the current text engine's insertion and selection edge auto-scroll handling.
