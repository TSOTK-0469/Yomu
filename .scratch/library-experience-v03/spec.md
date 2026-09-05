# Library experience v0.3

Status: resolved

Improve repeat Album opening, Album naming, Bookshelf navigation, Mount Source management, and home-grid density without weakening the persisted-index boundary recorded in ADR-0002.

## Confirmed facts

- Indexed Albums open from Room and do not enumerate or naturally sort their directory.
- The current UI always shows a blocking dialog whose stale text says that images are being read and sorted.
- Album names currently come from source directory names and are overwritten on refresh.
- The Bookshelf drawer has no section hierarchy, while Mount Source management is constrained to a fixed-height bottom sheet.
- The home grid uses an adaptive 154 dp minimum width and fixed 214 dp cover height, which normally produces two large columns on phones.

## Resolved decision areas

- Feedback shown while opening an Album.
- Display-name versus physical-directory rename semantics.
- Navigation structure for Bookshelves, management, and Settings.
- Mount Source management presentation.
- Grid-density control model and defaults.

## Decisions — round 1

- Indexed Album opens show nothing for the first 300 ms; a slower open uses non-blocking feedback, while compatibility index backfill is identified explicitly and remains cancellable.
- Album rename creates an app-local display name and does not rename the source directory.
- Keep the drawer, split it into Bookshelf and management sections, use compact consistent rows, and keep management destinations at the bottom.
- Mount Source management becomes a full-screen destination.
- Album grid sizing uses global Comfortable, Standard, and Compact adaptive presets.

## Decisions — round 2

- After 300 ms, a slow indexed open shows loading feedback on the selected Album card without blocking the rest of the library.
- Album display names allow duplicates, are limited to 100 trimmed characters, and can be cleared to restore the source name.
- Drawer rows use consistent 40 dp covers or placeholders, explicit Album counts, aligned content, and discoverable overflow menus.
- Mount Source management shows readable path, availability, scan mode, visible/hidden counts, and last successful refresh time.
- Hidden Albums are collapsed under their Mount Source by default.
- Reauthorization is prominent only when needed; refresh and unmount move to overflow menus, while refresh-all and add actions live in the page header.
- Settings initially contains grid layout, cache management, and app version. Reader controls remain in the Reader.
- Standard is the new default density, targeting about three columns on a portrait phone; Comfortable targets two and Compact targets four.
- Comfortable cards show name, page count, and path; Standard hides path; Compact keeps name and cover progress only.
- The visual direction uses a light paper-yellow background with white content surfaces.

## Decisions — round 3

- Continue following the system theme. Light mode uses low-saturation paper yellow, white surfaces, dark grey-brown text, and muted amber selection; dark mode uses a warm charcoal counterpart.
- Album lists naturally sort by display name. Search matches display name, source name, and path.
- Rename is opened from the Album overflow menu; the dialog exposes the source name and a restore action.
- Only a genuinely missing image index shows a cancellable, explicitly one-time backfill dialog. Normal indexed opens never show that dialog.
- Density presets define adaptive target widths rather than fixed column counts; landscape and tablets gain columns automatically.
- Settings shows live card previews, applies density immediately, and saves automatically.
- Last refresh time changes only after successful mount, refresh, or reauthorization. Failures keep the prior successful time and show a separate failed state.
- Existing Mount Sources migrate with unknown refresh time and do not scan automatically after upgrade.

## Decisions — round 4

- The drawer has a fixed Yomu header, a Bookshelf section with an inline add action, a scrollable Bookshelf list, and fixed Mount Source management and Settings destinations at the bottom.
- Long Mount Source paths use a tidy two-line middle-ellipsis presentation; selecting the path reveals and can copy the complete value.
- Empty states distinguish no Mount Sources, an empty current Bookshelf, and no search matches, each with a relevant recovery action.
- Settings reports current disk-cache usage against the fixed 256 MiB limit and can clear it; memory cache remains implicit.
- Room schema v3 adds optional Album display name and nullable last-successful-refresh time through a non-destructive migration. Upgrade preserves all existing library data and performs no scan.
- Regression coverage verifies no modal for indexed opens, card-local feedback only after 300 ms, and a one-time dialog only for missing-index backfill.
- Duplicate taps do not start duplicate opens. Navigating away from the library destination cancels a pending open.
- The four changes ship together as version 0.3.0.
- The first GitHub artifact is `v0.3.0-beta.1`, published as a pre-release with a directly installable debug-signed APK. Permanent release signing is deferred until the experience is stable.

## Acceptance criteria

- Reopening an indexed Album never displays the obsolete sorting dialog and never enumerates the source directory.
- A fast indexed open shows no loading UI; a slower one shows only card-local, non-modal feedback after 300 ms.
- A migrated Album with no image rows shows a cancellable one-time index-building dialog.
- Album display names survive refresh and reauthorization, can be restored to the source name, drive natural sorting, and remain searchable alongside source names and paths.
- The redesigned drawer, full-screen Mount Source manager, and Settings screen follow the agreed paper-yellow/white visual hierarchy and warm-charcoal dark counterpart.
- Standard density is the global default and targets roughly three columns on a portrait phone; all density presets remain adaptive and use proportional covers.
- The v2-to-v3 migration preserves Mount Sources, Albums, image indexes, Bookshelves, covers, Hidden Album state, and Reading Position without an automatic scan.
