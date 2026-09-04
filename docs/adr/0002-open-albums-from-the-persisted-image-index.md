---
status: accepted
---

# Open albums from the persisted image index

Yomu persists each Album's naturally sorted image list and opens the Album from that index without enumerating its directory. Mounting and explicit Mount Source refreshes rebuild the index atomically; external file changes may therefore remain invisible until refresh. A migrated Album with no stored image rows performs one compatibility scan on first use, and an image that fails to decode is checked individually so a missing file can be skipped without turning every open into a full scan.
