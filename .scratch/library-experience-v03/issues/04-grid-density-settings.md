# Add configurable Album grid density

Type: feature
Status: resolved

The home grid normally produces two columns on a phone because it combines a 154 dp adaptive minimum width with fixed-height covers.

## Comments

- Decision: use Comfortable, Standard, and Compact adaptive density presets.
- Decision: the selected density is global across all Bookshelves.
- Covers must use an aspect ratio so denser layouts shrink vertically as well as horizontally.
- Decide whether the preference is global and which metadata remains visible in compact layouts.
- Decision: Standard is the new default and targets about three portrait-phone columns; Comfortable targets two and Compact targets four.
- Decision: Comfortable shows name, page count, and path; Standard shows name and page count; Compact shows name and cover progress.
- Decision: Settings initially contains layout, cache management, and app version; Reader controls remain in the Reader.
- Decision: presets use adaptive target card widths, so landscape and tablets add columns automatically.
- Decision: Settings provides a live card preview; changes apply immediately and save automatically.
- Decision: Settings reports disk-cache usage against the fixed 256 MiB limit and can clear it; memory cache is not exposed.
- Delivery: all four issues ship together as 0.3.0, first published as debug-signed `v0.3.0-beta.1` pre-release.

## Answer

Added a full-screen Settings destination with immediate, persistent Comfortable/Standard/Compact adaptive grid previews, proportional covers, density-specific metadata, disk-cache usage and clearing, and the current app version.
