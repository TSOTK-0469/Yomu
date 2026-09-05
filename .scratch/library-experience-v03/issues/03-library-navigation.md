# Clarify the Bookshelf drawer and Mount Source management

Type: feature
Status: resolved

The drawer mixes Bookshelves with management actions without section labels. Mount Source management uses a fixed-height bottom sheet, crowded trailing actions, and ungrouped Hidden Albums.

## Comments

- Decision: retain the drawer, split Bookshelves from management destinations, use compact consistent rows, and pin management destinations at the bottom.
- Decision: Mount Source management becomes a full-screen destination.
- Clarify count labels, state indicators, hidden-Album grouping, and destructive actions.
- Decision: use uniform 40 dp cover slots, explicit Album-count labels, aligned rows, and overflow menus for custom Bookshelves.
- Decision: show Mount Source path, state, mode, visible/hidden counts, and last successful refresh time.
- Decision: group Hidden Albums under a collapsed row for each Mount Source.
- Decision: show reauthorization as the primary recovery action and move refresh/unmount into an overflow menu.
- Decision: use a light paper-yellow background with white content surfaces.
- Decision: light mode uses low-saturation paper yellow, dark grey-brown text, and muted amber selection. Dark mode follows the system with warm charcoal surfaces.
- Decision: successful mount, refresh, and reauthorization update the last refresh time; failures preserve it and show a separate status.
- Decision: upgraded Mount Sources show unknown refresh time without automatic scanning.
- Decision: the drawer keeps its header and Bookshelf add action above a scrollable list, while Mount Source management and Settings remain fixed at the bottom.
- Decision: long paths use a two-line middle ellipsis and can reveal and copy their full value.
- Decision: empty states distinguish no Mount Sources, empty Bookshelf, and no search results with contextual actions.
- Decision: Room v3 stores nullable last-successful-refresh time without forcing an upgrade scan.

## Answer

Redesigned the drawer with a fixed Bookshelf section and bottom management destinations. Mount management is now full screen with source cards, path reveal/copy, access and mode status, visible/hidden counts, successful-refresh time, reauthorization, overflow actions, and collapsible Hidden Albums.
