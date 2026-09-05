# Add persistent Album display names

Type: feature
Status: resolved

Albums have no rename action. The existing name is the source directory name and refresh overwrites it.

## Comments

- Decision: rename means an app-local display name; never rename the source directory.
- Store the optional display name separately from the source name through a non-destructive Room migration so refresh and reauthorization retain it.
- Recorded as ADR-0003.
- Decision: allow duplicate display names, trim whitespace, limit names to 100 characters, and support restoring the source name.
- Decision: natural sorting uses the display name; search covers display name, source name, and path.
- Decision: the rename dialog shows the source name and exposes an explicit restore action.
- Decision: Room v3 adds the optional display name through a non-destructive migration and performs no source rename or automatic scan.

## Answer

Added an optional Room-backed display name, rename and restore actions, display-name sorting, and search across display name, source name, and path. Refresh and reauthorization retain the custom name without modifying the source directory.
